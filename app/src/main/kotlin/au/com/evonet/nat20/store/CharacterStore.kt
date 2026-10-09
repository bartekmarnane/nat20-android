package au.com.evonet.nat20.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import au.com.evonet.nat20.chronicle.ChronicleService
import au.com.evonet.nat20.data.CampaignRepository
import au.com.evonet.nat20.data.CharacterRepository
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.CampaignError
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterIntentError
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.domain.JournalProseKind
import au.com.evonet.nat20.domain.LoggedEvent
import au.com.evonet.nat20.domain.PartyMember
import au.com.evonet.nat20.domain.RulesetRegistry
import au.com.evonet.nat20.domain.apply
import au.com.evonet.nat20.domain.end
import au.com.evonet.nat20.ui.slugToTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID

/**
 * The roster's single source of truth for the UI, and the orchestrator for
 * campaign lifecycle. Port of the iOS `CharacterStore` (`@Observable
 * @MainActor`). Reads project the repository Flows into Compose-collectable
 * state; writes delegate to the repositories.
 *
 * Campaign ops (A7a) coordinate *both* stores: starting/ending flips the
 * character's phase and persists the campaign; applying an intent mutates the
 * character and appends to the campaign log atomically from the UI's view.
 */
class CharacterStore(
    private val characters: CharacterRepository,
    private val campaigns: CampaignRepository,
    private val registry: RulesetRegistry,
    private val chronicleService: ChronicleService,
    private val narrationStyle: () -> au.com.evonet.nat20.chronicle.NarrationStyle = { au.com.evonet.nat20.chronicle.NarrationStyle.STORIED },
) : ViewModel() {

    val roster: StateFlow<List<Character>> = characters.characters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Active campaign name per character id — the roster cards' campaign line. */
    val activeCampaignNames: StateFlow<Map<UUID, String>> = campaigns.activeCampaignNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _generatingChronicles = MutableStateFlow<Set<UUID>>(emptySet())
    /** Campaigns whose chronicle is currently being (re)generated — drives the journal's indicator. */
    val generatingChronicles: StateFlow<Set<UUID>> = _generatingChronicles.asStateFlow()

    /** Whether on-device chronicle generation is available (single AI flag). */
    val isChronicleAvailable: Boolean get() = chronicleService.isAvailable

    /**
     * Serialises every write that reads-then-writes a character or campaign, so
     * two quick taps can't both compute from the same pre-tap state.
     */
    private val mutations = Mutex()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** One-shot user-facing failures (a rejected intent, a campaign that won't start). */
    val errors: SharedFlow<String> = _errors

    /** Look a character up by id (the sheet/journal routes carry the id). */
    fun character(id: UUID): Character? = roster.value.firstOrNull { it.id == id }

    /** Live stream of a character's campaigns (active + past), newest first. */
    fun campaignsForCharacter(characterId: UUID): Flow<List<Campaign>> =
        campaigns.campaignsForCharacter(characterId)

    /** Persist a created or edited character (A6). */
    fun save(character: Character) {
        viewModelScope.launch { characters.upsert(character) }
    }

    /** Delete a character and every campaign tied to them (as the confirm dialog promises). */
    fun delete(id: UUID) {
        viewModelScope.launch {
            mutations.withLock {
                campaigns.deleteForCharacter(id)
                characters.delete(id)
            }
        }
    }

    /**
     * Begin a campaign: snapshot the character, commit it to the new campaign's
     * phase, and persist both. The character can no longer be freely edited —
     * mutations now flow through [applyIntent] and are logged.
     */
    fun startCampaign(character: Character, name: String, party: List<PartyMember> = emptyList()) {
        viewModelScope.launch {
            mutations.withLock {
                val latest = characters.character(character.id) ?: character
                if (latest.phase is CharacterPhase.InCampaign) {
                    _errors.tryEmit("${latest.name} is already in a campaign.")
                    return@withLock
                }
                val now = Instant.now()
                val started = Campaign.start(latest, name = name, startedAt = now).copy(party = party)
                // Seed an opening journal line so the journal isn't empty on day one (A7f).
                val campaign = registry.ruleset(latest.rulesetId)?.let { ruleset ->
                    val opening = ruleset.makeProseEvent(openingLine(latest, name), JournalProseKind.CAMPAIGN_OPENING)
                    started.copy(log = started.log + LoggedEvent(timestamp = now, event = opening))
                } ?: started
                campaigns.upsert(campaign)
                characters.upsert(
                    latest.copy(phase = CharacterPhase.InCampaign(campaign.id), updatedAt = now),
                )
            }
        }
    }

    /**
     * Rename the campaign (from campaign settings, parity #37). Re-reads the
     * latest row first so a concurrent log append isn't clobbered.
     */
    fun renameCampaign(campaign: Campaign, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val latest = campaigns.campaign(campaign.id) ?: return@launch
            campaigns.upsert(latest.copy(name = trimmed))
        }
    }

    /**
     * Replace the informational party roster (from campaign settings, parity
     * #37). Re-reads the latest row first so a concurrent log append isn't
     * clobbered.
     */
    fun updateParty(campaign: Campaign, party: List<PartyMember>) {
        viewModelScope.launch {
            val latest = campaigns.campaign(campaign.id) ?: return@launch
            campaigns.upsert(latest.copy(party = party))
        }
    }

    /** A deterministic opening journal sentence (AI-authored openings are a later step). */
    private fun openingLine(character: Character, campaignName: String): String {
        val payload = character.payload as? DnD5ePayload
        val descriptor = payload?.let {
            val race = it.race.takeIf { r -> r.isNotEmpty() }?.slugToTitle()
            val cls = it.characterClass.takeIf { c -> c.isNotEmpty() }?.slugToTitle()
            listOfNotNull(race, cls).joinToString(" ")
        }.orEmpty()
        val who = if (descriptor.isNotEmpty()) "${character.name}, the $descriptor," else character.name
        return "$who sets out on a new adventure: \"$campaignName.\""
    }

    /**
     * End a campaign: capture the final snapshot, then return the character to
     * building with its in-play state shed (per ruleset) and summons dismissed,
     * so effects, concentration, conditions, temp HP and familiars don't ride
     * into the next campaign. The snapshot keeps all of it for the chronicle.
     */
    fun endCampaign(character: Character, campaign: Campaign) {
        viewModelScope.launch {
            mutations.withLock {
                val latestCharacter = characters.character(character.id) ?: character
                val latestCampaign = campaigns.campaign(campaign.id) ?: campaign
                if (!latestCampaign.isActive) return@withLock
                val now = Instant.now()
                campaigns.upsert(latestCampaign.end(now, finalSnapshot = latestCharacter))
                val ruleset = registry.ruleset(latestCharacter.rulesetId)
                val cleared = ruleset?.payloadAfterCampaignEnd(latestCharacter.payload) ?: latestCharacter.payload
                characters.upsert(
                    latestCharacter.copy(
                        payload = cleared,
                        summons = emptyList(),
                        phase = CharacterPhase.Building,
                        updatedAt = now,
                    ),
                )
            }
        }
    }

    /**
     * Apply [intent] to [character], **phase-aware**: inside an active [campaign]
     * it runs through [Campaign.apply] so the action is journaled (the A7f play
     * loop); in the building phase ([campaign] null) it's a direct, unlogged edit
     * that just persists the mutated character.
     *
     * The caller's [character] / [campaign] are the composition's snapshot; both
     * are re-read from the store under the mutation lock so two taps landing
     * before Room re-emits don't both compute from the same state (and the
     * second overwrite the first's journal entry). Invalid intents and
     * campaign-gating failures are surfaced on [errors] rather than swallowed.
     */
    fun applyIntent(intent: CharacterIntent, character: Character, campaign: Campaign?) {
        val ruleset = registry.ruleset(character.rulesetId) ?: return
        viewModelScope.launch {
            mutations.withLock {
                val latestCharacter = characters.character(character.id) ?: character
                val latestCampaign = campaign?.let { campaigns.campaign(it.id) ?: it }
                try {
                    if (latestCampaign != null) {
                        val result = latestCampaign.apply(intent, latestCharacter, ruleset, Instant.now())
                        campaigns.upsert(result.campaign)
                        characters.upsert(result.character)
                    } else {
                        val result = intent.applyTo(latestCharacter, ruleset)
                        characters.upsert(result.character.copy(updatedAt = Instant.now()))
                    }
                } catch (e: CharacterIntentError) {
                    _errors.tryEmit(e.message ?: "That action isn't possible right now.")
                } catch (e: CampaignError) {
                    _errors.tryEmit(e.message ?: "The campaign didn't accept that action.")
                }
            }
        }
    }

    /**
     * Generate any missing per-session chronicle paragraphs for [campaign] and
     * persist them. No-op when on-device AI is unavailable, the campaign is
     * already generating, or every session already has prose — so it's safe to
     * call on every journal open. Generates oldest-session-first so each
     * paragraph can see the prior ones for tone/continuity.
     */
    fun generateChronicles(campaign: Campaign) {
        if (!chronicleService.isAvailable) return
        if (campaign.id in _generatingChronicles.value) return
        val sessions = campaign.sessions.sortedBy { it.number }
        if (sessions.all { campaign.chronicle(it.id) != null }) return

        _generatingChronicles.update { it + campaign.id }
        viewModelScope.launch {
            try {
                val context = characterContext(campaign.startSnapshot)
                val produced = campaign.chronicleParagraphs.toMutableList()
                for (session in sessions) {
                    if (produced.any { it.sessionId == session.id }) continue
                    val prior = sessions
                        .filter { it.number < session.number }
                        .mapNotNull { s -> produced.firstOrNull { it.sessionId == s.id }?.paragraph }
                    val chronicle = chronicleService.chronicle(
                        session = session,
                        campaignName = campaign.name,
                        character = context,
                        priorParagraphs = prior,
                        now = Instant.now(),
                        style = narrationStyle(),
                    ) ?: continue
                    produced.add(chronicle)
                }
                // Persist against the latest campaign so we don't clobber new log entries.
                val latest = campaigns.campaign(campaign.id) ?: return@launch
                campaigns.upsert(latest.copy(chronicleParagraphs = produced))
            } finally {
                _generatingChronicles.update { it - campaign.id }
            }
        }
    }

    /** Ground-truth context for the chronicler, derived from the 5e snapshot. */
    private fun characterContext(snapshot: Character): ChronicleService.CharacterContext {
        val payload = snapshot.payload as? DnD5ePayload
        val descriptor = payload?.let {
            val race = it.race.takeIf { r -> r.isNotEmpty() }?.slugToTitle()
            val cls = it.characterClass.takeIf { c -> c.isNotEmpty() }?.slugToTitle()
            listOfNotNull("Level ${it.level}", race, cls).joinToString(" ")
        }.orEmpty()
        return ChronicleService.CharacterContext(name = snapshot.name, descriptor = descriptor)
    }

    companion object {
        /** Factory that injects the repositories + registry + chronicler from the container. */
        fun factory(
            characters: CharacterRepository,
            campaigns: CampaignRepository,
            registry: RulesetRegistry,
            chronicleService: ChronicleService,
            narrationStyle: () -> au.com.evonet.nat20.chronicle.NarrationStyle = { au.com.evonet.nat20.chronicle.NarrationStyle.STORIED },
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { CharacterStore(characters, campaigns, registry, chronicleService, narrationStyle) }
        }
    }
}

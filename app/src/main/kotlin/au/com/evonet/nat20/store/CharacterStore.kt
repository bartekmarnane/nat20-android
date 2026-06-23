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
import au.com.evonet.nat20.domain.RulesetRegistry
import au.com.evonet.nat20.domain.apply
import au.com.evonet.nat20.domain.end
import au.com.evonet.nat20.ui.slugToTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val narrationStyle: () -> au.com.evonet.nat20.chronicle.NarrationStyle = { au.com.evonet.nat20.chronicle.NarrationStyle.CHRONICLER },
) : ViewModel() {

    val roster: StateFlow<List<Character>> = characters.characters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _generatingChronicles = MutableStateFlow<Set<UUID>>(emptySet())
    /** Campaigns whose chronicle is currently being (re)generated — drives the journal's indicator. */
    val generatingChronicles: StateFlow<Set<UUID>> = _generatingChronicles.asStateFlow()

    /** Whether on-device chronicle generation is available (single AI flag). */
    val isChronicleAvailable: Boolean get() = chronicleService.isAvailable

    /** Look a character up by id (the sheet/journal routes carry the id). */
    fun character(id: UUID): Character? = roster.value.firstOrNull { it.id == id }

    /** Live stream of a character's campaigns (active + past), newest first. */
    fun campaignsForCharacter(characterId: UUID): Flow<List<Campaign>> =
        campaigns.campaignsForCharacter(characterId)

    /** Persist a created or edited character (A6). */
    fun save(character: Character) {
        viewModelScope.launch { characters.upsert(character) }
    }

    /** Delete a character by id (swipe-to-delete on the roster). */
    fun delete(id: UUID) {
        viewModelScope.launch { characters.delete(id) }
    }

    /**
     * Begin a campaign: snapshot the character, commit it to the new campaign's
     * phase, and persist both. The character can no longer be freely edited —
     * mutations now flow through [applyIntent] and are logged.
     */
    fun startCampaign(character: Character, name: String) {
        viewModelScope.launch {
            val now = Instant.now()
            val started = Campaign.start(character, name = name, startedAt = now)
            // Seed an opening journal line so the journal isn't empty on day one (A7f).
            val campaign = registry.ruleset(character.rulesetId)?.let { ruleset ->
                val opening = ruleset.makeProseEvent(openingLine(character, name), JournalProseKind.CAMPAIGN_OPENING)
                started.copy(log = started.log + LoggedEvent(timestamp = now, event = opening))
            } ?: started
            campaigns.upsert(campaign)
            characters.upsert(
                character.copy(phase = CharacterPhase.InCampaign(campaign.id), updatedAt = now),
            )
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

    /** End a campaign: capture the final snapshot and return the character to building. */
    fun endCampaign(character: Character, campaign: Campaign) {
        viewModelScope.launch {
            val now = Instant.now()
            campaigns.upsert(campaign.end(now, finalSnapshot = character))
            characters.upsert(character.copy(phase = CharacterPhase.Building, updatedAt = now))
        }
    }

    /**
     * Apply [intent] to [character], **phase-aware**: inside an active [campaign]
     * it runs through [Campaign.apply] so the action is journaled (the A7f play
     * loop); in the building phase ([campaign] null) it's a direct, unlogged edit
     * that just persists the mutated character. Invalid intents (e.g. casting
     * with no slots left, overspending a pool) and campaign-gating failures are
     * swallowed — the UI gates most of these, this is the backstop.
     */
    fun applyIntent(intent: CharacterIntent, character: Character, campaign: Campaign?) {
        val ruleset = registry.ruleset(character.rulesetId) ?: return
        viewModelScope.launch {
            try {
                if (campaign != null) {
                    val result = campaign.apply(intent, character, ruleset, Instant.now())
                    campaigns.upsert(result.campaign)
                    characters.upsert(result.character)
                } else {
                    val result = intent.applyTo(character, ruleset)
                    characters.upsert(result.character.copy(updatedAt = Instant.now()))
                }
            } catch (_: CharacterIntentError) {
                // Invalid edit — ignore (buttons are gated, this is a backstop).
            } catch (_: CampaignError) {
                // Campaign gating failed (ended / phase mismatch) — ignore.
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
            narrationStyle: () -> au.com.evonet.nat20.chronicle.NarrationStyle = { au.com.evonet.nat20.chronicle.NarrationStyle.CHRONICLER },
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { CharacterStore(characters, campaigns, registry, chronicleService, narrationStyle) }
        }
    }
}

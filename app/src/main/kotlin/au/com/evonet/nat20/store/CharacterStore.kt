package au.com.evonet.nat20.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import au.com.evonet.nat20.data.CampaignRepository
import au.com.evonet.nat20.data.CharacterRepository
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.domain.RulesetRegistry
import au.com.evonet.nat20.domain.apply
import au.com.evonet.nat20.domain.end
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
) : ViewModel() {

    val roster: StateFlow<List<Character>> = characters.characters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            val campaign = Campaign.start(character, name = name, startedAt = now)
            campaigns.upsert(campaign)
            characters.upsert(
                character.copy(phase = CharacterPhase.InCampaign(campaign.id), updatedAt = now),
            )
        }
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
     * Apply [intent] to [character] within [campaign]: mutates the character and
     * appends the journal entry, persisting both. Silently no-ops if the
     * ruleset can't be resolved (shouldn't happen for a live character).
     */
    fun applyIntent(intent: CharacterIntent, character: Character, campaign: Campaign) {
        val ruleset = registry.ruleset(character.rulesetId) ?: return
        viewModelScope.launch {
            val result = campaign.apply(intent, character, ruleset, Instant.now())
            campaigns.upsert(result.campaign)
            characters.upsert(result.character)
        }
    }

    companion object {
        /** Factory that injects the repositories + registry from the app's container. */
        fun factory(
            characters: CharacterRepository,
            campaigns: CampaignRepository,
            registry: RulesetRegistry,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { CharacterStore(characters, campaigns, registry) }
        }
    }
}

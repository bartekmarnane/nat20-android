package au.com.evonet.nat20.domain

import java.time.Instant
import java.util.UUID

/**
 * A single playthrough of a character. Its [log] records every in-campaign
 * intent applied via [apply]; [startSnapshot]/[endSnapshot] capture the
 * character at start and end so you can see how they changed without replaying
 * events. Port of the iOS `Campaign`.
 *
 * Logs live here, **not** on the character — a character can have many past
 * campaigns, and the live sheet stays a clean snapshot of current state.
 *
 * A7a scope: the lifecycle + journal log. Chronicle prose (A7d), derived
 * sessions, and party membership (A23) are added by their own steps.
 */
data class Campaign(
    val id: UUID,
    val characterId: UUID,
    val name: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val startSnapshot: Character,
    val endSnapshot: Character?,
    val log: List<LoggedEvent>,
    /**
     * Generated per-session narrative prose (A7d), keyed by session id. Empty
     * until the on-device model produces them; best-effort, never load-bearing
     * for play.
     */
    val chronicleParagraphs: List<SessionChronicle> = emptyList(),
) {
    /** Active until it's ended. */
    val isActive: Boolean get() = endedAt == null

    /** Derived play sessions (newest first), the unit a chronicle paragraph covers. */
    val sessions: List<CampaignSession> get() = CampaignSession.derive(log)

    /** The stored chronicle for [sessionId], if one has been generated. */
    fun chronicle(sessionId: Instant): SessionChronicle? =
        chronicleParagraphs.firstOrNull { it.sessionId == sessionId }

    companion object {
        /** A fresh, active campaign for [character], capturing it as the start snapshot. */
        fun start(
            character: Character,
            name: String,
            startedAt: Instant,
            id: UUID = UUID.randomUUID(),
        ): Campaign = Campaign(
            id = id,
            characterId = character.id,
            name = name,
            startedAt = startedAt,
            endedAt = null,
            startSnapshot = character,
            endSnapshot = null,
            log = emptyList(),
        )
    }
}

/** The outcome of applying an intent within a campaign. */
data class CampaignApplyResult(
    /** The campaign with the new entry appended to its log. */
    val campaign: Campaign,
    /** The mutated character (HP, slots, …), `updatedAt` bumped. */
    val character: Character,
    /** The journal entry produced. */
    val logged: LoggedEvent,
)

/**
 * Runs [intent] against [character], appends the resulting event to the log,
 * and bumps the character's `updatedAt`. Validates that the campaign is active,
 * the character is committed to *this* campaign, and the ruleset matches.
 * Returns new value types — nothing is mutated in place.
 */
fun Campaign.apply(
    intent: CharacterIntent,
    character: Character,
    ruleset: Ruleset,
    timestamp: Instant,
): CampaignApplyResult {
    if (!isActive) throw CampaignError.AlreadyEnded
    val phase = character.phase
    if (phase !is CharacterPhase.InCampaign || phase.campaignId != id) {
        throw CampaignError.CharacterNotInCampaign
    }
    if (ruleset.id != character.rulesetId) {
        throw CampaignError.RulesetMismatch(character.rulesetId, ruleset.id)
    }
    val result = intent.applyTo(character, ruleset)
    val logged = LoggedEvent(timestamp = timestamp, event = result.event)
    return CampaignApplyResult(
        campaign = copy(log = log + logged),
        character = result.character.copy(updatedAt = timestamp),
        logged = logged,
    )
}

/** Closes the campaign and captures the final character state. No-op if already ended. */
fun Campaign.end(timestamp: Instant, finalSnapshot: Character): Campaign =
    if (!isActive) this
    else copy(endedAt = timestamp, endSnapshot = finalSnapshot)

/** Failures raised while applying a campaign operation. */
sealed class CampaignError(message: String) : Exception(message) {
    data object AlreadyEnded : CampaignError("Campaign has already ended") {
        private fun readResolve(): Any = AlreadyEnded
    }

    data object CharacterNotInCampaign : CampaignError("Character is not in this campaign") {
        private fun readResolve(): Any = CharacterNotInCampaign
    }

    data class RulesetMismatch(val campaignCharacter: RulesetId, val supplied: RulesetId) :
        CampaignError("Ruleset mismatch: character is $campaignCharacter, supplied $supplied")
}

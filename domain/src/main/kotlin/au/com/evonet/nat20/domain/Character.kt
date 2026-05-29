package au.com.evonet.nat20.domain

import java.time.Instant
import java.util.UUID

/**
 * A character in any ruleset. The ruleset-specific data lives in [payload];
 * cross-ruleset concepts (name, portrait, summoned creatures, lifecycle
 * phase) live here. Port of the iOS `Character` struct — value semantics are
 * preserved via an immutable data class plus `copy()`.
 *
 * Mutations during a campaign go through [CharacterIntent]; build-phase edits
 * use the (later) editor path. Use the factory functions in the companion to
 * mint fresh characters and the constructor directly to rehydrate persisted ones.
 */
data class Character(
    val id: UUID,
    val name: String,
    val rulesetId: RulesetId,
    val payload: CharacterPayload,
    val phase: CharacterPhase,
    val createdAt: Instant,
    val updatedAt: Instant,
    /**
     * Optional user-supplied portrait, JPEG-encoded and downscaled at import.
     * Cross-ruleset, so it lives here rather than in a payload. Behaviour
     * (import/downscale) lands with the portrait feature; the field is stubbed
     * to null for now.
     *
     * Note: [ByteArray] gives this data class reference-based equality. That's
     * acceptable while portraits are unused; revisit (custom equals or a value
     * wrapper) when the feature lands.
     */
    val portraitData: ByteArray? = null,
    /**
     * Familiars, beast companions, summoned creatures, animated dead — the
     * cross-ruleset "secondary entity attached to a PC" concept. Populated at
     * A15; defaults empty.
     */
    val summons: List<Summon> = emptyList(),
) {
    companion object {
        /**
         * A brand-new character in the [CharacterPhase.Building] phase. Fields
         * can be freely edited without producing journal entries until the
         * character enters a campaign.
         */
        fun new(
            name: String,
            ruleset: Ruleset,
            timestamp: Instant,
        ): Character = Character(
            id = UUID.randomUUID(),
            name = name,
            rulesetId = ruleset.id,
            payload = ruleset.makeInitialPayload(name),
            phase = CharacterPhase.Building,
            createdAt = timestamp,
            updatedAt = timestamp,
        )

        /** A brand-new character with an explicit payload (used by the build form). */
        fun new(
            name: String,
            ruleset: Ruleset,
            payload: CharacterPayload,
            timestamp: Instant,
            portraitData: ByteArray? = null,
        ): Character = Character(
            id = UUID.randomUUID(),
            name = name,
            rulesetId = ruleset.id,
            payload = payload,
            phase = CharacterPhase.Building,
            createdAt = timestamp,
            updatedAt = timestamp,
            portraitData = portraitData,
        )
    }
}

/** Whether a character is being built or is committed to a campaign. */
sealed class CharacterPhase {
    /** Freely editable; intents are not logged. */
    data object Building : CharacterPhase()

    /** Committed to the campaign with the given id; mutations are logged. */
    data class InCampaign(val campaignId: UUID) : CharacterPhase()
}

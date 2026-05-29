package au.com.evonet.nat20.domain

/** Stable string identifier for a ruleset (e.g. `"dnd5e-2014"`). */
typealias RulesetId = String

/**
 * A game system Nat20 can run. A ruleset owns its concrete payload/event
 * types and knows how to (de)serialise them, so the rest of the app can deal
 * in the [CharacterPayload] / [CharacterEvent] abstractions without knowing
 * what system a character belongs to.
 *
 * Port of the iOS `Ruleset` protocol. The Swift codec surface used `Data`
 * (JSON bytes); on the JVM we use JSON `String`, which is what
 * `kotlinx.serialization` produces natively and is value-comparable.
 */
interface Ruleset {
    val id: RulesetId
    val displayName: String

    /** A fresh, valid payload for a brand-new character with the given name. */
    fun makeInitialPayload(name: String): CharacterPayload

    /** Encodes a payload to JSON. The ruleset knows its concrete type. */
    fun encodePayload(payload: CharacterPayload): String

    /** Decodes a payload from JSON previously produced by [encodePayload]. */
    fun decodePayload(json: String): CharacterPayload

    /**
     * String discriminator for an event type, used when persisting campaign
     * journal entries so the right concrete type can be decoded back later.
     */
    fun eventTypeId(event: CharacterEvent): String

    /** Encodes a journal event to JSON. */
    fun encodeEvent(event: CharacterEvent): String

    /** Decodes a journal event from JSON + its type id (from [eventTypeId]). */
    fun decodeEvent(json: String, typeId: String): CharacterEvent

    /**
     * Wraps a generated prose sentence in whatever event type this ruleset
     * uses for free-form journal entries. [kind] lets the ruleset pick a
     * fitting icon/category per purpose without leaking ruleset-specific
     * categories into the domain.
     */
    fun makeProseEvent(text: String, kind: JournalProseKind): CharacterEvent
}

/** Reasons the persistence layer asks a ruleset to mint a prose journal entry. */
enum class JournalProseKind {
    CAMPAIGN_OPENING,
    PARTY_JOINED,
    PARTY_LEFT,
}

/** Marker for a ruleset's character data. Concrete payloads are `@Serializable`. */
interface CharacterPayload

/** Failures when (de)serialising characters/events through a ruleset codec. */
sealed class CharacterCodecError(message: String) : Exception(message) {
    data class UnknownRuleset(val rulesetId: RulesetId) :
        CharacterCodecError("Unknown ruleset: $rulesetId")

    data class UnknownEventType(val typeId: String) :
        CharacterCodecError("Unknown event type: $typeId")
}

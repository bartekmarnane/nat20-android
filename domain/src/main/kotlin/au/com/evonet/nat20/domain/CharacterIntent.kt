package au.com.evonet.nat20.domain

/**
 * Intents are the only way to mutate a [Character]. Port of the iOS
 * `CharacterIntent` protocol; because Kotlin data classes are immutable, an
 * intent returns the updated character plus the event it produced (the Swift
 * version mutated `inout Character`) rather than mutating in place.
 *
 * Implementations must validate before constructing the result — there is no
 * partial-mutation state to roll back, but throwing leaves the caller's
 * character untouched.
 */
interface CharacterIntent {
    /**
     * Validates against [character] under [ruleset] and returns the resulting
     * state. Throws [CharacterIntentError] on invalid input.
     */
    fun applyTo(character: Character, ruleset: Ruleset): IntentResult
}

/** The outcome of applying a [CharacterIntent]: the new character + journal event. */
data class IntentResult(
    val character: Character,
    val event: CharacterEvent,
)

/** Failures raised while applying an intent. */
sealed class CharacterIntentError(message: String) : Exception(message) {
    data class RulesetMismatch(val expected: RulesetId, val got: RulesetId) :
        CharacterIntentError("Ruleset mismatch: expected $expected, got $got")

    data class Invalid(val reason: String) :
        CharacterIntentError("Invalid intent: $reason")
}

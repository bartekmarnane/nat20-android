package au.com.evonet.nat20.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Minimal in-test ruleset/payload/event/intent implementations, enough to
 * exercise the domain machinery without depending on the 5e engine (A3).
 */

@Serializable
data class FakePayload(val name: String, val notes: String = "") : CharacterPayload

@Serializable
data class FakeEvent(override val summary: String) : CharacterEvent

class FakeRuleset(
    override val id: RulesetId = "fake",
    override val displayName: String = "Fake Ruleset",
) : Ruleset {
    private val json = Json

    override fun makeInitialPayload(name: String): CharacterPayload = FakePayload(name = name)

    override fun encodePayload(payload: CharacterPayload): String =
        json.encodeToString(FakePayload.serializer(), payload as FakePayload)

    override fun decodePayload(json: String): CharacterPayload =
        this.json.decodeFromString(FakePayload.serializer(), json)

    override fun eventTypeId(event: CharacterEvent): String = "fake"

    override fun encodeEvent(event: CharacterEvent): String =
        json.encodeToString(FakeEvent.serializer(), event as FakeEvent)

    override fun decodeEvent(json: String, typeId: String): CharacterEvent =
        this.json.decodeFromString(FakeEvent.serializer(), json)

    override fun makeProseEvent(text: String, kind: JournalProseKind): CharacterEvent =
        FakeEvent(summary = text)
}

/** Renames a character, rejecting blank names — exercises validate-then-apply. */
data class RenameIntent(val newName: String) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (character.rulesetId != ruleset.id) {
            throw CharacterIntentError.RulesetMismatch(character.rulesetId, ruleset.id)
        }
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            throw CharacterIntentError.Invalid("name must not be blank")
        }
        val updated = character.copy(name = trimmed)
        return IntentResult(updated, FakeEvent(summary = "Renamed to $trimmed"))
    }
}

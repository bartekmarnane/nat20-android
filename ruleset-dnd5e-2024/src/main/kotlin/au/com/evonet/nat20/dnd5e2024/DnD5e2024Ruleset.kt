package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.domain.CharacterCodecError
import au.com.evonet.nat20.domain.CharacterEvent
import au.com.evonet.nat20.domain.CharacterPayload
import au.com.evonet.nat20.domain.JournalProseKind
import au.com.evonet.nat20.domain.NoteKind
import au.com.evonet.nat20.domain.Ruleset
import au.com.evonet.nat20.domain.RulesetId
import kotlinx.serialization.json.Json

/**
 * The D&D 5e (2024) ruleset — a sibling of the 2014 edition (`rulesetID
 * "dnd-5e-2024"`). Owns its [DnD5e2024Payload] + 2024 event types and their JSON
 * codec; the engine math is reused from `:ruleset-dnd5e-core`. Port of the iOS
 * `DnD5e2024Ruleset`.
 */
class DnD5e2024Ruleset : Ruleset {
    override val id: RulesetId = RULESET_ID
    override val displayName: String = "D&D 5e (2024)"

    private val json = Json { ignoreUnknownKeys = true }

    override fun makeInitialPayload(name: String): CharacterPayload = DnD5e2024Payload()

    override fun encodePayload(payload: CharacterPayload): String =
        json.encodeToString(DnD5e2024Payload.serializer(), payload as DnD5e2024Payload)

    override fun decodePayload(json: String): CharacterPayload =
        this.json.decodeFromString(DnD5e2024Payload.serializer(), json)

    override fun eventTypeId(event: CharacterEvent): String = when (event) {
        is Note2024Event -> "dnd5e2024.note"
        is DamageTaken2024Event -> "dnd5e2024.damage"
        is Healed2024Event -> "dnd5e2024.heal"
        is TempHpGained2024Event -> "dnd5e2024.tempHP"
        is ExhaustionChanged2024Event -> "dnd5e2024.exhaustion"
        is InspirationChanged2024Event -> "dnd5e2024.inspiration"
        is ConditionChanged2024Event -> "dnd5e2024.condition"
        is LeveledUp2024Event -> "dnd5e2024.level"
        else -> "dnd5e2024.unknown"
    }

    override fun encodeEvent(event: CharacterEvent): String = when (event) {
        is Note2024Event -> json.encodeToString(Note2024Event.serializer(), event)
        is DamageTaken2024Event -> json.encodeToString(DamageTaken2024Event.serializer(), event)
        is Healed2024Event -> json.encodeToString(Healed2024Event.serializer(), event)
        is TempHpGained2024Event -> json.encodeToString(TempHpGained2024Event.serializer(), event)
        is ExhaustionChanged2024Event -> json.encodeToString(ExhaustionChanged2024Event.serializer(), event)
        is InspirationChanged2024Event -> json.encodeToString(InspirationChanged2024Event.serializer(), event)
        is ConditionChanged2024Event -> json.encodeToString(ConditionChanged2024Event.serializer(), event)
        is LeveledUp2024Event -> json.encodeToString(LeveledUp2024Event.serializer(), event)
        else -> throw CharacterCodecError.UnknownEventType(event::class.simpleName ?: "unknown")
    }

    override fun decodeEvent(json: String, typeId: String): CharacterEvent = when (typeId) {
        "dnd5e2024.note" -> this.json.decodeFromString(Note2024Event.serializer(), json)
        "dnd5e2024.damage" -> this.json.decodeFromString(DamageTaken2024Event.serializer(), json)
        "dnd5e2024.heal" -> this.json.decodeFromString(Healed2024Event.serializer(), json)
        "dnd5e2024.tempHP" -> this.json.decodeFromString(TempHpGained2024Event.serializer(), json)
        "dnd5e2024.exhaustion" -> this.json.decodeFromString(ExhaustionChanged2024Event.serializer(), json)
        "dnd5e2024.inspiration" -> this.json.decodeFromString(InspirationChanged2024Event.serializer(), json)
        "dnd5e2024.condition" -> this.json.decodeFromString(ConditionChanged2024Event.serializer(), json)
        "dnd5e2024.level" -> this.json.decodeFromString(LeveledUp2024Event.serializer(), json)
        else -> throw CharacterCodecError.UnknownEventType(typeId)
    }

    override fun makeProseEvent(text: String, kind: JournalProseKind): CharacterEvent {
        val noteKind = when (kind) {
            JournalProseKind.CAMPAIGN_OPENING -> NoteKind.QUEST
            JournalProseKind.PARTY_JOINED, JournalProseKind.PARTY_LEFT -> NoteKind.NPC
        }
        return Note2024Event(text, noteKind)
    }

    companion object {
        const val RULESET_ID: RulesetId = "dnd-5e-2024"
    }
}

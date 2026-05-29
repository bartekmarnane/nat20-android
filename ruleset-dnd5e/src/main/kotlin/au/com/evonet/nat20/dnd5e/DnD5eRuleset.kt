package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.domain.CharacterCodecError
import au.com.evonet.nat20.domain.CharacterEvent
import au.com.evonet.nat20.domain.CharacterPayload
import au.com.evonet.nat20.domain.JournalProseKind
import au.com.evonet.nat20.domain.NoteKind
import au.com.evonet.nat20.domain.Ruleset
import au.com.evonet.nat20.domain.RulesetId
import kotlinx.serialization.json.Json

/**
 * The D&D 5e (2014) ruleset. Owns its payload/event types and their JSON
 * codec. Port of the iOS `DnD5eRuleset`; the codec covers the A3 event slice
 * and grows as more intents land.
 */
class DnD5eRuleset : Ruleset {
    override val id: RulesetId = RULESET_ID
    override val displayName: String = "D&D 5e (2014)"

    private val json = Json { ignoreUnknownKeys = true }

    override fun makeInitialPayload(name: String): CharacterPayload = DnD5ePayload()

    override fun encodePayload(payload: CharacterPayload): String =
        json.encodeToString(DnD5ePayload.serializer(), payload as DnD5ePayload)

    override fun decodePayload(json: String): CharacterPayload =
        this.json.decodeFromString(DnD5ePayload.serializer(), json)

    override fun eventTypeId(event: CharacterEvent): String = when (event) {
        is DamageTakenEvent -> "dnd5e.damage"
        is HealedEvent -> "dnd5e.heal"
        is TempHpGainedEvent -> "dnd5e.tempHP"
        is LeveledUpEvent -> "dnd5e.level"
        is NoteEvent -> "dnd5e.note"
        else -> "dnd5e.unknown"
    }

    override fun encodeEvent(event: CharacterEvent): String = when (event) {
        is DamageTakenEvent -> json.encodeToString(DamageTakenEvent.serializer(), event)
        is HealedEvent -> json.encodeToString(HealedEvent.serializer(), event)
        is TempHpGainedEvent -> json.encodeToString(TempHpGainedEvent.serializer(), event)
        is LeveledUpEvent -> json.encodeToString(LeveledUpEvent.serializer(), event)
        is NoteEvent -> json.encodeToString(NoteEvent.serializer(), event)
        else -> throw CharacterCodecError.UnknownEventType(event::class.simpleName ?: "unknown")
    }

    override fun decodeEvent(json: String, typeId: String): CharacterEvent = when (typeId) {
        "dnd5e.damage" -> this.json.decodeFromString(DamageTakenEvent.serializer(), json)
        "dnd5e.heal" -> this.json.decodeFromString(HealedEvent.serializer(), json)
        "dnd5e.tempHP" -> this.json.decodeFromString(TempHpGainedEvent.serializer(), json)
        "dnd5e.level" -> this.json.decodeFromString(LeveledUpEvent.serializer(), json)
        "dnd5e.note" -> this.json.decodeFromString(NoteEvent.serializer(), json)
        else -> throw CharacterCodecError.UnknownEventType(typeId)
    }

    override fun makeProseEvent(text: String, kind: JournalProseKind): CharacterEvent {
        val noteKind = when (kind) {
            JournalProseKind.CAMPAIGN_OPENING -> NoteKind.QUEST
            JournalProseKind.PARTY_JOINED, JournalProseKind.PARTY_LEFT -> NoteKind.NPC
        }
        return NoteEvent(text = text, kind = noteKind)
    }

    companion object {
        const val RULESET_ID: RulesetId = "dnd-5e-2014"

        /**
         * 5e proficiency bonus: +2 at L1–4, +3 at L5–8, +4 at L9–12,
         * +5 at L13–16, +6 at L17–20.
         */
        fun proficiencyBonus(level: Int): Int = 2 + (maxOf(level, 1) - 1) / 4
    }
}

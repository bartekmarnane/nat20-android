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
        is ItemAcquiredEvent -> "dnd5e.itemAcquired"
        is ItemDroppedEvent -> "dnd5e.itemDropped"
        is ItemUsedEvent -> "dnd5e.itemUsed"
        is CoinAdjustedEvent -> "dnd5e.coin"
        is CastSpellEvent -> "dnd5e.castSpell"
        is SpellPreparedEvent -> "dnd5e.prepareSpell"
        is SpellUnpreparedEvent -> "dnd5e.unprepareSpell"
        is SpellSlotExpendedEvent -> "dnd5e.expendSlot"
        is ShortRestEvent -> "dnd5e.shortRest"
        is LongRestEvent -> "dnd5e.longRest"
        is HitDieSpentEvent -> "dnd5e.hitDie"
        is ResourceSpentEvent -> "dnd5e.resource"
        is ClassFeatureUsedEvent -> "dnd5e.feature"
        is InspirationChangedEvent -> "dnd5e.inspiration"
        is DeathSaveMarkedEvent -> "dnd5e.deathSave"
        is ConditionAppliedEvent -> "dnd5e.conditionApplied"
        is ConditionClearedEvent -> "dnd5e.conditionCleared"
        is ExhaustionChangedEvent -> "dnd5e.exhaustion"
        else -> "dnd5e.unknown"
    }

    override fun encodeEvent(event: CharacterEvent): String = when (event) {
        is DamageTakenEvent -> json.encodeToString(DamageTakenEvent.serializer(), event)
        is HealedEvent -> json.encodeToString(HealedEvent.serializer(), event)
        is TempHpGainedEvent -> json.encodeToString(TempHpGainedEvent.serializer(), event)
        is LeveledUpEvent -> json.encodeToString(LeveledUpEvent.serializer(), event)
        is NoteEvent -> json.encodeToString(NoteEvent.serializer(), event)
        is ItemAcquiredEvent -> json.encodeToString(ItemAcquiredEvent.serializer(), event)
        is ItemDroppedEvent -> json.encodeToString(ItemDroppedEvent.serializer(), event)
        is ItemUsedEvent -> json.encodeToString(ItemUsedEvent.serializer(), event)
        is CoinAdjustedEvent -> json.encodeToString(CoinAdjustedEvent.serializer(), event)
        is CastSpellEvent -> json.encodeToString(CastSpellEvent.serializer(), event)
        is SpellPreparedEvent -> json.encodeToString(SpellPreparedEvent.serializer(), event)
        is SpellUnpreparedEvent -> json.encodeToString(SpellUnpreparedEvent.serializer(), event)
        is SpellSlotExpendedEvent -> json.encodeToString(SpellSlotExpendedEvent.serializer(), event)
        is ShortRestEvent -> json.encodeToString(ShortRestEvent.serializer(), event)
        is LongRestEvent -> json.encodeToString(LongRestEvent.serializer(), event)
        is HitDieSpentEvent -> json.encodeToString(HitDieSpentEvent.serializer(), event)
        is ResourceSpentEvent -> json.encodeToString(ResourceSpentEvent.serializer(), event)
        is ClassFeatureUsedEvent -> json.encodeToString(ClassFeatureUsedEvent.serializer(), event)
        is InspirationChangedEvent -> json.encodeToString(InspirationChangedEvent.serializer(), event)
        is DeathSaveMarkedEvent -> json.encodeToString(DeathSaveMarkedEvent.serializer(), event)
        is ConditionAppliedEvent -> json.encodeToString(ConditionAppliedEvent.serializer(), event)
        is ConditionClearedEvent -> json.encodeToString(ConditionClearedEvent.serializer(), event)
        is ExhaustionChangedEvent -> json.encodeToString(ExhaustionChangedEvent.serializer(), event)
        else -> throw CharacterCodecError.UnknownEventType(event::class.simpleName ?: "unknown")
    }

    override fun decodeEvent(json: String, typeId: String): CharacterEvent = when (typeId) {
        "dnd5e.damage" -> this.json.decodeFromString(DamageTakenEvent.serializer(), json)
        "dnd5e.heal" -> this.json.decodeFromString(HealedEvent.serializer(), json)
        "dnd5e.tempHP" -> this.json.decodeFromString(TempHpGainedEvent.serializer(), json)
        "dnd5e.level" -> this.json.decodeFromString(LeveledUpEvent.serializer(), json)
        "dnd5e.note" -> this.json.decodeFromString(NoteEvent.serializer(), json)
        "dnd5e.itemAcquired" -> this.json.decodeFromString(ItemAcquiredEvent.serializer(), json)
        "dnd5e.itemDropped" -> this.json.decodeFromString(ItemDroppedEvent.serializer(), json)
        "dnd5e.itemUsed" -> this.json.decodeFromString(ItemUsedEvent.serializer(), json)
        "dnd5e.coin" -> this.json.decodeFromString(CoinAdjustedEvent.serializer(), json)
        "dnd5e.castSpell" -> this.json.decodeFromString(CastSpellEvent.serializer(), json)
        "dnd5e.prepareSpell" -> this.json.decodeFromString(SpellPreparedEvent.serializer(), json)
        "dnd5e.unprepareSpell" -> this.json.decodeFromString(SpellUnpreparedEvent.serializer(), json)
        "dnd5e.expendSlot" -> this.json.decodeFromString(SpellSlotExpendedEvent.serializer(), json)
        "dnd5e.shortRest" -> this.json.decodeFromString(ShortRestEvent.serializer(), json)
        "dnd5e.longRest" -> this.json.decodeFromString(LongRestEvent.serializer(), json)
        "dnd5e.hitDie" -> this.json.decodeFromString(HitDieSpentEvent.serializer(), json)
        "dnd5e.resource" -> this.json.decodeFromString(ResourceSpentEvent.serializer(), json)
        "dnd5e.feature" -> this.json.decodeFromString(ClassFeatureUsedEvent.serializer(), json)
        "dnd5e.inspiration" -> this.json.decodeFromString(InspirationChangedEvent.serializer(), json)
        "dnd5e.deathSave" -> this.json.decodeFromString(DeathSaveMarkedEvent.serializer(), json)
        "dnd5e.conditionApplied" -> this.json.decodeFromString(ConditionAppliedEvent.serializer(), json)
        "dnd5e.conditionCleared" -> this.json.decodeFromString(ConditionClearedEvent.serializer(), json)
        "dnd5e.exhaustion" -> this.json.decodeFromString(ExhaustionChangedEvent.serializer(), json)
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
        // Proficiency-by-level moved to `:ruleset-dnd5e-core` (`Proficiency.bonus`)
        // at the multi-ruleset split — it's edition-agnostic 5e machinery.
    }
}

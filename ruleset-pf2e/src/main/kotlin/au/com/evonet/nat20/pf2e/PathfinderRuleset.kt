package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.domain.CharacterCodecError
import au.com.evonet.nat20.domain.CharacterEvent
import au.com.evonet.nat20.domain.CharacterPayload
import au.com.evonet.nat20.domain.JournalProseKind
import au.com.evonet.nat20.domain.NoteKind
import au.com.evonet.nat20.domain.Ruleset
import au.com.evonet.nat20.domain.RulesetId
import kotlinx.serialization.json.Json

/**
 * The Pathfinder 2e (Remaster) ruleset — the first **non-D&D** system
 * (`rulesetID "pf2e-remaster"`). Owns its [PathfinderPayload] + PF2e event types
 * and their JSON codec; the maths come from `:ruleset-pf2e-core`. The `:domain`
 * abstraction holds unchanged — registering this touches only the registry.
 * Port of the iOS `PathfinderRuleset`.
 */
class PathfinderRuleset : Ruleset {
    override val id: RulesetId = RULESET_ID
    override val displayName: String = "Pathfinder 2e (Remaster)"

    private val json = Json { ignoreUnknownKeys = true }

    override fun makeInitialPayload(name: String): CharacterPayload = PathfinderPayload()

    override fun encodePayload(payload: CharacterPayload): String =
        json.encodeToString(PathfinderPayload.serializer(), payload as PathfinderPayload)

    override fun decodePayload(json: String): CharacterPayload =
        this.json.decodeFromString(PathfinderPayload.serializer(), json)

    override fun eventTypeId(event: CharacterEvent): String = when (event) {
        is PfNoteEvent -> "pf2e.note"
        is PfDamageTakenEvent -> "pf2e.damage"
        is PfHealedEvent -> "pf2e.heal"
        is PfTempHpGainedEvent -> "pf2e.tempHP"
        is PfDyingChangedEvent -> "pf2e.dying"
        is PfWoundedChangedEvent -> "pf2e.wounded"
        is PfHeroPointsChangedEvent -> "pf2e.heroPoints"
        is PfConditionChangedEvent -> "pf2e.condition"
        is PfStrikeEvent -> "pf2e.strike"
        is PfSpellCastEvent -> "pf2e.castSpell"
        is PfDailyPrepEvent -> "pf2e.dailyPrep"
        else -> "pf2e.unknown"
    }

    override fun encodeEvent(event: CharacterEvent): String = when (event) {
        is PfNoteEvent -> json.encodeToString(PfNoteEvent.serializer(), event)
        is PfDamageTakenEvent -> json.encodeToString(PfDamageTakenEvent.serializer(), event)
        is PfHealedEvent -> json.encodeToString(PfHealedEvent.serializer(), event)
        is PfTempHpGainedEvent -> json.encodeToString(PfTempHpGainedEvent.serializer(), event)
        is PfDyingChangedEvent -> json.encodeToString(PfDyingChangedEvent.serializer(), event)
        is PfWoundedChangedEvent -> json.encodeToString(PfWoundedChangedEvent.serializer(), event)
        is PfHeroPointsChangedEvent -> json.encodeToString(PfHeroPointsChangedEvent.serializer(), event)
        is PfConditionChangedEvent -> json.encodeToString(PfConditionChangedEvent.serializer(), event)
        is PfStrikeEvent -> json.encodeToString(PfStrikeEvent.serializer(), event)
        is PfSpellCastEvent -> json.encodeToString(PfSpellCastEvent.serializer(), event)
        is PfDailyPrepEvent -> json.encodeToString(PfDailyPrepEvent.serializer(), event)
        else -> throw CharacterCodecError.UnknownEventType(event::class.simpleName ?: "unknown")
    }

    override fun decodeEvent(json: String, typeId: String): CharacterEvent = when (typeId) {
        "pf2e.note" -> this.json.decodeFromString(PfNoteEvent.serializer(), json)
        "pf2e.damage" -> this.json.decodeFromString(PfDamageTakenEvent.serializer(), json)
        "pf2e.heal" -> this.json.decodeFromString(PfHealedEvent.serializer(), json)
        "pf2e.tempHP" -> this.json.decodeFromString(PfTempHpGainedEvent.serializer(), json)
        "pf2e.dying" -> this.json.decodeFromString(PfDyingChangedEvent.serializer(), json)
        "pf2e.wounded" -> this.json.decodeFromString(PfWoundedChangedEvent.serializer(), json)
        "pf2e.heroPoints" -> this.json.decodeFromString(PfHeroPointsChangedEvent.serializer(), json)
        "pf2e.condition" -> this.json.decodeFromString(PfConditionChangedEvent.serializer(), json)
        "pf2e.strike" -> this.json.decodeFromString(PfStrikeEvent.serializer(), json)
        "pf2e.castSpell" -> this.json.decodeFromString(PfSpellCastEvent.serializer(), json)
        "pf2e.dailyPrep" -> this.json.decodeFromString(PfDailyPrepEvent.serializer(), json)
        else -> throw CharacterCodecError.UnknownEventType(typeId)
    }

    override fun makeProseEvent(text: String, kind: JournalProseKind): CharacterEvent {
        val noteKind = when (kind) {
            JournalProseKind.CAMPAIGN_OPENING -> NoteKind.QUEST
            JournalProseKind.PARTY_JOINED, JournalProseKind.PARTY_LEFT -> NoteKind.NPC
        }
        return PfNoteEvent(text, noteKind)
    }

    companion object {
        const val RULESET_ID: RulesetId = "pf2e-remaster"
    }
}

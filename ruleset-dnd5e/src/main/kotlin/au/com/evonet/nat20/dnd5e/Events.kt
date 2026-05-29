package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.domain.CharacterEvent
import kotlinx.serialization.Serializable

/**
 * Journal events for the A3 intent slice. The full app has ~35 event types;
 * these are the four that pair with the vitals + level intents. `summary` is a
 * computed getter, so it isn't serialized — only the stored facts are.
 */

@Serializable
data class DamageTakenEvent(
    val amount: Int,
    val damageType: String? = null,
    val note: String? = null,
    val previousHp: Int,
    val newHp: Int,
    val tempAbsorbed: Int = 0,
    val maxHp: Int? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val head = if (!damageType.isNullOrEmpty()) {
                "Took $amount ${damageType.lowercase()} damage"
            } else {
                "Took $amount damage"
            }
            val narrative = if (!note.isNullOrEmpty()) " — $note" else ""
            val tempNote = if (tempAbsorbed > 0) " ($tempAbsorbed absorbed by temp)" else ""
            return "$head$narrative (HP $previousHp → $newHp)$tempNote"
        }
}

@Serializable
data class HealedEvent(
    val amount: Int,
    val source: String? = null,
    val previousHp: Int,
    val newHp: Int,
    val maxHp: Int? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val tail = source?.let { " from $it" } ?: ""
            return "Healed $amount$tail (HP $previousHp → $newHp)"
        }
}

@Serializable
data class TempHpGainedEvent(
    val amount: Int,
    val source: String? = null,
    val previous: Int,
    val newValue: Int,
) : CharacterEvent {
    override val summary: String
        get() {
            val src = source?.let { " from $it" } ?: ""
            return when {
                newValue == amount && previous == 0 -> "Gained $amount temp HP$src"
                newValue > previous -> "Gained $amount temp HP$src (now $newValue)"
                else -> "Considered $amount temp HP$src — kept existing $previous"
            }
        }
}

@Serializable
data class LeveledUpEvent(
    val classId: String,
    val className: String,
    val isNewClass: Boolean,
    val previousLevel: Int,
    val newLevel: Int,
    val classLevelAfter: Int,
    val hpChoice: HpChoice,
    val hpGained: Int,
) : CharacterEvent {
    override val summary: String
        get() {
            val displayClass = className.ifEmpty { classId }
            val classLabel = if (displayClass.isEmpty()) "level" else "$displayClass $classLevelAfter"
            val hpPart = if (hpGained > 0) " — +$hpGained HP" else ""
            val newPart = if (isNewClass) " (multiclass)" else ""
            return "Leveled up to $classLabel$newPart$hpPart"
        }
}

/** Free-form journal note (used by [DnD5eRuleset.makeProseEvent]). */
@Serializable
data class NoteEvent(
    val text: String,
    val kind: au.com.evonet.nat20.domain.NoteKind? = null,
) : CharacterEvent {
    override val summary: String get() = text
}

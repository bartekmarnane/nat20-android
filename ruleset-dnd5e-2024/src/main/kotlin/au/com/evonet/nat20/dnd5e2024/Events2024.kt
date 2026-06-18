package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.domain.CharacterEvent
import au.com.evonet.nat20.domain.NoteKind
import kotlinx.serialization.Serializable

/** Journal events for the D&D 5e (2024) starter intent slice (A18). */

@Serializable
data class Note2024Event(val text: String, val kind: NoteKind? = null) : CharacterEvent {
    override val summary: String get() = text
}

@Serializable
data class DamageTaken2024Event(
    val amount: Int,
    val damageType: String? = null,
    val previousHp: Int,
    val newHp: Int,
    val tempAbsorbed: Int = 0,
) : CharacterEvent {
    override val summary: String
        get() {
            val type = damageType?.takeIf { it.isNotBlank() }?.let { " ${it.lowercase()}" } ?: ""
            val temp = if (tempAbsorbed > 0) " ($tempAbsorbed absorbed by temp)" else ""
            return "Took $amount$type damage (HP $previousHp → $newHp)$temp"
        }
}

@Serializable
data class Healed2024Event(val amount: Int, val previousHp: Int, val newHp: Int) : CharacterEvent {
    override val summary: String get() = "Healed $amount (HP $previousHp → $newHp)"
}

@Serializable
data class TempHpGained2024Event(val amount: Int, val newValue: Int) : CharacterEvent {
    override val summary: String get() = "Gained $amount temporary HP (now $newValue)"
}

@Serializable
data class ExhaustionChanged2024Event(val previousLevel: Int, val newLevel: Int) : CharacterEvent {
    override val summary: String
        get() = when {
            Exhaustion2024.isFatal(newLevel) -> "Exhaustion reached level 6 — death"
            newLevel > previousLevel -> "Exhaustion rose to level $newLevel (${Exhaustion2024.d20Modifier(newLevel)} to d20 tests)"
            else -> "Exhaustion eased to level $newLevel"
        }
}

@Serializable
data class InspirationChanged2024Event(val hasInspiration: Boolean) : CharacterEvent {
    override val summary: String get() = if (hasInspiration) "Gained Heroic Inspiration" else "Used Heroic Inspiration"
}

@Serializable
data class ConditionChanged2024Event(val name: String, val applied: Boolean) : CharacterEvent {
    override val summary: String get() = if (applied) "Gained the $name condition" else "Cleared the $name condition"
}

@Serializable
data class LeveledUp2024Event(
    val classId: String,
    val className: String,
    val newLevel: Int,
    val classLevelAfter: Int,
    val hpGained: Int,
) : CharacterEvent {
    override val summary: String
        get() {
            val label = className.ifEmpty { classId }
            return "Leveled up to $label $classLevelAfter — +$hpGained HP"
        }
}

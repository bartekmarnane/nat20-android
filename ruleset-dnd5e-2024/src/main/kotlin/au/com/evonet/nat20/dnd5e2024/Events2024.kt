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
    val resistanceApplied: Boolean = false,
    val concentrationCheckDC: Int? = null,
    val relentlessEndurance: Boolean = false,
) : CharacterEvent {
    override val summary: String
        get() {
            val type = damageType?.takeIf { it.isNotBlank() }?.let { " ${it.lowercase()}" } ?: ""
            val resist = if (resistanceApplied) " (resisted)" else ""
            val temp = if (tempAbsorbed > 0) " ($tempAbsorbed absorbed by temp)" else ""
            val conc = concentrationCheckDC?.let { " · concentration save DC $it" } ?: ""
            val relentless = if (relentlessEndurance) " · Relentless Endurance kept them at 1 HP" else ""
            return "Took $amount$type damage$resist (HP $previousHp → $newHp)$temp$conc$relentless"
        }
}

@Serializable
data class ConcentrationEnded2024Event(val target: String? = null) : CharacterEvent {
    override val summary: String get() = target?.let { "Lost concentration on $it" } ?: "Ended concentration"
}

@Serializable
data class EffectApplied2024Event(val name: String) : CharacterEvent {
    override val summary: String get() = "Gained the $name effect"
}

@Serializable
data class EffectCancelled2024Event(val name: String) : CharacterEvent {
    override val summary: String get() = "Lost the $name effect"
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
data class LongRested2024Event(val hpRestored: Int, val slotsRestored: Int, val exhaustionRecovered: Boolean) : CharacterEvent {
    override val summary: String
        get() {
            val parts = buildList {
                if (slotsRestored > 0) add("$slotsRestored spell slot${if (slotsRestored == 1) "" else "s"}")
                if (exhaustionRecovered) add("a level of exhaustion")
            }
            val extras = if (parts.isEmpty()) "" else " (${parts.joinToString(" + ")} restored)"
            return "Took a long rest — back to full health$extras"
        }
}

@Serializable
data class HitDieSpent2024Event(val healingRolled: Int, val previousHp: Int, val newHp: Int, val remaining: Int) : CharacterEvent {
    override val summary: String get() = "Spent a hit die — recovered $healingRolled HP (HP $previousHp → $newHp), $remaining left"
}

@Serializable
data class CastSpell2024Event(val spellID: String, val spellName: String, val slotLevel: Int, val wasUpcast: Boolean, val target: String? = null) : CharacterEvent {
    override val summary: String
        get() {
            val head = when {
                slotLevel == 0 -> "Cast $spellName"
                wasUpcast -> "Cast $spellName at ${ordinal(slotLevel)} level"
                else -> "Cast $spellName"
            }
            return head + (target?.takeIf { it.isNotBlank() }?.let { " on $it" } ?: "")
        }
}

@Serializable
data class SlotExpended2024Event(val slotLevel: Int, val remaining: Int, val source: String? = null) : CharacterEvent {
    override val summary: String get() = "Expended a ${ordinal(slotLevel)}-level spell slot" + (source?.let { " — $it" } ?: "")
}

@Serializable
data class SpellPrep2024Event(val spellName: String, val prepared: Boolean) : CharacterEvent {
    override val summary: String get() = if (prepared) "Prepared $spellName" else "Unprepared $spellName"
}

@Serializable
data class DeathSaveRolled2024Event(
    val d20: Int,
    val previous: au.com.evonet.nat20.dnd5e.core.DeathSaves,
    val newState: au.com.evonet.nat20.dnd5e.core.DeathSaves,
    val revivedAt: Int? = null,
) : CharacterEvent {
    override val summary: String
        get() = when {
            revivedAt != null -> "Rolled a natural 20 — back at $revivedAt HP"
            d20 == 1 -> "Rolled a 1 — two death-save failures (${newState.failures}/3)"
            newState.isStable -> "Rolled $d20 — stabilized"
            newState.isDead -> "Rolled $d20 — fell"
            d20 >= 10 -> "Rolled $d20 — death save success (${newState.successes}/3)"
            else -> "Rolled $d20 — death save failure (${newState.failures}/3)"
        }
}

@Serializable
data class Initiative2024Event(val value: Int?) : CharacterEvent {
    override val summary: String get() = if (value != null) "Rolled initiative: $value" else "Cleared initiative"
}

@Serializable
data class ItemAcquired2024Event(val itemName: String, val quantity: Int) : CharacterEvent {
    override val summary: String get() = "Acquired ${if (quantity > 1) "$quantity× " else ""}$itemName"
}

@Serializable
data class ItemDropped2024Event(val itemName: String, val quantity: Int) : CharacterEvent {
    override val summary: String get() = "Dropped ${if (quantity > 1) "$quantity× " else ""}$itemName"
}

@Serializable
data class ArmorEquipped2024Event(val armorName: String? = null) : CharacterEvent {
    override val summary: String get() = armorName?.let { "Donned $it" } ?: "Removed armor"
}

@Serializable
data class ShieldChanged2024Event(val equipped: Boolean) : CharacterEvent {
    override val summary: String get() = if (equipped) "Readied a shield" else "Stowed the shield"
}

@Serializable
data class CoinAdjusted2024Event(val coin: au.com.evonet.nat20.dnd5e.core.Coin, val delta: Int, val source: String? = null) : CharacterEvent {
    override val summary: String
        get() {
            val verb = if (delta >= 0) "Gained" else "Spent"
            val tail = source?.let { " — $it" } ?: ""
            return "$verb ${kotlin.math.abs(delta)} ${coin.abbreviation}$tail"
        }
}

@Serializable
data class Attack2024Event(
    val weaponName: String,
    val attackTotal: Int,
    val outcome: au.com.evonet.nat20.dnd5e.core.AttackOutcome,
    val damage: Int? = null,
    val damageType: String? = null,
    val mastery: String? = null,
    val target: String? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val tgt = target?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
            val dmg = damage?.let { " for $it" + (damageType?.takeIf { t -> t.isNotBlank() }?.let { t -> " ${t.lowercase()}" } ?: "") + " damage" } ?: ""
            val mast = mastery?.takeIf { it.isNotBlank() }?.let { " · ${it.replaceFirstChar(Char::uppercase)}" } ?: ""
            return when (outcome) {
                au.com.evonet.nat20.dnd5e.core.AttackOutcome.MISS -> "Attacked$tgt with $weaponName — missed (rolled $attackTotal)"
                au.com.evonet.nat20.dnd5e.core.AttackOutcome.HIT -> "Hit$tgt with $weaponName$dmg (rolled $attackTotal)$mast"
                au.com.evonet.nat20.dnd5e.core.AttackOutcome.CRITICAL -> "Critical hit$tgt with $weaponName$dmg!$mast"
            }
        }
}

@Serializable
data class WeaponMasteries2024Event(val masteries: List<String>) : CharacterEvent {
    override val summary: String
        get() = if (masteries.isEmpty()) "Cleared weapon masteries"
        else "Set weapon masteries: " + masteries.joinToString(", ") { it.replaceFirstChar(Char::uppercase) }
}

private fun ordinal(n: Int): String = when (n) { 1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${n}th" }

@Serializable
data class LeveledUp2024Event(
    val classId: String,
    val className: String,
    val newLevel: Int,
    val classLevelAfter: Int,
    val hpGained: Int,
    val subclass: String? = null,
    val abilityIncreases: Map<au.com.evonet.nat20.dnd5e.core.Ability, Int> = emptyMap(),
    val feat: String? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val label = className.ifEmpty { classId }
            val extras = buildList {
                subclass?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (abilityIncreases.isNotEmpty()) add(abilityIncreases.entries.joinToString(", ") { "+${it.value} ${it.key.abbreviation}" })
                feat?.let { add(Feats2024.feat(it)?.name ?: it.replaceFirstChar(Char::uppercase)) }
            }
            val extra = if (extras.isEmpty()) "" else " · ${extras.joinToString(" · ")}"
            return "Leveled up to $label $classLevelAfter — +$hpGained HP$extra"
        }
}

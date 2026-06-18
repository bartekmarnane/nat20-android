package au.com.evonet.nat20.dnd5e.core

import kotlinx.serialization.Serializable

/** How a level's hit-point gain is determined. Port of the iOS `HPChoice`. */
@Serializable
sealed class HpChoice {
    /** The PHB "average" shorthand: half the die rounded up, plus 1. */
    @Serializable
    data object Average : HpChoice()

    /** A rolled die value, clamped into 1..hitDie by the math. */
    @Serializable
    data class Rolled(val value: Int) : HpChoice()
}

/**
 * Pure HP math shared by character creation (L1 HP) and the level-up intent,
 * so the formula can't drift between the two paths. Port of the iOS
 * `LevelUpMath` (HP portion; the ASI-cadence helpers arrive with the level-up
 * wizard at A11).
 */
object LevelUpMath {
    /** Average gain shorthand, CON-independent: d6→4, d8→5, d10→6, d12→7. */
    fun averageGain(hitDie: Int): Int = maxOf(1, hitDie / 2 + 1)

    /** 5e RAW first-ever character level: max hit die + CON mod, floor 1. */
    fun firstLevelHp(hitDie: Int, conMod: Int): Int = maxOf(1, hitDie + conMod)

    /** HP gained on a single level-up (L2+): die value + CON mod, floor +1. */
    fun levelUpHp(choice: HpChoice, hitDie: Int, conMod: Int): Int {
        val dieValue = when (choice) {
            is HpChoice.Average -> averageGain(hitDie)
            is HpChoice.Rolled -> maxOf(1, minOf(hitDie, choice.value))
        }
        return maxOf(1, dieValue + conMod)
    }
}

/**
 * Standard PHB hit dice for the 12 core classes. Stand-in for the full
 * `ClassCatalog` (SRD JSON loading lands with the content steps); unknown
 * ids fall back to d8, matching the iOS catalogue-miss behaviour.
 */
object DnD5eClasses {
    private val HIT_DICE: Map<String, Int> = mapOf(
        "barbarian" to 12,
        "fighter" to 10,
        "paladin" to 10,
        "ranger" to 10,
        "bard" to 8,
        "cleric" to 8,
        "druid" to 8,
        "monk" to 8,
        "rogue" to 8,
        "warlock" to 8,
        "sorcerer" to 6,
        "wizard" to 6,
    )

    fun hitDie(classId: String): Int = HIT_DICE[classId.lowercase()] ?: 8
}

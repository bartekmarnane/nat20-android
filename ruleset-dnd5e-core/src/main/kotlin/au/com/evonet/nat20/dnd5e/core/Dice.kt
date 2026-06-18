package au.com.evonet.nat20.dnd5e.core

import kotlin.random.Random

/**
 * Edition-agnostic dice model + roller behind the die-roll primitive (A16).
 * Port of the iOS `RollSpec`/`RollBonus`/`RollResult`, with one improvement: the
 * roller takes an injectable [Random] so the math is deterministically testable
 * (iOS rolls straight off the system RNG). The Compose `RollResultView` animates
 * a [RollResult]; everything here is pure and UI-free.
 */

/** Which of the rolled dice are kept toward the total. */
sealed interface Keep {
    data object All : Keep
    data class Highest(val n: Int) : Keep
    data class Lowest(val n: Int) : Keep
    data class DropLowest(val n: Int) : Keep
    data class DropHighest(val n: Int) : Keep
}

/** Describes the dice to roll: [count] d[faces], a flat [modifier], and a [keep] rule. */
data class RollSpec(
    val count: Int,
    val faces: Int,
    val modifier: Int = 0,
    val keep: Keep = Keep.All,
) {
    init {
        require(count >= 1) { "count must be ≥ 1" }
        require(faces >= 2) { "faces must be ≥ 2" }
    }

    /** Dice notation without the modifier (it renders as its own chip): "d20", "4d6", "2d20 advantage". */
    val displayNotation: String
        get() {
            val base = if (count == 1) "d$faces" else "${count}d$faces"
            return when (keep) {
                is Keep.Highest -> "$base advantage"
                is Keep.Lowest -> "$base disadvantage"
                is Keep.DropLowest -> "$base drop lowest"
                is Keep.DropHighest -> "$base drop highest"
                Keep.All -> base
            }
        }

    /** Indices into a rolled `values` list that count toward the total under this keep rule. */
    fun keptIndices(values: List<Int>): Set<Int> {
        val indexed = values.withIndex().toList()
        return when (val k = keep) {
            Keep.All -> values.indices.toSet()
            is Keep.Highest -> indexed.sortedByDescending { it.value }.take(k.n).map { it.index }.toSet()
            is Keep.Lowest -> indexed.sortedBy { it.value }.take(k.n).map { it.index }.toSet()
            is Keep.DropLowest -> values.indices.toSet() - indexed.sortedBy { it.value }.take(k.n).map { it.index }.toSet()
            is Keep.DropHighest -> values.indices.toSet() - indexed.sortedByDescending { it.value }.take(k.n).map { it.index }.toSet()
        }
    }

    companion object {
        /** `d(1, 20, mod = 3)` → 1d20+3 in dice-notation order. */
        fun d(count: Int, faces: Int, mod: Int = 0, keep: Keep = Keep.All): RollSpec =
            RollSpec(count, faces, mod, keep)

        /** 2d[faces] keep highest 1. */
        fun advantage(faces: Int = 20, mod: Int = 0): RollSpec = RollSpec(2, faces, mod, Keep.Highest(1))

        /** 2d[faces] keep lowest 1. */
        fun disadvantage(faces: Int = 20, mod: Int = 0): RollSpec = RollSpec(2, faces, mod, Keep.Lowest(1))

        /** 4d6 drop lowest — the ability-score generator. */
        val abilityRoll: RollSpec = RollSpec(4, 6, 0, Keep.DropLowest(1))
    }
}

/** A named contribution to a roll's total; renders as a chip beside the dice. */
data class RollBonus(val label: String, val value: Int) {
    val sign: String get() = if (value >= 0) "+" else "−"
    val magnitude: Int get() = kotlin.math.abs(value)
}

/**
 * The outcome of a roll: the raw dice, which were kept, the flat modifier, and
 * the named bonus breakdown. When [bonuses] are present they own the breakdown
 * and [modifier] is ignored — callers pass one or the other, never both.
 */
data class RollResult(
    val dice: List<Int>,
    val keptIndices: Set<Int>,
    val modifier: Int,
    val bonuses: List<RollBonus>,
    /** The spec's die faces — lets the UI know whether crit/fumble apply (d20). */
    val faces: Int,
) {
    val keptDice: List<Int> get() = dice.filterIndexed { i, _ -> i in keptIndices }
    val keptSum: Int get() = keptDice.sum()
    val bonusSum: Int get() = bonuses.sumOf { it.value }
    val total: Int get() = keptSum + (if (bonuses.isEmpty()) modifier else bonusSum)

    /** The single kept d20 value, or null when this isn't a d20 roll. */
    val naturalD20: Int? get() = if (faces == 20) keptDice.firstOrNull() else null
    val isNatural20: Boolean get() = naturalD20 == 20
    val isNatural1: Boolean get() = naturalD20 == 1
}

/** Rolls [spec] (+ optional [bonuses]) using [random] (injectable for tests). */
object DiceRoller {
    fun roll(spec: RollSpec, bonuses: List<RollBonus> = emptyList(), random: Random = Random.Default): RollResult {
        val dice = List(spec.count) { random.nextInt(1, spec.faces + 1) }
        return RollResult(
            dice = dice,
            keptIndices = spec.keptIndices(dice),
            modifier = spec.modifier,
            bonuses = bonuses,
            faces = spec.faces,
        )
    }
}

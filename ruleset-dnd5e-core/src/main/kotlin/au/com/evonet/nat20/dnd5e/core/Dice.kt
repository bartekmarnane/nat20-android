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

    /** This spec with the dice count doubled — the 5e critical-hit damage rule. */
    fun critDoubled(): RollSpec = copy(count = count * 2)

    companion object {
        private val NOTATION = Regex("""\s*(\d+)\s*d\s*(\d+)\s*([+-]\s*\d+)?\s*""", RegexOption.IGNORE_CASE)

        /** `d(1, 20, mod = 3)` → 1d20+3 in dice-notation order. */
        fun d(count: Int, faces: Int, mod: Int = 0, keep: Keep = Keep.All): RollSpec =
            RollSpec(count, faces, mod, keep)

        /** Parses dice notation like "1d8", "2d6", "1d10+2"; null if it doesn't match. */
        fun parse(notation: String): RollSpec? {
            val m = NOTATION.matchEntire(notation) ?: return null
            val mod = m.groupValues[3].replace(" ", "").toIntOrNull() ?: 0
            return RollSpec(m.groupValues[1].toInt(), m.groupValues[2].toInt(), mod)
        }

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

    companion object {
        /**
         * A bare single-die result the caller already knows the value of — no
         * keep rules, no bonuses. Used to re-seed a roll readout from state
         * that outlived the composable that produced it (a wizard's HP roll
         * surviving a step change or process death).
         */
        fun single(value: Int, faces: Int): RollResult = RollResult(
            dice = listOf(value),
            keptIndices = setOf(0),
            modifier = 0,
            bonuses = emptyList(),
            faces = faces,
        )
    }
}

/**
 * The physical-dice path (A25): the player rolled real dice at the table and
 * types the faces in instead of tapping ROLL.
 *
 * Deliberately thin. The premise is that a hand-entered roll is *the same roll*
 * — the player supplies only the raw faces and every other rule (keep/drop,
 * bonus chips, crit doubling, nat-20 detection) runs exactly as it does for an
 * RNG roll, so the event that reaches the journal is indistinguishable.
 * Edition-agnostic, so 2014 / 2024 / PF2e all get it from here.
 */
object ManualRollEntry {
    /**
     * How many faces the player types — one per die, including any a keep rule
     * will later drop (you rolled both d20s, so you enter both).
     */
    fun slotCount(spec: RollSpec): Int = spec.count

    /** The faces this die can show, in pad order. */
    fun enterableFaces(spec: RollSpec): IntRange = 1..spec.faces

    /** Whether a typed face is one the die could actually have landed on. */
    fun isValid(face: Int, spec: RollSpec): Boolean = face in enterableFaces(spec)

    /**
     * Builds the [RollResult] an RNG roll of [spec] would have produced, from
     * faces the player entered. Out-of-range entries clamp rather than throw —
     * the pad only offers valid faces, so this guards the exotic paths.
     */
    fun result(
        faces: List<Int>,
        spec: RollSpec,
        bonuses: List<RollBonus> = emptyList(),
    ): RollResult {
        val dice = faces.map { it.coerceIn(1, spec.faces) }
        return RollResult(
            dice = dice,
            keptIndices = spec.keptIndices(dice),
            modifier = spec.modifier,
            bonuses = bonuses,
            faces = spec.faces,
        )
    }

    /**
     * Indices of kept d20s showing a 1 — the dice Halfling Lucky lets you roll
     * again. With physical dice the app can't re-roll for the player, so the
     * view re-opens these slots and asks what the new die shows.
     */
    fun luckyRerollIndices(values: List<Int>, spec: RollSpec): List<Int> {
        if (spec.faces != 20) return emptyList()
        val kept = spec.keptIndices(values)
        return values.indices.filter { it in kept && values[it] == 1 }
    }
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

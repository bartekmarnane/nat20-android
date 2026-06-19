package au.com.evonet.nat20.pf2e.core

/**
 * Pathfinder 2e spell-slot progression — the PF2e analogue of the 5e slot
 * tables. The maths are uniform across the full casters: the same slots-per-rank
 * table and the same DC/attack formula. Spell **rank** runs 1–10; a caster gains
 * a new rank at every odd level (rank N at character level 2N−1) with 2 slots,
 * rising to 3 the next level — except rank 10 (reached at level 19, a single
 * slot). Port of the iOS `SpellcastingProgression`.
 */
object SpellcastingProgression {
    /** Cantrips a full caster knows/prepares per day, at every level. */
    const val CANTRIPS_PER_DAY: Int = 5

    /** The highest spell rank a full caster can cast at [level] (`ceil(level/2)`, capped at 10). */
    fun maxSpellRank(level: Int): Int = minOf(10, (level + 1) / 2)

    /** Slots per rank for a full caster at [level], keyed by spell rank (1..10). Cantrips are at-will. */
    fun fullCasterSlots(level: Int): Map<Int, Int> {
        val maxRank = maxSpellRank(level)
        if (maxRank < 1) return emptyMap()
        return (1..maxRank).associateWith { rank -> slotCount(rank, level) }
    }

    /** 1 for the capstone 10th rank, otherwise 2 the level it unlocks and 3 thereafter. */
    private fun slotCount(rank: Int, level: Int): Int {
        if (rank == 10) return 1
        val unlockLevel = 2 * rank - 1
        return if (level > unlockLevel) 3 else 2
    }
}

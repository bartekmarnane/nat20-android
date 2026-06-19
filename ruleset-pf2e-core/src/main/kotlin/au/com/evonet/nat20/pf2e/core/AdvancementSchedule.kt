package au.com.evonet.nat20.pf2e.core

/**
 * The **universal** Pathfinder 2e level-advancement cadence — the parts of the
 * per-level table identical for every class. (The class-specific half —
 * proficiency-rank increases, class-feat cadence — lives with the class
 * catalogue.) Port of the iOS `AdvancementSchedule`.
 *
 * - **Ability boosts** — four free boosts at levels 5, 10, 15, 20 (+2, dropping
 *   to +1 once 18+, no duplicate within the four).
 * - **Skill increases** — one at every odd level from 3 (raise a skill one rank),
 *   capped by level: expert from 3, master from 7, legendary from 15.
 * - **General / ancestry / skill feats** on their fixed clocks.
 */
object AdvancementSchedule {
    val ABILITY_BOOST_LEVELS: Set<Int> = setOf(5, 10, 15, 20)
    const val BOOSTS_PER_ABILITY_BOOST_LEVEL: Int = 4
    val SKILL_INCREASE_LEVELS: Set<Int> = setOf(3, 5, 7, 9, 11, 13, 15, 17, 19)
    val GENERAL_FEAT_LEVELS: Set<Int> = setOf(3, 7, 11, 15, 19)
    val ANCESTRY_FEAT_LEVELS: Set<Int> = setOf(1, 5, 9, 13, 17)

    fun grantsAbilityBoosts(level: Int): Boolean = level in ABILITY_BOOST_LEVELS
    fun grantsSkillIncrease(level: Int): Boolean = level in SKILL_INCREASE_LEVELS

    /** A Rogue gains a skill feat every level; everyone else at each even level. */
    fun skillFeatLevels(rogue: Boolean): Set<Int> =
        if (rogue) (1..20).toSet() else (2..20 step 2).toSet()

    /** The highest rank a skill increase taken *at* [level] may raise a skill to. */
    fun maxSkillRank(level: Int): Proficiency = when {
        level >= 15 -> Proficiency.LEGENDARY
        level >= 7 -> Proficiency.MASTER
        else -> Proficiency.EXPERT
    }
}

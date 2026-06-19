package au.com.evonet.nat20.pf2e.core

import kotlinx.serialization.Serializable

/**
 * A Pathfinder 2e proficiency rank — *the* structural departure from 5e. Where
 * 5e has a single proficiency bonus, PF2e ranks every check, save, AC, attack,
 * and DC on a five-step ladder, and the rank scales the bonus independently of
 * level. Under the default rule the bonus is `level + rankBonus` once at least
 * **trained**; while **untrained** you add neither level nor a rank bonus (so an
 * untrained high-level character is genuinely bad at the thing). Port of the iOS
 * `PathfinderCore/Proficiency`.
 */
@Serializable
enum class Proficiency(val rank: Int, val rankBonus: Int, val displayName: String) {
    UNTRAINED(0, 0, "Untrained"),
    TRAINED(1, 2, "Trained"),
    EXPERT(2, 4, "Expert"),
    MASTER(3, 6, "Master"),
    LEGENDARY(4, 8, "Legendary");

    /** Total proficiency bonus at [level]: `level + rankBonus` when trained or better, else 0. */
    fun bonus(level: Int): Int = if (this == UNTRAINED) 0 else level + rankBonus

    /** One-letter sheet tag (U/T/E/M/L). */
    val letter: String get() = displayName.first().toString()

    /** The next rank up (the single-step raise a skill increase performs), or null at the top. */
    val next: Proficiency? get() = entries.firstOrNull { it.rank == rank + 1 }

    companion object {
        fun fromRank(rank: Int): Proficiency = entries.firstOrNull { it.rank == rank } ?: UNTRAINED
    }
}

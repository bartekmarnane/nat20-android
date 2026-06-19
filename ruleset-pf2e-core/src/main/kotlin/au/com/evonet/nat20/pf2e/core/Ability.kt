package au.com.evonet.nat20.pf2e.core

import kotlinx.serialization.Serializable

/**
 * The six Pathfinder 2e ability scores. The roster coincides with D&D's six, but
 * PF2e builds scores by **boosts** and **flaws** at creation (a +2 boost, +1 once
 * a score is already 18+, a −2 flaw) and several derived stats key off abilities
 * differently. This type is the closed set + the (shared-by-coincidence) modifier
 * formula. Port of the iOS `PathfinderCore/Ability`.
 */
@Serializable
enum class PfAbility(val abbreviation: String, val displayName: String) {
    STRENGTH("STR", "Strength"),
    DEXTERITY("DEX", "Dexterity"),
    CONSTITUTION("CON", "Constitution"),
    INTELLIGENCE("INT", "Intelligence"),
    WISDOM("WIS", "Wisdom"),
    CHARISMA("CHA", "Charisma"),
}

/** The six ability scores; modifier = floor((score − 10) / 2), identical to 5e. */
@Serializable
data class PfAbilityScores(
    val strength: Int = DEFAULT,
    val dexterity: Int = DEFAULT,
    val constitution: Int = DEFAULT,
    val intelligence: Int = DEFAULT,
    val wisdom: Int = DEFAULT,
    val charisma: Int = DEFAULT,
) {
    fun score(ability: PfAbility): Int = when (ability) {
        PfAbility.STRENGTH -> strength
        PfAbility.DEXTERITY -> dexterity
        PfAbility.CONSTITUTION -> constitution
        PfAbility.INTELLIGENCE -> intelligence
        PfAbility.WISDOM -> wisdom
        PfAbility.CHARISMA -> charisma
    }

    fun with(ability: PfAbility, value: Int): PfAbilityScores = when (ability) {
        PfAbility.STRENGTH -> copy(strength = value)
        PfAbility.DEXTERITY -> copy(dexterity = value)
        PfAbility.CONSTITUTION -> copy(constitution = value)
        PfAbility.INTELLIGENCE -> copy(intelligence = value)
        PfAbility.WISDOM -> copy(wisdom = value)
        PfAbility.CHARISMA -> copy(charisma = value)
    }

    fun modifier(ability: PfAbility): Int = modifier(score(ability))

    companion object {
        const val DEFAULT: Int = 10

        /** Ability modifier = floor((score − 10) / 2). Negative branch shifted by 1 (toward −∞). */
        fun modifier(score: Int): Int {
            val diff = score - 10
            return if (diff >= 0) diff / 2 else (diff - 1) / 2
        }
    }
}

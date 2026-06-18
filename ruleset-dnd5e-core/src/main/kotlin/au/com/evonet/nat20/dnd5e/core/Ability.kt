package au.com.evonet.nat20.dnd5e.core

import kotlinx.serialization.Serializable

/** The six D&D 5e ability scores. */
@Serializable
enum class Ability(val abbreviation: String) {
    STRENGTH("STR"),
    DEXTERITY("DEX"),
    CONSTITUTION("CON"),
    INTELLIGENCE("INT"),
    WISDOM("WIS"),
    CHARISMA("CHA");

    companion object {
        /** Resolve a catalogue key like `"strength"` (case-insensitive) to an [Ability]. */
        fun fromKey(key: String): Ability? = entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
    }
}

/**
 * The six ability scores as a value type. Immutable — use [with] to derive a
 * changed copy. Port of the iOS `AbilityScores`.
 */
@Serializable
data class AbilityScores(
    val strength: Int = DEFAULT_SCORE,
    val dexterity: Int = DEFAULT_SCORE,
    val constitution: Int = DEFAULT_SCORE,
    val intelligence: Int = DEFAULT_SCORE,
    val wisdom: Int = DEFAULT_SCORE,
    val charisma: Int = DEFAULT_SCORE,
) {
    /** The raw score for [ability]. */
    fun score(ability: Ability): Int = when (ability) {
        Ability.STRENGTH -> strength
        Ability.DEXTERITY -> dexterity
        Ability.CONSTITUTION -> constitution
        Ability.INTELLIGENCE -> intelligence
        Ability.WISDOM -> wisdom
        Ability.CHARISMA -> charisma
    }

    /** A copy with [ability] set to [value]. */
    fun with(ability: Ability, value: Int): AbilityScores = when (ability) {
        Ability.STRENGTH -> copy(strength = value)
        Ability.DEXTERITY -> copy(dexterity = value)
        Ability.CONSTITUTION -> copy(constitution = value)
        Ability.INTELLIGENCE -> copy(intelligence = value)
        Ability.WISDOM -> copy(wisdom = value)
        Ability.CHARISMA -> copy(charisma = value)
    }

    /** The 5e modifier for [ability]. */
    fun modifier(ability: Ability): Int = modifier(score(ability))

    companion object {
        const val DEFAULT_SCORE: Int = 10
        val VALID_RANGE: IntRange = 1..30

        /**
         * 5e ability modifier = floor((score - 10) / 2). Kotlin integer
         * division truncates toward zero, so the negative branch is shifted
         * by one to match the floor.
         */
        fun modifier(score: Int): Int {
            val diff = score - 10
            return if (diff >= 0) diff / 2 else (diff - 1) / 2
        }
    }
}

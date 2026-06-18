package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.domain.CharacterPayload
import kotlinx.serialization.Serializable

/**
 * D&D 5e character data. This is the **minimal A3 slice** — race, classes,
 * the six abilities, and HP. The shipped iOS payload carries far more
 * (spells, inventory, effects, conditions, coins, rest state, …); those
 * fields arrive with the steps that need them (A7+/A10). Greenfield, so no
 * legacy-migration decoders — only the current schema.
 *
 * [level] and [characterClass] are derived from [classes] (no stored
 * duplication), matching the iOS computed properties.
 */
@Serializable
data class DnD5ePayload(
    val race: String = "",
    val classes: List<ClassEntry> = emptyList(),
    val abilityScores: AbilityScores = AbilityScores(),
    val maxHp: Int = MINIMUM_MAX_HP,
    /** Defaults to full health (= [maxHp]) for a fresh character. */
    val currentHp: Int = maxHp,
    /**
     * Temporary HP — a separate pool that absorbs damage before [currentHp]
     * and doesn't stack (a higher value replaces, otherwise discarded).
     */
    val temporaryHp: Int = 0,
    /** Background id (A8); resolved through [DnD5eCatalog.background] at display. */
    val background: String = "",
    /** Skill ids the character is proficient in (background-granted + class picks). */
    val selectedSkills: List<String> = emptyList(),
    /** Alignment label, e.g. "Lawful Good"; null until chosen. */
    val alignment: String? = null,
) : CharacterPayload {

    /** Total character level = sum of all class entry levels, floored at 1. */
    val level: Int
        get() = maxOf(MIN_LEVEL, classes.sumOf { it.level })

    /** Primary class id (the first entry), or "" if no class chosen yet. */
    val characterClass: String
        get() = classes.firstOrNull()?.classId ?: ""

    companion object {
        const val MIN_LEVEL: Int = 1
        const val MAX_LEVEL: Int = 20
        const val MINIMUM_MAX_HP: Int = 1
    }
}

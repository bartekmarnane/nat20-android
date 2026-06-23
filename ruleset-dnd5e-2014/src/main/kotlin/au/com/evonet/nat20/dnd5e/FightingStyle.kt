package au.com.evonet.nat20.dnd5e

import kotlinx.serialization.Serializable

/**
 * The D&D 5e (2014) **Fighting Style** feature (A11 deep class catalogue): a
 * combat specialisation chosen by Fighters (level 1), Paladins and Rangers
 * (level 2). The clean always-on riders fold into derived stats — **Defense**
 * (+1 AC while armored), **Archery** (+2 ranged attack), **Dueling** (+2 damage
 * with a single one-handed melee weapon). The rest (Great Weapon Fighting's
 * reroll, Two-Weapon Fighting's off-hand mod, Protection's reaction) are recorded
 * and surfaced as reminders. Port of the iOS `FightingStyleCatalog`.
 */
@Serializable
data class FightingStyle(val id: String, val name: String, val description: String)

object FightingStyles {
    val all: List<FightingStyle> = listOf(
        FightingStyle("archery", "Archery", "You gain a +2 bonus to attack rolls you make with ranged weapons."),
        FightingStyle("defense", "Defense", "While you are wearing armor, you gain a +1 bonus to AC."),
        FightingStyle("dueling", "Dueling", "When wielding a melee weapon in one hand and no other weapon, you gain +2 to its damage."),
        FightingStyle("great-weapon-fighting", "Great Weapon Fighting", "Reroll a 1 or 2 on a damage die for a two-handed or versatile melee weapon."),
        FightingStyle("protection", "Protection", "When a creature you can see attacks an ally within 5 feet, use your reaction to impose Disadvantage."),
        FightingStyle("two-weapon-fighting", "Two-Weapon Fighting", "Add your ability modifier to the damage of your off-hand attack."),
    )

    private val byId = all.associateBy { it.id }
    fun style(id: String): FightingStyle? = byId[id]

    /** The class level at which a class grants its Fighting Style (null if it never does). */
    fun grantLevel(classId: String): Int? = when (classId.lowercase()) {
        "fighter" -> 1
        "paladin", "ranger" -> 2
        else -> null
    }

    /** Whether a class of [classLevel] should have chosen a Fighting Style by now. */
    fun grantsBy(classId: String, classLevel: Int): Boolean {
        val level = grantLevel(classId) ?: return false
        return classLevel >= level
    }
}

package au.com.evonet.nat20.pf2e.core

import kotlinx.serialization.Serializable

/** The three Pathfinder 2e saving throws, each governed by an ability + its own proficiency rank. */
@Serializable
enum class Save(val displayName: String, val ability: PfAbility) {
    FORTITUDE("Fortitude", PfAbility.CONSTITUTION),
    REFLEX("Reflex", PfAbility.DEXTERITY),
    WILL("Will", PfAbility.WISDOM),
}

/**
 * The fixed Pathfinder 2e skills and their governing ability. The open-ended
 * **Lore** skill (always Intelligence, subtyped by string) is modelled outside
 * this enum — characters carry Lore proficiencies keyed by subtype in the payload.
 * The Remaster roster differs from 5e (Crafting/Occultism/Society/Thievery; no
 * History/Investigation/Insight/Animal Handling split). Port of iOS `PFSkill`.
 */
@Serializable
enum class PfSkill(val displayName: String, val ability: PfAbility) {
    ACROBATICS("Acrobatics", PfAbility.DEXTERITY),
    ARCANA("Arcana", PfAbility.INTELLIGENCE),
    ATHLETICS("Athletics", PfAbility.STRENGTH),
    CRAFTING("Crafting", PfAbility.INTELLIGENCE),
    DECEPTION("Deception", PfAbility.CHARISMA),
    DIPLOMACY("Diplomacy", PfAbility.CHARISMA),
    INTIMIDATION("Intimidation", PfAbility.CHARISMA),
    MEDICINE("Medicine", PfAbility.WISDOM),
    NATURE("Nature", PfAbility.WISDOM),
    OCCULTISM("Occultism", PfAbility.INTELLIGENCE),
    PERFORMANCE("Performance", PfAbility.CHARISMA),
    RELIGION("Religion", PfAbility.WISDOM),
    SOCIETY("Society", PfAbility.INTELLIGENCE),
    STEALTH("Stealth", PfAbility.DEXTERITY),
    SURVIVAL("Survival", PfAbility.WISDOM),
    THIEVERY("Thievery", PfAbility.DEXTERITY);

    companion object {
        /** Intelligence governs every Lore skill, regardless of subtype. */
        val LORE_ABILITY: PfAbility = PfAbility.INTELLIGENCE
    }
}

/**
 * The four Pathfinder 2e magical traditions. PF2e splits all magic across four
 * traditions rather than 5e's arcane/divine class split; the casting ability is a
 * property of the *class*, not the tradition, so it isn't recorded here. Spell
 * rank runs 1–10 (cantrips are "rank 0"). Port of iOS `SpellTradition`.
 */
@Serializable
enum class SpellTradition(val displayName: String) {
    ARCANE("Arcane"),
    DIVINE("Divine"),
    OCCULT("Occult"),
    PRIMAL("Primal");

    companion object {
        /** The highest spell rank in PF2e (10); cantrips are rank 0, auto-heightened to half level rounded up. */
        const val MAX_SPELL_RANK: Int = 10
    }
}

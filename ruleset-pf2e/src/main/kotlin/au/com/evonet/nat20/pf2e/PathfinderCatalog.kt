package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save
import au.com.evonet.nat20.pf2e.core.SpellTradition

/**
 * A hand-authored Pathfinder 2e (Remaster) seed catalogue for the creation
 * wizard (A22) — a representative set of ancestries, backgrounds, and classes
 * with the data the wizard needs (ability boosts/flaws, HP, initial
 * proficiencies). Full ORC coverage + a move to bundled JSON is a follow-up
 * (mirrors iOS's `Ancestry`/`Background`/`PFClass` catalogues). All content is
 * ORC-licensed (Archives of Nethys).
 */

/** A 2024-style ancestry: fixed ability boosts, a flaw, free boosts, base HP/size/speed, and heritages. */
data class PfAncestry(
    val id: String,
    val name: String,
    val hp: Int,
    val size: String,
    val speed: Int,
    val boosts: List<PfAbility> = emptyList(),
    val flaw: PfAbility? = null,
    /** Number of free boosts the ancestry grants (a "free" ancestry like Human gives 2). */
    val freeBoosts: Int = 1,
    val heritages: List<String> = emptyList(),
)

/** A background: two boost options (pick one) + a free boost, a trained skill, and a Lore. */
data class PfBackground(
    val id: String,
    val name: String,
    val boostOptions: List<PfAbility>,
    val trainedSkill: PfSkill,
    val lore: String,
)

/** A class: key-ability options, HP/level, and the level-1 proficiency ranks it grants. */
data class PfClass(
    val id: String,
    val name: String,
    val keyAbilityOptions: List<PfAbility>,
    val hpPerLevel: Int,
    val perception: Proficiency,
    val saves: Map<Save, Proficiency>,
    val classDC: Proficiency,
    val unarmored: Proficiency,
    /** How many additional trained skills the class grants (plus INT modifier). */
    val trainedSkills: Int,
    val classSkills: List<PfSkill>,
    val tradition: SpellTradition? = null,
)

object PathfinderCatalog {
    val ancestries: List<PfAncestry> = listOf(
        PfAncestry("human", "Human", 8, "Medium", 25, freeBoosts = 2, heritages = listOf("skilled-heritage", "versatile-heritage")),
        PfAncestry("dwarf", "Dwarf", 10, "Medium", 20, boosts = listOf(PfAbility.CONSTITUTION, PfAbility.WISDOM), flaw = PfAbility.CHARISMA, heritages = listOf("ancient-blooded", "death-warden", "forge-blessed", "rock-dwarf")),
        PfAncestry("elf", "Elf", 6, "Medium", 30, boosts = listOf(PfAbility.DEXTERITY, PfAbility.INTELLIGENCE), flaw = PfAbility.CONSTITUTION, heritages = listOf("ancient-elf", "cavern-elf", "seer-elf", "whisper-elf")),
        PfAncestry("gnome", "Gnome", 8, "Small", 25, boosts = listOf(PfAbility.CONSTITUTION, PfAbility.CHARISMA), flaw = PfAbility.STRENGTH, heritages = listOf("chameleon-gnome", "fey-touched-gnome", "sensate-gnome", "wellspring-gnome")),
        PfAncestry("goblin", "Goblin", 6, "Small", 25, boosts = listOf(PfAbility.DEXTERITY, PfAbility.CHARISMA), flaw = PfAbility.WISDOM, heritages = listOf("charhide-goblin", "irongut-goblin", "razortooth-goblin", "snow-goblin")),
        PfAncestry("halfling", "Halfling", 6, "Small", 25, boosts = listOf(PfAbility.DEXTERITY, PfAbility.WISDOM), flaw = PfAbility.STRENGTH, heritages = listOf("gutsy-halfling", "hillock-halfling", "nomadic-halfling", "twilight-halfling")),
        PfAncestry("leshy", "Leshy", 8, "Small", 25, boosts = listOf(PfAbility.CONSTITUTION, PfAbility.WISDOM), flaw = PfAbility.INTELLIGENCE, heritages = listOf("fruit-leshy", "fungus-leshy", "gourd-leshy", "leaf-leshy")),
        PfAncestry("orc", "Orc", 10, "Medium", 25, boosts = listOf(PfAbility.STRENGTH), freeBoosts = 1, heritages = listOf("badlands-orc", "battle-ready-orc", "deep-orc", "hold-scarred-orc")),
    )

    val backgrounds: List<PfBackground> = listOf(
        PfBackground("acolyte", "Acolyte", listOf(PfAbility.INTELLIGENCE, PfAbility.WISDOM), PfSkill.RELIGION, "Scribing Lore"),
        PfBackground("criminal", "Criminal", listOf(PfAbility.DEXTERITY, PfAbility.INTELLIGENCE), PfSkill.STEALTH, "Underworld Lore"),
        PfBackground("noble", "Noble", listOf(PfAbility.INTELLIGENCE, PfAbility.CHARISMA), PfSkill.SOCIETY, "Genealogy Lore"),
        PfBackground("scholar", "Scholar", listOf(PfAbility.INTELLIGENCE, PfAbility.WISDOM), PfSkill.ARCANA, "Academia Lore"),
        PfBackground("warrior", "Warrior", listOf(PfAbility.STRENGTH, PfAbility.CONSTITUTION), PfSkill.INTIMIDATION, "Warfare Lore"),
        PfBackground("hunter", "Hunter", listOf(PfAbility.DEXTERITY, PfAbility.WISDOM), PfSkill.SURVIVAL, "Tanning Lore"),
    )

    val classes: List<PfClass> = listOf(
        PfClass("fighter", "Fighter", listOf(PfAbility.STRENGTH, PfAbility.DEXTERITY), 10, Proficiency.EXPERT, mapOf(Save.FORTITUDE to Proficiency.EXPERT, Save.REFLEX to Proficiency.EXPERT, Save.WILL to Proficiency.TRAINED), Proficiency.TRAINED, Proficiency.TRAINED, 3, listOf(PfSkill.ATHLETICS, PfSkill.ACROBATICS, PfSkill.INTIMIDATION, PfSkill.SURVIVAL)),
        PfClass("rogue", "Rogue", listOf(PfAbility.DEXTERITY), 8, Proficiency.EXPERT, mapOf(Save.FORTITUDE to Proficiency.TRAINED, Save.REFLEX to Proficiency.EXPERT, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 7, PfSkill.entries),
        PfClass("cleric", "Cleric", listOf(PfAbility.WISDOM), 8, Proficiency.TRAINED, mapOf(Save.FORTITUDE to Proficiency.TRAINED, Save.REFLEX to Proficiency.TRAINED, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 2, listOf(PfSkill.RELIGION, PfSkill.DIPLOMACY, PfSkill.MEDICINE), SpellTradition.DIVINE),
        PfClass("wizard", "Wizard", listOf(PfAbility.INTELLIGENCE), 6, Proficiency.TRAINED, mapOf(Save.FORTITUDE to Proficiency.TRAINED, Save.REFLEX to Proficiency.TRAINED, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 2, listOf(PfSkill.ARCANA, PfSkill.CRAFTING, PfSkill.SOCIETY), SpellTradition.ARCANE),
        PfClass("sorcerer", "Sorcerer", listOf(PfAbility.CHARISMA), 6, Proficiency.TRAINED, mapOf(Save.FORTITUDE to Proficiency.TRAINED, Save.REFLEX to Proficiency.TRAINED, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 2, listOf(PfSkill.ARCANA, PfSkill.DECEPTION, PfSkill.DIPLOMACY, PfSkill.INTIMIDATION), SpellTradition.ARCANE),
        PfClass("ranger", "Ranger", listOf(PfAbility.STRENGTH, PfAbility.DEXTERITY), 10, Proficiency.EXPERT, mapOf(Save.FORTITUDE to Proficiency.EXPERT, Save.REFLEX to Proficiency.EXPERT, Save.WILL to Proficiency.TRAINED), Proficiency.TRAINED, Proficiency.TRAINED, 4, listOf(PfSkill.NATURE, PfSkill.SURVIVAL, PfSkill.ATHLETICS, PfSkill.STEALTH)),
        PfClass("champion", "Champion", listOf(PfAbility.STRENGTH, PfAbility.DEXTERITY), 10, Proficiency.TRAINED, mapOf(Save.FORTITUDE to Proficiency.EXPERT, Save.REFLEX to Proficiency.TRAINED, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 2, listOf(PfSkill.RELIGION, PfSkill.DIPLOMACY, PfSkill.INTIMIDATION)),
        PfClass("bard", "Bard", listOf(PfAbility.CHARISMA), 8, Proficiency.EXPERT, mapOf(Save.FORTITUDE to Proficiency.TRAINED, Save.REFLEX to Proficiency.TRAINED, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 4, listOf(PfSkill.OCCULTISM, PfSkill.PERFORMANCE, PfSkill.DIPLOMACY, PfSkill.ACROBATICS), SpellTradition.OCCULT),
    )

    fun ancestry(id: String): PfAncestry? = ancestries.firstOrNull { it.id == id }
    fun background(id: String): PfBackground? = backgrounds.firstOrNull { it.id == id }
    fun pfClass(id: String): PfClass? = classes.firstOrNull { it.id == id }
}

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

/**
 * A class's defining subclass choice (a Rogue's racket, a Sorcerer's bloodline).
 * [grantedSkill] is a skill the subclass trains, folded into the character's
 * trained skills at creation; null when the subclass grants none.
 */
data class PfSubclass(
    val id: String,
    val name: String,
    val summary: String,
    val grantedSkill: PfSkill? = null,
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
    /** Level-1 defense proficiency per armor category. */
    val armorProf: Map<ArmorCategory, Proficiency> = mapOf(ArmorCategory.UNARMORED to Proficiency.TRAINED),
    /** Level-1 attack proficiency per weapon category. */
    val weaponProf: Map<WeaponCategory, Proficiency> = mapOf(WeaponCategory.SIMPLE to Proficiency.TRAINED),
    /** What the class calls its subclass ("Racket", "Bloodline") — used as the wizard step title. */
    val subclassLabel: String = "Subclass",
    val subclasses: List<PfSubclass> = emptyList(),
) {
    /** Whether the class commits to a subclass at creation (the Fighter and Monk don't). */
    val hasSubclass: Boolean get() = subclasses.isNotEmpty()
}

/** Defense ranks for a martial class: trained in all worn armor categories. */
private val MARTIAL_ARMOR = mapOf(
    ArmorCategory.UNARMORED to Proficiency.TRAINED, ArmorCategory.LIGHT to Proficiency.TRAINED,
    ArmorCategory.MEDIUM to Proficiency.TRAINED, ArmorCategory.HEAVY to Proficiency.TRAINED,
)
private val LIGHT_MEDIUM_ARMOR = mapOf(
    ArmorCategory.UNARMORED to Proficiency.TRAINED, ArmorCategory.LIGHT to Proficiency.TRAINED, ArmorCategory.MEDIUM to Proficiency.TRAINED,
)
private val CASTER_ARMOR = mapOf(ArmorCategory.UNARMORED to Proficiency.TRAINED)
private val ALL_WEAPONS = mapOf(WeaponCategory.SIMPLE to Proficiency.TRAINED, WeaponCategory.MARTIAL to Proficiency.TRAINED)
private val FIGHTER_WEAPONS = mapOf(WeaponCategory.SIMPLE to Proficiency.EXPERT, WeaponCategory.MARTIAL to Proficiency.EXPERT)
private val SIMPLE_WEAPONS = mapOf(WeaponCategory.SIMPLE to Proficiency.TRAINED)

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
        PfClass("fighter", "Fighter", listOf(PfAbility.STRENGTH, PfAbility.DEXTERITY), 10, Proficiency.EXPERT, mapOf(Save.FORTITUDE to Proficiency.EXPERT, Save.REFLEX to Proficiency.EXPERT, Save.WILL to Proficiency.TRAINED), Proficiency.TRAINED, Proficiency.TRAINED, 3, listOf(PfSkill.ATHLETICS, PfSkill.ACROBATICS, PfSkill.INTIMIDATION, PfSkill.SURVIVAL), armorProf = MARTIAL_ARMOR, weaponProf = FIGHTER_WEAPONS),
        // Fighters have no level-1 subclass — they're defined by their feats (mirrors iOS).
        PfClass("rogue", "Rogue", listOf(PfAbility.DEXTERITY), 8, Proficiency.EXPERT, mapOf(Save.FORTITUDE to Proficiency.TRAINED, Save.REFLEX to Proficiency.EXPERT, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 7, PfSkill.entries, armorProf = LIGHT_MEDIUM_ARMOR, weaponProf = SIMPLE_WEAPONS,
            subclassLabel = "Racket",
            subclasses = listOf(
                PfSubclass("thief", "Thief", "Add Dexterity to sneak-attack damage instead of Strength; nimble and precise."),
                PfSubclass("scoundrel", "Scoundrel", "Feint with Deception to make foes off-guard to your allies too."),
                PfSubclass("ruffian", "Ruffian", "Use Strength and medium armor; sneak attack with simple weapons."),
                PfSubclass("mastermind", "Mastermind", "Recall Knowledge to make foes off-guard; an investigator at heart."),
            )),
        PfClass("cleric", "Cleric", listOf(PfAbility.WISDOM), 8, Proficiency.TRAINED, mapOf(Save.FORTITUDE to Proficiency.TRAINED, Save.REFLEX to Proficiency.TRAINED, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 2, listOf(PfSkill.RELIGION, PfSkill.DIPLOMACY, PfSkill.MEDICINE), SpellTradition.DIVINE, armorProf = CASTER_ARMOR, weaponProf = SIMPLE_WEAPONS,
            subclassLabel = "Doctrine",
            subclasses = listOf(
                PfSubclass("cloistered", "Cloistered Cleric", "A scholar of the faith — faster spell progression and a domain-fed font of magic."),
                PfSubclass("warpriest", "Warpriest", "A martial servant — armor and weapon training to fight on the front line."),
            )),
        PfClass("wizard", "Wizard", listOf(PfAbility.INTELLIGENCE), 6, Proficiency.TRAINED, mapOf(Save.FORTITUDE to Proficiency.TRAINED, Save.REFLEX to Proficiency.TRAINED, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 2, listOf(PfSkill.ARCANA, PfSkill.CRAFTING, PfSkill.SOCIETY), SpellTradition.ARCANE, armorProf = CASTER_ARMOR, weaponProf = SIMPLE_WEAPONS,
            subclassLabel = "School",
            subclasses = listOf(
                PfSubclass("ars-grammatica", "School of Ars Grammatica", "The linguistic and mathematical underpinnings of magic — runes and wards."),
                PfSubclass("battle-magic", "School of Battle Magic", "The combat application of arcane power."),
                PfSubclass("civic-wizardry", "School of Civic Wizardry", "Magic in service of construction and community."),
                PfSubclass("mentalism", "School of Mentalism", "Figments, illusions, and the warping of minds."),
                PfSubclass("protean-form", "School of Protean Form", "Changing objects and creatures between forms."),
                PfSubclass("the-boundary", "School of the Boundary", "Magic that reaches beyond the world to summon and bind."),
                PfSubclass("unified-magical-theory", "School of Unified Magical Theory", "The generalist — drawing flexibly from every school via your bonded item."),
            )),
        PfClass("sorcerer", "Sorcerer", listOf(PfAbility.CHARISMA), 6, Proficiency.TRAINED, mapOf(Save.FORTITUDE to Proficiency.TRAINED, Save.REFLEX to Proficiency.TRAINED, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 2, listOf(PfSkill.ARCANA, PfSkill.DECEPTION, PfSkill.DIPLOMACY, PfSkill.INTIMIDATION), SpellTradition.ARCANE, armorProf = CASTER_ARMOR, weaponProf = SIMPLE_WEAPONS,
            subclassLabel = "Bloodline",
            // Bloodline-resolved traditions are deferred (the Android sorcerer casts arcane);
            // the bloodline's trained skill folds in at creation.
            subclasses = listOf(
                PfSubclass("aberrant", "Aberrant", "Something unknowable from beyond whispers in your blood — occult magic.", grantedSkill = PfSkill.OCCULTISM),
                PfSubclass("angelic", "Angelic", "Celestial light runs in your veins — divine magic.", grantedSkill = PfSkill.RELIGION),
                PfSubclass("demonic", "Demonic", "Abyssal fury fuels your magic — divine tradition.", grantedSkill = PfSkill.INTIMIDATION),
                PfSubclass("diabolic", "Diabolic", "Infernal contracts and pride — divine tradition.", grantedSkill = PfSkill.DECEPTION),
                PfSubclass("draconic", "Draconic", "The blood of dragons — arcane magic and a fearsome presence.", grantedSkill = PfSkill.ARCANA),
                PfSubclass("elemental", "Elemental", "An elemental influence imbues your blood — primal magic.", grantedSkill = PfSkill.NATURE),
                PfSubclass("fey", "Fey", "The First World stirs in you — primal magic and trickery.", grantedSkill = PfSkill.NATURE),
                PfSubclass("hag", "Hag", "A hag's bargain marks your line — occult magic.", grantedSkill = PfSkill.DECEPTION),
                PfSubclass("imperial", "Imperial", "Ancient arcane lineage — arcane magic in the blood.", grantedSkill = PfSkill.ARCANA),
                PfSubclass("undead", "Undead", "The grave's touch lingers in you — divine magic.", grantedSkill = PfSkill.INTIMIDATION),
            )),
        PfClass("ranger", "Ranger", listOf(PfAbility.STRENGTH, PfAbility.DEXTERITY), 10, Proficiency.EXPERT, mapOf(Save.FORTITUDE to Proficiency.EXPERT, Save.REFLEX to Proficiency.EXPERT, Save.WILL to Proficiency.TRAINED), Proficiency.TRAINED, Proficiency.TRAINED, 4, listOf(PfSkill.NATURE, PfSkill.SURVIVAL, PfSkill.ATHLETICS, PfSkill.STEALTH), armorProf = MARTIAL_ARMOR, weaponProf = ALL_WEAPONS,
            subclassLabel = "Hunter's Edge",
            subclasses = listOf(
                PfSubclass("flurry", "Flurry", "Reduce the multiple-attack penalty against your hunted prey."),
                PfSubclass("precision", "Precision", "Deal extra precision damage to your prey on your first hit each turn."),
                PfSubclass("outwit", "Outwit", "Bonuses to skills, AC, and avoiding deception against your prey."),
            )),
        PfClass("champion", "Champion", listOf(PfAbility.STRENGTH, PfAbility.DEXTERITY), 10, Proficiency.TRAINED, mapOf(Save.FORTITUDE to Proficiency.EXPERT, Save.REFLEX to Proficiency.TRAINED, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 2, listOf(PfSkill.RELIGION, PfSkill.DIPLOMACY, PfSkill.INTIMIDATION), armorProf = MARTIAL_ARMOR, weaponProf = ALL_WEAPONS,
            subclassLabel = "Cause",
            subclasses = listOf(
                PfSubclass("justice", "Justice", "Uphold the law and punish transgressors in your deity's name."),
                PfSubclass("liberation", "Liberation", "Free others from bondage, tyranny, and restriction."),
                PfSubclass("obedience", "Obedience", "Uphold societal order, proper hierarchy, and right conduct."),
                PfSubclass("redemption", "Redemption", "Compassion and forgiveness — redeem the wicked (holy)."),
                PfSubclass("grandeur", "Grandeur", "A shining celestial example, promoting virtue and humility (holy)."),
                PfSubclass("desecration", "Desecration", "Self-interest and the subversion of purity (unholy)."),
                PfSubclass("iniquity", "Iniquity", "Dishonor and the shattering of false kindness (unholy)."),
            )),
        PfClass("bard", "Bard", listOf(PfAbility.CHARISMA), 8, Proficiency.EXPERT, mapOf(Save.FORTITUDE to Proficiency.TRAINED, Save.REFLEX to Proficiency.TRAINED, Save.WILL to Proficiency.EXPERT), Proficiency.TRAINED, Proficiency.TRAINED, 4, listOf(PfSkill.OCCULTISM, PfSkill.PERFORMANCE, PfSkill.DIPLOMACY, PfSkill.ACROBATICS), SpellTradition.OCCULT, armorProf = CASTER_ARMOR, weaponProf = SIMPLE_WEAPONS,
            subclassLabel = "Muse",
            subclasses = listOf(
                PfSubclass("enigma", "Enigma", "A muse of mystery and knowledge — gain the True Strike composition and a scholar's edge."),
                PfSubclass("maestro", "Maestro", "A muse of music and performance — gain the Lingering Composition feat and inspiring works."),
                PfSubclass("polymath", "Polymath", "A muse of versatility — Versatile Performance and a broad repertoire."),
                PfSubclass("warrior", "Warrior", "A muse of battle — the Courageous Anthem and martial training."),
            )),
    )

    fun ancestry(id: String): PfAncestry? = ancestries.firstOrNull { it.id == id }
    fun background(id: String): PfBackground? = backgrounds.firstOrNull { it.id == id }
    fun pfClass(id: String): PfClass? = classes.firstOrNull { it.id == id }
}

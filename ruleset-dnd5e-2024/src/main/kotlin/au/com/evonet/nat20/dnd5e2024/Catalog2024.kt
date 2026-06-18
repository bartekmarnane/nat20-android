package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.dnd5e.core.Ability
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The bundled D&D 5e (2024) SRD 5.2 catalogues (A21). Loaded from module
 * resources via the classloader, so the same code serves JVM tests and the app.
 * Port of the iOS `Catalog2024`; the spell list is the SRD 5.2 (CC BY 4.0,
 * Open5e `srd-2024`) set of 339 spells. Species/backgrounds/classes follow.
 */
object DnD5e2024Catalog {
    private val json = Json { ignoreUnknownKeys = true }

    /** SRD 5.2 spells (339), sorted by level then name, for the 2024 Spell Library + caster picker. */
    val spellLibrary: List<Spell2024> by lazy {
        load("Spells2024.json", Spell2024.serializer()).sortedWith(compareBy({ it.level }, { it.name }))
    }

    private val spellsById by lazy { spellLibrary.associateBy { it.id } }
    fun spell(id: String): Spell2024? = spellsById[id]

    /**
     * Hand-authored SRD 5.2 seed catalogues for the creation wizard — species
     * (traits only, no ability bumps), backgrounds (which grant the ability
     * bumps + an origin feat + skills), and classes. A representative set;
     * full coverage + a move to bundled JSON is a follow-up (mirrors iOS's
     * `Catalog2024` "phase H" note).
     */
    val species: List<Species2024> = listOf(
        Species2024("human", "Human", "Versatile and ambitious; a free skill and Heroic Inspiration on a long rest.", traits = listOf("Resourceful", "Skillful", "Versatile")),
        Species2024("elf", "Elf", "Fey lineage with keen senses and a Trance instead of sleep.", speed = 30, darkvision = 60, traits = listOf("Darkvision 60 ft", "Fey Ancestry", "Keen Senses", "Trance")),
        Species2024("dwarf", "Dwarf", "Stout and stoic; resilient against poison, with tremorsense.", speed = 30, darkvision = 120, traits = listOf("Darkvision 120 ft", "Dwarven Resilience", "Dwarven Toughness", "Stonecunning")),
        Species2024("halfling", "Halfling", "Small, lucky, and nimble; hard to frighten.", speed = 30, traits = listOf("Brave", "Halfling Nimbleness", "Luck", "Naturally Stealthy")),
        Species2024("dragonborn", "Dragonborn", "Draconic ancestry granting a breath weapon and damage resistance.", traits = listOf("Breath Weapon", "Damage Resistance", "Darkvision 60 ft", "Draconic Flight")),
        Species2024("gnome", "Gnome", "Small and clever, with an innate knack against magic.", speed = 30, darkvision = 60, traits = listOf("Darkvision 60 ft", "Gnomish Cunning")),
        Species2024("orc", "Orc", "Powerful and relentless; a surge of speed and an endurance against death.", speed = 30, darkvision = 120, traits = listOf("Adrenaline Rush", "Darkvision 120 ft", "Relentless Endurance")),
        Species2024("tiefling", "Tiefling", "Fiendish lineage carrying darkvision and an innate spell legacy.", speed = 30, darkvision = 60, traits = listOf("Darkvision 60 ft", "Fiendish Legacy", "Otherworldly Presence")),
    )

    val backgrounds: List<Background2024> = listOf(
        Background2024("acolyte", "Acolyte", "A servant of a temple, versed in rites and lore.", listOf(Ability.INTELLIGENCE, Ability.WISDOM, Ability.CHARISMA), "magic-initiate-cleric", listOf("insight", "religion")),
        Background2024("criminal", "Criminal", "A practiced lawbreaker with an underworld contact.", listOf(Ability.DEXTERITY, Ability.CONSTITUTION, Ability.INTELLIGENCE), "alert", listOf("sleightOfHand", "stealth")),
        Background2024("sage", "Sage", "A scholar steeped in arcane and historical knowledge.", listOf(Ability.CONSTITUTION, Ability.INTELLIGENCE, Ability.WISDOM), "magic-initiate-wizard", listOf("arcana", "history")),
        Background2024("soldier", "Soldier", "A trained veteran of war and discipline.", listOf(Ability.STRENGTH, Ability.DEXTERITY, Ability.CONSTITUTION), "savage-attacker", listOf("athletics", "intimidation")),
        Background2024("artisan", "Artisan", "A skilled crafter who built a trade by hand.", listOf(Ability.STRENGTH, Ability.DEXTERITY, Ability.INTELLIGENCE), "crafter", listOf("investigation", "persuasion")),
        Background2024("guide", "Guide", "A wilderness expert at home far from cities.", listOf(Ability.DEXTERITY, Ability.CONSTITUTION, Ability.WISDOM), "magic-initiate-druid", listOf("stealth", "survival")),
    )

    val classes: List<CharacterClass2024> = listOf(
        CharacterClass2024("barbarian", "Barbarian", 12, listOf(Ability.STRENGTH, Ability.CONSTITUTION), 2, listOf("animalHandling", "athletics", "intimidation", "nature", "perception", "survival")),
        CharacterClass2024("bard", "Bard", 8, listOf(Ability.DEXTERITY, Ability.CHARISMA), 3, ALL_SKILLS, caster = "full"),
        CharacterClass2024("cleric", "Cleric", 8, listOf(Ability.WISDOM, Ability.CHARISMA), 2, listOf("history", "insight", "medicine", "persuasion", "religion"), caster = "full"),
        CharacterClass2024("druid", "Druid", 8, listOf(Ability.INTELLIGENCE, Ability.WISDOM), 2, listOf("arcana", "animalHandling", "insight", "medicine", "nature", "perception", "religion", "survival"), caster = "full"),
        CharacterClass2024("fighter", "Fighter", 10, listOf(Ability.STRENGTH, Ability.CONSTITUTION), 2, listOf("acrobatics", "animalHandling", "athletics", "history", "insight", "intimidation", "perception", "survival")),
        CharacterClass2024("monk", "Monk", 8, listOf(Ability.STRENGTH, Ability.DEXTERITY), 2, listOf("acrobatics", "athletics", "history", "insight", "religion", "stealth")),
        CharacterClass2024("paladin", "Paladin", 10, listOf(Ability.WISDOM, Ability.CHARISMA), 2, listOf("athletics", "insight", "intimidation", "medicine", "persuasion", "religion"), caster = "half"),
        CharacterClass2024("ranger", "Ranger", 10, listOf(Ability.STRENGTH, Ability.DEXTERITY), 3, listOf("animalHandling", "athletics", "insight", "investigation", "nature", "perception", "stealth", "survival"), caster = "half"),
        CharacterClass2024("rogue", "Rogue", 8, listOf(Ability.DEXTERITY, Ability.INTELLIGENCE), 4, listOf("acrobatics", "athletics", "deception", "insight", "intimidation", "investigation", "perception", "performance", "persuasion", "sleightOfHand", "stealth")),
        CharacterClass2024("sorcerer", "Sorcerer", 6, listOf(Ability.CONSTITUTION, Ability.CHARISMA), 2, listOf("arcana", "deception", "insight", "intimidation", "persuasion", "religion"), caster = "full"),
        CharacterClass2024("warlock", "Warlock", 8, listOf(Ability.WISDOM, Ability.CHARISMA), 2, listOf("arcana", "deception", "history", "intimidation", "investigation", "nature", "religion"), caster = "pact"),
        CharacterClass2024("wizard", "Wizard", 6, listOf(Ability.INTELLIGENCE, Ability.WISDOM), 2, listOf("arcana", "history", "insight", "investigation", "medicine", "nature", "religion"), caster = "full"),
    )

    fun species(id: String): Species2024? = species.firstOrNull { it.id == id }
    fun background(id: String): Background2024? = backgrounds.firstOrNull { it.id == id }
    fun characterClass(id: String): CharacterClass2024? = classes.firstOrNull { it.id == id }

    private fun <T> load(path: String, element: kotlinx.serialization.KSerializer<T>): List<T> {
        val text = javaClass.getResourceAsStream("/catalogues/$path")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Missing bundled 2024 catalogue: $path")
        return json.decodeFromString(ListSerializer(element), text)
    }
}

/** Bard/Rogue draw from every skill; ids match the shared 18-skill vocabulary. */
private val ALL_SKILLS = listOf(
    "acrobatics", "animalHandling", "arcana", "athletics", "deception", "history", "insight",
    "intimidation", "investigation", "medicine", "nature", "perception", "performance",
    "persuasion", "religion", "sleightOfHand", "stealth", "survival",
)

/** A 2024 species: traits only (no ability bumps — those come from the background). */
data class Species2024(
    val id: String,
    val name: String,
    val description: String = "",
    val size: String = "Medium",
    val speed: Int = 30,
    val darkvision: Int = 0,
    val traits: List<String> = emptyList(),
)

/** A 2024 background: grants the ability bumps ([abilityOptions]), an [originFeat], and skill proficiencies. */
data class Background2024(
    val id: String,
    val name: String,
    val description: String = "",
    /** The three abilities the +2/+1 (or +1/+1/+1) may be allocated across. */
    val abilityOptions: List<Ability>,
    val originFeat: String,
    val skills: List<String>,
)

/** A 2024 class for the creation wizard — hit die, saving throws, skill picks (subclass is always chosen at level 3). */
data class CharacterClass2024(
    val id: String,
    val name: String,
    val hitDie: Int,
    val savingThrows: List<Ability>,
    val skillChoiceCount: Int,
    val skillChoiceFrom: List<String>,
    /** "full" / "half" / "pact"; null = non-caster. */
    val caster: String? = null,
) {
    val isCaster: Boolean get() = caster != null
}

/** A D&D 5e (2024) spell — the SRD 5.2 shape (flat school/classes strings + rules text). */
@Serializable
data class Spell2024(
    val id: String,
    val name: String,
    val level: Int = 0,
    val school: String = "",
    val classes: List<String> = emptyList(),
    val concentration: Boolean = false,
    val ritual: Boolean = false,
    val description: String = "",
) {
    val isCantrip: Boolean get() = level == 0
    val levelLabel: String get() = if (level == 0) "Cantrip" else "Level $level"
    val classNames: List<String> get() = classes.map { it.replaceFirstChar(Char::uppercase) }
}

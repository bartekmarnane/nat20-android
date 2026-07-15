package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Loads and caches the bundled 5e (2014) SRD catalogues from module resources.
 * Resource streams work identically under JVM unit tests and on Android, so the
 * wizard and tests read the same data. Port of the iOS catalogue accessors.
 */
object DnD5eCatalog {
    private val json = Json { ignoreUnknownKeys = true }

    /** The bundled SRD races; the public [races] overlays homebrews on top. */
    private val srdRaces: List<Race> by lazy { load("Races/5e-SRD-Races.json", Race.serializer()) }

    /**
     * SRD races plus the player's homebrews ([CustomRaceLibrary]), appended
     * after the bundled list sorted case-insensitively by name. Homebrews are
     * always included — immune to any future source filtering.
     */
    val races: List<Race>
        get() = srdRaces + CustomRaceLibrary.races.value.sortedBy { it.name.lowercase() }
    val classes: List<CharacterClass> by lazy {
        load("Classes/5e-SRD-Classes.json", CharacterClass.serializer())
    }
    val backgrounds: List<Background> by lazy {
        load("Backgrounds/5e-SRD-Backgrounds.json", Background.serializer())
    }
    val skills: List<Skill> get() = SkillCatalog.all
    /**
     * The full 2014 spell library, level then name: SRD 5.1 (319, CC BY 4.0)
     * merged with the per-source supplement files — the licensed PHB gap set
     * (the ~41 PHB spells outside the SRD, e.g. Aura of Vitality and the
     * smites) plus the Xanathar's/Tasha's drops. Mirrors iOS `SpellCatalog`'s
     * layered per-publisher files.
     */
    val spellLibrary: List<Spell> by lazy {
        val files = listOf(
            "Spells/5e-SRD-Spells.json",
            "Spells/PHB-Spells.json",
            "Spells/Xanathars-Spells.json",
            "Spells/Tashas-Spells.json",
        )
        files.flatMap { load(it, Spell.serializer()) }
            .sortedWith(compareBy({ it.level }, { it.name }))
    }

    /** SRD weapons, name-sorted (A10 inventory). */
    val weapons: List<WeaponCatalogueEntry> by lazy {
        load("Inventory/5e-SRD-Weapons.json", WeaponCatalogueEntry.serializer()).sortedBy { it.name }
    }

    /** SRD armor, name-sorted. */
    val armor: List<ArmorCatalogueEntry> by lazy {
        load("Inventory/5e-SRD-Armor.json", ArmorCatalogueEntry.serializer()).sortedBy { it.name }
    }

    /** SRD shields / ammunition / potions / tools / mundane gear, name-sorted. */
    val gear: List<GearCatalogueEntry> by lazy {
        load("Inventory/5e-SRD-Gear.json", GearCatalogueEntry.serializer()).sortedBy { it.name }
    }

    fun race(id: String): Race? = races.firstOrNull { it.id == id }
    fun characterClass(id: String): CharacterClass? = classes.firstOrNull { it.id == id }
    fun background(id: String): Background? = backgrounds.firstOrNull { it.id == id }
    fun skill(id: String): Skill? = SkillCatalog.byId(id)
    fun weapon(id: String): WeaponCatalogueEntry? = weapons.firstOrNull { it.id == id }
    fun armorPiece(id: String): ArmorCatalogueEntry? = armor.firstOrNull { it.id == id }
    fun gearPiece(id: String): GearCatalogueEntry? = gear.firstOrNull { it.id == id }

    private fun <T> load(path: String, element: kotlinx.serialization.KSerializer<T>): List<T> {
        val text = javaClass.getResourceAsStream("/catalogues/$path")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Missing bundled catalogue: $path")
        return json.decodeFromString(ListSerializer(element), text)
    }
}

/** The 18 SRD skills and their governing ability. */
object SkillCatalog {
    val all: List<Skill> = listOf(
        // Ids match the bundled catalogue vocabulary (camelCase for the two
        // multi-word skills: animalHandling, sleightOfHand).
        Skill("acrobatics", "Acrobatics", Ability.DEXTERITY),
        Skill("animalHandling", "Animal Handling", Ability.WISDOM),
        Skill("arcana", "Arcana", Ability.INTELLIGENCE),
        Skill("athletics", "Athletics", Ability.STRENGTH),
        Skill("deception", "Deception", Ability.CHARISMA),
        Skill("history", "History", Ability.INTELLIGENCE),
        Skill("insight", "Insight", Ability.WISDOM),
        Skill("intimidation", "Intimidation", Ability.CHARISMA),
        Skill("investigation", "Investigation", Ability.INTELLIGENCE),
        Skill("medicine", "Medicine", Ability.WISDOM),
        Skill("nature", "Nature", Ability.INTELLIGENCE),
        Skill("perception", "Perception", Ability.WISDOM),
        Skill("performance", "Performance", Ability.CHARISMA),
        Skill("persuasion", "Persuasion", Ability.CHARISMA),
        Skill("religion", "Religion", Ability.INTELLIGENCE),
        Skill("sleightOfHand", "Sleight of Hand", Ability.DEXTERITY),
        Skill("stealth", "Stealth", Ability.DEXTERITY),
        Skill("survival", "Survival", Ability.WISDOM),
    )

    private val byId = all.associateBy { it.id }
    fun byId(id: String): Skill? = byId[id]
}

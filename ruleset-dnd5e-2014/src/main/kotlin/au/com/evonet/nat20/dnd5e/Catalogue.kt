package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Catalogue models for the 5e (2014) content the creation wizard reads (A8).
 * These mirror the **pre-transformed** SRD JSON bundled under
 * `resources/catalogues/` (already cleaned by the iOS build pipeline), so they
 * decode directly. Only the fields the wizard + codex need are declared;
 * `ignoreUnknownKeys` drops the rest, and the long tail (subclasses, features,
 * equipment, spellcasting detail) is wired as the content steps need it (A10+).
 */

@Serializable
data class Race(
    val id: String,
    val name: String,
    val description: String = "",
    val abilityScoreIncreases: AbilityScoreIncreases = AbilityScoreIncreases(),
    val size: String = "medium",
    val speed: Int = 30,
    val darkvision: Int = 0,
    /** Known tongues, e.g. `["Common", "Dwarvish"]` (race detail card, A8 restyle). */
    val languages: List<String> = emptyList(),
    /** Named racial traits shown as quote blocks on the race detail card. */
    val traits: List<RaceTraitEntry> = emptyList(),
    /** Typical adulthood age in years; null when the JSON omits it. */
    val typicalAge: Int? = null,
    val maxAge: Int? = null,
) {
    /** Fixed ability bonuses as an [Ability]-keyed map (unknown keys dropped). */
    fun abilityBonuses(): Map<Ability, Int> =
        abilityScoreIncreases.fixed.mapNotNull { bump ->
            Ability.fromKey(bump.ability)?.let { it to bump.amount }
        }.toMap()
}

@Serializable
data class AbilityScoreIncreases(
    val fixed: List<AbilityBonus> = emptyList(),
)

@Serializable
data class AbilityBonus(val ability: String, val amount: Int)

/** A named racial trait (the JSON's `traits` entries; extra keys dropped). */
@Serializable
data class RaceTraitEntry(val name: String, val body: String = "")

@Serializable
data class CharacterClass(
    val id: String,
    val name: String,
    val description: String = "",
    val hitDie: Int = 8,
    val savingThrows: List<String> = emptyList(),
    val skillChoiceCount: Int = 0,
    val skillChoiceFrom: List<String> = emptyList(),
    /** Casting block; null ⇒ non-caster. Only [Caster.kind] is needed for now. */
    val caster: Caster? = null,
    /** Subclass options (id + name); chosen at [subclassLevel] in the level-up wizard (A11). */
    val subclasses: List<Subclass> = emptyList(),
    /** The class level at which a subclass is chosen (3 for most; 1 cleric, 2 wizard/druid, …). */
    val subclassLevel: Int = 3,
    /** Level-1 starting kit as catalogue-id + quantity pairs (wizard Equipment step, A8). */
    val starterEquipment: List<StarterEquipmentEntry> = emptyList(),
    /** Class features by level (name + body), shown on the class detail card. */
    val features: List<ClassFeatureEntry> = emptyList(),
) {
    /** Saving-throw-proficient abilities as [Ability] values. */
    fun savingThrowAbilities(): List<Ability> = savingThrows.mapNotNull(Ability::fromKey)

    val isCaster: Boolean get() = caster?.kind?.let { it.isNotEmpty() && it != "none" } ?: false
}

/** One starter-kit line: an equipment catalogue id and how many (JSON `starterEquipment`). */
@Serializable
data class StarterEquipmentEntry(val id: String, val quantity: Int = 1)

/** A class feature granted at a level (JSON `features`; extra keys dropped). */
@Serializable
data class ClassFeatureEntry(val level: Int = 1, val name: String, val body: String = "")

/** A subclass option for a class (the features list is decoded later, when A19 wires traits). */
@Serializable
data class Subclass(
    val id: String,
    val name: String,
    val description: String = "",
)

@Serializable
data class Caster(
    /** "full" / "half" / "third" / "pact". */
    val kind: String? = null,
)

@Serializable
data class Background(
    val id: String,
    val name: String,
    val description: String = "",
    val grantedSkills: List<String> = emptyList(),
    /** Tool proficiencies granted, e.g. Herbalism kit; empty for most. */
    val toolProficiencies: List<String> = emptyList(),
    /** Number of extra languages of the player's choice. */
    val languageCount: Int = 0,
    /** Free-text starting-gear lines ("A holy symbol", "5 sticks of incense", …). */
    val equipment: List<String> = emptyList(),
    /** The background's flavour feature (name + body). */
    val feature: BackgroundFeature? = null,
)

/** A background's flavour feature (JSON `feature`). */
@Serializable
data class BackgroundFeature(val name: String, val body: String = "")

/** A 5e skill and the ability that governs it. Static (no SRD JSON ships skills). */
data class Skill(val id: String, val name: String, val ability: Ability)

/**
 * A spell for the read-only Spell Library (A10). Decodes the raw 5e-database
 * spell shape (nested `school`/`classes` objects, list `desc`); only the
 * browse-relevant fields are declared, the rest dropped via `ignoreUnknownKeys`.
 */
@Serializable
data class Spell(
    val index: String,
    val name: String,
    val level: Int = 0,
    val school: NamedRef = NamedRef(),
    val desc: List<String> = emptyList(),
    @SerialName("higher_level") val higherLevel: List<String> = emptyList(),
    @SerialName("casting_time") val castingTime: String = "",
    val range: String = "",
    val duration: String = "",
    val components: List<String> = emptyList(),
    val concentration: Boolean = false,
    val ritual: Boolean = false,
    val classes: List<NamedRef> = emptyList(),
) {
    val schoolName: String get() = school.name
    val description: String get() = desc.joinToString("\n\n")
    val higherLevelText: String get() = higherLevel.joinToString("\n\n")
    val classNames: List<String> get() = classes.map { it.name }
    val levelLabel: String get() = if (level == 0) "Cantrip" else "Level $level"
}

/** A `{ index, name, url }` reference in the SRD JSON; only [name] is needed. */
@Serializable
data class NamedRef(val name: String = "")

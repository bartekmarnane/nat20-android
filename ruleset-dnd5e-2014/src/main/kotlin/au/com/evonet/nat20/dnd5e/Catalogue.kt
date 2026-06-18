package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
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
) {
    /** Saving-throw-proficient abilities as [Ability] values. */
    fun savingThrowAbilities(): List<Ability> = savingThrows.mapNotNull(Ability::fromKey)

    val isCaster: Boolean get() = caster?.kind?.let { it.isNotEmpty() && it != "none" } ?: false
}

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
)

/** A 5e skill and the ability that governs it. Static (no SRD JSON ships skills). */
data class Skill(val id: String, val name: String, val ability: Ability)

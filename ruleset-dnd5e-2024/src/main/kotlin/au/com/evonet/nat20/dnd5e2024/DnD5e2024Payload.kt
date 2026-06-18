package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.CastingProgression
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.dnd5e.core.DeathSaves
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.SpellSlotTable
import au.com.evonet.nat20.domain.CharacterPayload
import kotlinx.serialization.Serializable

/**
 * D&D 5e (2024) character data. A **sibling** of the 2014 payload, not a subtype:
 * the two editions stay isolated (no cross-edition migration). It reuses every
 * edition-agnostic value type from `:ruleset-dnd5e-core` (`AbilityScores`,
 * `DeathSaves`, `ActiveEffect`, `Coin`, `CastingProgression`, `SpellSlotTable`),
 * which is the whole point of A18 — the engine is shared, only the edition shape
 * differs. Port of the iOS `DnD5e2024Payload`.
 *
 * 2024 differences modelled here: **species** (not race; traits only), ability
 * bumps from **background** (`backgroundASI`), the numeric **−2/level**
 * exhaustion track, explicit `originFeat` / `fightingStyle` slots, and
 * `weaponMasteries`. (Inventory, the full feat/advancement system, and the 6-tab
 * codex are follow-up slices — see the README.)
 */
@Serializable
data class DnD5e2024Payload(
    val species: String = "",
    val classes: List<ClassEntry2024> = emptyList(),
    val background: String = "",
    /** Final scores after the background bumps are applied. */
    val abilityScores: AbilityScores = AbilityScores(),
    /** Starting scores before any bonuses (persisted so creation can be re-edited). */
    val baseAbilityScores: AbilityScores = AbilityScores(),
    /** The background's +2/+1 (or +1/+1/+1) ability bumps. */
    val backgroundASI: Map<Ability, Int> = emptyMap(),
    val maxHp: Int = MINIMUM_MAX_HP,
    val currentHp: Int = maxHp,
    val temporaryHp: Int = 0,
    val hitDiceSpent: Int = 0,
    val deathSaves: DeathSaves = DeathSaves.cleared,
    val activeEffects: List<ActiveEffect> = emptyList(),
    val activeConditions: List<String> = emptyList(),
    /** 2024 exhaustion: a 0–6 numeric track (−2/level to d20 tests, −5 ft/level speed, level 6 = death). */
    val exhaustionLevel: Int = 0,
    val hasInspiration: Boolean = false,
    val concentratingOn: String? = null,
    val currentSpellSlots: Map<Int, Int> = emptyMap(),
    val preparedSpells: Map<String, List<String>> = emptyMap(),
    val cantripsKnown: List<String> = emptyList(),
    val coins: Map<Coin, Int> = emptyMap(),
    /** Skill proficiencies the class granted (background skills fold in via derived stats). */
    val skillProficiencies: List<String> = emptyList(),
    /** The level-1 origin feat granted by the background. */
    val originFeat: String? = null,
    /** General/Epic feats taken at advancement levels. */
    val chosenFeats: List<String> = emptyList(),
    /** Explicit Fighting Style slot (Fighter/Paladin/Ranger). */
    val fightingStyle: String? = null,
    /** Weapon-mastery picks (martial classes): cleave/graze/nick/push/sap/slow/topple/vex. */
    val weaponMasteries: List<String> = emptyList(),
    val alignment: String? = null,
    val initiative: Int? = null,
) : CharacterPayload {

    val level: Int get() = maxOf(MIN_LEVEL, classes.sumOf { it.level })
    val characterClass: String get() = classes.firstOrNull()?.classId ?: ""

    /** Total hit dice = level; spent dice come off the top. */
    val maxHitDice: Int get() = level
    val currentHitDice: Int get() = maxOf(0, maxHitDice - hitDiceSpent)

    /** Effective ability score = base + active-effect deltas, floored by the highest `.abilitySet`. */
    fun effectiveScore(ability: Ability): Int {
        val base = abilityScores.score(ability)
        var delta = 0
        var setValue: Int? = null
        for (effect in activeEffects) {
            for (modifier in effect.modifiers) {
                when (modifier) {
                    is EffectModifier.AbilityDelta -> if (modifier.ability == ability) delta += modifier.value
                    is EffectModifier.AbilitySet -> if (modifier.ability == ability) setValue = maxOf(setValue ?: Int.MIN_VALUE, modifier.value)
                    else -> {}
                }
            }
        }
        val withDelta = base + delta
        return if (setValue != null) maxOf(setValue, withDelta) else withDelta
    }

    val effectiveAbilityScores: AbilityScores
        get() = Ability.entries.fold(abilityScores) { scores, ability -> scores.with(ability, effectiveScore(ability)) }

    /** Maximum regular spell slots, combining caster classes against the full-caster table (warlock excluded). */
    val maxSpellSlots: Map<Int, Int>
        get() {
            val casterLevel = classes.sumOf { entry ->
                when (CastingProgression.forClass(entry.classId)) {
                    CastingProgression.FULL -> entry.level
                    CastingProgression.HALF -> entry.level / 2
                    CastingProgression.THIRD -> entry.level / 3
                    CastingProgression.WARLOCK, CastingProgression.NONE -> 0
                }
            }
            return if (casterLevel > 0) SpellSlotTable.slots(CastingProgression.FULL, casterLevel) else emptyMap()
        }

    /** Downed at 0 HP and still rolling death saves. */
    val isDying: Boolean get() = currentHp == 0 && !deathSaves.isStable && !deathSaves.isDead
    val isDead: Boolean get() = deathSaves.isDead || Exhaustion2024.isFatal(exhaustionLevel)

    companion object {
        const val MIN_LEVEL: Int = 1
        const val MAX_LEVEL: Int = 20
        const val MINIMUM_MAX_HP: Int = 1
    }
}

/** A class line for a (possibly multiclass) 2024 character — subclass is chosen at level 3 for every class. */
@Serializable
data class ClassEntry2024(
    val classId: String,
    val level: Int = 1,
    val subclass: String? = null,
)

/** The 2024 exhaustion model: numeric penalties that scale with the level (vs. 2014's per-level effect table). */
object Exhaustion2024 {
    const val MAX: Int = 6
    fun clamp(level: Int): Int = level.coerceIn(0, MAX)
    fun isFatal(level: Int): Boolean = level >= MAX
    /** −2 to every d20 test per level of exhaustion. */
    fun d20Modifier(level: Int): Int = -2 * clamp(level)
    /** −5 ft of speed per level of exhaustion. */
    fun speedPenaltyFeet(level: Int): Int = 5 * clamp(level)
}

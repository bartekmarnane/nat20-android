package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.dnd5e.core.ACOverrideFormula
import au.com.evonet.nat20.dnd5e.core.ACOverrideRequirement
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.EffectSource

/**
 * Folds [DnD5e2024Payload.activeEffects] (+ passive class effects) into live
 * stats — the 2024 side of the A17 effect model, reusing the same `-core` types.
 */

val DnD5e2024Payload.allEffects: List<ActiveEffect>
    get() = activeEffects + passiveClassEffects() + SpeciesTraits2024.passiveEffects(species) + passiveFeatEffects()

/** Every feat the character holds — origin + fighting style + advancement-chosen. */
val DnD5e2024Payload.allFeats: Set<String>
    get() = (listOfNotNull(originFeat, fightingStyle) + chosenFeats).toSet()

/** Feats with a clean always-on rider that folds into derived stats (currently Defense → +1 AC while armored). */
fun DnD5e2024Payload.passiveFeatEffects(): List<ActiveEffect> = buildList {
    if ("defense" in allFeats && equippedArmor != null) {
        add(
            ActiveEffect(
                id = "passive:feat:defense",
                name = "Defense",
                source = EffectSource.Feature("feat:defense"),
                modifiers = listOf(EffectModifier.AcBonus(1)),
                duration = EffectDuration.UntilCancelled,
            ),
        )
    }
}

/** Skill proficiencies the character has, including those auto-granted by a species trait (Elf Keen Senses). */
val DnD5e2024Payload.effectiveSkillProficiencies: List<String>
    get() = (skillProficiencies + SpeciesTraits2024.grantedSkills(species)).distinct()

/** Barbarian / Monk Unarmored Defense as always-on AC overrides (gated on no armor by the calculator). */
fun DnD5e2024Payload.passiveClassEffects(): List<ActiveEffect> = buildList {
    for (entry in classes) when (entry.classId.lowercase()) {
        "barbarian" -> add(unarmored("barbarian", ACOverrideFormula.BaseDexAbility(10, Ability.CONSTITUTION, ACOverrideRequirement.NONE)))
        "monk" -> add(unarmored("monk", ACOverrideFormula.BaseDexAbility(10, Ability.WISDOM, ACOverrideRequirement.NO_SHIELD)))
    }
}

private fun unarmored(classId: String, formula: ACOverrideFormula) = ActiveEffect(
    id = "passive:$classId:unarmored-defense",
    name = "Unarmored Defense",
    source = EffectSource.Feature("$classId-unarmored-defense"),
    modifiers = listOf(EffectModifier.AcOverride(formula)),
    duration = EffectDuration.UntilCancelled,
)

fun DnD5e2024Payload.temporarySaveBonus(ability: Ability): Int =
    activeEffects.sumOf { e -> e.modifiers.sumOf { if (it is EffectModifier.SaveBonus && (it.ability == null || it.ability == ability)) it.value else 0 } }

fun DnD5e2024Payload.temporarySkillBonus(skillId: String): Int =
    activeEffects.sumOf { e -> e.modifiers.sumOf { if (it is EffectModifier.SkillBonus && it.skillId == skillId) it.value else 0 } }

val DnD5e2024Payload.effectAttackBonus: Int
    get() = activeEffects.sumOf { e -> e.modifiers.sumOf { if (it is EffectModifier.AttackBonus) it.value else 0 } }

val DnD5e2024Payload.effectDamageBonus: Int
    get() = activeEffects.sumOf { e -> e.modifiers.sumOf { if (it is EffectModifier.DamageBonus) it.value else 0 } }

val DnD5e2024Payload.effectiveDamageResistances: Set<String>
    get() = allEffects.flatMap { e -> e.modifiers.mapNotNull { (it as? EffectModifier.DamageResistance)?.type?.trim()?.lowercase()?.takeIf { t -> t.isNotEmpty() } } }.toSet()

/**
 * Computes a 2024 character's Armor Class from worn armor + shield + active
 * effects. Rules (mirrors the 2014 `ArmorClassCalculator`, on the 2024 payload):
 * - **Worn armor** ([DnD5e2024Payload.equippedArmor] resolved in [Armors2024]):
 *   base + DEX by the category cap (light uncapped / medium +2 / heavy ignores).
 *   AC overrides (Unarmored Defense, Mage Armor) do **not** apply while armored.
 * - **Unarmored**: the highest of 10 + DEX and any `.acOverride` (Barbarian
 *   10+DEX+CON, Monk 10+DEX+WIS gated on no shield), per RAW.
 * - **Shield** ([DnD5e2024Payload.hasShield]) adds a flat +2.
 * - Every effect `.acBonus` (Shield spell +5, Shield of Faith +2) stacks on top.
 */
object ArmorClass2024 {
    /** One contributing line in the breakdown — a label and its signed value. */
    data class Row(val label: String, val value: Int)

    /** The full breakdown; [total] is the sum of all [rows]. */
    data class Breakdown(val rows: List<Row>) {
        val total: Int get() = rows.sumOf { it.value }
    }

    fun compute(payload: DnD5e2024Payload): Breakdown {
        val dexMod = AbilityScores.modifier(payload.effectiveScore(Ability.DEXTERITY))
        val rows = mutableListOf<Row>()

        val worn = payload.equippedArmor?.let { Armors2024.armor(it) }
        if (worn != null) {
            rows += Row(worn.name, worn.baseAC)
            when (val cap = worn.category.dexCap) {
                0 -> rows += Row("DEX (ignored by heavy)", 0)
                null -> rows += Row("DEX", dexMod)
                else -> {
                    val label = if (dexMod > cap) "DEX (capped at +$cap)" else "DEX"
                    rows += Row(label, minOf(cap, dexMod))
                }
            }
        } else {
            var bestBase = 10
            var baseLabel = "Unarmored"
            for (effect in payload.allEffects) for (m in effect.modifiers) {
                if (m !is EffectModifier.AcOverride) continue
                if (m.formula.requirement == ACOverrideRequirement.NO_SHIELD && payload.hasShield) continue
                val candidate = when (val f = m.formula) {
                    is ACOverrideFormula.BaseDex -> f.base
                    is ACOverrideFormula.BaseDexAbility -> f.base + AbilityScores.modifier(payload.effectiveScore(f.ability))
                }
                if (candidate > bestBase) { bestBase = candidate; baseLabel = effect.name }
            }
            rows += Row(baseLabel, bestBase)
            rows += Row("DEX", dexMod)
        }

        if (payload.hasShield) rows += Row("Shield", Armors2024.SHIELD_BONUS)

        for (effect in payload.allEffects) for (m in effect.modifiers) {
            if (m is EffectModifier.AcBonus && m.value != 0) rows += Row(effect.name, m.value)
        }
        return Breakdown(rows)
    }
}

/** Convenience: just the total AC for a 2024 character. */
val DnD5e2024Payload.armorClass: Int
    get() = ArmorClass2024.compute(this).total

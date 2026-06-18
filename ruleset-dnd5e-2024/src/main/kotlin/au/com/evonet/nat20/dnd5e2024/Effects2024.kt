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
    get() = activeEffects + passiveClassEffects()

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
    get() = activeEffects.flatMap { e -> e.modifiers.mapNotNull { (it as? EffectModifier.DamageResistance)?.type?.trim()?.lowercase()?.takeIf { t -> t.isNotEmpty() } } }.toSet()

/** Armor Class for a 2024 character (no inventory yet): 10 + DEX, beaten by the best AC override, plus flat riders. */
val DnD5e2024Payload.armorClass: Int
    get() {
        val dexMod = AbilityScores.modifier(effectiveScore(Ability.DEXTERITY))
        var base = 10
        for (effect in allEffects) for (m in effect.modifiers) {
            if (m !is EffectModifier.AcOverride) continue
            val candidate = when (val f = m.formula) {
                is ACOverrideFormula.BaseDex -> f.base
                is ACOverrideFormula.BaseDexAbility -> f.base + AbilityScores.modifier(effectiveScore(f.ability))
            }
            if (candidate > base) base = candidate
        }
        val bonuses = allEffects.sumOf { e -> e.modifiers.sumOf { if (it is EffectModifier.AcBonus) it.value else 0 } }
        return base + dexMod + bonuses
    }

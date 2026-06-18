package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.ACOverrideFormula
import au.com.evonet.nat20.dnd5e.core.ACOverrideRequirement
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.EffectModifier

/**
 * Computes a character's Armor Class from equipped inventory **and active
 * effects** (A17). Port of the iOS `ArmorClassCalculator`.
 *
 * Rules modelled here:
 * - At most one equipped [ItemKind.ARMOR] sets the base + DEX-cap rule.
 * - Unarmored: the highest of 10 + DEX and any `.acOverride` (Mage Armor 13+DEX,
 *   Barbarian 10+DEX+CON, Monk 10+DEX+WIS gated on no shield). Overrides only
 *   apply while no armor is worn (RAW).
 * - Every equipped item's [InventoryItem.acBonus] (shields, cloaks) and every
 *   effect `.acBonus` (Shield spell +5, Shield of Faith +2) stack on top.
 * - DEX/secondary abilities use the **effective** score (so Cat's Grace etc. count).
 */
object ArmorClassCalculator {

    /** One contributing line in the AC breakdown — a label and its signed value. */
    data class Row(val label: String, val value: Int)

    /** The full breakdown; [total] is the sum of all [rows]. */
    data class Breakdown(val rows: List<Row>) {
        val total: Int get() = rows.sumOf { it.value }
    }

    fun compute(payload: DnD5ePayload): Breakdown {
        val dexMod = AbilityScores.modifier(payload.effectiveScore(Ability.DEXTERITY))
        val effects = payload.allEffects
        val rows = mutableListOf<Row>()

        val equippedArmor = payload.inventory.firstOrNull { it.equipped && it.kind == ItemKind.ARMOR }
        val armorProps = equippedArmor?.armor
        if (equippedArmor != null && armorProps != null) {
            rows += Row(equippedArmor.name, armorProps.baseAC)
            when {
                armorProps.dexCap == 0 -> rows += Row("DEX (ignored by heavy)", 0)
                armorProps.dexCap != null -> {
                    val capped = minOf(armorProps.dexCap, dexMod)
                    val label = if (dexMod > armorProps.dexCap) "DEX (capped at +${armorProps.dexCap})" else "DEX"
                    rows += Row(label, capped)
                }
                else -> rows += Row("DEX", dexMod)
            }
        } else {
            // Unarmored: 10 + DEX floor, beaten by the best applicable AC override.
            val shieldEquipped = payload.inventory.any { it.equipped && it.kind == ItemKind.SHIELD }
            var bestBase = 10
            var baseLabel = "Unarmored"
            for (effect in effects) {
                for (modifier in effect.modifiers) {
                    if (modifier !is EffectModifier.AcOverride) continue
                    if (modifier.formula.requirement == ACOverrideRequirement.NO_SHIELD && shieldEquipped) continue
                    val candidate = overrideBase(modifier.formula, payload)
                    if (candidate > bestBase) {
                        bestBase = candidate
                        baseLabel = effect.name
                    }
                }
            }
            rows += Row(baseLabel, bestBase)
            rows += Row("DEX", dexMod)
        }

        // Flat AC bonuses from every other equipped item (shields, cloaks, rings).
        for (item in payload.inventory) {
            if (!item.equipped || item.kind == ItemKind.ARMOR) continue
            val bonus = item.acBonus ?: continue
            if (bonus == 0) continue
            rows += Row(item.name, bonus)
        }

        // Flat `.acBonus` riders from effects (Shield +5, Shield of Faith +2, Haste +2).
        for (effect in effects) {
            for (modifier in effect.modifiers) {
                if (modifier is EffectModifier.AcBonus && modifier.value != 0) {
                    rows += Row(effect.name, modifier.value)
                }
            }
        }

        return Breakdown(rows)
    }

    /** Convenience: just the total AC. */
    fun armorClass(payload: DnD5ePayload): Int = compute(payload).total

    /** The base AC an override formula yields (the secondary ability is folded in here). */
    private fun overrideBase(formula: ACOverrideFormula, payload: DnD5ePayload): Int = when (formula) {
        is ACOverrideFormula.BaseDex -> formula.base
        is ACOverrideFormula.BaseDexAbility -> formula.base + AbilityScores.modifier(payload.effectiveScore(formula.ability))
    }
}

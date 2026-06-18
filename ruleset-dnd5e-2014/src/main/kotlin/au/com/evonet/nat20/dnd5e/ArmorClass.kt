package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores

/**
 * Computes a character's Armor Class from equipped inventory. Port of the iOS
 * `ArmorClassCalculator`, minus the active-effects layer (Mage Armor, Barbarian /
 * Monk Unarmored Defense, Shield spell) which lands with effects (A17) — for now
 * the unarmored base is the flat 10 + DEX.
 *
 * Rules modelled here:
 * - At most one equipped [ItemKind.ARMOR] sets the base + DEX-cap rule.
 * - With no armor: 10 + DEX.
 * - Every other equipped item's [InventoryItem.acBonus] stacks (shields +2, etc.).
 */
object ArmorClassCalculator {

    /** One contributing line in the AC breakdown — a label and its signed value. */
    data class Row(val label: String, val value: Int)

    /** The full breakdown; [total] is the sum of all [rows]. */
    data class Breakdown(val rows: List<Row>) {
        val total: Int get() = rows.sumOf { it.value }
    }

    fun compute(payload: DnD5ePayload): Breakdown {
        val dexMod = AbilityScores.modifier(payload.abilityScores.score(Ability.DEXTERITY))
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
            rows += Row("Unarmored", 10)
            rows += Row("DEX", dexMod)
        }

        // Flat AC bonuses from every other equipped item (shields, cloaks, rings).
        for (item in payload.inventory) {
            if (!item.equipped || item.kind == ItemKind.ARMOR) continue
            val bonus = item.acBonus ?: continue
            if (bonus == 0) continue
            rows += Row(item.name, bonus)
        }

        return Breakdown(rows)
    }

    /** Convenience: just the total AC. */
    fun armorClass(payload: DnD5ePayload): Int = compute(payload).total
}

package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.ACOverrideFormula
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.EffectSource
import au.com.evonet.nat20.dnd5e.core.RestKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Worn magic items whose riders the engine folds for itself — the Robe of the
 * Archmagi (sets the unarmored AC base, boosts spell DC + spell attack) and the
 * Ring of Protection (flat AC + saves). Mirrors the iOS `Worn magic items` suite.
 */
class WornMagicItemTests {

    private fun robe(base: Int = 15, equipped: Boolean = true) = InventoryItem(
        id = "robe",
        name = "Robe of the Archmagi",
        kind = ItemKind.WONDROUS,
        equipped = equipped,
        advantageOn = listOf("saves vs spells and magical effects"),
        acOverride = ACOverrideFormula.BaseDex(base),
        spellDcBonus = 2,
        spellAttackBonus = 2,
    )

    private fun ring(bonus: Int = 2) = InventoryItem(
        id = "ring",
        name = "Ring of Protection +$bonus",
        kind = ItemKind.WONDROUS,
        equipped = true,
        acBonus = bonus,
        saveBonus = bonus,
    )

    private fun sorcerer(
        dex: Int = 14,
        cha: Int = 18,
        level: Int = 5,
        inventory: List<InventoryItem> = emptyList(),
        effects: List<ActiveEffect> = emptyList(),
    ) = DnD5ePayload(
        classes = listOf(ClassEntry("sorcerer", level)),
        abilityScores = AbilityScores(dexterity = dex, charisma = cha),
        inventory = inventory,
        activeEffects = effects,
    )

    @Test
    fun `robe sets the unarmored base and the ring stacks on top`() {
        val payload = sorcerer(inventory = listOf(robe(), ring()))
        val breakdown = ArmorClassCalculator.compute(payload)
        // Robe 15 + DEX 2 + Ring 2 = 19
        assertEquals(19, breakdown.total)
        assertTrue(breakdown.rows.any { it.label == "Robe of the Archmagi" && it.value == 15 })
        assertTrue(breakdown.rows.any { it.label == "Ring of Protection +2" && it.value == 2 })
    }

    @Test
    fun `item override and Mage Armor pick the best base, never both`() {
        val mageArmor = ActiveEffect(
            id = "mage-armor",
            name = "Mage Armor",
            source = EffectSource.Spell("mage-armor"),
            modifiers = listOf(EffectModifier.AcOverride(ACOverrideFormula.BaseDex(13))),
            duration = EffectDuration.UntilRest(RestKind.LONG),
        )
        val payload = sorcerer(inventory = listOf(robe()), effects = listOf(mageArmor))
        // Robe 15 beats Mage Armor 13 — 15 + DEX 2, not 15 + 13.
        assertEquals(17, ArmorClassCalculator.armorClass(payload))
    }

    @Test
    fun `armor suppresses the robe's override, RAW`() {
        val leather = InventoryItem(
            id = "leather",
            name = "Leather",
            kind = ItemKind.ARMOR,
            armor = ArmorProperties(ArmorProperties.Kind.LIGHT, baseAC = 11, dexCap = null),
            equipped = true,
        )
        val payload = sorcerer(inventory = listOf(robe(), leather))
        // Leather 11 + DEX 2 = 13 — the robe's base drops out entirely.
        assertEquals(13, ArmorClassCalculator.armorClass(payload))
    }

    @Test
    fun `an unequipped robe contributes nothing`() {
        val payload = sorcerer(inventory = listOf(robe(equipped = false)))
        assertEquals(12, ArmorClassCalculator.armorClass(payload), "bare 10 + DEX 2")
        assertEquals(0, payload.equippedItemSpellDcBonus)
        assertEquals(0, payload.equippedItemSpellAttackBonus)
    }

    @Test
    fun `spell DC and spell attack pick up worn-item riders`() {
        val payload = sorcerer(inventory = listOf(robe(), ring()))
        val stat = payload.castingStats().single()
        // CHA 18 → +4, level 5 → PB +3. DC 8+3+4 = 15, +2 robe = 17; attack 7 +2 = 9.
        assertEquals(Ability.CHARISMA, stat.ability)
        assertEquals(17, stat.saveDC)
        assertEquals(9, stat.attackBonus)
    }

    @Test
    fun `the ring's save bonus reaches every saving throw`() {
        val bare = sorcerer()
        val worn = sorcerer(inventory = listOf(robe(), ring()))
        assertEquals(2, worn.equippedItemSaveBonus(Ability.WISDOM))
        assertEquals(
            bare.savingThrowBonus(Ability.WISDOM) + 2,
            worn.savingThrowBonus(Ability.WISDOM),
        )
        assertEquals(listOf("Ring of Protection +2" to 2), worn.equippedSaveBonusSources(Ability.WISDOM))
    }

    @Test
    fun `equipped items contribute resistances and advantage tags`() {
        val cloak = InventoryItem(
            id = "cloak",
            name = "Cloak of the Salamander",
            kind = ItemKind.WONDROUS,
            equipped = true,
            damageResistances = listOf("Fire"),
        )
        val stowed = cloak.copy(id = "stowed", name = "Spare", equipped = false, damageResistances = listOf("cold"))
        val payload = sorcerer(inventory = listOf(robe(), cloak, stowed))
        assertEquals(setOf("fire"), payload.effectiveDamageResistances, "lower-cased; the stowed cloak is ignored")
        assertTrue(payload.advantageDescriptors.contains("saves vs spells and magical effects"))
    }

    @Test
    fun `wondrous items are equippable — the riders are worn-only`() {
        assertTrue(ItemKind.WONDROUS.isEquippable)
        assertFalse(ItemKind.GEAR.isEquippable)
        assertFalse(ItemKind.POTION.isEquippable)
    }

    @Test
    fun `riders survive a payload round-trip`() {
        val payload = sorcerer(inventory = listOf(robe(), ring()))
        val decoded = DnD5eRuleset().decodePayload(DnD5eRuleset().encodePayload(payload)) as DnD5ePayload
        val robe = decoded.inventory.first { it.name.startsWith("Robe") }
        assertEquals(ACOverrideFormula.BaseDex(15), robe.acOverride)
        assertEquals(2, robe.spellDcBonus)
        assertEquals(2, robe.spellAttackBonus)
    }

    @ParameterizedTest
    @CsvSource(
        "Robe of the Archmagi,WONDROUS",
        "Ring of Protection +2,WONDROUS",
        "Boots of Elvenkind,WONDROUS",
        "Wand of Fireballs,WONDROUS",
        "+1 Longsword,WEAPON",
        "Dragonscale Breastplate,ARMOR",
        "Animated Shield,SHIELD",
        "Potion of Giant Strength,POTION",
        "Scroll of Revivify,SCROLL",
    )
    fun `homebrew names infer a kind from the noun`(name: String, expected: ItemKind) {
        assertEquals(expected, ItemKind.inferred(name))
    }

    @Test
    fun `unrecognisable names stay null so the caller can default to gear`() {
        assertNull(ItemKind.inferred("Bewildering Trinket"))
        assertNull(ItemKind.inferred(""))
    }
}

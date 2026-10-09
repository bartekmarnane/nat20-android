package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.DeathSaves
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CampaignEndAndMagicTests {
    @Test
    fun `campaign end sheds in-play state but keeps the build`() {
        val p = DnD5ePayload(
            classes = listOf(ClassEntry("cleric", 3)), maxHp = 20, currentHp = 14,
            temporaryHp = 4, concentratingOn = "Bless", activeConditions = listOf("prone"),
            deathSaves = DeathSaves(successes = 1, failures = 2), initiative = 14,
        )
        val after = DnD5eRuleset().payloadAfterCampaignEnd(p) as DnD5ePayload
        assertEquals(0, after.temporaryHp)
        assertNull(after.concentratingOn)
        assertTrue(after.activeConditions.isEmpty())
        assertEquals(DeathSaves.cleared, after.deathSaves)
        assertNull(after.initiative)
        assertEquals(14, after.currentHp)
        assertEquals(3, after.level)
    }

    @Test
    fun `a plus-one weapon adds to attack and damage`() {
        val sword = InventoryItem(
            id = "sword", name = "Longsword +1", kind = ItemKind.WEAPON, equipped = true, attackBonus = 1,
            weapon = WeaponProperties(kind = WeaponProperties.Kind.MELEE, damageDice = "1d8", damageType = "slashing"),
        )
        val p = DnD5ePayload(
            classes = listOf(ClassEntry("fighter", 1)),
            abilityScores = AbilityScores(strength = 16),
            inventory = listOf(sword),
        )
        val attack = AttackMath.forWeapon(sword, p)!!
        assertEquals(3 + 2 + 1, attack.attackBonuses.sumOf { it.value })
        assertEquals(3 + 1, attack.damageBonuses.sumOf { it.value })
        assertTrue(attack.attackBonuses.any { it.label == "Magic" })
    }
}

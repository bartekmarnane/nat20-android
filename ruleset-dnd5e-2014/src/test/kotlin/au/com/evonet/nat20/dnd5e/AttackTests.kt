package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.AttackOutcome
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val ruleset = DnD5eRuleset()

private fun fighter(str: Int = 16, dex: Int = 12, level: Int = 5, inventory: List<InventoryItem> = emptyList()): Character =
    Character.new(
        "Bron", ruleset,
        DnD5ePayload(
            classes = listOf(ClassEntry("fighter", level)),
            abilityScores = AbilityScores(strength = str, dexterity = dex),
            inventory = inventory,
        ),
        NOW,
    )

private fun Character.payload() = payload as DnD5ePayload

class DiceParseTests {
    @Test
    fun `parses standard weapon notation`() {
        assertEquals(RollSpec.d(1, 8), RollSpec.parse("1d8"))
        assertEquals(RollSpec.d(2, 6), RollSpec.parse("2d6"))
        assertEquals(RollSpec(1, 10, 2), RollSpec.parse("1d10+2"))
        assertNull(RollSpec.parse("sword"))
    }

    @Test
    fun `crit doubles the dice count`() {
        assertEquals(RollSpec.d(2, 8), RollSpec.d(1, 8).critDoubled())
    }
}

class AttackMathTests {
    @Test
    fun `a longsword uses STR and proficiency`() {
        val sword = DnD5eCatalog.weapon("longsword")!!.makeItem(equipped = true)
        val attack = AttackMath.forWeapon(sword, fighter(str = 16, level = 5).payload())!!
        // STR 16 → +3, fighter L5 proficiency +3.
        assertEquals("STR", attack.abilityLabel)
        assertEquals(listOf(3, 3), attack.attackBonuses.map { it.value })
        assertEquals(RollSpec.d(1, 8), attack.damageSpec)
        assertEquals(3, attack.damageBonus?.value)
        assertEquals("slashing", attack.damageType)
    }

    @Test
    fun `a finesse weapon takes the better of STR and DEX`() {
        val dagger = DnD5eCatalog.weapon("dagger")!!.makeItem(equipped = true)
        val attack = AttackMath.forWeapon(dagger, fighter(str = 10, dex = 18).payload())!!
        assertEquals("DEX", attack.abilityLabel) // DEX +4 beats STR +0
    }

    @Test
    fun `a ranged weapon uses DEX`() {
        val bow = DnD5eCatalog.weapon("shortbow")!!.makeItem(equipped = true)
        val attack = AttackMath.forWeapon(bow, fighter(str = 16, dex = 14).payload())!!
        assertEquals("DEX", attack.abilityLabel)
    }

    @Test
    fun `equipped-weapons and initiative bonus read off the payload`() {
        val sword = DnD5eCatalog.weapon("longsword")!!.makeItem(equipped = true)
        val p = fighter(dex = 14, inventory = listOf(sword)).payload()
        assertEquals(1, p.equippedWeapons.size)
        assertEquals(2, p.initiativeBonus) // DEX 14 → +2
    }
}

class AttackIntentTests {
    @Test
    fun `a hit logs damage`() {
        val result = MakeAttack("Longsword", attackTotal = 18, outcome = AttackOutcome.HIT, damage = 11, damageType = "Slashing").applyTo(fighter(), ruleset)
        assertEquals("Hit with Longsword for 11 slashing damage (rolled 18)", result.event.summary)
    }

    @Test
    fun `a miss drops any damage`() {
        val result = MakeAttack("Longsword", attackTotal = 7, outcome = AttackOutcome.MISS, damage = 11).applyTo(fighter(), ruleset)
        assertEquals("Attacked with Longsword — missed (rolled 7)", result.event.summary)
        assertNull((result.event as AttackEvent).damage)
    }

    @Test
    fun `a crit reads as a critical hit with target`() {
        val result = MakeAttack("Dagger", attackTotal = 24, outcome = AttackOutcome.CRITICAL, damage = 9, damageType = "Piercing", target = "the goblin").applyTo(fighter(), ruleset)
        assertEquals("Critical hit the goblin with Dagger for 9 piercing damage!", result.event.summary)
    }

    @Test
    fun `a blank weapon is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            MakeAttack("  ", attackTotal = 10, outcome = AttackOutcome.HIT).applyTo(fighter(), ruleset)
        }
    }

    @Test
    fun `the attacker is unchanged`() {
        val before = fighter()
        val after = MakeAttack("Longsword", 18, AttackOutcome.HIT, 11).applyTo(before, ruleset).character
        assertEquals(before.payload, after.payload)
    }
}

class InitiativeIntentTests {
    @Test
    fun `setting and clearing initiative`() {
        var c = SetInitiative(17).applyTo(fighter(), ruleset).character
        assertEquals(17, c.payload().initiative)
        val cleared = SetInitiative(null).applyTo(c, ruleset)
        assertNull(cleared.character.payload().initiative)
        assertEquals("Cleared initiative", cleared.event.summary)
    }
}

class CombatCodecTests {
    @Test
    fun `payload with initiative round-trips`() {
        val payload = DnD5ePayload(classes = listOf(ClassEntry("fighter", 5)), initiative = 17)
        assertEquals(payload, ruleset.decodePayload(ruleset.encodePayload(payload)))
    }

    @Test
    fun `attack and initiative events round-trip`() {
        val events = listOf(
            AttackEvent("Longsword", 18, AttackOutcome.HIT, damage = 11, damageType = "Slashing", target = "orc"),
            InitiativeEvent(17),
        )
        for (event in events) {
            val typeId = ruleset.eventTypeId(event)
            assertFalse(typeId == "dnd5e.unknown")
            assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
        }
    }
}

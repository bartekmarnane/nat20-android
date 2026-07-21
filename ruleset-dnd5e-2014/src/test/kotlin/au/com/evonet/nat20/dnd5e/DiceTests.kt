package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.DiceRoller
import au.com.evonet.nat20.dnd5e.core.Keep
import au.com.evonet.nat20.dnd5e.core.ManualRollEntry
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollResult
import au.com.evonet.nat20.dnd5e.core.RollSpec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

private fun result(dice: List<Int>, spec: RollSpec, bonuses: List<RollBonus> = emptyList()) = RollResult(
    dice = dice,
    keptIndices = spec.keptIndices(dice),
    modifier = spec.modifier,
    bonuses = bonuses,
    faces = spec.faces,
)

class KeepRuleTests {
    @Test
    fun `advantage keeps the higher d20`() {
        val r = result(listOf(7, 18), RollSpec.advantage())
        assertEquals(setOf(1), r.keptIndices)
        assertEquals(18, r.keptSum)
    }

    @Test
    fun `disadvantage keeps the lower d20`() {
        val r = result(listOf(7, 18), RollSpec.disadvantage())
        assertEquals(setOf(0), r.keptIndices)
        assertEquals(7, r.keptSum)
    }

    @Test
    fun `ability roll drops the lowest of four d6`() {
        val r = result(listOf(2, 5, 4, 6), RollSpec.abilityRoll) // drop the 2
        assertEquals(15, r.keptSum)
        assertEquals(3, r.keptDice.size)
    }
}

class RollTotalTests {
    @Test
    fun `bonuses own the breakdown and supersede the flat modifier`() {
        val spec = RollSpec.d(1, 20, mod = 99) // modifier ignored when bonuses present
        val r = result(listOf(10), spec, listOf(RollBonus("DEX", 3), RollBonus("Proficiency", 2)))
        assertEquals(15, r.total) // 10 + 3 + 2
        assertEquals(5, r.bonusSum)
    }

    @Test
    fun `flat modifier applies when there are no bonuses`() {
        val r = result(listOf(8), RollSpec.d(1, 20, mod = 3))
        assertEquals(11, r.total)
    }

    @Test
    fun `crit and fumble are flagged only for d20 rolls`() {
        assertTrue(result(listOf(20), RollSpec.d(1, 20)).isNatural20)
        assertTrue(result(listOf(1), RollSpec.d(1, 20)).isNatural1)
        assertFalse(result(listOf(6), RollSpec.d(1, 6)).isNatural20) // d6, not a d20
        assertNull(result(listOf(6), RollSpec.d(1, 6)).naturalD20)
    }

    @Test
    fun `advantage crit reads the kept die`() {
        // Rolled a 1 and a 20 with advantage → keeps 20 → crit, not fumble.
        val r = result(listOf(1, 20), RollSpec.advantage())
        assertTrue(r.isNatural20)
        assertFalse(r.isNatural1)
    }
}

class DiceRollerTests {
    @Test
    fun `seeded rolls are deterministic and in range`() {
        val a = DiceRoller.roll(RollSpec.d(4, 6), random = Random(42))
        val b = DiceRoller.roll(RollSpec.d(4, 6), random = Random(42))
        assertEquals(a, b)
        assertTrue(a.dice.all { it in 1..6 })
        assertEquals(4, a.dice.size)
    }

    @Test
    fun `notation reads in dice order`() {
        assertEquals("d20", RollSpec.d(1, 20).displayNotation)
        assertEquals("4d6 drop lowest", RollSpec.abilityRoll.displayNotation)
        assertEquals("2d20 advantage", RollSpec.advantage().displayNotation)
    }
}

/**
 * Physical dice (A25). The premise of hand-entered rolls is that they're *the
 * same roll* — the player supplies the faces, every other rule runs unchanged.
 */
class ManualRollEntryTests {
    @Test
    fun `one slot per die rolled, keep rules included`() {
        assertEquals(1, ManualRollEntry.slotCount(RollSpec.d(1, 20)))
        // Advantage means two physical d20s hit the table, so both get typed.
        assertEquals(2, ManualRollEntry.slotCount(RollSpec.advantage()))
        assertEquals(4, ManualRollEntry.slotCount(RollSpec.abilityRoll))
    }

    @Test
    fun `the pad offers exactly the faces the die has`() {
        assertEquals(1..20, ManualRollEntry.enterableFaces(RollSpec.d(1, 20)))
        assertTrue(ManualRollEntry.isValid(6, RollSpec.d(1, 6)))
        assertFalse(ManualRollEntry.isValid(7, RollSpec.d(1, 6)))
        assertFalse(ManualRollEntry.isValid(0, RollSpec.d(1, 6)))
    }

    @Test
    fun `a hand-entered roll matches an RNG roll of the same faces`() {
        val spec = RollSpec.d(2, 6, mod = 3)
        val bonuses = listOf(RollBonus("STR", 3))
        // Same seed both sides, so the RNG path is reproducible.
        val fromRng = DiceRoller.roll(spec, bonuses, Random(99))
        val entered = ManualRollEntry.result(fromRng.dice, spec, bonuses)
        assertEquals(fromRng, entered)
    }

    @Test
    fun `keep rules apply to entered dice`() {
        assertEquals(listOf(18), ManualRollEntry.result(listOf(3, 18), RollSpec.advantage()).keptDice)
        assertEquals(listOf(3), ManualRollEntry.result(listOf(3, 18), RollSpec.disadvantage()).keptDice)
        assertEquals(15, ManualRollEntry.result(listOf(2, 4, 5, 6), RollSpec.abilityRoll).keptSum)
    }

    @Test
    fun `bonus chips win over the flat modifier, same as a rolled result`() {
        val r = ManualRollEntry.result(
            listOf(14),
            RollSpec.d(1, 20, mod = 99),
            listOf(RollBonus("DEX", 3), RollBonus("PROF", 2)),
        )
        assertEquals(19, r.total)
    }

    @Test
    fun `out-of-range entries clamp rather than corrupt the roll`() {
        assertEquals(listOf(1), ManualRollEntry.result(listOf(0), RollSpec.d(1, 20)).dice)
        assertEquals(listOf(20), ManualRollEntry.result(listOf(99), RollSpec.d(1, 20)).dice)
    }

    @Test
    fun `lucky offers a reroll on an entered kept 1`() {
        assertEquals(listOf(0), ManualRollEntry.luckyRerollIndices(listOf(1), RollSpec.d(1, 20)))
        assertTrue(ManualRollEntry.luckyRerollIndices(listOf(14), RollSpec.d(1, 20)).isEmpty())
        // The dropped d20 of an advantage pair isn't kept, so Lucky doesn't fire on it.
        assertTrue(ManualRollEntry.luckyRerollIndices(listOf(1, 12), RollSpec.advantage()).isEmpty())
        // Double 1s: exactly one die is kept, so exactly one gets the reroll.
        assertEquals(1, ManualRollEntry.luckyRerollIndices(listOf(1, 1), RollSpec.advantage()).size)
        // Lucky is d20-only — a 1 on a damage die stands.
        assertTrue(ManualRollEntry.luckyRerollIndices(listOf(1), RollSpec.d(1, 8)).isEmpty())
    }

    @Test
    fun `naturals read off entered faces`() {
        assertTrue(ManualRollEntry.result(listOf(20), RollSpec.d(1, 20)).isNatural20)
        assertTrue(ManualRollEntry.result(listOf(1, 1), RollSpec.advantage()).isNatural1)
    }
}

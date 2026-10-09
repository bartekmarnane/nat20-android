package au.com.evonet.nat20.dnd5e

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Single-class half / third casters read their own table; multiclass combines. */
class SingleClassSlotTests {
    @Test
    fun `a lone paladin reads the half-caster table, not the multiclass rounding`() {
        // PHB Paladin 5: 4 L1 + 2 L2. The multiclass rule (5/2 → full L2) gave 3 L1.
        assertEquals(mapOf(1 to 4, 2 to 2), Spellcasting.combinedSpellSlots(listOf(ClassEntry("paladin", 5))))
        assertEquals(mapOf(1 to 2), Spellcasting.combinedSpellSlots(listOf(ClassEntry("ranger", 2))))
        assertEquals(emptyMap<Int, Int>(), Spellcasting.combinedSpellSlots(listOf(ClassEntry("paladin", 1))))
    }

    @Test
    fun `a lone eldritch knight reads the third-caster table`() {
        val ek = listOf(ClassEntry("fighter", 4, subclass = "Eldritch Knight"))
        assertEquals(mapOf(1 to 3), Spellcasting.combinedSpellSlots(ek))
        val ek7 = listOf(ClassEntry("fighter", 7, subclass = "Eldritch Knight"))
        assertEquals(mapOf(1 to 4, 2 to 2), Spellcasting.combinedSpellSlots(ek7))
    }

    @Test
    fun `multiclass still combines against the full-caster table`() {
        // Paladin 3 (→1) + Wizard 3 (→3) = full-caster L4: 4 L1 + 3 L2.
        val multi = listOf(ClassEntry("paladin", 3), ClassEntry("wizard", 3))
        assertEquals(mapOf(1 to 4, 2 to 3), Spellcasting.combinedSpellSlots(multi))
    }
}

package au.com.evonet.nat20.dnd5e

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class StartingEquipmentTests {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "barbarian", "bard", "cleric", "druid", "fighter", "monk",
            "paladin", "ranger", "rogue", "sorcerer", "warlock", "wizard",
        ],
    )
    fun `every class kit seeds a non-empty inventory with an equipped weapon`(classId: String) {
        val items = StartingEquipment.seed(classId)
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.kind == ItemKind.WEAPON && it.equipped })
    }

    @Test
    fun `fighter kit resolves weapons armor and shield with the firsts equipped`() {
        val items = StartingEquipment.seed("fighter", backgroundId = "acolyte")
        // Longsword is the first weapon in the kit → equipped; the crossbow isn't.
        assertTrue(items.single { it.name == "Longsword" }.equipped)
        assertTrue(!items.single { it.name == "Crossbow, light" }.equipped)
        assertTrue(items.single { it.kind == ItemKind.ARMOR }.equipped) // chain mail
        assertTrue(items.single { it.kind == ItemKind.SHIELD }.equipped)
        // Background free-text gear lines are appended as plain gear items.
        assertTrue(items.any { it.kind == ItemKind.GEAR && it.name == "A holy symbol" })
    }

    @Test
    fun `unknown class with no kit falls back to an equipped dagger`() {
        val items = StartingEquipment.seed("not-a-class")
        assertEquals(1, items.size)
        val dagger = items.single()
        assertEquals("Dagger", dagger.name)
        assertEquals(ItemKind.WEAPON, dagger.kind)
        assertTrue(dagger.equipped)
    }

    @Test
    fun `rogue kit honours quantities`() {
        val daggers = StartingEquipment.seed("rogue").single { it.name == "Dagger" }
        assertEquals(2, daggers.quantity)
        assertTrue(!daggers.equipped) // rapier came first
    }
}

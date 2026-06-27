package au.com.evonet.nat20.dnd5e

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MonsterTests {

    @Test
    fun `the raw SRD monster catalogue decodes and sorts by challenge rating`() {
        val all = MonsterCatalog.all
        assertTrue(all.size > 300, "expected the full SRD bestiary, got ${all.size}")
        // sorted ascending by CR
        val crs = all.map { it.challengeRating }
        assertEquals(crs.sorted(), crs)
    }

    @Test
    fun `goblin decodes its nested armor class, speed, senses and proficiencies into display strings`() {
        val goblin = MonsterCatalog.monster("goblin")
        assertNotNull(goblin)
        goblin!!
        assertEquals("Goblin", goblin.name)
        assertEquals(15, goblin.armorClass)
        assertTrue(goblin.armorDetail.contains("leather", ignoreCase = true))
        assertEquals("30 ft.", goblin.speedDisplay)
        assertEquals(9, goblin.passivePerception)
        assertTrue(goblin.sensesDisplay.contains("darkvision"))
        assertTrue(goblin.skillsDisplay.contains("Stealth"))
        assertEquals("1/4", goblin.crLabel)
        assertTrue(goblin.subtitle.startsWith("Small humanoid"))
        assertTrue(goblin.traits.any { it.name == "Nimble Escape" })
        assertTrue(goblin.actions.any { it.name == "Scimitar" })
    }

    @Test
    fun `a legendary creature exposes legendary actions`() {
        val legendary = MonsterCatalog.all.firstOrNull { it.legendaryActions.isNotEmpty() }
        assertNotNull(legendary, "expected at least one creature with legendary actions")
    }
}

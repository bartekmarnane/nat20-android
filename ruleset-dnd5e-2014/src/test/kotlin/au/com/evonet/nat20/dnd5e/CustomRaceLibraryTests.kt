package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Homebrew race library semantics + the catalogue overlay (parity #11). */
class CustomRaceLibraryTests {

    @BeforeEach
    @AfterEach
    fun reset() {
        CustomRaceLibrary.onChange = null
        CustomRaceLibrary.replaceAll(emptyList())
    }

    private fun homebrew(name: String, id: String = CustomRaceLibrary.newId()) = Race(
        id = id,
        name = name,
        description = "A test homebrew.",
        abilityScoreIncreases = AbilityScoreIncreases(fixed = listOf(AbilityBonus("constitution", 2))),
        size = "small",
        speed = 25,
        darkvision = 60,
        languages = listOf("Common", "Sylvan"),
        traits = listOf(RaceTraitEntry("Night Sight", "You see in the dark.")),
    )

    @Test
    fun `homebrew ids carry the custom prefix and isCustom gates on it`() {
        val id = CustomRaceLibrary.newId()
        assertTrue(id.startsWith("custom:"))
        assertTrue(CustomRaceLibrary.isCustom(id))
        assertFalse(CustomRaceLibrary.isCustom("mountain-dwarf"))
    }

    @Test
    fun `add appends and fires onChange with the new list`() {
        var observed: List<Race>? = null
        CustomRaceLibrary.onChange = { observed = it }
        val race = homebrew("Vampire Dwarf")
        CustomRaceLibrary.add(race)
        assertEquals(listOf(race), CustomRaceLibrary.races.value)
        assertEquals(listOf(race), observed)
    }

    @Test
    fun `update replaces by id and fires onChange`() {
        val race = homebrew("Vampire Dwarf")
        CustomRaceLibrary.add(race)
        var observed: List<Race>? = null
        CustomRaceLibrary.onChange = { observed = it }
        val renamed = race.copy(name = "Dhampir Dwarf", speed = 35)
        CustomRaceLibrary.update(renamed)
        assertEquals(listOf(renamed), CustomRaceLibrary.races.value)
        assertEquals(listOf(renamed), observed)
    }

    @Test
    fun `delete removes by id and fires onChange`() {
        val keep = homebrew("Tabaxi-kin")
        val drop = homebrew("Vampire Dwarf")
        CustomRaceLibrary.add(keep)
        CustomRaceLibrary.add(drop)
        var observed: List<Race>? = null
        CustomRaceLibrary.onChange = { observed = it }
        CustomRaceLibrary.delete(drop.id)
        assertEquals(listOf(keep), CustomRaceLibrary.races.value)
        assertEquals(listOf(keep), observed)
    }

    @Test
    fun `replaceAll hydrates without firing onChange`() {
        var fired = false
        CustomRaceLibrary.onChange = { fired = true }
        CustomRaceLibrary.replaceAll(listOf(homebrew("Vampire Dwarf")))
        assertEquals(1, CustomRaceLibrary.races.value.size)
        assertFalse(fired)
    }

    @Test
    fun `catalogue appends homebrews after the SRD list, name-sorted`() {
        val srdCount = DnD5eCatalog.races.size
        CustomRaceLibrary.add(homebrew("Zebra-kin"))
        CustomRaceLibrary.add(homebrew("aarakocra-kin"))
        val all = DnD5eCatalog.races
        assertEquals(srdCount + 2, all.size)
        // Appended after the bundled list, sorted case-insensitively by name.
        assertEquals(listOf("aarakocra-kin", "Zebra-kin"), all.takeLast(2).map { it.name })
    }

    @Test
    fun `homebrew races round-trip through the persistence json codec`() {
        val race = homebrew("Vampire Dwarf")
        assertEquals(race, CustomRaceJson.decode(CustomRaceJson.encode(race)))
        assertNull(CustomRaceJson.decode("not json"))
    }

    @Test
    fun `catalogue resolves a homebrew by id, with working ability bonuses`() {
        val race = homebrew("Vampire Dwarf")
        CustomRaceLibrary.add(race)
        val found = DnD5eCatalog.race(race.id)
        assertNotNull(found)
        assertEquals(2, found!!.abilityBonuses()[Ability.CONSTITUTION])
        CustomRaceLibrary.delete(race.id)
        assertNull(DnD5eCatalog.race(race.id))
    }
}

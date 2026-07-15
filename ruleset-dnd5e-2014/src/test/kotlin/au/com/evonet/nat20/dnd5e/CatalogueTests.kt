package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CatalogueTests {

    @Test
    fun `races load from the bundled SRD json with ability bonuses`() {
        val dwarf = DnD5eCatalog.race("mountain-dwarf")
        assertNotNull(dwarf)
        assertEquals("Mountain Dwarf", dwarf!!.name)
        assertEquals(2, dwarf.abilityBonuses()[Ability.STRENGTH])
        assertEquals(2, dwarf.abilityBonuses()[Ability.CONSTITUTION])
    }

    @Test
    fun `classes load with hit die and saving throws`() {
        val fighter = DnD5eCatalog.characterClass("fighter")
        assertNotNull(fighter)
        assertEquals(10, fighter!!.hitDie)
        assertEquals(listOf(Ability.STRENGTH, Ability.CONSTITUTION), fighter.savingThrowAbilities())
        assertTrue(fighter.skillChoiceCount >= 1)
    }

    @Test
    fun `backgrounds load with granted skills`() {
        val acolyte = DnD5eCatalog.background("acolyte")
        assertNotNull(acolyte)
        assertEquals(listOf("insight", "religion"), acolyte!!.grantedSkills)
    }

    @Test
    fun `skill catalogue has all eighteen skills mapped to abilities`() {
        assertEquals(18, SkillCatalog.all.size)
        assertEquals(Ability.WISDOM, DnD5eCatalog.skill("perception")?.ability)
        assertEquals(Ability.DEXTERITY, DnD5eCatalog.skill("stealth")?.ability)
    }

    @Test
    fun `catalogues are non-empty`() {
        assertTrue(DnD5eCatalog.races.isNotEmpty())
        assertTrue(DnD5eCatalog.classes.isNotEmpty())
        assertTrue(DnD5eCatalog.backgrounds.isNotEmpty())
    }

    @Test
    fun `spell library merges SRD, PHB gap set, and supplements, sorted by level then name`() {
        val spells = DnD5eCatalog.spellLibrary
        // SRD 5.1 (319) + licensed PHB gap set (41) + Xanathar's (8) + Tasha's (8).
        assertEquals(376, spells.size)
        // No duplicate ids across the merged files.
        assertEquals(spells.size, spells.map { it.index }.toSet().size)
        // Sorted: cantrips (level 0) first.
        assertEquals(0, spells.first().level)

        val acidArrow = spells.first { it.index == "acid-arrow" }
        assertEquals("Acid Arrow", acidArrow.name)
        assertEquals(2, acidArrow.level)
        assertEquals("Evocation", acidArrow.schoolName)
        assertTrue(acidArrow.description.isNotEmpty())
    }

    @Test
    fun `licensed PHB gap spells resolve with their book stats`() {
        val aura = DnD5eCatalog.spellLibrary.first { it.index == "aura-of-vitality" }
        assertEquals("Aura of Vitality", aura.name)
        assertEquals(3, aura.level)
        assertEquals("Evocation", aura.schoolName)
        assertTrue(aura.concentration)
        assertEquals(listOf("Paladin"), aura.classNames)

        val hex = DnD5eCatalog.spellLibrary.first { it.index == "hex" }
        assertEquals(1, hex.level)
        assertEquals("1 bonus action", hex.castingTime)
        assertEquals(listOf("Warlock"), hex.classNames)

        // A supplement spell now loads too (the files existed but were unwired).
        val tollTheDead = DnD5eCatalog.spellLibrary.first { it.index == "toll-the-dead" }
        assertEquals(0, tollTheDead.level)
    }
}

package au.com.evonet.nat20.dnd5e

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Mirrors the iOS `SourceUsageAuditTests` case-for-case — the two platforms
 * read the same bundled catalogue JSON, so the same character must produce the
 * same warning.
 */
class SourceUsageTests {

    /**
     * A Tasha's-heavy wizard: Custom Lineage race, Bladesinging subclass,
     * Skill Expert feat, Mind Sliver cantrip. Everything but the class itself
     * comes from the book.
     */
    private fun bladesinger() = DnD5ePayload(
        race = "custom-lineage",
        classes = listOf(ClassEntry("wizard", 6, "Bladesinging")),
        chosenFeats = listOf("skill-expert"),
        cantripsKnown = listOf("mind-sliver", "fire-bolt"),
        spellsKnown = mapOf("wizard" to listOf("mind-spike", "magic-missile")),
    )

    @Test
    fun `finds every Tasha's selection, tagged by slot`() {
        val usages = SourceUsageAudit.usages("tashas", bladesinger())

        assertEquals(
            listOf("Race", "Subclass", "Feat", "Spell", "Spell"),
            usages.map { it.category },
        )
        assertEquals(
            listOf("Custom Lineage", "Bladesinging", "Skill Expert", "Mind Sliver", "Mind Spike"),
            usages.map { it.name },
        )
    }

    @Test
    fun `ignores selections from other books`() {
        assertTrue(SourceUsageAudit.usages("xanathars", bladesinger()).isEmpty())
    }

    @Test
    fun `a pure-PHB character has nothing to lose`() {
        val payload = DnD5ePayload(
            race = "mountain-dwarf",
            classes = listOf(ClassEntry("fighter", 3, "Champion")),
            fightingStyles = listOf("defense"),
        )
        SourceCatalog.all.filterNot { it.isLocked }.forEach { source ->
            assertTrue(
                SourceUsageAudit.usages(source.id, payload).isEmpty(),
                "expected no ${source.abbreviation} usage",
            )
        }
    }

    @Test
    fun `counts prepared spells the known list never mentions`() {
        // Prepared casters pick from the whole class list, so a Cleric's
        // Tasha's spell can be prepared without ever landing in spellsKnown.
        val payload = DnD5ePayload(
            classes = listOf(ClassEntry("cleric", 5)),
            preparedSpells = mapOf("cleric" to listOf("wither-and-bloom", "cure-wounds")),
        )
        assertEquals(
            listOf("Wither and Bloom"),
            SourceUsageAudit.usages("tashas", payload).map { it.name },
        )
    }

    @Test
    fun `a spell held twice over is reported once`() {
        val payload = DnD5ePayload(
            classes = listOf(ClassEntry("wizard", 5)),
            cantripsKnown = listOf("mind-sliver"),
            spellsKnown = mapOf("wizard" to listOf("mind-sliver")),
            preparedSpells = mapOf("wizard" to listOf("mind-sliver")),
        )
        assertEquals(1, SourceUsageAudit.usages("tashas", payload).size)
    }

    @Test
    fun `matches a subclass stored as a slug, not a display name`() {
        val payload = DnD5ePayload(classes = listOf(ClassEntry("warlock", 3, "hexblade")))
        assertEquals(
            listOf("The Hexblade"),
            SourceUsageAudit.usages("xanathars", payload).map { it.name },
        )
    }

    @Test
    fun `pickers narrow to the enabled sources`() {
        val coreOnly = SourceCatalog.defaultEnabled
        val withTashas = coreOnly + SourceCatalog.tashas.id

        assertTrue(DnD5eCatalog.races(coreOnly).none { it.id == "custom-lineage" })
        assertTrue(DnD5eCatalog.races(withTashas).any { it.id == "custom-lineage" })
        assertTrue(Feats.all(coreOnly).none { it.id == "skill-expert" })
        assertTrue(Feats.all(withTashas).any { it.id == "skill-expert" })
        assertTrue(DnD5eCatalog.spellLibrary(coreOnly).none { it.index == "mind-sliver" })
        assertTrue(DnD5eCatalog.spellLibrary(withTashas).any { it.index == "mind-sliver" })
    }

    @Test
    fun `an empty stored set still resolves to the core book`() {
        // Characters built before the field existed decode to an empty list.
        assertEquals(SourceCatalog.defaultEnabled, DnD5ePayload().effectiveSources)
    }
}

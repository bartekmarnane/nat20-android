package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.domain.Character
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

private val RT_NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val rtRuleset = DnD5eRuleset()

private fun raceChar(
    race: String,
    classId: String = "fighter",
    level: Int = 3,
    currentHp: Int = 24,
    maxHp: Int = 24,
    selectedSkills: List<String> = emptyList(),
): Character = Character.new(
    "Test",
    rtRuleset,
    DnD5ePayload(
        race = race,
        classes = listOf(ClassEntry(classId, level)),
        abilityScores = AbilityScores(),
        maxHp = maxHp,
        currentHp = currentHp,
        selectedSkills = selectedSkills,
    ),
    RT_NOW,
)

private fun Character.p() = payload as DnD5ePayload

class RaceTraitsTests {
    @Test
    fun `innate resistances fold into damage`() {
        val tiefling = raceChar("tiefling")
        assertTrue("fire" in tiefling.p().effectiveDamageResistances)
        val hurt = TakeDamage(8, "fire").applyTo(tiefling, rtRuleset)
        assertEquals(24 - 4, hurt.character.p().currentHp) // 8 fire halved to 4
        assertTrue(hurt.event.summary.contains("resisted"))
        // Dwarves resist poison; a human resists nothing.
        assertTrue("poison" in raceChar("mountain-dwarf").p().effectiveDamageResistances)
        assertTrue(raceChar("human").p().effectiveDamageResistances.isEmpty())
    }

    @Test
    fun `elf Keen Senses auto-grants Perception`() {
        val elf = raceChar("wood-elf", selectedSkills = listOf("stealth"))
        assertTrue("perception" in elf.p().effectiveSkillProficiencies)
        assertFalse("perception" in raceChar("human", selectedSkills = listOf("stealth")).p().effectiveSkillProficiencies)
    }

    @Test
    fun `half-orc Relentless Endurance keeps the character at 1 HP once per long rest`() {
        val orc = raceChar("half-orc", currentHp = 6, maxHp = 24)
        val downed = TakeDamage(12).applyTo(orc, rtRuleset) // leftover 6 < 24 max ⇒ not overkill
        assertEquals(1, downed.character.p().currentHp)
        assertTrue(downed.character.p().relentlessEnduranceUsed)
        assertTrue(downed.event.summary.contains("Relentless Endurance"))
        // Already used → drops to 0.
        val again = TakeDamage(12).applyTo(downed.character.copy(payload = downed.character.p().copy(currentHp = 6)), rtRuleset)
        assertEquals(0, again.character.p().currentHp)
        // Long rest restores it.
        assertFalse(LongRest().applyTo(again.character, rtRuleset).character.p().relentlessEnduranceUsed)
    }

    @Test
    fun `massive overkill kills outright despite Relentless Endurance`() {
        val orc = raceChar("half-orc", currentHp = 6, maxHp = 24)
        val dead = TakeDamage(6 + 24).applyTo(orc, rtRuleset)
        assertEquals(0, dead.character.p().currentHp)
        assertFalse(dead.character.p().relentlessEnduranceUsed)
    }

    @Test
    fun `trait predicates and reminders are consistent`() {
        assertTrue(RaceTraits.hasHalflingLuck("lightfoot-halfling"))
        assertTrue(RaceTraits.hasRelentlessEndurance("half-orc"))
        assertTrue(RaceTraits.hasSavageAttacks("half-orc"))
        assertFalse(RaceTraits.hasHalflingLuck("human"))
        listOf("mountain-dwarf", "wood-elf", "half-orc", "tiefling", "lightfoot-halfling", "rock-gnome", "half-elf").forEach {
            assertTrue(RaceTraits.reminders(it).isNotEmpty(), "no reminders for $it")
        }
    }
}

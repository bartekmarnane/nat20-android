package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val ruleset = DnD5eRuleset()

private fun character(
    classId: String = "wizard",
    level: Int = 3,
    scores: AbilityScores = AbilityScores(),
    subclass: String? = null,
    currentSlots: Map<Int, Int>? = null,
): Character {
    var payload = DnD5ePayload(
        classes = listOf(ClassEntry(classId, level, subclass)),
        abilityScores = scores,
        maxHp = 20,
        currentHp = 20,
    )
    payload = payload.copy(currentSpellSlots = currentSlots ?: payload.maxSpellSlots)
    return Character.new("Aria", ruleset, payload, NOW)
}

private fun Character.payload() = payload as DnD5ePayload

class AsiCadenceTests {
    @ParameterizedTest
    @CsvSource("wizard,4,true", "wizard,5,false", "wizard,8,true", "wizard,19,true", "wizard,20,false",
        "fighter,6,true", "fighter,7,false", "fighter,14,true", "rogue,10,true", "wizard,10,false")
    fun `ASI cadence by class and level`(classId: String, level: Int, expected: Boolean) {
        assertEquals(expected, LevelUpMath.grantsAbilityScoreImprovement(classId, level))
    }
}

class LevelUpChoiceTests {
    @Test
    fun `an ASI raises scores and caps at 20`() {
        val c = character(scores = AbilityScores(intelligence = 19, dexterity = 14))
        val result = LevelUp("wizard", abilityIncreases = mapOf(Ability.INTELLIGENCE to 2)).applyTo(c, ruleset)
        // INT 19 + 2 → capped at 20.
        assertEquals(20, result.character.payload().abilityScores.intelligence)
    }

    @Test
    fun `splitting plus one across two abilities`() {
        val result = LevelUp("wizard", abilityIncreases = mapOf(Ability.DEXTERITY to 1, Ability.CONSTITUTION to 1)).applyTo(character(), ruleset)
        val p = result.character.payload()
        assertEquals(11, p.abilityScores.dexterity)
        assertEquals(11, p.abilityScores.constitution)
    }

    @Test
    fun `more than plus two total is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            LevelUp("wizard", abilityIncreases = mapOf(Ability.STRENGTH to 2, Ability.DEXTERITY to 1)).applyTo(character(), ruleset)
        }
    }

    @Test
    fun `choosing a subclass records it on the class line`() {
        val result = LevelUp("fighter", subclass = "champion", className = "Fighter").applyTo(character("fighter", 2), ruleset)
        val entry = result.character.payload().classes.first { it.classId == "fighter" }
        assertEquals("champion", entry.subclass)
        assertTrue(result.event.summary.contains("champion"))
    }

    @Test
    fun `leveling a caster grants the newly unlocked slots while keeping spent ones`() {
        // Wizard 2 ({1:3}) with one L1 slot spent → level to 3 ({1:4, 2:2}).
        val c = character("wizard", level = 2, currentSlots = mapOf(1 to 2))
        val p = LevelUp("wizard").applyTo(c, ruleset).character.payload()
        assertEquals(mapOf(1 to 4, 2 to 2), p.maxSpellSlots)
        // L1: had 2 remaining, gained 1 (3→4 max) → 3; L2 newly unlocked → full 2.
        assertEquals(3, p.currentSpellSlots[1])
        assertEquals(2, p.currentSpellSlots[2])
    }

    @Test
    fun `the extended event round-trips through the codec`() {
        val event = LeveledUpEvent(
            classId = "fighter", className = "Fighter", isNewClass = false,
            previousLevel = 3, newLevel = 4, classLevelAfter = 4,
            hpChoice = au.com.evonet.nat20.dnd5e.core.HpChoice.Average, hpGained = 7,
            subclass = "champion", abilityIncreases = mapOf(Ability.STRENGTH to 2),
        )
        val typeId = ruleset.eventTypeId(event)
        assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
    }
}

class SubclassCatalogueTests {
    @Test
    fun `classes load their subclasses and subclass level`() {
        val fighter = DnD5eCatalog.characterClass("fighter")!!
        assertEquals(3, fighter.subclassLevel)
        assertTrue(fighter.subclasses.any { it.id == "champion" || it.name == "Champion" })

        val cleric = DnD5eCatalog.characterClass("cleric")!!
        assertEquals(1, cleric.subclassLevel)
        assertTrue(cleric.subclasses.isNotEmpty())
    }
}

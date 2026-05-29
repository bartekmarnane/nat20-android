package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import au.com.evonet.nat20.domain.CharacterPhase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-05-29T00:00:00Z")

class RulesetAndPayloadTests {
    private val ruleset = DnD5eRuleset()

    @Test
    fun `ruleset identity matches the iOS reference`() {
        assertEquals("dnd-5e-2014", ruleset.id)
        assertEquals("D&D 5e (2014)", ruleset.displayName)
    }

    @Test
    fun `fresh payload has sensible defaults`() {
        val p = ruleset.makeInitialPayload("Aria") as DnD5ePayload
        assertEquals("", p.race)
        assertEquals(emptyList<ClassEntry>(), p.classes)
        assertEquals(1, p.level) // empty classes still read as level 1
        assertEquals("", p.characterClass)
        assertEquals(AbilityScores(), p.abilityScores)
        assertEquals(1, p.maxHp)
        assertEquals(1, p.currentHp) // defaults to full
        assertEquals(0, p.temporaryHp)
    }

    @Test
    fun `level and characterClass derive from classes`() {
        val p = DnD5ePayload(classes = listOf(ClassEntry("fighter", 3), ClassEntry("rogue", 2)))
        assertEquals(5, p.level)
        assertEquals("fighter", p.characterClass)
    }
}

class ProficiencyAndModifierTests {
    @ParameterizedTest
    @CsvSource("1,2", "4,2", "5,3", "8,3", "9,4", "12,4", "13,5", "16,5", "17,6", "20,6")
    fun `proficiency bonus by level`(level: Int, expected: Int) {
        assertEquals(expected, DnD5eRuleset.proficiencyBonus(level))
    }

    @ParameterizedTest
    @CsvSource("1,-5", "8,-1", "9,-1", "10,0", "11,0", "12,1", "14,2", "15,2", "20,5", "30,10")
    fun `ability modifier floors toward negative infinity`(score: Int, expected: Int) {
        assertEquals(expected, AbilityScores.modifier(score))
    }
}

class HpIntentTests {
    private val ruleset = DnD5eRuleset()
    private fun character(maxHp: Int, currentHp: Int = maxHp, tempHp: Int = 0): Character =
        Character.new(
            name = "Aria",
            ruleset = ruleset,
            payload = DnD5ePayload(
                classes = listOf(ClassEntry("fighter", 1)),
                maxHp = maxHp,
                currentHp = currentHp,
                temporaryHp = tempHp,
            ),
            timestamp = NOW,
        )

    private fun Character.payload() = payload as DnD5ePayload

    @Test
    fun `damage reduces current HP and floors at zero`() {
        val result = TakeDamage(amount = 100).applyTo(character(maxHp = 12), ruleset)
        assertEquals(0, result.character.payload().currentHp)
    }

    @Test
    fun `temp HP absorbs damage first`() {
        val result = TakeDamage(amount = 5).applyTo(character(maxHp = 12, tempHp = 3), ruleset)
        val p = result.character.payload()
        assertEquals(0, p.temporaryHp)   // 3 absorbed
        assertEquals(10, p.currentHp)    // remaining 2 hit HP
        assertEquals("Took 5 damage (HP 12 → 10) (3 absorbed by temp)", result.event.summary)
    }

    @Test
    fun `non-positive damage is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            TakeDamage(amount = 0).applyTo(character(maxHp = 12), ruleset)
        }
    }

    @Test
    fun `heal clamps to max HP`() {
        val result = Heal(amount = 100).applyTo(character(maxHp = 12, currentHp = 4), ruleset)
        assertEquals(12, result.character.payload().currentHp)
    }

    @Test
    fun `temp HP does not stack - keeps the higher value`() {
        val withFive = GainTempHp(amount = 5).applyTo(character(maxHp = 12), ruleset)
        assertEquals(5, withFive.character.payload().temporaryHp)

        val withThree = GainTempHp(amount = 3).applyTo(withFive.character, ruleset)
        assertEquals(5, withThree.character.payload().temporaryHp) // kept the 5
    }
}

class LevelUpTests {
    private val ruleset = DnD5eRuleset()
    private fun fighter(con: Int = 14, maxHp: Int = 12): Character = Character.new(
        name = "Aria",
        ruleset = ruleset,
        payload = DnD5ePayload(
            classes = listOf(ClassEntry("fighter", 1)),
            abilityScores = AbilityScores(constitution = con),
            maxHp = maxHp,
        ),
        timestamp = NOW,
    )

    private fun Character.payload() = payload as DnD5ePayload

    @Test
    fun `average level-up adds half-die-plus-one plus CON mod`() {
        // Fighter d10: average gain 6, CON 14 → +2, total +8.
        val result = LevelUp(classId = "fighter").applyTo(fighter(), ruleset)
        val p = result.character.payload()
        assertEquals(2, p.level)
        assertEquals(20, p.maxHp) // 12 + 8
        assertEquals(20, p.currentHp)
        assertEquals("Leveled up to fighter 2 — +8 HP", result.event.summary)
    }

    @Test
    fun `rolled HP outside the die range is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            LevelUp(classId = "fighter", hpChoice = HpChoice.Rolled(11)).applyTo(fighter(), ruleset)
        }
    }

    @Test
    fun `cannot add a class that already exists`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            LevelUp(classId = "fighter", isNewClass = true).applyTo(fighter(), ruleset)
        }
    }

    @Test
    fun `multiclass inserts a new class line`() {
        val result = LevelUp(classId = "wizard", isNewClass = true).applyTo(fighter(), ruleset)
        val p = result.character.payload()
        assertEquals(2, p.level)
        assertEquals(listOf("fighter", "wizard"), p.classes.map { it.classId })
    }

    @Test
    fun `cannot level past 20`() {
        val twenty = Character.new(
            name = "Cap",
            ruleset = ruleset,
            payload = DnD5ePayload(classes = listOf(ClassEntry("fighter", 20))),
            timestamp = NOW,
        )
        assertThrows(CharacterIntentError.Invalid::class.java) {
            LevelUp(classId = "fighter").applyTo(twenty, ruleset)
        }
    }
}

class CodecTests {
    private val ruleset = DnD5eRuleset()

    @Test
    fun `payload round-trips through the codec`() {
        val payload = DnD5ePayload(
            race = "mountain-dwarf",
            classes = listOf(ClassEntry("fighter", 3, subclass = "champion")),
            abilityScores = AbilityScores(strength = 16, constitution = 14),
            maxHp = 28,
            currentHp = 20,
            temporaryHp = 5,
        )
        val decoded = ruleset.decodePayload(ruleset.encodePayload(payload))
        assertEquals(payload, decoded)
    }

    @Test
    fun `each event round-trips with its type id`() {
        val events = listOf(
            DamageTakenEvent(amount = 5, previousHp = 12, newHp = 7),
            HealedEvent(amount = 4, source = "potion", previousHp = 7, newHp = 11),
            TempHpGainedEvent(amount = 5, previous = 0, newValue = 5),
            LeveledUpEvent(
                classId = "fighter", className = "", isNewClass = false,
                previousLevel = 1, newLevel = 2, classLevelAfter = 2,
                hpChoice = HpChoice.Average, hpGained = 8,
            ),
        )
        for (event in events) {
            val typeId = ruleset.eventTypeId(event)
            val decoded = ruleset.decodeEvent(ruleset.encodeEvent(event), typeId)
            assertEquals(event, decoded)
        }
    }
}

class MiniCharacterBuildTests {
    private val ruleset = DnD5eRuleset()
    private fun Character.payload() = payload as DnD5ePayload

    @Test
    fun `build a fighter, level twice, take a hit, then heal`() {
        var character = Character.new(
            name = "Thorgar",
            ruleset = ruleset,
            payload = DnD5ePayload(
                race = "mountain-dwarf",
                classes = listOf(ClassEntry("fighter", 1)),
                abilityScores = AbilityScores(strength = 16, constitution = 16),
                maxHp = 13, // d10 max (10) + CON +3 at L1
            ),
            timestamp = NOW,
        )
        assertEquals(CharacterPhase.Building, character.phase)

        // Two average level-ups: each +6 (avg d10) +3 (CON) = +9.
        character = LevelUp(classId = "fighter").applyTo(character, ruleset).character
        character = LevelUp(classId = "fighter").applyTo(character, ruleset).character
        assertEquals(3, character.payload().level)
        assertEquals(31, character.payload().maxHp) // 13 + 9 + 9
        assertEquals(2, DnD5eRuleset.proficiencyBonus(character.payload().level)) // L3 is still in the +2 band

        // Take 10, then heal 4.
        character = TakeDamage(amount = 10, damageType = "Slashing").applyTo(character, ruleset).character
        assertEquals(21, character.payload().currentHp)
        character = Heal(amount = 4).applyTo(character, ruleset).character
        assertEquals(25, character.payload().currentHp)
    }
}

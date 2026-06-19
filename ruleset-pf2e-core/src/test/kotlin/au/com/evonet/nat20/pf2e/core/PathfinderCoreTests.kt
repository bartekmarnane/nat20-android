package au.com.evonet.nat20.pf2e.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ProficiencyTests {
    @ParameterizedTest
    @CsvSource(
        // rank, level, expectedBonus
        "UNTRAINED,5,0",   // untrained never adds level
        "TRAINED,5,7",     // 5 + 2
        "EXPERT,1,5",      // 1 + 4
        "MASTER,10,16",    // 10 + 6
        "LEGENDARY,20,28", // 20 + 8
    )
    fun `bonus is level plus rank, except untrained`(rank: Proficiency, level: Int, expected: Int) {
        assertEquals(expected, rank.bonus(level))
    }

    @Test
    fun `ladder steps and compares`() {
        assertTrue(Proficiency.EXPERT > Proficiency.TRAINED)
        assertEquals(Proficiency.EXPERT, Proficiency.TRAINED.next)
        assertNull(Proficiency.LEGENDARY.next)
        assertEquals("L", Proficiency.LEGENDARY.letter)
    }
}

class DegreeOfSuccessTests {
    @ParameterizedTest
    @CsvSource(
        // total, dc, expected
        "30,20,CRITICAL_SUCCESS", // beat by 10+
        "20,20,SUCCESS",          // meet
        "15,20,FAILURE",          // below
        "10,20,CRITICAL_FAILURE", // miss by 10
    )
    fun `degree keys off beating or missing the DC by ten`(total: Int, dc: Int, expected: DegreeOfSuccess) {
        assertEquals(expected, DegreeOfSuccess.resolve(total, dc))
    }

    @Test
    fun `natural 20 and 1 step the degree`() {
        // A plain success on a nat 20 becomes a critical success.
        assertEquals(DegreeOfSuccess.CRITICAL_SUCCESS, DegreeOfSuccess.resolve(20, 20, naturalRoll = 20))
        // A plain failure on a nat 1 becomes a critical failure.
        assertEquals(DegreeOfSuccess.CRITICAL_FAILURE, DegreeOfSuccess.resolve(15, 20, naturalRoll = 1))
        // A nat 1 on what would be a critical success drops to a plain success.
        assertEquals(DegreeOfSuccess.SUCCESS, DegreeOfSuccess.resolve(35, 20, naturalRoll = 1))
    }
}

class AbilityAndSkillTests {
    @ParameterizedTest
    @CsvSource("10,0", "12,1", "18,4", "7,-2", "1,-5", "20,5")
    fun `modifier is floor of score minus ten over two`(score: Int, expected: Int) {
        assertEquals(expected, PfAbilityScores.modifier(score))
    }

    @Test
    fun `skills and saves carry their governing ability`() {
        assertEquals(PfAbility.DEXTERITY, PfSkill.STEALTH.ability)
        assertEquals(PfAbility.INTELLIGENCE, PfSkill.OCCULTISM.ability)
        assertEquals(PfAbility.CONSTITUTION, Save.FORTITUDE.ability)
        assertEquals(16, PfSkill.entries.size)
        assertEquals(4, SpellTradition.entries.size)
    }

    @Test
    fun `full-caster slot progression unlocks a rank at every odd level`() {
        assertEquals(emptyMap<Int, Int>(), SpellcastingProgression.fullCasterSlots(0))
        assertEquals(mapOf(1 to 2), SpellcastingProgression.fullCasterSlots(1))     // rank 1 unlocks at 2 slots
        assertEquals(mapOf(1 to 3, 2 to 2), SpellcastingProgression.fullCasterSlots(3)) // rank 1 → 3, rank 2 unlocks
        assertEquals(10, SpellcastingProgression.maxSpellRank(19))
        assertEquals(1, SpellcastingProgression.fullCasterSlots(20)[10]) // capstone rank = single slot
    }

    @Test
    fun `valued conditions label with their number`() {
        assertEquals("Frightened 2", ValuedCondition("frightened", 2).label("Frightened"))
        assertEquals("Prone", ValuedCondition("prone").label("Prone"))
        assertTrue(ValuedCondition("clumsy", 1).isValued)
    }
}

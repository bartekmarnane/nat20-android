package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.pf2e.core.AbilityBuild
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AbilityBuildTests {
    @Test
    fun `a boost is plus two, but plus one once the score is eighteen`() {
        // Charisma boosted three times: 10 -> 12 -> 14 -> 16, then a fourth in its own
        // step from an 18 start gives +1.
        val build = AbilityBuild(boostSteps = listOf(
            listOf(PfAbility.CHARISMA), listOf(PfAbility.CHARISMA), listOf(PfAbility.CHARISMA), listOf(PfAbility.CHARISMA),
        ))
        // 10 +2 =12 +2 =14 +2 =16 +2 =18 (the fourth is +2 because it was 16 at the time).
        assertEquals(18, build.resolved().charisma)
        // A fifth from 18 is only +1.
        val five = AbilityBuild(boostSteps = build.boostSteps + listOf(listOf(PfAbility.CHARISMA)))
        assertEquals(19, five.resolved().charisma)
    }

    @Test
    fun `flaws apply before boosts and duplicates within a step are illegal`() {
        val build = AbilityBuild(flaws = listOf(PfAbility.STRENGTH), boostSteps = listOf(listOf(PfAbility.DEXTERITY, PfAbility.CONSTITUTION)))
        assertEquals(8, build.resolved().strength)
        assertEquals(12, build.resolved().dexterity)
        assertFalse(build.hasDuplicateBoostInAStep)
        assertTrue(AbilityBuild(boostSteps = listOf(listOf(PfAbility.STRENGTH, PfAbility.STRENGTH))).hasDuplicateBoostInAStep)
    }
}

class PathfinderBuilderTests {
    @Test
    fun `building a sorcerer folds ancestry, background, class, and free boosts`() {
        val choices = PathfinderBuilder.Choices(
            name = "Seoni",
            ancestryId = "human", heritage = "skilled-heritage", backgroundId = "noble",
            classId = "sorcerer", keyAbility = PfAbility.CHARISMA,
            ancestryFreeBoosts = listOf(PfAbility.CHARISMA, PfAbility.CONSTITUTION), // human: 2 free
            backgroundBoost = PfAbility.CHARISMA, backgroundFreeBoost = PfAbility.DEXTERITY,
            freeBoosts = listOf(PfAbility.CHARISMA, PfAbility.DEXTERITY, PfAbility.CONSTITUTION, PfAbility.WISDOM),
            chosenSkills = listOf(PfSkill.ARCANA, PfSkill.DECEPTION),
        )
        val p = PathfinderBuilder.build(choices)
        // CHA boosted by ancestry-free, background, class-key, free = four +2s → 18.
        assertEquals(18, p.abilityScores.charisma)
        assertEquals(PfAbility.CHARISMA, p.keyAbility)
        // HP = human 8 + sorcerer 6 + CON mod. CON: 10 +2 (ancestry free) +2 (free) = 14 → +2 → 16 HP.
        assertEquals(8 + 6 + 2, p.maxHp)
        // Sorcerer save ranks + class DC + the background skill/lore folded in.
        assertEquals(Proficiency.EXPERT, p.saves[Save.WILL])
        assertEquals(Proficiency.TRAINED, p.skills[PfSkill.SOCIETY]) // Noble's trained skill
        assertEquals(Proficiency.TRAINED, p.skills[PfSkill.ARCANA])  // a picked class skill
        assertTrue(p.loreSkills.containsKey("Genealogy Lore"))
    }

    @Test
    fun `skill slots scale with the class plus a positive Intelligence modifier`() {
        val base = PathfinderBuilder.Choices("x", "human", null, "noble", "wizard", PfAbility.INTELLIGENCE,
            freeBoosts = listOf(PfAbility.INTELLIGENCE, PfAbility.DEXTERITY, PfAbility.CONSTITUTION, PfAbility.WISDOM))
        // Wizard trains 2 + INT mod. INT: background default boost +2, class-key +2, free +2 → 16 → +3 mod → 2 + 3 = 5.
        assertEquals(5, PathfinderBuilder.skillSlots(base))
    }
}

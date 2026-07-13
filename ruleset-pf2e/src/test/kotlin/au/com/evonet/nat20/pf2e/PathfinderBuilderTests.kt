package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.pf2e.core.AbilityBuild
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

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

/** The wizard-parity creation path (A22 wizard rework): subclass, spells, feats, equipment, wealth, above-L1. */
class PathfinderBuilderCreationTests {
    private val ruleset = PathfinderRuleset()

    private fun sorcerer(targetLevel: Int = 1) = PathfinderBuilder.Choices(
        name = "Seoni",
        ancestryId = "human", heritage = "skilled-heritage", backgroundId = "noble",
        classId = "sorcerer", keyAbility = PfAbility.CHARISMA,
        ancestryFreeBoosts = listOf(PfAbility.CHARISMA, PfAbility.CONSTITUTION),
        backgroundBoost = PfAbility.CHARISMA, backgroundFreeBoost = PfAbility.DEXTERITY,
        freeBoosts = listOf(PfAbility.CHARISMA, PfAbility.DEXTERITY, PfAbility.CONSTITUTION, PfAbility.WISDOM),
        chosenSkills = listOf(PfSkill.ARCANA, PfSkill.DECEPTION),
        subclass = "draconic",
        targetLevel = targetLevel,
    )

    @Test
    fun `the subclass folds into the payload and its granted skill trains`() {
        // Draconic bloodline grants Arcana — pick skills that DON'T include it to see the fold.
        val p = PathfinderBuilder.build(sorcerer().copy(chosenSkills = listOf(PfSkill.DECEPTION, PfSkill.DIPLOMACY)))
        assertEquals("draconic", p.subclass)
        assertEquals(Proficiency.TRAINED, p.skills[PfSkill.ARCANA])
        // An id the class doesn't offer is dropped.
        assertNull(PathfinderBuilder.build(sorcerer().copy(subclass = "thief")).subclass)
        assertTrue(PathfinderCatalog.pfClass("sorcerer")!!.hasSubclass)
        assertFalse(PathfinderCatalog.pfClass("fighter")!!.hasSubclass)
    }

    @Test
    fun `a level-1 caster's spell picks are written, filtered to the tradition, and capped`() {
        val p = PathfinderBuilder.build(sorcerer().copy(
            // "heal" is divine/primal — filtered out of an arcane repertoire; picks over the cap of 5 drop.
            cantrips = listOf("daze", "light", "electric-arc", "shield", "prestidigitation", "telekinetic-projectile", "heal", "bogus"),
            rank1Spells = listOf("magic-missile", "grease", "bless", "fear"), // bless is divine/occult; cap 2 at L1
        ))
        assertEquals(5, p.cantrips.size)
        assertFalse("heal" in p.cantrips)
        assertEquals(listOf("magic-missile", "grease"), p.knownSpells[1])
        assertEquals(2, p.currentSpellSlots[1]) // slots start full
    }

    @ParameterizedTest(name = "level {0} starts with {1} gp")
    @CsvSource("1,15", "2,30", "5,270", "10,2300", "20,112000")
    fun `starting wealth follows the GM Core table and seeds the purse`(level: Int, gp: Int) {
        assertEquals(gp, Wealth.startingWealthGP(level))
        assertEquals(mapOf(PFCoin.GP to gp), PathfinderBuilder.build(sorcerer(targetLevel = level)).coins)
    }

    @Test
    fun `feats are written in earn order — ancestry, class, then advancement by level`() {
        val p = PathfinderBuilder.build(sorcerer(targetLevel = 3).copy(
            ancestryFeat = "natural-ambition",
            classFeat = "dangerous-sorcery",
            advancementFeats = mapOf(
                FeatSlotKey(3, PfFeatType.GENERAL) to "toughness",
                FeatSlotKey(2, PfFeatType.SKILL) to "battle-medicine",
                FeatSlotKey(2, PfFeatType.CLASS) to "widen-spell",
            ),
        ))
        assertEquals(listOf("natural-ambition", "dangerous-sorcery", "widen-spell", "battle-medicine", "toughness"), p.feats)
    }

    @Test
    fun `non-proficient armor and weapons are dropped while legal picks stay`() {
        // A sorcerer is only trained unarmored + simple weapons.
        val caster = PathfinderBuilder.build(sorcerer().copy(armor = "full-plate", shield = "buckler", weapons = listOf("longsword", "staff")))
        assertNull(caster.armor)
        assertEquals("buckler", caster.shield)
        assertEquals(listOf("staff"), caster.weapons)
        // A fighter keeps the same picks.
        val fighter = PathfinderBuilder.build(sorcerer().copy(
            classId = "fighter", keyAbility = PfAbility.STRENGTH, subclass = null,
            armor = "full-plate", weapons = listOf("longsword", "staff"),
        ))
        assertEquals("full-plate", fighter.armor)
        assertEquals(listOf("longsword", "staff"), fighter.weapons)
    }

    @Test
    fun `building at level five matches a hand-leveled PfLevelUp chain`() {
        val boosts = listOf(PfAbility.CHARISMA, PfAbility.DEXTERITY, PfAbility.CONSTITUTION, PfAbility.WISDOM)
        val built = PathfinderBuilder.build(sorcerer(targetLevel = 5).copy(
            skillIncreases = mapOf(3 to PfSkill.ARCANA, 5 to PfSkill.DECEPTION),
            advancementBoosts = mapOf(5 to boosts),
        ))
        var hand = Character.new("Seoni", ruleset, PathfinderBuilder.build(sorcerer()), Instant.parse("2026-06-18T00:00:00Z"))
        hand = PfLevelUp().applyTo(hand, ruleset).character
        hand = PfLevelUp(skillIncrease = PfSkill.ARCANA).applyTo(hand, ruleset).character
        hand = PfLevelUp().applyTo(hand, ruleset).character
        hand = PfLevelUp(skillIncrease = PfSkill.DECEPTION, abilityBoosts = boosts).applyTo(hand, ruleset).character
        // Identical payloads except the purse (build seeds level-5 wealth; the chain keeps level 1's).
        assertEquals((hand.payload as PathfinderPayload).copy(coins = emptyMap()), built.copy(coins = emptyMap()))
        assertEquals(5, built.level)
        assertEquals(Proficiency.EXPERT, built.skills[PfSkill.ARCANA])
        assertEquals(19, built.abilityScores.charisma) // 18 at creation, +1 at the level-5 boost
    }
}

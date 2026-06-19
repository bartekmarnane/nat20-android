package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val ruleset = PathfinderRuleset()

private fun character(payload: PathfinderPayload): Character = Character.new("Seoni", ruleset, payload, NOW)
private fun Character.p() = payload as PathfinderPayload

class PathfinderIdentityTests {
    @Test
    fun `identity is the first non-D&D ruleset`() {
        assertEquals("pf2e-remaster", ruleset.id)
        assertEquals("Pathfinder 2e (Remaster)", ruleset.displayName)
        assertTrue(ruleset.makeInitialPayload("Seoni") is PathfinderPayload)
    }
}

class PathfinderStatsTests {
    private val sorcerer = PathfinderPayload(
        className = "sorcerer", level = 3, keyAbility = PfAbility.CHARISMA,
        abilityScores = PfAbilityScores(dexterity = 14, wisdom = 11, charisma = 18),
        perception = Proficiency.TRAINED, unarmoredProficiency = Proficiency.TRAINED, classDC = Proficiency.TRAINED,
        saves = mapOf(Save.WILL to Proficiency.EXPERT),
        skills = mapOf(PfSkill.DIPLOMACY to Proficiency.EXPERT),
    )

    @Test
    fun `derived stats are ability modifier plus level plus rank`() {
        // Perception: WIS 0 + (3 + 2 trained) = 5.
        assertEquals(5, sorcerer.perceptionBonus)
        // AC: 10 + DEX 2 + (3 + 2 trained) = 17.
        assertEquals(17, sorcerer.armorClass)
        // Will save: WIS 0 + (3 + 4 expert) = 7. Untrained Fortitude: CON 0 + 0 = 0.
        assertEquals(7, sorcerer.saveBonus(Save.WILL))
        assertEquals(0, sorcerer.saveBonus(Save.FORTITUDE))
        // Diplomacy (expert): CHA 4 + (3 + 4) = 11. Untrained Athletics: STR 0 + 0 = 0.
        assertEquals(11, sorcerer.skillBonus(PfSkill.DIPLOMACY))
        assertEquals(0, sorcerer.skillBonus(PfSkill.ATHLETICS))
        // Class DC: 10 + CHA 4 + (3 + 2) = 19.
        assertEquals(19, sorcerer.classDcValue)
    }

    @Test
    fun `frightened applies a blanket status penalty`() {
        val scared = sorcerer.copy(conditions = listOf(au.com.evonet.nat20.pf2e.core.ValuedCondition("frightened", 2)))
        assertEquals(sorcerer.perceptionBonus - 2, scared.perceptionBonus)
        assertEquals(sorcerer.skillBonus(PfSkill.DIPLOMACY) - 2, scared.skillBonus(PfSkill.DIPLOMACY))
        assertEquals(sorcerer.armorClass - 2, scared.armorClass)
    }
}

class PathfinderVitalsTests {
    private fun fresh(currentHp: Int = 30, maxHp: Int = 30, dying: Int = 0, wounded: Int = 0) =
        character(PathfinderPayload(maxHp = maxHp, currentHp = currentHp, dying = dying, wounded = wounded))

    @Test
    fun `dropping to 0 HP starts dying at one plus wounded`() {
        val downed = PfTakeDamage(40).applyTo(fresh(wounded = 1), ruleset)
        assertEquals(0, downed.character.p().currentHp)
        assertEquals(2, downed.character.p().dying) // 1 + wounded 1
        assertTrue(downed.event.summary.contains("Dying 2"))
        // Further damage while down increases dying.
        val worse = PfTakeDamage(5).applyTo(downed.character, ruleset)
        assertEquals(3, worse.character.p().dying)
    }

    @Test
    fun `recovering from dying clears it and increases wounded`() {
        val downed = PfTakeDamage(40).applyTo(fresh(), ruleset).character // dying 1
        val healed = PfHeal(10).applyTo(downed, ruleset)
        assertEquals(10, healed.character.p().currentHp)
        assertEquals(0, healed.character.p().dying)
        assertEquals(1, healed.character.p().wounded)
        assertTrue(healed.event.summary.contains("no longer dying"))
    }

    @Test
    fun `hero points clamp and conditions apply or update`() {
        val gained = PfAdjustHeroPoints(1).applyTo(fresh(), ruleset) // 1 -> 2
        assertEquals(2, gained.character.p().heroPoints)
        assertThrows(CharacterIntentError.Invalid::class.java) { PfAdjustHeroPoints(1).applyTo(character(PathfinderPayload(heroPoints = 3)), ruleset) }
        var c = PfApplyCondition("frightened", 2).applyTo(fresh(), ruleset).character
        assertEquals(2, c.p().conditions.first().value)
        c = PfApplyCondition("frightened", 1).applyTo(c, ruleset).character // updates, not duplicates
        assertEquals(1, c.p().conditions.size)
        assertEquals(1, c.p().conditions.first().value)
        c = PfClearCondition("frightened").applyTo(c, ruleset).character
        assertTrue(c.p().conditions.isEmpty())
    }
}

class PathfinderCodecTests {
    @Test
    fun `a full payload round-trips`() {
        val payload = PathfinderPayload(
            ancestry = "human", heritage = "skilled-heritage", background = "noble", className = "sorcerer",
            level = 3, keyAbility = PfAbility.CHARISMA, abilityScores = PfAbilityScores(charisma = 18),
            maxHp = 30, currentHp = 22, dying = 1, wounded = 1, heroPoints = 2,
            perception = Proficiency.TRAINED, classDC = Proficiency.TRAINED,
            saves = mapOf(Save.WILL to Proficiency.EXPERT),
            skills = mapOf(PfSkill.DIPLOMACY to Proficiency.EXPERT),
            loreSkills = mapOf("Genealogy Lore" to Proficiency.TRAINED),
            conditions = listOf(au.com.evonet.nat20.pf2e.core.ValuedCondition("frightened", 1)),
        )
        assertEquals(payload, ruleset.decodePayload(ruleset.encodePayload(payload)))
    }

    @Test
    fun `events round-trip with their type ids`() {
        val events = listOf(
            PfDamageTakenEvent(10, 30, 20),
            PfHealedEvent(5, 0, 5, recoveredFromDying = true),
            PfDyingChangedEvent(1, 0),
            PfHeroPointsChangedEvent(1, 2),
            PfConditionChangedEvent("Frightened", 2, applied = true),
        )
        for (e in events) {
            val typeId = ruleset.eventTypeId(e)
            assertFalse(typeId == "pf2e.unknown")
            assertEquals(e, ruleset.decodeEvent(ruleset.encodeEvent(e), typeId))
        }
    }
}

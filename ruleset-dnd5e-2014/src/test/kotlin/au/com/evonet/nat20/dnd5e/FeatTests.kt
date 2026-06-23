package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val ruleset = DnD5eRuleset()

private fun character(
    classId: String = "fighter",
    level: Int = 3,
    scores: AbilityScores = AbilityScores(),
    maxHp: Int = 24,
    chosenFeats: List<String> = emptyList(),
): Character = Character.new(
    "Aria", ruleset,
    DnD5ePayload(
        classes = listOf(ClassEntry(classId, level)),
        abilityScores = scores,
        maxHp = maxHp, currentHp = maxHp,
        chosenFeats = chosenFeats,
    ),
    NOW,
)

private fun Character.payload() = payload as DnD5ePayload

class FeatCatalogTests {
    @Test
    fun `the catalogue carries the iOS feat set`() {
        assertEquals(29, Feats.all.size)
        assertTrue(Feats.all.map { it.id }.containsAll(listOf("great-weapon-master", "tough", "alert", "resilient", "war-caster")))
    }

    @Test
    fun `half-feats expose their ability options and full feats do not`() {
        assertTrue(Feats.feat("resilient")!!.grantsAbilityIncrease)
        assertEquals(listOf(Ability.STRENGTH, Ability.DEXTERITY), Feats.feat("athlete")!!.halfFeatAbilities)
        assertFalse(Feats.feat("tough")!!.grantsAbilityIncrease)
    }

    @Test
    fun `prerequisites gate availability`() {
        val nonCaster = AbilityScores(charisma = 10)
        // War Caster needs spellcasting; Inspiring Leader needs CHA 13.
        assertFalse(Feats.feat("war-caster")!!.isAvailable(nonCaster, isSpellcaster = false))
        assertTrue(Feats.feat("war-caster")!!.isAvailable(nonCaster, isSpellcaster = true))
        assertFalse(Feats.feat("inspiring-leader")!!.isAvailable(AbilityScores(charisma = 12), isSpellcaster = false))
        assertTrue(Feats.feat("inspiring-leader")!!.isAvailable(AbilityScores(charisma = 13), isSpellcaster = false))
        // No prerequisite ⇒ always available.
        assertTrue(Feats.feat("alert")!!.isAvailable(nonCaster))
    }
}

class FeatLevelUpTests {
    @Test
    fun `taking a feat records it and journals the name`() {
        val result = LevelUp("fighter", feat = "alert", className = "Fighter").applyTo(character(level = 3), ruleset)
        assertTrue("alert" in result.character.payload().chosenFeats)
        assertTrue(result.event.summary.contains("Alert"))
    }

    @Test
    fun `a half-feat's plus-one rides through abilityIncreases`() {
        val c = character(level = 3, scores = AbilityScores(strength = 15))
        val result = LevelUp("fighter", abilityIncreases = mapOf(Ability.STRENGTH to 1), feat = "athlete").applyTo(c, ruleset)
        val p = result.character.payload()
        assertEquals(16, p.abilityScores.strength)
        assertTrue("athlete" in p.chosenFeats)
    }

    @Test
    fun `an unknown feat is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            LevelUp("fighter", feat = "not-a-feat").applyTo(character(), ruleset)
        }
    }

    @Test
    fun `Tough raises effective max HP by two per level and tops up on a long rest`() {
        // Base fighter L3, maxHp 24. Tough adds +2/level = +6 → 30.
        val toughened = character(level = 3, maxHp = 24, chosenFeats = listOf("tough"))
        assertEquals(30, toughened.payload().effectiveMaxHp)
        // A long rest fills to the effective max.
        val damaged = toughened.payload().copy(currentHp = 10)
        val rested = LongRest().applyTo(Character.new("Aria", ruleset, damaged, NOW), ruleset)
        assertEquals(30, rested.character.payload().currentHp)
    }

    @Test
    fun `taking Tough this level raises the max and adds the new level's share to current`() {
        val c = character(level = 3, maxHp = 24) // full at 24, no feats
        val result = LevelUp("fighter", hpChoice = au.com.evonet.nat20.dnd5e.core.HpChoice.Average, feat = "tough", className = "Fighter").applyTo(c, ruleset)
        val p = result.character.payload()
        // effectiveMaxHp = base(24 + avg gain) + 2*4 level. Current got +avg +2 (this level's Tough share).
        assertEquals(p.maxHp + 8, p.effectiveMaxHp)
        assertTrue(p.currentHp < p.effectiveMaxHp) // retroactive Tough HP shows as missing, RAW-ish
    }

    @Test
    fun `Alert adds five to the initiative bonus`() {
        assertEquals(character(scores = AbilityScores(dexterity = 14)).payload().initiativeBonus, 2)
        assertEquals(character(scores = AbilityScores(dexterity = 14), chosenFeats = listOf("alert")).payload().initiativeBonus, 7)
    }
}

class FeatCodecTests {
    @Test
    fun `chosenFeats round-trip through the payload codec`() {
        val payload = DnD5ePayload(classes = listOf(ClassEntry("fighter", 4)), chosenFeats = listOf("alert", "tough"))
        assertEquals(payload, ruleset.decodePayload(ruleset.encodePayload(payload)))
    }

    @Test
    fun `a level-up event carrying a feat round-trips`() {
        val event = LeveledUpEvent("fighter", "Fighter", false, 3, 4, 4, au.com.evonet.nat20.dnd5e.core.HpChoice.Average, 6, feat = "alert")
        val typeId = ruleset.eventTypeId(event)
        assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
    }
}

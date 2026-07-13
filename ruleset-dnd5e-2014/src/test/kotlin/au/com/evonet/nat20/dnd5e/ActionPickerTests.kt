package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AttackOutcome
import au.com.evonet.nat20.dnd5e.core.SaveOutcome
import au.com.evonet.nat20.domain.Character
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-07-13T00:00:00Z")
private val ruleset = DnD5eRuleset()

private fun spell(id: String): Spell = DnD5eCatalog.spellLibrary.first { it.index == id }

private fun wizard(level: Int = 5): Character {
    val payload = DnD5ePayload(
        classes = listOf(ClassEntry("wizard", level)),
        maxHp = 22,
        currentHp = 22,
        cantripsKnown = listOf("fire-bolt"),
        spellsKnown = mapOf("wizard" to listOf("magic-missile", "fireball")),
    ).let { it.copy(currentSpellSlots = it.maxSpellSlots) }
    return Character.new("Wisp", ruleset, payload, NOW)
}

/**
 * Parity #19 slice B — the resolution metadata the cast/attack/check pickers
 * feed the engine: SRD spell resolution fields, CastSpell/MakeAttack/RollCheck
 * resolution passthrough, and the race-trait save-advantage hints.
 */
class SpellResolutionTests {

    @ParameterizedTest(name = "{0} resolves as {1}")
    @CsvSource(
        "fire-bolt, ATTACK",
        "fireball, SAVE",
        "magic-missile, DAMAGE_ONLY",
        "bless, UTILITY",
        "cure-wounds, UTILITY",
    )
    fun `resolution kind derives from the SRD fields`(id: String, expected: SpellResolutionKind) {
        assertEquals(expected, spell(id).resolutionKind)
    }

    @Test
    fun `fire bolt is a ranged attack cantrip scaling by character level`() {
        val s = spell("fire-bolt")
        assertEquals("ranged", s.attackType)
        assertTrue(s.dealsDamage)
        assertEquals("1d10", s.damageDice(slotLevel = 0, characterLevel = 1))
        assertEquals("2d10", s.damageDice(slotLevel = 0, characterLevel = 5))
        assertEquals("4d10", s.damageDice(slotLevel = 0, characterLevel = 20))
    }

    @Test
    fun `fireball is a DEX save for half scaling by slot`() {
        val s = spell("fireball")
        assertEquals("DEX", s.dc?.dcType?.name)
        assertTrue(s.saveHalves)
        assertEquals("8d6", s.damageDice(slotLevel = 3, characterLevel = 5))
        assertEquals("9d6", s.damageDice(slotLevel = 4, characterLevel = 5))
    }

    @Test
    fun `utility spells report no damage dice`() {
        assertFalse(spell("bless").dealsDamage)
        assertNull(spell("bless").damageDice(slotLevel = 1, characterLevel = 5))
    }
}

class CastResolutionIntentTests {

    @Test
    fun `cast spell threads attack resolution into the event`() {
        val result = CastSpell(
            spellID = "fire-bolt", spellName = "Fire Bolt", spellLevel = 0, slotLevel = 0,
            target = "Goblin", attackD20 = 20, attackTotal = 24, attackOutcome = AttackOutcome.CRITICAL,
            damage = 18, damageType = "fire", targetKilled = true,
        ).applyTo(wizard(), ruleset)
        val event = result.event as CastSpellEvent
        assertEquals(AttackOutcome.CRITICAL, event.attackOutcome)
        assertEquals(20, event.attackD20)
        assertEquals(18, event.damage)
        assertTrue(event.targetKilled)
        assertTrue(event.summary.contains("critical hit"))
        assertTrue(event.summary.contains("for 18 fire damage"))
        assertTrue(event.summary.contains("felled"))
    }

    @Test
    fun `cast spell threads a halved save into the event`() {
        val result = CastSpell(
            spellID = "fireball", spellName = "Fireball", spellLevel = 3, slotLevel = 3,
            saveDC = 15, saveAbility = "DEX", saveOutcome = SaveOutcome.PASSED,
            damageHalvedBySave = true, damage = 14, damageType = "fire",
        ).applyTo(wizard(), ruleset)
        val event = result.event as CastSpellEvent
        assertEquals(SaveOutcome.PASSED, event.saveOutcome)
        assertTrue(event.summary.contains("target saved vs DC 15 (half damage)"))
        assertTrue(event.summary.contains("for 14 fire damage"))
        // The slot still drains as before.
        val payload = result.character.payload as DnD5ePayload
        assertEquals((payload.maxSpellSlots[3] ?: 0) - 1, payload.currentSpellSlots[3])
    }

    @Test
    fun `cast event with resolution round-trips through the codec`() {
        val event = CastSpell(
            spellID = "fireball", spellName = "Fireball", spellLevel = 3, slotLevel = 4,
            target = "Ogres", saveDC = 15, saveAbility = "DEX", saveOutcome = SaveOutcome.FAILED,
            damage = 30, damageType = "fire", targetKilled = true,
        ).applyTo(wizard(7), ruleset).event
        val typeId = ruleset.eventTypeId(event)
        assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
    }

    @Test
    fun `attack intent threads target killed except on a miss`() {
        val fighter = Character.new(
            "Bron",
            ruleset,
            DnD5ePayload(classes = listOf(ClassEntry("fighter", 3)), maxHp = 28, currentHp = 28),
            NOW,
        )
        val hit = MakeAttack("Longsword", 19, AttackOutcome.HIT, damage = 9, targetKilled = true)
            .applyTo(fighter, ruleset).event as AttackEvent
        assertTrue(hit.targetKilled)
        assertTrue(hit.summary.contains("felled"))

        val miss = MakeAttack("Longsword", 4, AttackOutcome.MISS, targetKilled = true)
            .applyTo(fighter, ruleset).event as AttackEvent
        assertFalse(miss.targetKilled)
    }

    @Test
    fun `roll check records the player's judgment and note without a DC`() {
        val event = RollCheck("Stealth check", total = 17, naturalD20 = 14, judgedSuccess = true, note = "past the guards")
            .applyTo(wizard(), ruleset).event as CheckRolledEvent
        assertEquals(true, event.success)
        assertEquals("past the guards", event.note)
        assertTrue(event.summary.contains("success"))
        assertTrue(event.summary.contains("past the guards"))
        // A DC still wins over the player's call.
        val judged = RollCheck("Stealth check", total = 9, dc = 15, judgedSuccess = true)
            .applyTo(wizard(), ruleset).event as CheckRolledEvent
        assertEquals(false, judged.success)
    }
}

class SaveAdvantageHintTests {

    @ParameterizedTest(name = "{0} vs {1}/{2} magical={3} → {4}")
    @CsvSource(
        "hill-dwarf, poison, , false, Dwarven Resilience",
        "stout-halfling, poison, , false, Stout Resilience",
        "stout-halfling, , Poisoned, false, Stout Resilience",
        "wood-elf, , Charmed, false, Fey Ancestry",
        "half-elf, , Charmed, false, Fey Ancestry",
        "lightfoot-halfling, , Frightened, false, Brave",
        "rock-gnome, , , true, Gnome Cunning",
    )
    fun `race traits light up save advantage`(
        race: String,
        damageType: String?,
        condition: String?,
        magical: Boolean,
        trait: String,
    ) {
        val ability = if (race == "rock-gnome") Ability.WISDOM else Ability.CONSTITUTION
        val hint = RaceTraits.advantageOnSave(race, ability, damageType, condition, magical)
        assertNotNull(hint)
        assertEquals(trait, hint?.traitName)
    }

    @Test
    fun `no context or wrong ability yields no hint`() {
        assertNull(RaceTraits.advantageOnSave("hill-dwarf", Ability.CONSTITUTION))
        assertNull(RaceTraits.advantageOnSave("rock-gnome", Ability.STRENGTH, isMagicalSource = true))
        assertNull(RaceTraits.advantageOnSave("human", Ability.WISDOM, damageType = "poison"))
    }
}

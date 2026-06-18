package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.CastingProgression
import au.com.evonet.nat20.dnd5e.core.SpellSlotTable
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val ruleset = DnD5eRuleset()

private fun caster(
    vararg classes: ClassEntry,
    abilityScores: AbilityScores = AbilityScores(),
    maxHp: Int = 20,
    currentHp: Int = maxHp,
    tempHp: Int = 0,
    cantrips: List<String> = emptyList(),
    known: Map<String, List<String>> = emptyMap(),
    prepared: Map<String, List<String>> = emptyMap(),
    currentSlots: Map<Int, Int>? = null,
    pact: Int? = null,
): Character {
    var payload = DnD5ePayload(
        classes = classes.toList(),
        abilityScores = abilityScores,
        maxHp = maxHp,
        currentHp = currentHp,
        temporaryHp = tempHp,
        cantripsKnown = cantrips,
        spellsKnown = known,
        preparedSpells = prepared,
    )
    payload = if (currentSlots != null || pact != null) {
        payload.copy(
            currentSpellSlots = currentSlots ?: payload.maxSpellSlots,
            currentPactSlots = pact ?: payload.maxPactSlots,
        )
    } else {
        payload.withFullSpellSlots()
    }
    return Character.new("Mage", ruleset, payload, NOW)
}

private fun Character.payload() = payload as DnD5ePayload

// ── Slot tables (core) ────────────────────────────────────────────────────────

class SpellSlotTableTests {
    @Test
    fun `full caster spot-checks across the table`() {
        assertEquals(mapOf(1 to 2), SpellSlotTable.slots(CastingProgression.FULL, 1))
        assertEquals(mapOf(1 to 4, 2 to 3, 3 to 2), SpellSlotTable.slots(CastingProgression.FULL, 5))
        assertEquals(
            mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 2, 7 to 2, 8 to 1, 9 to 1),
            SpellSlotTable.slots(CastingProgression.FULL, 20),
        )
    }

    @Test
    fun `half caster has no slots at level one and ramps from two`() {
        assertEquals(emptyMap<Int, Int>(), SpellSlotTable.slots(CastingProgression.HALF, 1))
        assertEquals(mapOf(1 to 2), SpellSlotTable.slots(CastingProgression.HALF, 2))
        assertEquals(mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2), SpellSlotTable.slots(CastingProgression.HALF, 20))
    }

    @Test
    fun `third caster has no slots until level three`() {
        assertEquals(emptyMap<Int, Int>(), SpellSlotTable.slots(CastingProgression.THIRD, 2))
        assertEquals(mapOf(1 to 2), SpellSlotTable.slots(CastingProgression.THIRD, 3))
    }

    @ParameterizedTest
    @CsvSource("1,1,1", "2,2,1", "5,2,3", "11,3,5", "17,4,5", "20,4,5")
    fun `warlock pact slot count and level`(level: Int, expectedCount: Int, expectedLevel: Int) {
        assertEquals(expectedCount, SpellSlotTable.warlockSlotCount(level))
        assertEquals(expectedLevel, SpellSlotTable.warlockSlotLevel(level))
        assertEquals(mapOf(expectedLevel to expectedCount), SpellSlotTable.slots(CastingProgression.WARLOCK, level))
    }
}

// ── Progression + multiclass math ─────────────────────────────────────────────

class CastingProgressionTests {
    @Test
    fun `base class progressions`() {
        assertEquals(CastingProgression.FULL, Spellcasting.progression(ClassEntry("wizard", 5)))
        assertEquals(CastingProgression.HALF, Spellcasting.progression(ClassEntry("paladin", 5)))
        assertEquals(CastingProgression.WARLOCK, Spellcasting.progression(ClassEntry("warlock", 5)))
        assertEquals(CastingProgression.NONE, Spellcasting.progression(ClassEntry("barbarian", 5)))
    }

    @Test
    fun `eldritch knight and arcane trickster are third casters via subclass`() {
        assertEquals(CastingProgression.THIRD, Spellcasting.progression(ClassEntry("fighter", 3, "eldritch-knight")))
        assertEquals(CastingProgression.THIRD, Spellcasting.progression(ClassEntry("rogue", 3, "arcane trickster")))
        assertEquals(CastingProgression.NONE, Spellcasting.progression(ClassEntry("fighter", 3, "champion")))
        assertEquals(Ability.INTELLIGENCE, Spellcasting.spellcastingAbility(ClassEntry("fighter", 3, "eldritch-knight")))
    }

    @Test
    fun `multiclass combines caster levels against the full table`() {
        // Wizard 6 (full → 6) + Paladin 4 (half → 2) + Fighter/EK 3 (third → 1) = caster level 9.
        val classes = listOf(ClassEntry("wizard", 6), ClassEntry("paladin", 4), ClassEntry("fighter", 3, "eldritch-knight"))
        assertEquals(9, Spellcasting.combinedCasterLevel(classes))
        assertEquals(SpellSlotTable.slots(CastingProgression.FULL, 9), Spellcasting.combinedSpellSlots(classes))
    }

    @Test
    fun `warlock levels stay out of the combined caster level and drive pact slots`() {
        val classes = listOf(ClassEntry("warlock", 5), ClassEntry("sorcerer", 2))
        assertEquals(2, Spellcasting.combinedCasterLevel(classes)) // only the sorcerer counts
        assertEquals(2, Spellcasting.maxPactSlots(classes))
        assertEquals(3, Spellcasting.pactSlotLevel(classes))
    }

    @Test
    fun `prepared limit and spellcasting ability by class`() {
        assertEquals(Ability.INTELLIGENCE, CastingProgression.spellcastingAbility("wizard"))
        assertTrue(CastingProgression.usesPreparation("cleric"))
        assertFalse(CastingProgression.usesPreparation("sorcerer"))
        // Cleric 5, WIS +3 → 8 prepared.
        assertEquals(8, CastingProgression.preparedSpellLimit("cleric", 5, 3))
        // Paladin 6, CHA +2 → 5 prepared.
        assertEquals(5, CastingProgression.preparedSpellLimit("paladin", 6, 2))
    }
}

// ── Derived payload slot state ────────────────────────────────────────────────

class DerivedSlotStateTests {
    @Test
    fun `total slots merge pact into the regular pool at the pact level`() {
        // Warlock 5 + Wizard 1: caster level 1 → {1:2} regular; pact 2 slots at level 3.
        val p = caster(ClassEntry("warlock", 5), ClassEntry("wizard", 1)).payload()
        assertEquals(mapOf(1 to 2), p.maxSpellSlots)
        assertEquals(2, p.maxPactSlots)
        assertEquals(3, p.pactSlotLevel)
        assertEquals(mapOf(1 to 2, 3 to 2), p.totalMaxSlots)
        assertEquals(mapOf(1 to 2, 3 to 2), p.totalCurrentSlots) // seeded full
    }

    @Test
    fun `castable ids union cantrips with prepared and known buckets`() {
        val p = caster(
            ClassEntry("wizard", 3), // prepared caster
            cantrips = listOf("fire-bolt", "light"),
            known = mapOf("wizard" to listOf("ignored-spellbook-entry")),
            prepared = mapOf("wizard" to listOf("magic-missile", "shield")),
        ).payload()
        // Prepared caster: known bucket is the spellbook (not castable); prepared + cantrips are.
        assertEquals(setOf("fire-bolt", "light", "magic-missile", "shield"), p.castableSpellIDs)
    }
}

// ── Spell intents ─────────────────────────────────────────────────────────────

class SpellIntentTests {
    @Test
    fun `casting a leveled spell consumes the matching slot`() {
        val wizard = caster(ClassEntry("wizard", 3)) // {1:4, 2:2}
        val result = CastSpell("magic-missile", "Magic Missile", spellLevel = 1, slotLevel = 1).applyTo(wizard, ruleset)
        assertEquals(3, result.character.payload().currentSpellSlots[1])
        assertEquals("Cast Magic Missile", result.event.summary)
    }

    @Test
    fun `upcasting drains the higher slot and the event notes it`() {
        val wizard = caster(ClassEntry("wizard", 3))
        val result = CastSpell("magic-missile", "Magic Missile", spellLevel = 1, slotLevel = 2).applyTo(wizard, ruleset)
        assertEquals(1, result.character.payload().currentSpellSlots[2]) // 2 → 1
        assertEquals("Cast Magic Missile at 2nd level", result.event.summary)
    }

    @Test
    fun `cantrips, rituals, and scrolls consume no slot`() {
        val wizard = caster(ClassEntry("wizard", 3))
        val before = wizard.payload().currentSpellSlots
        val cantrip = CastSpell("fire-bolt", "Fire Bolt", spellLevel = 0, slotLevel = 0).applyTo(wizard, ruleset)
        assertEquals(before, cantrip.character.payload().currentSpellSlots)
        val ritual = CastSpell("detect-magic", "Detect Magic", spellLevel = 1, slotLevel = 1, asRitual = true).applyTo(wizard, ruleset)
        assertEquals(before, ritual.character.payload().currentSpellSlots)
        assertEquals("Cast Detect Magic as a ritual", ritual.event.summary)
        val scroll = CastSpell("fireball", "Fireball", spellLevel = 3, slotLevel = 3, fromScroll = true).applyTo(wizard, ruleset)
        assertEquals(before, scroll.character.payload().currentSpellSlots)
    }

    @Test
    fun `casting at the pact level drains pact slots first`() {
        // Pure warlock 5: no regular slots, 2 pact slots at level 3.
        val warlock = caster(ClassEntry("warlock", 5))
        val result = CastSpell("fireball", "Fireball", spellLevel = 3, slotLevel = 3).applyTo(warlock, ruleset)
        val p = result.character.payload()
        assertEquals(1, p.currentPactSlots)
        assertTrue((result.event as CastSpellEvent).fromPact)
    }

    @Test
    fun `casting below the spell level is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            CastSpell("fireball", "Fireball", spellLevel = 3, slotLevel = 2).applyTo(caster(ClassEntry("wizard", 5)), ruleset)
        }
    }

    @Test
    fun `casting with no slots remaining is rejected`() {
        val wizard = caster(ClassEntry("wizard", 1), currentSlots = emptyMap(), pact = 0)
        assertThrows(CharacterIntentError.Invalid::class.java) {
            CastSpell("magic-missile", "Magic Missile", spellLevel = 1, slotLevel = 1).applyTo(wizard, ruleset)
        }
    }

    @Test
    fun `prepare and unprepare mutate the per-class bucket`() {
        var cleric = caster(ClassEntry("cleric", 3))
        cleric = PrepareSpell("bless", "Bless", "cleric").applyTo(cleric, ruleset).character
        cleric = PrepareSpell("cure-wounds", "Cure Wounds", "cleric").applyTo(cleric, ruleset).character
        assertEquals(listOf("bless", "cure-wounds"), cleric.payload().preparedSpells["cleric"])

        cleric = UnprepareSpell("bless", "Bless", "cleric").applyTo(cleric, ruleset).character
        assertEquals(listOf("cure-wounds"), cleric.payload().preparedSpells["cleric"])
    }

    @Test
    fun `preparing the same spell twice is a no-op`() {
        var cleric = caster(ClassEntry("cleric", 3))
        cleric = PrepareSpell("bless", "Bless", "cleric").applyTo(cleric, ruleset).character
        val again = PrepareSpell("bless", "Bless", "cleric").applyTo(cleric, ruleset)
        assertEquals(listOf("bless"), again.character.payload().preparedSpells["cleric"])
        assertFalse((again.event as SpellPreparedEvent).wasNew)
    }

    @Test
    fun `expend spell slot drains and validates the range`() {
        val wizard = caster(ClassEntry("wizard", 3))
        val result = ExpendSpellSlot(1, source = "Counterspell").applyTo(wizard, ruleset)
        assertEquals(3, result.character.payload().currentSpellSlots[1])
        assertThrows(CharacterIntentError.Invalid::class.java) {
            ExpendSpellSlot(0).applyTo(wizard, ruleset)
        }
    }
}

// ── Rests ─────────────────────────────────────────────────────────────────────

class RestIntentTests {
    @Test
    fun `short rest restores only pact slots`() {
        // Warlock 5 with both pools spent.
        val warlock = caster(ClassEntry("warlock", 5), ClassEntry("wizard", 2), currentSlots = mapOf(1 to 0), pact = 0)
        val result = ShortRest().applyTo(warlock, ruleset)
        val p = result.character.payload()
        assertEquals(2, p.currentPactSlots) // restored
        assertEquals(0, p.currentSpellSlots[1] ?: 0) // regular untouched
        assertEquals("Took a short rest — recovered 2 pact slots", result.event.summary)
    }

    @Test
    fun `long rest restores HP, clears temp, and refills all slots`() {
        val wizard = caster(
            ClassEntry("wizard", 5),
            maxHp = 30, currentHp = 11, tempHp = 4,
            currentSlots = mapOf(1 to 1), pact = 0,
        )
        val result = LongRest().applyTo(wizard, ruleset)
        val p = result.character.payload()
        assertEquals(30, p.currentHp)
        assertEquals(0, p.temporaryHp)
        assertEquals(p.maxSpellSlots, p.currentSpellSlots)
        val event = result.event as LongRestEvent
        assertEquals(19, event.hpRestored)
        assertEquals(4, event.tempCleared)
    }
}

// ── Hit dice ──────────────────────────────────────────────────────────────────

class HitDiceTests {
    private fun fighter(level: Int, maxHp: Int = 40, currentHp: Int = maxHp, spent: Int = 0): Character =
        Character.new(
            "Bron", ruleset,
            DnD5ePayload(classes = listOf(ClassEntry("fighter", level)), maxHp = maxHp, currentHp = currentHp, hitDiceSpent = spent),
            NOW,
        )

    @Test
    fun `current and max hit dice derive from level and spent`() {
        val p = fighter(level = 5, spent = 2).payload()
        assertEquals(5, p.maxHitDice)
        assertEquals(3, p.currentHitDice)
    }

    @Test
    fun `spending a hit die heals, clamps to max, and decrements the pool`() {
        val result = SpendHitDie(healingRolled = 9).applyTo(fighter(level = 5, maxHp = 40, currentHp = 35), ruleset)
        val p = result.character.payload()
        assertEquals(40, p.currentHp) // 35 + 9 clamped to 40
        assertEquals(1, p.hitDiceSpent)
        assertEquals(4, p.currentHitDice)
        val event = result.event as HitDieSpentEvent
        assertEquals(4, event.remaining)
    }

    @Test
    fun `spending with no hit dice left is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            SpendHitDie(5).applyTo(fighter(level = 3, spent = 3), ruleset)
        }
    }

    @Test
    fun `long rest regains half the spent hit dice, minimum one`() {
        // Level 10, 5 spent → regain max(1, 5/2)=2 → 3 spent left.
        val result = LongRest().applyTo(fighter(level = 10, maxHp = 80, currentHp = 40, spent = 5), ruleset)
        val p = result.character.payload()
        assertEquals(3, p.hitDiceSpent)
        assertEquals(7, p.currentHitDice)
        assertEquals(2, (result.event as LongRestEvent).hitDiceRegained)
    }

    @Test
    fun `long rest with one spent die still regains one`() {
        val result = LongRest().applyTo(fighter(level = 4, spent = 1), ruleset)
        assertEquals(0, result.character.payload().hitDiceSpent)
    }
}

// ── Codec ─────────────────────────────────────────────────────────────────────

class SpellCodecTests {
    @Test
    fun `payload with spell state round-trips`() {
        val payload = DnD5ePayload(
            classes = listOf(ClassEntry("wizard", 5)),
            cantripsKnown = listOf("fire-bolt", "light"),
            spellsKnown = mapOf("wizard" to listOf("fireball", "shield")),
            preparedSpells = mapOf("wizard" to listOf("magic-missile")),
            currentSpellSlots = mapOf(1 to 3, 2 to 3, 3 to 1),
            currentPactSlots = 0,
        )
        val decoded = ruleset.decodePayload(ruleset.encodePayload(payload))
        assertEquals(payload, decoded)
    }

    @Test
    fun `spell and rest events round-trip with their type ids`() {
        val events = listOf(
            CastSpellEvent("fireball", "Fireball", 3, 4, wasUpcast = true, target = "the goblins"),
            SpellPreparedEvent("bless", "Bless", "cleric", wasNew = true),
            SpellUnpreparedEvent("bless", "Bless", "cleric", wasRemoved = true),
            SpellSlotExpendedEvent(2, fromPactPool = true, remaining = 0, source = "Counterspell"),
            ShortRestEvent(pactSlotsRestored = 2),
            LongRestEvent(hpRestored = 12, tempCleared = 4, slotsRestored = 9, hitDiceRegained = 3),
            HitDieSpentEvent(healingRolled = 9, previousHp = 20, newHp = 29, remaining = 4),
        )
        for (event in events) {
            val typeId = ruleset.eventTypeId(event)
            assertFalse(typeId == "dnd5e.unknown")
            assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
        }
    }
}

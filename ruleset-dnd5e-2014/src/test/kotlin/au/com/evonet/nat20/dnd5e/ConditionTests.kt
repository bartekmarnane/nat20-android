package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Condition
import au.com.evonet.nat20.dnd5e.core.Exhaustion
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
    conditions: List<String> = emptyList(),
    exhaustion: Int = 0,
    currentHp: Int = 20,
    maxHp: Int = 20,
): Character = Character.new(
    "Aria", ruleset,
    DnD5ePayload(
        classes = listOf(ClassEntry("fighter", 3)),
        maxHp = maxHp,
        currentHp = currentHp,
        activeConditions = conditions,
        exhaustionLevel = exhaustion,
    ),
    NOW,
)

private fun Character.payload() = payload as DnD5ePayload

class ConditionCatalogueTests {
    @Test
    fun `fourteen standard conditions, exhaustion excluded`() {
        assertEquals(14, Condition.entries.size)
        assertTrue(Condition.entries.none { it.displayName.equals("exhaustion", ignoreCase = true) })
    }

    @Test
    fun `condition names resolve case-insensitively`() {
        assertEquals(Condition.POISONED, Condition.fromName("poisoned"))
        assertEquals(Condition.PRONE, Condition.fromName("  Prone "))
        assertEquals(null, Condition.fromName("cursed")) // homebrew → not standard
    }

    @Test
    fun `exhaustion clamps and flags death at six`() {
        assertEquals(0, Exhaustion.clamp(-3))
        assertEquals(6, Exhaustion.clamp(9))
        assertTrue(Exhaustion.isFatal(6))
        assertFalse(Exhaustion.isFatal(5))
        assertEquals("Hit point maximum halved", Exhaustion.effect(4))
    }
}

class ConditionIntentTests {
    @Test
    fun `applying a condition adds it once`() {
        var c = character()
        c = ApplyCondition("Poisoned", source = "giant spider").applyTo(c, ruleset).character
        assertEquals(listOf("Poisoned"), c.payload().activeConditions)

        val again = ApplyCondition("poisoned").applyTo(c, ruleset) // case-insensitive dedupe
        assertEquals(listOf("Poisoned"), again.character.payload().activeConditions)
        assertFalse((again.event as ConditionAppliedEvent).wasNew)
    }

    @Test
    fun `applying reports the source in the summary`() {
        val result = ApplyCondition("Frightened", source = "the dragon").applyTo(character(), ruleset)
        assertEquals("Gained the Frightened condition from the dragon", result.event.summary)
    }

    @Test
    fun `an empty condition name is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            ApplyCondition("  ").applyTo(character(), ruleset)
        }
    }

    @Test
    fun `clearing removes case-insensitively and reports presence`() {
        val present = ClearCondition("prone").applyTo(character(conditions = listOf("Prone", "Poisoned")), ruleset)
        assertEquals(listOf("Poisoned"), present.character.payload().activeConditions)
        assertTrue((present.event as ConditionClearedEvent).wasPresent)

        val absent = ClearCondition("Stunned").applyTo(character(conditions = listOf("Poisoned")), ruleset)
        assertFalse((absent.event as ConditionClearedEvent).wasPresent)
    }

    @Test
    fun `homebrew condition names are allowed`() {
        val result = ApplyCondition("Cursed").applyTo(character(), ruleset)
        assertEquals(listOf("Cursed"), result.character.payload().activeConditions)
    }
}

class ExhaustionIntentTests {
    @Test
    fun `raising exhaustion clamps and death at six`() {
        var c = character(exhaustion = 5)
        c = AdjustExhaustion(1).applyTo(c, ruleset).character
        assertEquals(6, c.payload().exhaustionLevel)
        assertTrue(c.payload().isDead)
        assertThrows(CharacterIntentError.Invalid::class.java) { AdjustExhaustion(1).applyTo(c, ruleset) } // already max
    }

    @Test
    fun `lowering below zero is rejected at zero`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            AdjustExhaustion(-1).applyTo(character(exhaustion = 0), ruleset)
        }
    }

    @Test
    fun `a zero adjustment is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            AdjustExhaustion(0).applyTo(character(exhaustion = 2), ruleset)
        }
    }

    @Test
    fun `long rest sheds one level of exhaustion`() {
        val result = LongRest().applyTo(character(exhaustion = 3, currentHp = 10), ruleset)
        assertEquals(2, result.character.payload().exhaustionLevel)
        assertTrue((result.event as LongRestEvent).exhaustionRecovered)
    }
}

class ConditionCodecTests {
    @Test
    fun `payload with conditions and exhaustion round-trips`() {
        val payload = DnD5ePayload(
            classes = listOf(ClassEntry("fighter", 3)),
            activeConditions = listOf("Poisoned", "Prone"),
            exhaustionLevel = 2,
        )
        assertEquals(payload, ruleset.decodePayload(ruleset.encodePayload(payload)))
    }

    @Test
    fun `condition and exhaustion events round-trip with their type ids`() {
        val events = listOf(
            ConditionAppliedEvent("Poisoned", source = "spider", wasNew = true),
            ConditionClearedEvent("Prone", wasPresent = true),
            ExhaustionChangedEvent(previousLevel = 2, newLevel = 3),
        )
        for (event in events) {
            val typeId = ruleset.eventTypeId(event)
            assertFalse(typeId == "dnd5e.unknown")
            assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
        }
    }
}

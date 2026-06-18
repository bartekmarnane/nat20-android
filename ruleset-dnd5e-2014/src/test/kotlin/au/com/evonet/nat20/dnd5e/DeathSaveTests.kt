package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.DeathSaveOutcome
import au.com.evonet.nat20.dnd5e.core.DeathSaves
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

private fun downed(
    successes: Int = 0,
    failures: Int = 0,
    currentHp: Int = 0,
    maxHp: Int = 20,
): Character = Character.new(
    "Aria", ruleset,
    DnD5ePayload(
        classes = listOf(ClassEntry("fighter", 3)),
        maxHp = maxHp,
        currentHp = currentHp,
        deathSaves = DeathSaves(successes, failures),
    ),
    NOW,
)

private fun Character.payload() = payload as DnD5ePayload

class DeathSavesModelTests {
    @Test
    fun `state flags reflect the thresholds`() {
        assertTrue(DeathSaves(successes = 3).isStable)
        assertTrue(DeathSaves(failures = 3).isDead)
        assertTrue(DeathSaves.cleared.isCleared)
        assertFalse(DeathSaves(successes = 1, failures = 2).isCleared)
    }

    @Test
    fun `counts out of range are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { DeathSaves(successes = 4) }
        assertEquals(DeathSaves(3, 1), DeathSaves.clamped(successes = 9, failures = 1))
    }
}

class DeathSavePayloadTests {
    @Test
    fun `isDying only while down with an open tracker`() {
        assertTrue(downed().payload().isDying)
        assertFalse(downed(currentHp = 5).payload().isDying) // conscious
        assertFalse(downed(successes = 3).payload().isDying) // stable
        assertFalse(downed(failures = 3).payload().isDying)  // dead
        assertTrue(downed(failures = 3).payload().isDead)
    }
}

class MarkDeathSaveTests {
    @Test
    fun `successes accumulate to stable`() {
        var c = downed()
        repeat(2) { c = MarkDeathSave(DeathSaveOutcome.SUCCESS).applyTo(c, ruleset).character }
        assertFalse(c.payload().deathSaves.isStable)
        val third = MarkDeathSave(DeathSaveOutcome.SUCCESS).applyTo(c, ruleset)
        assertTrue(third.character.payload().deathSaves.isStable)
        assertEquals("Stabilized — three death-save successes", third.event.summary)
    }

    @Test
    fun `failures accumulate to death`() {
        var c = downed(failures = 2)
        val third = MarkDeathSave(DeathSaveOutcome.FAILURE).applyTo(c, ruleset)
        assertTrue(third.character.payload().deathSaves.isDead)
        assertEquals("Fell — three death-save failures", third.event.summary)
    }

    @Test
    fun `a single failure reports progress`() {
        val result = MarkDeathSave(DeathSaveOutcome.FAILURE).applyTo(downed(), ruleset)
        assertEquals("Death save failure (1/3)", result.event.summary)
    }

    @Test
    fun `marking past stable or dead is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            MarkDeathSave(DeathSaveOutcome.SUCCESS).applyTo(downed(successes = 3), ruleset)
        }
        assertThrows(CharacterIntentError.Invalid::class.java) {
            MarkDeathSave(DeathSaveOutcome.FAILURE).applyTo(downed(failures = 3), ruleset)
        }
    }

    @Test
    fun `clearing an already-clear tracker is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            MarkDeathSave(DeathSaveOutcome.CLEAR).applyTo(downed(), ruleset)
        }
        val cleared = MarkDeathSave(DeathSaveOutcome.CLEAR).applyTo(downed(successes = 1, failures = 2), ruleset)
        assertTrue(cleared.character.payload().deathSaves.isCleared)
    }
}

class DeathSaveRecoveryTests {
    @Test
    fun `healing off zero clears death saves and flags the revive`() {
        val result = Heal(amount = 5).applyTo(downed(successes = 1, failures = 2, currentHp = 0), ruleset)
        val p = result.character.payload()
        assertEquals(5, p.currentHp)
        assertTrue(p.deathSaves.isCleared)
        assertTrue((result.event as HealedEvent).revived)
    }

    @Test
    fun `healing an already-conscious character does not flag a revive`() {
        val result = Heal(amount = 5).applyTo(downed(currentHp = 4), ruleset)
        assertFalse((result.event as HealedEvent).revived)
    }

    @Test
    fun `long rest clears death saves and notes it`() {
        val result = LongRest().applyTo(downed(failures = 2, currentHp = 0, maxHp = 20), ruleset)
        assertTrue(result.character.payload().deathSaves.isCleared)
        assertTrue((result.event as LongRestEvent).deathSavesCleared)
    }
}

class DeathSaveCodecTests {
    @Test
    fun `payload with death saves round-trips`() {
        val payload = DnD5ePayload(
            classes = listOf(ClassEntry("fighter", 3)),
            currentHp = 0,
            deathSaves = DeathSaves(successes = 1, failures = 2),
        )
        assertEquals(payload, ruleset.decodePayload(ruleset.encodePayload(payload)))
    }

    @Test
    fun `death-save event round-trips with its type id`() {
        val event = DeathSaveMarkedEvent(
            DeathSaveOutcome.FAILURE,
            previous = DeathSaves(failures = 1),
            newState = DeathSaves(failures = 2),
        )
        val typeId = ruleset.eventTypeId(event)
        assertFalse(typeId == "dnd5e.unknown")
        assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
    }
}

package au.com.evonet.nat20.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class CharacterCreationTests {
    private val ruleset = FakeRuleset()
    private val now: Instant = Instant.parse("2026-05-29T00:00:00Z")

    @Test
    fun `new character starts in building phase with ruleset's initial payload`() {
        val c = Character.new(name = "Aria", ruleset = ruleset, timestamp = now)

        assertEquals("Aria", c.name)
        assertEquals(ruleset.id, c.rulesetId)
        assertEquals(CharacterPhase.Building, c.phase)
        assertEquals(now, c.createdAt)
        assertEquals(now, c.updatedAt)
        assertNull(c.portraitData)
        assertTrue(c.summons.isEmpty())
        assertEquals(FakePayload(name = "Aria"), c.payload)
    }

    @Test
    fun `each new character gets a distinct id`() {
        val a = Character.new(name = "A", ruleset = ruleset, timestamp = now)
        val b = Character.new(name = "B", ruleset = ruleset, timestamp = now)
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `new character with explicit payload keeps that payload`() {
        val payload = FakePayload(name = "Bran", notes = "a note")
        val c = Character.new(name = "Bran", ruleset = ruleset, payload = payload, timestamp = now)
        assertEquals(payload, c.payload)
    }
}

class IntentApplicationTests {
    private val ruleset = FakeRuleset()
    private val now: Instant = Instant.parse("2026-05-29T00:00:00Z")
    private fun character() = Character.new(name = "Aria", ruleset = ruleset, timestamp = now)

    @Test
    fun `rename intent returns renamed character and an event`() {
        val result = RenameIntent("Aria the Bold").applyTo(character(), ruleset)

        assertEquals("Aria the Bold", result.character.name)
        assertEquals("Renamed to Aria the Bold", result.event.summary)
    }

    @Test
    fun `rename trims surrounding whitespace`() {
        val result = RenameIntent("  Aria  ").applyTo(character(), ruleset)
        assertEquals("Aria", result.character.name)
    }

    @Test
    fun `blank rename is rejected and leaves the character untouched`() {
        val original = character()
        val error = assertThrows(CharacterIntentError.Invalid::class.java) {
            RenameIntent("   ").applyTo(original, ruleset)
        }
        assertEquals("name must not be blank", error.reason)
        assertEquals("Aria", original.name) // value type — caller's copy is unchanged
    }

    @Test
    fun `intent rejects a mismatched ruleset`() {
        val other = FakeRuleset(id = "other")
        val error = assertThrows(CharacterIntentError.RulesetMismatch::class.java) {
            RenameIntent("X").applyTo(character(), other)
        }
        assertEquals("fake", error.expected)
        assertEquals("other", error.got)
    }
}

class CodecAndEventTests {
    private val ruleset = FakeRuleset()

    @Test
    fun `payload survives an encode-decode round trip`() {
        val payload = FakePayload(name = "Aria", notes = "level 1 rogue")
        val decoded = ruleset.decodePayload(ruleset.encodePayload(payload))
        assertEquals(payload, decoded)
    }

    @Test
    fun `event survives an encode-decode round trip`() {
        val event = FakeEvent(summary = "Gained 10 XP")
        val typeId = ruleset.eventTypeId(event)
        val decoded = ruleset.decodeEvent(ruleset.encodeEvent(event), typeId)
        assertEquals(event.summary, decoded.summary)
    }

    @Test
    fun `prose event carries its text as the summary`() {
        val event = ruleset.makeProseEvent("The party set out at dawn.", JournalProseKind.CAMPAIGN_OPENING)
        assertEquals("The party set out at dawn.", event.summary)
    }

    @Test
    fun `logged event display summary prefers an override`() {
        val base = LoggedEvent(timestamp = Instant.EPOCH, event = FakeEvent("auto text"))
        assertEquals("auto text", base.displaySummary)

        val overridden = base.copy(summaryOverride = "my words")
        assertEquals("my words", overridden.displaySummary)
    }
}

class NoteKindTests {
    @Test
    fun `only travel npc and quest are narrative`() {
        assertEquals(listOf(NoteKind.TRAVEL, NoteKind.NPC, NoteKind.QUEST), NoteKind.NARRATIVE)
        assertTrue(NoteKind.TRAVEL.isNarrative)
        assertTrue(!NoteKind.COMBAT.isNarrative)
    }
}

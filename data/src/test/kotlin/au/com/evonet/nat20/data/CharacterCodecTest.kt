package au.com.evonet.nat20.data

import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterCodecError
import au.com.evonet.nat20.domain.CharacterEvent
import au.com.evonet.nat20.domain.CharacterPayload
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.domain.JournalProseKind
import au.com.evonet.nat20.domain.Ruleset
import au.com.evonet.nat20.domain.RulesetId
import au.com.evonet.nat20.domain.RulesetRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Codec round-trip coverage. A fake ruleset keeps `:data` ruleset-agnostic —
 * the codec only needs *some* ruleset to delegate payload (de)coding to, not
 * the real 5e engine.
 */
class CharacterCodecTest {

    /** Minimal payload: one int, encoded as its decimal string. */
    private data class FakePayload(val value: Int) : CharacterPayload

    private class FakeRuleset(override val id: RulesetId = "fake") : Ruleset {
        override val displayName = "Fake"
        override fun makeInitialPayload(name: String): CharacterPayload = FakePayload(0)
        override fun encodePayload(payload: CharacterPayload): String =
            (payload as FakePayload).value.toString()
        override fun decodePayload(json: String): CharacterPayload = FakePayload(json.toInt())
        override fun eventTypeId(event: CharacterEvent): String = error("unused")
        override fun encodeEvent(event: CharacterEvent): String = error("unused")
        override fun decodeEvent(json: String, typeId: String): CharacterEvent = error("unused")
        override fun makeProseEvent(text: String, kind: JournalProseKind): CharacterEvent =
            error("unused")
    }

    private val ruleset = FakeRuleset()
    private val registry = object : RulesetRegistry {
        override fun ruleset(id: RulesetId): Ruleset? = ruleset.takeIf { it.id == id }
    }
    private val codec = CharacterCodec(registry)

    private fun character(phase: CharacterPhase) = Character(
        id = UUID.randomUUID(),
        name = "Aria",
        rulesetId = ruleset.id,
        payload = FakePayload(42),
        phase = phase,
        createdAt = Instant.parse("2026-06-18T10:00:00Z"),
        updatedAt = Instant.parse("2026-06-18T11:30:00Z"),
    )

    @Test
    fun `building character round-trips through the codec`() {
        val original = character(CharacterPhase.Building)
        val decoded = codec.toDomain(codec.toEntity(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `in-campaign phase preserves the campaign id`() {
        val campaignId = UUID.randomUUID()
        val original = character(CharacterPhase.InCampaign(campaignId))
        val entity = codec.toEntity(original)
        assertEquals(campaignId.toString(), entity.inCampaignId)
        assertEquals(original, codec.toDomain(entity))
    }

    @Test
    fun `building phase flattens to a null campaign id`() {
        assertNull(codec.toEntity(character(CharacterPhase.Building)).inCampaignId)
    }

    @Test
    fun `an unregistered ruleset is rejected`() {
        val emptyRegistry = object : RulesetRegistry {
            override fun ruleset(id: RulesetId): Ruleset? = null
        }
        assertThrows(CharacterCodecError.UnknownRuleset::class.java) {
            CharacterCodec(emptyRegistry).toEntity(character(CharacterPhase.Building))
        }
    }
}

package au.com.evonet.nat20.data

import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterEvent
import au.com.evonet.nat20.domain.CharacterPayload
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.domain.JournalProseKind
import au.com.evonet.nat20.domain.LoggedEvent
import au.com.evonet.nat20.domain.NoteKind
import au.com.evonet.nat20.domain.PartyMember
import au.com.evonet.nat20.domain.Ruleset
import au.com.evonet.nat20.domain.RulesetId
import au.com.evonet.nat20.domain.RulesetRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Round-trip coverage for the campaign codec — snapshots and logged events
 * (including display overrides) must survive the row encoding intact. A fake
 * ruleset with a working event codec keeps `:data` ruleset-agnostic.
 */
class CampaignCodecTest {

    private data class FakePayload(val value: Int) : CharacterPayload
    private data class FakeEvent(val text: String) : CharacterEvent {
        override val summary: String get() = text
    }

    private class FakeRuleset(override val id: RulesetId = "fake") : Ruleset {
        override val displayName = "Fake"
        override fun makeInitialPayload(name: String): CharacterPayload = FakePayload(0)
        override fun encodePayload(payload: CharacterPayload): String =
            (payload as FakePayload).value.toString()
        override fun decodePayload(json: String): CharacterPayload = FakePayload(json.toInt())
        override fun eventTypeId(event: CharacterEvent): String = "fake.event"
        override fun encodeEvent(event: CharacterEvent): String = (event as FakeEvent).text
        override fun decodeEvent(json: String, typeId: String): CharacterEvent = FakeEvent(json)
        override fun makeProseEvent(text: String, kind: JournalProseKind): CharacterEvent =
            FakeEvent(text)
    }

    private val ruleset = FakeRuleset()
    private val registry = object : RulesetRegistry {
        override fun ruleset(id: RulesetId): Ruleset? = ruleset.takeIf { it.id == id }
    }
    private val codec = CampaignCodec(registry, CharacterCodec(registry))

    private val character = Character(
        id = UUID.randomUUID(),
        name = "Aria",
        rulesetId = ruleset.id,
        payload = FakePayload(7),
        phase = CharacterPhase.Building,
        createdAt = Instant.parse("2026-06-18T10:00:00Z"),
        updatedAt = Instant.parse("2026-06-18T10:00:00Z"),
    )

    @Test
    fun `active campaign with log round-trips`() {
        val committed = character.copy(phase = CharacterPhase.InCampaign(UUID.randomUUID()))
        val campaign = Campaign.start(committed, name = "The Sunless Citadel", startedAt = character.createdAt)
            .copy(
                log = listOf(
                    LoggedEvent(
                        timestamp = Instant.parse("2026-06-18T11:00:00Z"),
                        event = FakeEvent("Took 5 damage"),
                    ),
                    LoggedEvent(
                        timestamp = Instant.parse("2026-06-18T11:05:00Z"),
                        event = FakeEvent("Found a torch"),
                        summaryOverride = "Picked up a guttering torch",
                        iconKindOverride = NoteKind.ITEM,
                    ),
                ),
            )

        assertEquals(campaign, codec.toDomain(codec.toEntity(campaign)))
    }

    @Test
    fun `party roster round-trips`() {
        val committed = character.copy(phase = CharacterPhase.InCampaign(UUID.randomUUID()))
        val campaign = Campaign.start(committed, name = "The Sunless Citadel", startedAt = character.createdAt)
            .copy(
                party = listOf(
                    PartyMember(name = "Bram", characterClass = "Fighter", race = "Half-Orc", level = 3),
                    PartyMember(name = "Sable"),
                ),
            )

        val decoded = codec.toDomain(codec.toEntity(campaign))
        assertEquals(campaign, decoded)
        assertEquals(2, decoded.party.size)
        assertEquals("Fighter", decoded.party.first().characterClass)
        assertEquals(3, decoded.party.first().level)
    }

    @Test
    fun `ended campaign preserves both snapshots`() {
        val ended = Campaign.start(character, name = "Quest", startedAt = character.createdAt)
            .copy(endedAt = Instant.parse("2026-06-19T10:00:00Z"), endSnapshot = character.copy(name = "Aria the Bold"))

        val decoded = codec.toDomain(codec.toEntity(ended))
        assertEquals(ended, decoded)
        assertEquals("Aria the Bold", decoded.endSnapshot?.name)
    }
}

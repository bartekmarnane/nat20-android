package au.com.evonet.nat20.data

import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.CharacterCodecError
import au.com.evonet.nat20.domain.LoggedEvent
import au.com.evonet.nat20.domain.NoteKind
import au.com.evonet.nat20.domain.PartyMember
import au.com.evonet.nat20.domain.Ruleset
import au.com.evonet.nat20.domain.RulesetRegistry
import au.com.evonet.nat20.domain.SessionChronicle
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

/**
 * Bridges domain [Campaign]s to/from Room [PersistentCampaign] rows. Port of
 * the iOS `CampaignCodec`. Snapshots reuse [CharacterCodec]'s JSON envelope;
 * each logged event is encoded through the owning ruleset (type id + JSON), so
 * the codec stays ruleset-agnostic and resolves the ruleset via [registry].
 */
class CampaignCodec(
    private val registry: RulesetRegistry,
    private val characterCodec: CharacterCodec,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val logSerializer = ListSerializer(LoggedEventEnvelope.serializer())
    private val chronicleSerializer = ListSerializer(ChronicleEnvelope.serializer())
    private val partySerializer = ListSerializer(PartyMemberEnvelope.serializer())

    fun toEntity(campaign: Campaign): PersistentCampaign {
        val rulesetId = campaign.startSnapshot.rulesetId
        val ruleset = registry.ruleset(rulesetId)
            ?: throw CharacterCodecError.UnknownRuleset(rulesetId)
        val envelopes = campaign.log.map { it.toEnvelope(ruleset) }
        return PersistentCampaign(
            id = campaign.id.toString(),
            characterId = campaign.characterId.toString(),
            name = campaign.name,
            rulesetId = rulesetId,
            startedAt = campaign.startedAt.toString(),
            endedAt = campaign.endedAt?.toString(),
            startSnapshotJson = characterCodec.encodeToJson(campaign.startSnapshot),
            endSnapshotJson = campaign.endSnapshot?.let(characterCodec::encodeToJson),
            logJson = json.encodeToString(logSerializer, envelopes),
            chronicleJson = json.encodeToString(
                chronicleSerializer,
                campaign.chronicleParagraphs.map { it.toEnvelope() },
            ),
            partyJson = json.encodeToString(partySerializer, campaign.party.map { it.toEnvelope() }),
        )
    }

    fun toDomain(row: PersistentCampaign): Campaign {
        val ruleset = registry.ruleset(row.rulesetId)
            ?: throw CharacterCodecError.UnknownRuleset(row.rulesetId)
        val log = json.decodeFromString(logSerializer, row.logJson).map { it.toDomain(ruleset) }
        return Campaign(
            id = UUID.fromString(row.id),
            characterId = UUID.fromString(row.characterId),
            name = row.name,
            startedAt = Instant.parse(row.startedAt),
            endedAt = row.endedAt?.let(Instant::parse),
            startSnapshot = characterCodec.decodeFromJson(row.startSnapshotJson),
            endSnapshot = row.endSnapshotJson?.let(characterCodec::decodeFromJson),
            log = log,
            chronicleParagraphs = json.decodeFromString(chronicleSerializer, row.chronicleJson)
                .map { it.toDomain() },
            party = json.decodeFromString(partySerializer, row.partyJson).map { it.toDomain() },
        )
    }

    private fun LoggedEvent.toEnvelope(ruleset: Ruleset) = LoggedEventEnvelope(
        id = id.toString(),
        timestamp = timestamp.toString(),
        eventTypeId = ruleset.eventTypeId(event),
        eventJson = ruleset.encodeEvent(event),
        summaryOverride = summaryOverride,
        iconKindOverride = iconKindOverride?.name,
    )

    private fun LoggedEventEnvelope.toDomain(ruleset: Ruleset) = LoggedEvent(
        id = UUID.fromString(id),
        timestamp = Instant.parse(timestamp),
        event = ruleset.decodeEvent(eventJson, eventTypeId),
        summaryOverride = summaryOverride,
        iconKindOverride = iconKindOverride?.let(NoteKind::valueOf),
    )

    private fun SessionChronicle.toEnvelope() = ChronicleEnvelope(
        sessionId = sessionId.toString(),
        paragraph = paragraph,
        generatedAt = generatedAt.toString(),
    )

    private fun ChronicleEnvelope.toDomain() = SessionChronicle(
        sessionId = Instant.parse(sessionId),
        paragraph = paragraph,
        generatedAt = Instant.parse(generatedAt),
    )

    private fun PartyMember.toEnvelope() = PartyMemberEnvelope(
        id = id.toString(),
        name = name,
        characterClass = characterClass,
        race = race,
        level = level,
    )

    private fun PartyMemberEnvelope.toDomain() = PartyMember(
        id = UUID.fromString(id),
        name = name,
        characterClass = characterClass,
        race = race,
        level = level,
    )
}

/** Serializable mirror of a [LoggedEvent]; the event is stored as type id + JSON. */
@Serializable
internal data class LoggedEventEnvelope(
    val id: String,
    val timestamp: String,
    val eventTypeId: String,
    val eventJson: String,
    val summaryOverride: String? = null,
    val iconKindOverride: String? = null,
)

/** Serializable mirror of a [SessionChronicle]. */
@Serializable
internal data class ChronicleEnvelope(
    val sessionId: String,
    val paragraph: String,
    val generatedAt: String,
)

/** Serializable mirror of a [PartyMember] (parity #37). */
@Serializable
internal data class PartyMemberEnvelope(
    val id: String,
    val name: String,
    val characterClass: String? = null,
    val race: String? = null,
    val level: Int? = null,
)

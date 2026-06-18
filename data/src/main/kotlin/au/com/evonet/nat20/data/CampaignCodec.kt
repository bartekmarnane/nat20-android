package au.com.evonet.nat20.data

import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.CharacterCodecError
import au.com.evonet.nat20.domain.LoggedEvent
import au.com.evonet.nat20.domain.NoteKind
import au.com.evonet.nat20.domain.Ruleset
import au.com.evonet.nat20.domain.RulesetRegistry
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

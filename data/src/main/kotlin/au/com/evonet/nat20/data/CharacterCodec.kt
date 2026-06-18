package au.com.evonet.nat20.data

import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterCodecError
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.domain.RulesetRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

/**
 * Bridges value-type domain [Character]s to/from Room [PersistentCharacter]
 * rows. Port of the iOS `CharacterCodec`: the ruleset encodes/decodes its own
 * payload JSON, so this codec stays ruleset-agnostic and resolves the right
 * ruleset through a [RulesetRegistry].
 */
class CharacterCodec(private val registry: RulesetRegistry) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Domain → row. The ruleset must be registered (it owns the encoder). */
    fun toEntity(character: Character): PersistentCharacter {
        val ruleset = registry.ruleset(character.rulesetId)
            ?: throw CharacterCodecError.UnknownRuleset(character.rulesetId)
        return PersistentCharacter(
            id = character.id.toString(),
            name = character.name,
            rulesetId = character.rulesetId,
            payloadJson = ruleset.encodePayload(character.payload),
            inCampaignId = character.phase.inCampaignId(),
            createdAt = character.createdAt.toString(),
            updatedAt = character.updatedAt.toString(),
        )
    }

    /** Row → domain. Throws [CharacterCodecError.UnknownRuleset] if unresolved. */
    fun toDomain(row: PersistentCharacter): Character {
        val ruleset = registry.ruleset(row.rulesetId)
            ?: throw CharacterCodecError.UnknownRuleset(row.rulesetId)
        return Character(
            id = UUID.fromString(row.id),
            name = row.name,
            rulesetId = row.rulesetId,
            payload = ruleset.decodePayload(row.payloadJson),
            phase = phaseFrom(row.inCampaignId),
            createdAt = Instant.parse(row.createdAt),
            updatedAt = Instant.parse(row.updatedAt),
        )
    }

    /**
     * Domain → JSON envelope, for embedding a character *inside* another row
     * (a campaign's start/end snapshot). Same field shape as the row, just
     * serialized rather than columnar.
     */
    fun encodeToJson(character: Character): String {
        val ruleset = registry.ruleset(character.rulesetId)
            ?: throw CharacterCodecError.UnknownRuleset(character.rulesetId)
        val envelope = CharacterEnvelope(
            id = character.id.toString(),
            name = character.name,
            rulesetId = character.rulesetId,
            payloadJson = ruleset.encodePayload(character.payload),
            inCampaignId = character.phase.inCampaignId(),
            createdAt = character.createdAt.toString(),
            updatedAt = character.updatedAt.toString(),
        )
        return json.encodeToString(CharacterEnvelope.serializer(), envelope)
    }

    /** JSON envelope → domain (the inverse of [encodeToJson]). */
    fun decodeFromJson(envelopeJson: String): Character {
        val envelope = json.decodeFromString(CharacterEnvelope.serializer(), envelopeJson)
        val ruleset = registry.ruleset(envelope.rulesetId)
            ?: throw CharacterCodecError.UnknownRuleset(envelope.rulesetId)
        return Character(
            id = UUID.fromString(envelope.id),
            name = envelope.name,
            rulesetId = envelope.rulesetId,
            payload = ruleset.decodePayload(envelope.payloadJson),
            phase = phaseFrom(envelope.inCampaignId),
            createdAt = Instant.parse(envelope.createdAt),
            updatedAt = Instant.parse(envelope.updatedAt),
        )
    }

    private fun CharacterPhase.inCampaignId(): String? = when (this) {
        is CharacterPhase.Building -> null
        is CharacterPhase.InCampaign -> campaignId.toString()
    }

    private fun phaseFrom(inCampaignId: String?): CharacterPhase =
        if (inCampaignId == null) CharacterPhase.Building
        else CharacterPhase.InCampaign(UUID.fromString(inCampaignId))
}

/** Serializable mirror of a [Character]'s persisted fields (embedded snapshots). */
@Serializable
internal data class CharacterEnvelope(
    val id: String,
    val name: String,
    val rulesetId: String,
    val payloadJson: String,
    val inCampaignId: String?,
    val createdAt: String,
    val updatedAt: String,
)

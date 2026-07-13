package au.com.evonet.nat20.data

import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterCodecError
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.domain.RulesetRegistry
import au.com.evonet.nat20.domain.Summon
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * Bridges value-type domain [Character]s to/from Room [PersistentCharacter]
 * rows. Port of the iOS `CharacterCodec`: the ruleset encodes/decodes its own
 * payload JSON, so this codec stays ruleset-agnostic and resolves the right
 * ruleset through a [RulesetRegistry].
 */
class CharacterCodec(private val registry: RulesetRegistry) {
    private val json = Json { ignoreUnknownKeys = true }
    private val summonsSerializer = ListSerializer(Summon.serializer())

    private fun encodeSummons(summons: List<Summon>): String = json.encodeToString(summonsSerializer, summons)
    private fun decodeSummons(text: String): List<Summon> =
        if (text.isBlank()) emptyList() else json.decodeFromString(summonsSerializer, text)

    // Portraits are raw bytes: columnar rows keep the BLOB verbatim, but the JSON
    // envelope (campaign snapshots) has no binary type, so it carries Base64.
    private fun encodePortrait(bytes: ByteArray?): String? = bytes?.let { Base64.getEncoder().encodeToString(it) }
    private fun decodePortrait(text: String?): ByteArray? = text?.let { Base64.getDecoder().decode(it) }

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
            summonsJson = encodeSummons(character.summons),
            portraitData = character.portraitData,
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
            summons = decodeSummons(row.summonsJson),
            portraitData = row.portraitData,
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
            summonsJson = encodeSummons(character.summons),
            portraitDataBase64 = encodePortrait(character.portraitData),
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
            summons = decodeSummons(envelope.summonsJson),
            portraitData = decodePortrait(envelope.portraitDataBase64),
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
    val summonsJson: String = "[]",
    /** Base64 of the portrait bytes (parity #23); null when there's no portrait. */
    val portraitDataBase64: String? = null,
)

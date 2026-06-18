package au.com.evonet.nat20.data

import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterCodecError
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.domain.RulesetRegistry
import java.time.Instant
import java.util.UUID

/**
 * Bridges value-type domain [Character]s to/from Room [PersistentCharacter]
 * rows. Port of the iOS `CharacterCodec`: the ruleset encodes/decodes its own
 * payload JSON, so this codec stays ruleset-agnostic and resolves the right
 * ruleset through a [RulesetRegistry].
 */
class CharacterCodec(private val registry: RulesetRegistry) {

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

    private fun CharacterPhase.inCampaignId(): String? = when (this) {
        is CharacterPhase.Building -> null
        is CharacterPhase.InCampaign -> campaignId.toString()
    }

    private fun phaseFrom(inCampaignId: String?): CharacterPhase =
        if (inCampaignId == null) CharacterPhase.Building
        else CharacterPhase.InCampaign(UUID.fromString(inCampaignId))
}

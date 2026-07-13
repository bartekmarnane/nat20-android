package au.com.evonet.nat20.domain

import java.util.UUID

/**
 * A fellow adventurer travelling alongside the player character. The roster
 * lives on [Campaign.party] so the journal can reference the wider group
 * (watch order during rests, witnesses to events, etc.). It is purely
 * informational — names plus optional class/race/level — not the deferred
 * party-merged-chronicle feature (backlog A23). Class/race/level are optional
 * because non-D&D-family rulesets may not model them. Port of the iOS
 * `PartyMember`.
 */
data class PartyMember(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val characterClass: String? = null,
    val race: String? = null,
    val level: Int? = null,
)

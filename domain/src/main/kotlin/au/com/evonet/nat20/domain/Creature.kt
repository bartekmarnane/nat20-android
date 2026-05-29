package au.com.evonet.nat20.domain

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A runtime statblock instance attached to a character — familiar, beast
 * companion, summoned creature, animated undead, mount.
 *
 * Distinct from a ruleset's *monster catalogue* entry: the catalogue holds
 * the template (a wolf's AC, attacks); a [Creature] is *this particular wolf*
 * with its rolled max HP, current HP and applied conditions. The
 * ruleset-specific statblock lives in [rulesetPayload] as opaque JSON — the
 * domain doesn't know what's inside; the owning ruleset decodes it on demand.
 *
 * The behaviour that spawns/dismisses creatures lands at A15; this value type
 * fixes the shape now.
 */
@Serializable
data class Creature(
    @Serializable(with = UuidSerializer::class) val id: UUID = UUID.randomUUID(),
    val name: String,
    val currentHp: Int,
    val maxHp: Int,
    val tempHp: Int = 0,
    /** Free-form condition names (e.g. "poisoned", "prone"), ruleset-agnostic. */
    val conditions: List<String> = emptyList(),
    /** Encoded ruleset-specific statblock; the owning ruleset decodes it. */
    val rulesetPayload: String,
)

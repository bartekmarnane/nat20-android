package au.com.evonet.nat20.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for a homebrew creature (parity #44). Same thin-envelope pattern as
 * [PersistentCustomRace]: `creatureId`/`name` are real columns for lookup, the
 * full `CustomCreature` shape is opaque JSON encoded by the ruleset layer.
 */
@Entity(tableName = "custom_creatures")
data class PersistentCustomCreature(
    @PrimaryKey val id: String,
    /** The creature id, `"custom:" + UUID`. */
    val creatureId: String = "",
    /** Denormalised display name (list ordering without decoding payloads). */
    val name: String = "",
    /** The full `CustomCreature` JSON, encoded/decoded by the app's ruleset layer. */
    val payloadJson: String = "{}",
    /** ISO-8601 instants. */
    val createdAt: String = "",
    val updatedAt: String = "",
)

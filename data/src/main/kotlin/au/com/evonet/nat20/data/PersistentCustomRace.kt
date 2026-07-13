package au.com.evonet.nat20.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for a homebrew race (parity #11). The same thin-envelope pattern as
 * [PersistentCharacter]: `raceId`/`name` are real columns for lookup, the full
 * `Race` shape is opaque JSON encoded by the ruleset layer — `:data` never
 * learns the 5e model. Every column defaults so future additive migrations
 * stay trivial.
 */
@Entity(tableName = "custom_races")
data class PersistentCustomRace(
    @PrimaryKey val id: String,
    /** The catalogue race id, `"custom:" + UUID`. */
    val raceId: String = "",
    /** Denormalised display name (list ordering without decoding payloads). */
    val name: String = "",
    /** The full `Race` JSON, encoded/decoded by the app's ruleset layer. */
    val payloadJson: String = "{}",
    /** ISO-8601 instants. */
    val createdAt: String = "",
    val updatedAt: String = "",
)

package au.com.evonet.nat20.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for a character. A thin JSON envelope, mirroring the iOS
 * `PersistentCharacter` (`payloadData: Data`): cross-ruleset facts are real
 * columns, the ruleset-specific payload is opaque JSON tagged by [rulesetId]
 * and decoded through the owning ruleset's codec.
 *
 * A5 scope: characters only. Campaign logs persist with the `Campaign` type at
 * A7a; portraits and summons get their own columns when those features land.
 */
@Entity(tableName = "characters")
data class PersistentCharacter(
    @PrimaryKey val id: String,
    val name: String,
    val rulesetId: String,
    /** The ruleset payload, encoded by `Ruleset.encodePayload`. */
    val payloadJson: String,
    /**
     * Campaign lifecycle, flattened: null = `Building`, otherwise the id of the
     * campaign the character is committed to (`InCampaign`). Mirrors the iOS
     * `currentCampaignID` approach.
     */
    val inCampaignId: String?,
    /** ISO-8601 instants (see domain `InstantSerializer`). */
    val createdAt: String,
    val updatedAt: String,
    /** Cross-ruleset summoned creatures (familiars, companions, summons), JSON-encoded. A18; defaults `[]`. */
    val summonsJson: String = "[]",
)

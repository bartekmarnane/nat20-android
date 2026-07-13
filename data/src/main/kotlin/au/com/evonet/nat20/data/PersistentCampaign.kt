package au.com.evonet.nat20.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for a campaign (A7a). The character snapshots and the journal log
 * are ruleset-specific, so they're stored as JSON envelopes (decoded through
 * the owning ruleset's codec) rather than relational columns — mirroring the
 * iOS `PersistentCampaign` / `CampaignCodec`. [rulesetId] tags the row so the
 * codec knows which ruleset to decode the snapshots/events with.
 */
@Entity(
    tableName = "campaigns",
    indices = [Index("characterId")],
)
data class PersistentCampaign(
    @PrimaryKey val id: String,
    val characterId: String,
    val name: String,
    val rulesetId: String,
    val startedAt: String,
    val endedAt: String?,
    val startSnapshotJson: String,
    val endSnapshotJson: String?,
    /** JSON array of logged-event envelopes (event encoded via the ruleset). */
    val logJson: String,
    /**
     * JSON array of session-chronicle envelopes (A7d). Added in DB v3 with a
     * `'[]'` default so the v2→v3 migration matches the generated schema.
     */
    @ColumnInfo(defaultValue = "[]")
    val chronicleJson: String = "[]",
    /**
     * JSON array of party-member envelopes (parity #37). Added in DB v7 with a
     * `'[]'` default so the v6→v7 migration matches the generated schema.
     */
    @ColumnInfo(defaultValue = "[]")
    val partyJson: String = "[]",
)

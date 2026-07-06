package au.com.evonet.nat20.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Lightweight (characterId, name) projection of an active campaign, for the roster's campaign line. */
data class ActiveCampaignName(
    val characterId: String,
    val name: String,
)

/** Room access for [PersistentCampaign] rows. */
@Dao
interface CampaignDao {
    /** All campaigns for a character, newest-started first, as a live stream. */
    @Query("SELECT * FROM campaigns WHERE characterId = :characterId ORDER BY startedAt DESC")
    fun observeForCharacter(characterId: String): Flow<List<PersistentCampaign>>

    /** Every active campaign's (characterId, name), skipping the JSON envelopes. */
    @Query("SELECT characterId, name FROM campaigns WHERE endedAt IS NULL")
    fun observeActiveCampaignNames(): Flow<List<ActiveCampaignName>>

    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun byId(id: String): PersistentCampaign?

    @Upsert
    suspend fun upsert(campaign: PersistentCampaign)

    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun delete(id: String)
}

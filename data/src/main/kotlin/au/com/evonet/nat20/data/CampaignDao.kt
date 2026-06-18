package au.com.evonet.nat20.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Room access for [PersistentCampaign] rows. */
@Dao
interface CampaignDao {
    /** All campaigns for a character, newest-started first, as a live stream. */
    @Query("SELECT * FROM campaigns WHERE characterId = :characterId ORDER BY startedAt DESC")
    fun observeForCharacter(characterId: String): Flow<List<PersistentCampaign>>

    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun byId(id: String): PersistentCampaign?

    @Upsert
    suspend fun upsert(campaign: PersistentCampaign)

    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun delete(id: String)
}

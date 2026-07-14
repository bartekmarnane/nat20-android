package au.com.evonet.nat20.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Room access for [PersistentCustomCreature] rows. */
@Dao
interface CustomCreatureDao {
    /** All homebrew creatures, newest-created first. */
    @Query("SELECT * FROM custom_creatures ORDER BY createdAt DESC")
    suspend fun all(): List<PersistentCustomCreature>

    @Upsert
    suspend fun upsert(row: PersistentCustomCreature)

    @Query("DELETE FROM custom_creatures WHERE creatureId = :creatureId")
    suspend fun deleteByCreatureId(creatureId: String)
}

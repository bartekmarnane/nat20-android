package au.com.evonet.nat20.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Room access for [PersistentCustomRace] rows. */
@Dao
interface CustomRaceDao {
    /** All homebrew races, name-ordered. One-shot: the library holds live state in memory. */
    @Query("SELECT * FROM custom_races ORDER BY name")
    suspend fun all(): List<PersistentCustomRace>

    /** Insert or replace a homebrew row by primary key. */
    @Upsert
    suspend fun upsert(row: PersistentCustomRace)

    @Query("DELETE FROM custom_races WHERE raceId = :raceId")
    suspend fun deleteByRaceId(raceId: String)
}

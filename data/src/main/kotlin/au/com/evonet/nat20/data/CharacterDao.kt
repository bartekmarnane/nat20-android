package au.com.evonet.nat20.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Room access for [PersistentCharacter] rows. */
@Dao
interface CharacterDao {
    /** All characters, newest-created first, as a live stream. */
    @Query("SELECT * FROM characters ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PersistentCharacter>>

    /** Insert or replace a character by primary key. */
    @Upsert
    suspend fun upsert(character: PersistentCharacter)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun count(): Int
}

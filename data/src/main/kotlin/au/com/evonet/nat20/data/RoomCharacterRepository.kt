package au.com.evonet.nat20.data

import android.util.Log
import au.com.evonet.nat20.domain.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Room-backed [CharacterRepository]. Maps rows ↔ domain through [CharacterCodec]. */
class RoomCharacterRepository(
    private val dao: CharacterDao,
    private val codec: CharacterCodec,
) : CharacterRepository {

    // Per-row decoding: one record this build can't read (a newer ruleset or a
    // malformed blob) is skipped and logged rather than throwing into the
    // roster flow, which used to crash the app on every launch.
    override val characters: Flow<List<Character>> =
        dao.observeAll().map { rows ->
            rows.mapNotNull { row ->
                runCatching { codec.toDomain(row) }
                    .onFailure { Log.w("Nat20", "Skipping undecodable character ${row.id}", it) }
                    .getOrNull()
            }
        }

    override suspend fun character(id: UUID): Character? =
        dao.byId(id.toString())?.let { row ->
            runCatching { codec.toDomain(row) }
                .onFailure { Log.w("Nat20", "Undecodable character ${row.id}", it) }
                .getOrNull()
        }

    override suspend fun upsert(character: Character) {
        dao.upsert(codec.toEntity(character))
    }

    override suspend fun delete(id: UUID) {
        dao.delete(id.toString())
    }

    override suspend fun seedIfEmpty(characters: List<Character>) {
        if (dao.count() == 0) {
            characters.forEach { dao.upsert(codec.toEntity(it)) }
        }
    }
}

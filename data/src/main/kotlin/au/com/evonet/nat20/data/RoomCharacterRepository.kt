package au.com.evonet.nat20.data

import au.com.evonet.nat20.domain.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Room-backed [CharacterRepository]. Maps rows ↔ domain through [CharacterCodec]. */
class RoomCharacterRepository(
    private val dao: CharacterDao,
    private val codec: CharacterCodec,
) : CharacterRepository {

    override val characters: Flow<List<Character>> =
        dao.observeAll().map { rows -> rows.map(codec::toDomain) }

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

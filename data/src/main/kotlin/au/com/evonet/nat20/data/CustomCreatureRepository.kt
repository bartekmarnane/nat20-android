package au.com.evonet.nat20.data

import java.time.Instant
import java.util.UUID

/** A persisted homebrew creature; the `CustomCreature` shape stays opaque JSON. */
data class CustomCreatureRecord(
    val id: UUID,
    val creatureId: String,
    val name: String,
    val payloadJson: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Persistence seam for homebrew creatures (same role as [CustomRaceRepository]). */
interface CustomCreatureRepository {
    suspend fun all(): List<CustomCreatureRecord>
    suspend fun upsert(record: CustomCreatureRecord)
    suspend fun deleteByCreatureId(creatureId: String)
}

/** Room-backed [CustomCreatureRepository]. */
class RoomCustomCreatureRepository(private val dao: CustomCreatureDao) : CustomCreatureRepository {
    override suspend fun all(): List<CustomCreatureRecord> = dao.all().map(CustomCreatureCodec::toRecord)

    override suspend fun upsert(record: CustomCreatureRecord) {
        dao.upsert(CustomCreatureCodec.toEntity(record))
    }

    override suspend fun deleteByCreatureId(creatureId: String) {
        dao.deleteByCreatureId(creatureId)
    }
}

/** Row ↔ record translation. */
internal object CustomCreatureCodec {
    fun toEntity(record: CustomCreatureRecord) = PersistentCustomCreature(
        id = record.id.toString(),
        creatureId = record.creatureId,
        name = record.name,
        payloadJson = record.payloadJson,
        createdAt = record.createdAt.toString(),
        updatedAt = record.updatedAt.toString(),
    )

    fun toRecord(entity: PersistentCustomCreature) = CustomCreatureRecord(
        id = UUID.fromString(entity.id),
        creatureId = entity.creatureId,
        name = entity.name,
        payloadJson = entity.payloadJson,
        createdAt = Instant.parse(entity.createdAt),
        updatedAt = Instant.parse(entity.updatedAt),
    )
}

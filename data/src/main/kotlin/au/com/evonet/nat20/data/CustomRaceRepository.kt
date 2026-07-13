package au.com.evonet.nat20.data

import java.time.Instant
import java.util.UUID

/**
 * A persisted homebrew race, ruleset-agnostic: the `Race` shape stays opaque
 * JSON ([payloadJson]) so `:data` never depends on a ruleset module. The app's
 * sync layer decodes payloads into the in-memory library and writes changes
 * back through this seam.
 */
data class CustomRaceRecord(
    val id: UUID,
    /** The catalogue race id (`"custom:" + UUID`) the payload describes. */
    val raceId: String,
    val name: String,
    val payloadJson: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Persistence seam for homebrew races (same role as [CharacterRepository]). */
interface CustomRaceRepository {
    /** All saved homebrews, name-ordered. */
    suspend fun all(): List<CustomRaceRecord>

    /** Insert or update a homebrew row. */
    suspend fun upsert(record: CustomRaceRecord)

    /** Remove the row describing catalogue race [raceId]. */
    suspend fun deleteByRaceId(raceId: String)
}

/** Room-backed [CustomRaceRepository]. Maps rows ↔ records through [CustomRaceCodec]. */
class RoomCustomRaceRepository(private val dao: CustomRaceDao) : CustomRaceRepository {
    override suspend fun all(): List<CustomRaceRecord> = dao.all().map(CustomRaceCodec::toRecord)

    override suspend fun upsert(record: CustomRaceRecord) {
        dao.upsert(CustomRaceCodec.toEntity(record))
    }

    override suspend fun deleteByRaceId(raceId: String) {
        dao.deleteByRaceId(raceId)
    }
}

/** Row ↔ record translation (ids and ISO-8601 instants to/from strings). */
internal object CustomRaceCodec {
    fun toEntity(record: CustomRaceRecord) = PersistentCustomRace(
        id = record.id.toString(),
        raceId = record.raceId,
        name = record.name,
        payloadJson = record.payloadJson,
        createdAt = record.createdAt.toString(),
        updatedAt = record.updatedAt.toString(),
    )

    fun toRecord(entity: PersistentCustomRace) = CustomRaceRecord(
        id = UUID.fromString(entity.id),
        raceId = entity.raceId,
        name = entity.name,
        payloadJson = entity.payloadJson,
        createdAt = Instant.parse(entity.createdAt),
        updatedAt = Instant.parse(entity.updatedAt),
    )
}

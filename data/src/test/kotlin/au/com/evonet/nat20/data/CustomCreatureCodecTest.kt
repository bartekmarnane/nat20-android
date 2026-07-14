package au.com.evonet.nat20.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Homebrew-creature row codec coverage (parity #44). The payload stays opaque
 * JSON — `:data` never decodes the `CustomCreature` shape — so the round-trip
 * exercises id/instant translation. The v7→v8 CREATE TABLE is validated against
 * the exported schema at runtime.
 */
class CustomCreatureCodecTest {

    private fun record() = CustomCreatureRecord(
        id = UUID.randomUUID(),
        creatureId = "custom:1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
        name = "Awakened Shrub",
        payloadJson = """{"id":"custom:1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d","name":"Awakened Shrub","armorClass":9}""",
        createdAt = Instant.parse("2026-07-14T10:00:00Z"),
        updatedAt = Instant.parse("2026-07-14T11:30:00Z"),
    )

    @Test
    fun `custom creature record round-trips through the row codec`() {
        val original = record()
        assertEquals(original, CustomCreatureCodec.toRecord(CustomCreatureCodec.toEntity(original)))
    }

    @Test
    fun `entity columns carry the record fields verbatim`() {
        val original = record()
        val entity = CustomCreatureCodec.toEntity(original)
        assertEquals(original.id.toString(), entity.id)
        assertEquals(original.creatureId, entity.creatureId)
        assertEquals("Awakened Shrub", entity.name)
        assertEquals(original.payloadJson, entity.payloadJson)
    }
}

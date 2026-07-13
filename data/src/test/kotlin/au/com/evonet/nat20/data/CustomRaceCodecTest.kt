package au.com.evonet.nat20.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Homebrew-race row codec coverage (parity #11). The payload stays an opaque
 * JSON string — `:data` never decodes the 5e `Race` shape — so the round-trip
 * exercises id/instant translation and field mapping. (A live migration test
 * needs a device/Robolectric harness this module's JVM test setup doesn't
 * carry; the v4→v5 SQL is validated against the exported schema at runtime.)
 */
class CustomRaceCodecTest {

    private fun record() = CustomRaceRecord(
        id = UUID.randomUUID(),
        raceId = "custom:8b9a4a1e-2f57-4a44-9a0e-3c5d6e7f8a9b",
        name = "Vampire Dwarf",
        payloadJson = """{"id":"custom:8b9a4a1e-2f57-4a44-9a0e-3c5d6e7f8a9b","name":"Vampire Dwarf","speed":25}""",
        createdAt = Instant.parse("2026-07-13T10:00:00Z"),
        updatedAt = Instant.parse("2026-07-13T11:30:00Z"),
    )

    @Test
    fun `custom race record round-trips through the row codec`() {
        val original = record()
        assertEquals(original, CustomRaceCodec.toRecord(CustomRaceCodec.toEntity(original)))
    }

    @Test
    fun `entity columns carry the record fields verbatim`() {
        val original = record()
        val entity = CustomRaceCodec.toEntity(original)
        assertEquals(original.id.toString(), entity.id)
        assertEquals(original.raceId, entity.raceId)
        assertEquals("Vampire Dwarf", entity.name)
        assertEquals(original.payloadJson, entity.payloadJson)
        assertEquals("2026-07-13T10:00:00Z", entity.createdAt)
        assertEquals("2026-07-13T11:30:00Z", entity.updatedAt)
    }
}

package au.com.evonet.nat20.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class CampaignSessionTests {
    private val base: Instant = Instant.parse("2026-06-18T10:00:00Z")

    private fun event(minutesFromBase: Long): LoggedEvent =
        LoggedEvent(timestamp = base.plus(Duration.ofMinutes(minutesFromBase)), event = FakeEvent("e"))

    @Test
    fun `empty log derives no sessions`() {
        assertTrue(CampaignSession.derive(emptyList()).isEmpty())
    }

    @Test
    fun `closely-spaced events form one session`() {
        val log = listOf(event(0), event(30), event(90))
        val sessions = CampaignSession.derive(log)
        assertEquals(1, sessions.size)
        assertEquals(3, sessions.first().events.size)
        assertEquals(1, sessions.first().number)
    }

    @Test
    fun `a gap over four hours splits sessions, newest first and numbered oldest-first`() {
        // Two on day one, then a ~21h gap, then two more.
        val log = listOf(
            event(0), event(60),
            event(60 * 24 + 0), event(60 * 24 + 30),
        )
        val sessions = CampaignSession.derive(log)

        assertEquals(2, sessions.size)
        // Newest session first.
        assertEquals(base.plus(Duration.ofMinutes(60 * 24)), sessions[0].id)
        assertEquals(2, sessions[0].number) // numbered oldest-first
        assertEquals(base, sessions[1].id)
        assertEquals(1, sessions[1].number)
    }

    @Test
    fun `derivation is independent of input order`() {
        val shuffled = listOf(event(90), event(0), event(30))
        val sessions = CampaignSession.derive(shuffled)
        assertEquals(1, sessions.size)
        assertEquals(base, sessions.first().startedAt)
        assertEquals(base.plus(Duration.ofMinutes(90)), sessions.first().endedAt)
    }
}

package au.com.evonet.nat20.domain

import java.time.Duration
import java.time.Instant

/**
 * A play session derived from a campaign's log. Sessions are runs of
 * consecutive entries separated by at most [defaultGap] of real time (4 hours
 * by default), so a group that plays late still counts as one session and the
 * next day starts a fresh one. Port of the iOS `CampaignSession`.
 *
 * [id] is the timestamp of the session's first event, stable across
 * re-derivations as long as that first event isn't deleted — which is what lets
 * a generated [SessionChronicle] stay attached to its session.
 */
data class CampaignSession(
    val id: Instant,
    val startedAt: Instant,
    val endedAt: Instant,
    /** 1-based, 1 == oldest. */
    val number: Int,
    val events: List<LoggedEvent>,
) {
    companion object {
        val defaultGap: Duration = Duration.ofHours(4)

        /**
         * Groups [log] into sessions, splitting whenever two consecutive entries
         * are more than [gap] apart. Returned newest-session-first, so
         * `sessions[0]` is the latest; `number` is 1-based with 1 == oldest.
         */
        fun derive(log: List<LoggedEvent>, gap: Duration = defaultGap): List<CampaignSession> {
            if (log.isEmpty()) return emptyList()
            val sorted = log.sortedBy { it.timestamp }
            val groups = mutableListOf(mutableListOf(sorted.first()))
            for (event in sorted.drop(1)) {
                val last = groups.last().last()
                if (Duration.between(last.timestamp, event.timestamp) > gap) {
                    groups.add(mutableListOf(event))
                } else {
                    groups.last().add(event)
                }
            }
            return groups
                .mapIndexed { index, events ->
                    CampaignSession(
                        id = events.first().timestamp,
                        startedAt = events.first().timestamp,
                        endedAt = events.last().timestamp,
                        number = index + 1,
                        events = events,
                    )
                }
                .reversed()
        }
    }
}

/**
 * One paragraph of generated chronicle prose, tagged with the session it
 * summarises (by the session's first-event timestamp). Persisted on
 * [Campaign.chronicleParagraphs]. Port of the iOS `SessionChronicle`.
 */
data class SessionChronicle(
    val sessionId: Instant,
    val paragraph: String,
    val generatedAt: Instant,
)

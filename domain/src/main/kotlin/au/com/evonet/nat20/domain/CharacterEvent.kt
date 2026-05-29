package au.com.evonet.nat20.domain

import java.time.Instant
import java.util.UUID

/**
 * Something that happened to a character, recorded in a campaign journal.
 * Concrete events live in the ruleset package; the domain only needs a
 * human-readable [summary]. Port of the iOS `CharacterEvent` protocol.
 */
interface CharacterEvent {
    val summary: String
}

/**
 * A [CharacterEvent] stamped with when it happened and given a stable id,
 * plus optional user edits to how it renders in the journal.
 */
data class LoggedEvent(
    val id: UUID = UUID.randomUUID(),
    val timestamp: Instant,
    val event: CharacterEvent,
    /**
     * User-edited summary text. When non-null the journal renders this in
     * place of [CharacterEvent.summary]; the underlying event is left intact.
     */
    val summaryOverride: String? = null,
    /**
     * User-picked icon kind. When non-null the journal renders this kind's
     * glyph instead of the event-type-derived glyph.
     */
    val iconKindOverride: NoteKind? = null,
) {
    /** Text the journal should display for this entry. */
    val displaySummary: String
        get() = summaryOverride ?: event.summary
}

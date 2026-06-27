package au.com.evonet.nat20.chronicle

import au.com.evonet.nat20.domain.CampaignSession
import au.com.evonet.nat20.domain.SessionChronicle
import java.time.Instant

/**
 * Turns a play [CampaignSession] into one narrative paragraph via the on-device
 * [ChronicleGenerator]. Port of the iOS `ChronicleService`: third-person past
 * tense, every event reflected in order, nothing invented. Generation is gated
 * on [isAvailable]; on unsupported devices this returns null and the journal
 * shows entries without prose.
 */
class ChronicleService(private val generator: ChronicleGenerator) {

    val isAvailable: Boolean get() = generator.isAvailable

    /** Ground-truth facts about the PC, passed verbatim to suppress invention. */
    data class CharacterContext(
        val name: String,
        /** e.g. "Level 5 Mountain Dwarf Fighter". */
        val descriptor: String,
        /** Known equipment; empty until inventory lands (A10). */
        val equipment: List<String> = emptyList(),
    )

    /**
     * Generates a recap for one session, or null if unavailable / the model
     * returns nothing. [priorParagraphs] (oldest first) are passed for tone and
     * continuity only.
     */
    suspend fun chronicle(
        session: CampaignSession,
        campaignName: String,
        character: CharacterContext,
        priorParagraphs: List<String>,
        now: Instant,
        style: NarrationStyle = NarrationStyle.STORIED,
    ): SessionChronicle? {
        // Simple narration never calls the on-device model — the journal shows
        // plain entries (mirrors iOS, where .simple is template-only).
        if (style != NarrationStyle.STORIED || !generator.isAvailable) return null
        val prompt = buildUserPrompt(session, campaignName, character, priorParagraphs)
        val text = generator.generate(instructions(), prompt)?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        return SessionChronicle(sessionId = session.id, paragraph = text, generatedAt = now)
    }

    companion object {
        /** Builds the system instruction. Storied prose uses one fixed atmospheric voice (A9). */
        fun instructions(): String =
            "You are the chronicler of a Dungeons & Dragons campaign. You are given a numbered, " +
                "ordered list of in-game events from one play session. Rewrite them as one short " +
                "paragraph (2–4 sentences) of third-person past-tense prose, in a slightly " +
                "archaic, hand-written-journal voice.\n\n" +
                "HARD RULES:\n" +
                "- Reflect EVERY numbered event, in the exact order given. Never drop, merge, " +
                "reorder, or flash back.\n" +
                "- Use only the names, items, places, and outcomes that appear in the events. " +
                "Invent nothing.\n" +
                "- Stay fully in-world: never use game terms (HP, AC, DC, temp HP, modifier, " +
                "saving throw) — render the same fact as lived experience instead.\n" +
                "- No headings, numbers, dates, times of day, or bullet points. Return only the prose."

        /** Builds the per-session user prompt. Pure — assembled from the session + context. */
        fun buildUserPrompt(
            session: CampaignSession,
            campaignName: String,
            character: CharacterContext,
            priorParagraphs: List<String>,
        ): String {
            val lines = session.events
                .mapIndexed { index, e -> "${index + 1}. ${e.displaySummary}" }
                .joinToString("\n")

            val context = buildString {
                append("Campaign: ").append(campaignName)
                append("\nPlayer character: ").append(character.name)
                if (character.descriptor.isNotEmpty()) append(" — ").append(character.descriptor)
                if (character.equipment.isNotEmpty()) {
                    append("\nKnown equipment (reference only these, invent no others): ")
                    append(character.equipment.joinToString(", "))
                }
            }

            val core = """
                |$context
                |
                |Session ${session.number} events (chronological, no times):
                |$lines
                |
                |Write the chronicle paragraph. Reflect every numbered event, in order.
            """.trimMargin()

            if (priorParagraphs.isEmpty()) return core
            val prior = priorParagraphs.joinToString("\n\n")
            return "For tone and continuity only (do not rewrite these):\n\n$prior\n\n---\n\n$core"
        }
    }
}

package au.com.evonet.nat20.domain

import kotlinx.serialization.Serializable

/**
 * Icon/category tag for a free-text journal entry. Used both by the
 * player-authored add-note flow and by auto-generated prose entries
 * (campaign opening, party joins/leaves) to select a glyph. Lives in the
 * domain because the journal display layer references it across rulesets.
 *
 * The iOS enum also carried `symbolName` (SF Symbols); the glyph mapping is
 * a UI concern and lives in `:app` on Android, so it's omitted here.
 */
@Serializable
enum class NoteKind(val label: String) {
    TRAVEL("Travel"),
    COMBAT("Combat"),
    ITEM("Item"),
    NPC("NPC"),
    REST("Rest"),
    QUEST("Quest"),
    MAGIC("Magic");

    /** Whether the user may pick this kind for a free-form note. */
    val isNarrative: Boolean
        get() = this in NARRATIVE

    companion object {
        /**
         * Kinds the user can pick for a free-form journal note. The
         * mechanical kinds (combat/rest/magic/item) are reserved for entries
         * produced by structured acts, so the chronicle stays grounded in
         * real game state.
         */
        val NARRATIVE: List<NoteKind> = listOf(TRAVEL, NPC, QUEST)
    }
}

package au.com.evonet.nat20.dnd5e

/**
 * The core book's id, spelled out as a compile-time constant so catalogue
 * models can default to it without depending on [SourceCatalog]'s
 * initialisation order.
 */
const val SOURCE_PHB = "phb"

/**
 * A publishing source for catalogue content — Player's Handbook, Tasha's
 * Cauldron of Everything, Xanathar's Guide to Everything, etc. Every
 * catalogue entry that can be *chosen* (race, class, subclass, background,
 * spell, feat, fighting style, metamagic, pact boon, invocation) carries a
 * [sourceId], and pickers filter by the character's
 * [DnD5ePayload.enabledSources] — so a table running PHB + Tasha's never
 * sees Xanathar's subclasses.
 *
 * Stored per-character rather than per-campaign so a Peace Cleric doesn't
 * become invalid when the table changes books. Default is PHB alone; every
 * supplement, Tasha's included, is opt-in.
 *
 * Port of the iOS `Source` / `SourceCatalog`. Ids and abbreviations match
 * exactly — the bundled catalogue JSON is shared between the platforms, and
 * its `sourceID` tags key off these strings.
 */
data class Source(
    val id: String,
    val name: String,
    val abbreviation: String,
    /**
     * True for sources that ship pre-enabled — the default enabled set for
     * a new character. Only PHB is on by default.
     */
    val isDefault: Boolean = false,
    /**
     * True for foundational sources supplying the base classes, races and
     * core spell list. Disabling one would leave an unbuildable character,
     * so pickers render these as always-on. PHB is the only locked source.
     */
    val isLocked: Boolean = false,
)

/**
 * The roster of supplements Nat20 knows about, ordered so the modern
 * defaults lead. Catalogue JSON tags entries with one of these ids
 * (defaulting to `phb` when the field is missing — every SRD entry is PHB
 * content). New supplements register here first, then bring content.
 */
object SourceCatalog {
    val phb = Source(SOURCE_PHB, "Player's Handbook", "PHB", isDefault = true, isLocked = true)
    val tashas = Source("tashas", "Tasha's Cauldron of Everything", "TCoE")
    val xanathars = Source("xanathars", "Xanathar's Guide to Everything", "XGtE")
    val mordenkainens = Source("mordenkainens", "Mordenkainen Presents: Monsters of the Multiverse", "MPMM")
    val swordCoast = Source("sword-coast", "Sword Coast Adventurer's Guide", "SCAG")
    val elementalEvil = Source("elemental-evil", "Elemental Evil Player's Companion", "EEPC")
    val eberron = Source("eberron", "Eberron: Rising from the Last War", "ERftLW")
    val strixhaven = Source("strixhaven", "Strixhaven: A Curriculum of Chaos", "SCC")

    val all: List<Source> = listOf(
        phb, tashas, xanathars, mordenkainens,
        swordCoast, elementalEvil, eberron, strixhaven,
    )

    /** Enabled set for a new character, and for legacy payloads that predate the field. */
    val defaultEnabled: Set<String> = all.filter { it.isDefault }.map { it.id }.toSet()

    /** Always available whatever the character's set says — see [Source.isLocked]. */
    val lockedIds: Set<String> = all.filter { it.isLocked }.map { it.id }.toSet()

    fun source(id: String): Source? = all.firstOrNull { it.id == id }
}

/** Catalogue entries that carry a publishing source tag. */
interface SourceTagged {
    val sourceId: String
}

/**
 * Keeps only entries whose [SourceTagged.sourceId] is enabled. Used at every
 * picker call site. The locked-PHB guarantee is upheld upstream by
 * [DnD5ePayload.effectiveSources] always unioning [SourceCatalog.lockedIds].
 */
fun <T : SourceTagged> List<T>.filteredBySources(enabled: Set<String>): List<T> =
    filter { it.sourceId in enabled }

/**
 * The character's source set, tolerant of the two ways it can arrive empty:
 * a payload written before the field existed, and a payload whose set
 * somehow dropped the core book. Both resolve to at least PHB.
 */
val DnD5ePayload.effectiveSources: Set<String>
    get() = enabledSources.toSet() + SourceCatalog.lockedIds

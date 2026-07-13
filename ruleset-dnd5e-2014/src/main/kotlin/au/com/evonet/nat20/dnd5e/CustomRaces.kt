package au.com.evonet.nat20.dnd5e

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * The homebrew race library (parity #11, port of iOS `CustomRaceLibrary`).
 *
 * A homebrew is a regular [Race] whose id is `"custom:" + UUID` — there is no
 * separate custom-race type, so everything that consumes races (the wizard, the
 * codex, ability-bonus math) works unchanged. The library is an in-memory
 * overlay: [DnD5eCatalog.races] appends its contents after the bundled SRD
 * list, name-sorted, always included regardless of any future source filter.
 *
 * Persistence stays outside this pure-JVM module: the app hydrates saved rows
 * through [replaceAll] at startup (which deliberately does NOT fire [onChange])
 * and installs an [onChange] write-through to reconcile the Room table after
 * every mutation.
 */
object CustomRaceLibrary {
    /** Id prefix that marks a race as homebrew — gates badges/edit affordances with no lookup. */
    const val ID_PREFIX = "custom:"

    private val state = MutableStateFlow<List<Race>>(emptyList())

    /** The homebrew races, in insertion order (the catalogue overlay sorts by name). */
    val races: StateFlow<List<Race>> get() = state

    /**
     * Fired after every user mutation ([add]/[update]/[delete]) with the new
     * list — the app's persistence write-through. NOT fired by [replaceAll].
     */
    var onChange: ((List<Race>) -> Unit)? = null

    /** True when [id] names a homebrew race. */
    fun isCustom(id: String): Boolean = id.startsWith(ID_PREFIX)

    /** A fresh homebrew race id. */
    fun newId(): String = ID_PREFIX + UUID.randomUUID()

    /** Append a new homebrew. */
    fun add(race: Race) = mutate { it + race }

    /** Replace the homebrew with the same id; a no-op when it isn't present. */
    fun update(race: Race) = mutate { list -> list.map { if (it.id == race.id) race else it } }

    /**
     * Remove the homebrew with [id]. Saved characters keep their dangling
     * `race` reference (iOS behaviour) — only the library entry goes away.
     */
    fun delete(id: String) = mutate { list -> list.filterNot { it.id == id } }

    /** Hydrate from persistence. Does NOT fire [onChange] (no write-back loop). */
    fun replaceAll(races: List<Race>) {
        state.value = races
    }

    private fun mutate(transform: (List<Race>) -> List<Race>) {
        val next = transform(state.value)
        state.value = next
        onChange?.invoke(next)
    }
}

/**
 * Race ↔ JSON for homebrew persistence rows. Lives here (like the ruleset's
 * payload codecs) so serialization stays inside the ruleset module — the app's
 * sync layer only shuttles opaque strings.
 */
object CustomRaceJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(race: Race): String = json.encodeToString(Race.serializer(), race)

    /** Null on malformed JSON (a bad row is skipped, never fatal). */
    fun decode(text: String): Race? =
        runCatching { json.decodeFromString(Race.serializer(), text) }.getOrNull()
}

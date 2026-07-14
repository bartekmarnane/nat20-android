package au.com.evonet.nat20.dnd5e

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * A homebrew creature (parity #44, port of the iOS `CustomCreature`). A simple
 * statblock the player authors in the Custom Creatures library — name, size,
 * type/alignment prose, defences, the six ability scores, challenge rating, and
 * a notes field. It is a companion reference, not an encounter engine, so the
 * shape stays flat. Its id is `"custom:" + UUID`.
 */
@Serializable
data class CustomCreature(
    val id: String,
    val name: String,
    val size: String = "Medium",
    val type: String = "beast",
    val alignment: String = "unaligned",
    val armorClass: Int = 12,
    val hitPoints: Int = 11,
    val hitDice: String = "2d8+2",
    val speed: Int = 30,
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10,
    val challengeRating: Double = 0.25,
    val notes: String = "",
)

/**
 * The homebrew creature library (port of iOS `CustomCreatureLibrary`), the same
 * in-memory-overlay + persistence-write-through shape as [CustomRaceLibrary].
 * Unlike races these aren't consumed by any character math — they're a
 * standalone reference list under Settings → Reference.
 */
object CustomCreatureLibrary {
    const val ID_PREFIX = "custom:"

    private val state = MutableStateFlow<List<CustomCreature>>(emptyList())

    /** The homebrew creatures, newest-first for the list. */
    val creatures: StateFlow<List<CustomCreature>> get() = state

    /** Fired after every user mutation with the new list — the app's write-through. Not fired by [replaceAll]. */
    var onChange: ((List<CustomCreature>) -> Unit)? = null

    fun newId(): String = ID_PREFIX + UUID.randomUUID()

    fun add(creature: CustomCreature) = mutate { listOf(creature) + it }

    fun update(creature: CustomCreature) = mutate { list -> list.map { if (it.id == creature.id) creature else it } }

    fun delete(id: String) = mutate { list -> list.filterNot { it.id == id } }

    /** Hydrate from persistence. Does NOT fire [onChange]. */
    fun replaceAll(creatures: List<CustomCreature>) {
        state.value = creatures
    }

    private fun mutate(transform: (List<CustomCreature>) -> List<CustomCreature>) {
        val next = transform(state.value)
        state.value = next
        onChange?.invoke(next)
    }
}

/** CustomCreature ↔ JSON for homebrew persistence rows (serialization stays in the ruleset module). */
object CustomCreatureJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(creature: CustomCreature): String = json.encodeToString(CustomCreature.serializer(), creature)

    fun decode(text: String): CustomCreature? =
        runCatching { json.decodeFromString(CustomCreature.serializer(), text) }.getOrNull()
}

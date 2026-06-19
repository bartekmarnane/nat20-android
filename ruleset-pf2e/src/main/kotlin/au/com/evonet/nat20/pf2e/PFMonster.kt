package au.com.evonet.nat20.pf2e

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * A single Pathfinder 2e creature, decoded from the bundled `PFMonsters.json`.
 *
 * **Attribution:** Pathfinder 2e Remaster content from the **Monster Core**,
 * licensed under the **ORC License** and sourced via Archives of Nethys. The
 * structured fields drive the list + filtering; [statblock] holds the full
 * statblock as cleaned Markdown, rendered in the detail view. Companion-app
 * scope: a static reference, not a combat tracker. Port of the iOS `PFMonster`.
 */
@Serializable
data class PFMonster(
    val id: String,
    val name: String,
    val level: Int,
    val size: String = "",
    val rarity: String = "Common",
    val traits: List<String> = emptyList(),
    val ac: Int = 0,
    val hp: Int = 0,
    val perception: Int = 0,
    val description: String = "",
    /** The full statblock as cleaned Markdown (`**bold**` labels, `---` rules). */
    val statblock: String = "",
) {
    /** "Level −1 · Small Construct" — the subtitle under the name. */
    val subtitle: String get() = "Level $level" + traits.takeIf { it.isNotEmpty() }?.let { " · ${it.joinToString(" ")}" }.orEmpty()
}

/** Read-only access to the bundled ORC Monster Core bestiary (445 creatures). */
object PFMonsterCatalog {
    private val json = Json { ignoreUnknownKeys = true }

    val all: List<PFMonster> by lazy {
        val text = javaClass.getResourceAsStream("/catalogues/PFMonsters.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("Missing bundled PF2e catalogue: PFMonsters.json")
        json.decodeFromString(ListSerializer(PFMonster.serializer()), text)
            .sortedWith(compareBy({ it.level }, { it.name }))
    }

    private val byId by lazy { all.associateBy { it.id } }
    fun monster(id: String): PFMonster? = byId[id]
}

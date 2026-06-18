package au.com.evonet.nat20.dnd5e2024

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The bundled D&D 5e (2024) SRD 5.2 catalogues (A21). Loaded from module
 * resources via the classloader, so the same code serves JVM tests and the app.
 * Port of the iOS `Catalog2024`; the spell list is the SRD 5.2 (CC BY 4.0,
 * Open5e `srd-2024`) set of 339 spells. Species/backgrounds/classes follow.
 */
object DnD5e2024Catalog {
    private val json = Json { ignoreUnknownKeys = true }

    /** SRD 5.2 spells (339), sorted by level then name, for the 2024 Spell Library + caster picker. */
    val spellLibrary: List<Spell2024> by lazy {
        load("Spells2024.json", Spell2024.serializer()).sortedWith(compareBy({ it.level }, { it.name }))
    }

    private val spellsById by lazy { spellLibrary.associateBy { it.id } }
    fun spell(id: String): Spell2024? = spellsById[id]

    private fun <T> load(path: String, element: kotlinx.serialization.KSerializer<T>): List<T> {
        val text = javaClass.getResourceAsStream("/catalogues/$path")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Missing bundled 2024 catalogue: $path")
        return json.decodeFromString(ListSerializer(element), text)
    }
}

/** A D&D 5e (2024) spell — the SRD 5.2 shape (flat school/classes strings + rules text). */
@Serializable
data class Spell2024(
    val id: String,
    val name: String,
    val level: Int = 0,
    val school: String = "",
    val classes: List<String> = emptyList(),
    val concentration: Boolean = false,
    val ritual: Boolean = false,
    val description: String = "",
) {
    val isCantrip: Boolean get() = level == 0
    val levelLabel: String get() = if (level == 0) "Cantrip" else "Level $level"
    val classNames: List<String> get() = classes.map { it.replaceFirstChar(Char::uppercase) }
}

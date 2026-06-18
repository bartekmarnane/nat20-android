package au.com.evonet.nat20.dnd5e2024

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * A single 2024 monster statblock, decoded from the bundled `Monsters2024.json`.
 *
 * **Attribution:** WotC System Reference Document 5.2 (CC BY 4.0), sourced via
 * the Open5e `srd-2024` dataset. Display-oriented: the transform pre-formats
 * speed, senses, saves, skills, and the resistance/immunity lines into
 * ready-to-render strings, so this is a flat `@Serializable` (JSON keys match
 * the property names). Read-only — the codex never mutates it. Port of the iOS
 * `Monster2024`.
 */
@Serializable
data class Monster2024(
    val id: String,
    val name: String,
    val size: String = "",
    val type: String = "",
    val alignment: String = "",
    val armorClass: Int = 10,
    val armorDetail: String = "",
    val hitPoints: Int = 0,
    val hitDice: String = "",
    val speed: String = "",
    val abilities: Abilities = Abilities(),
    val savesDisplay: String = "",
    val skillsDisplay: String = "",
    val damageResistances: String = "",
    val damageImmunities: String = "",
    val damageVulnerabilities: String = "",
    val conditionImmunities: String = "",
    val sensesDisplay: String = "",
    val passivePerception: Int = 0,
    val languages: String = "",
    val challengeRating: Double = 0.0,
    val crLabel: String = "",
    val proficiencyBonus: Int = 2,
    val xp: Int = 0,
    val traits: List<NamedText> = emptyList(),
    val actions: List<NamedText> = emptyList(),
    val bonusActions: List<NamedText> = emptyList(),
    val reactions: List<NamedText> = emptyList(),
    val legendaryActions: List<NamedText> = emptyList(),
) {
    @Serializable
    data class Abilities(
        val str: Int = 10, val dex: Int = 10, val con: Int = 10,
        val int: Int = 10, val wis: Int = 10, val cha: Int = 10,
    )

    /** A named block of rules text — a trait, action, reaction, etc. */
    @Serializable
    data class NamedText(val name: String, val desc: String)

    /** "Large Aberration, lawful evil" — the line under the name. */
    val subtitle: String get() = listOf(size, type).filter { it.isNotBlank() }.joinToString(" ") +
        alignment.takeIf { it.isNotBlank() }?.let { ", $it" }.orEmpty()
}

/** Read-only access to the bundled SRD 5.2 monster catalogue (330 statblocks). */
object MonsterCatalog2024 {
    private val json = Json { ignoreUnknownKeys = true }

    /** All 330 monsters, sorted by challenge rating then name. */
    val all: List<Monster2024> by lazy {
        val text = javaClass.getResourceAsStream("/catalogues/Monsters2024.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("Missing bundled 2024 catalogue: Monsters2024.json")
        json.decodeFromString(ListSerializer(Monster2024.serializer()), text)
            .sortedWith(compareBy({ it.challengeRating }, { it.name }))
    }

    private val byId by lazy { all.associateBy { it.id } }
    fun monster(id: String): Monster2024? = byId[id]
}

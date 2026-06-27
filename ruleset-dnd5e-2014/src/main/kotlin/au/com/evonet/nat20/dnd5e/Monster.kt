package au.com.evonet.nat20.dnd5e

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * A single 2014 monster statblock, decoded from the bundled raw `5e-SRD-Monsters.json`
 * (5e-bits / 5e-database, CC BY 4.0).
 *
 * Unlike the 2024 [au.com.evonet.nat20.dnd5e2024.Monster2024] (whose JSON is pre-flattened),
 * the 2014 dataset is the raw nested 5e-database shape: `armor_class`/`speed`/`proficiencies`/
 * `senses` are structured. This model decodes that shape verbatim and exposes ready-to-render
 * display strings via computed properties so the codex never has to re-walk the nesting.
 * Read-only — the reference codex never mutates it.
 */
@Serializable
data class Monster(
    val index: String,
    val name: String,
    val size: String = "",
    val type: String = "",
    val alignment: String = "",
    @SerialName("armor_class") val armorClassEntries: List<ArmorClassEntry> = emptyList(),
    @SerialName("hit_points") val hitPoints: Int = 0,
    @SerialName("hit_dice") val hitDice: String = "",
    val speed: Map<String, JsonElement> = emptyMap(),
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10,
    val proficiencies: List<ProficiencyEntry> = emptyList(),
    @SerialName("damage_vulnerabilities") val damageVulnerabilities: List<String> = emptyList(),
    @SerialName("damage_resistances") val damageResistances: List<String> = emptyList(),
    @SerialName("damage_immunities") val damageImmunities: List<String> = emptyList(),
    @SerialName("condition_immunities") val conditionImmunities: List<NamedRef> = emptyList(),
    val senses: Map<String, JsonElement> = emptyMap(),
    val languages: String = "",
    @SerialName("challenge_rating") val challengeRating: Double = 0.0,
    @SerialName("proficiency_bonus") val proficiencyBonus: Int = 2,
    val xp: Int = 0,
    @SerialName("special_abilities") val traits: List<NamedText> = emptyList(),
    val actions: List<NamedText> = emptyList(),
    @SerialName("legendary_actions") val legendaryActions: List<NamedText> = emptyList(),
) {
    @Serializable
    data class ArmorClassEntry(val type: String = "", val value: Int = 10, val armor: List<NamedRef> = emptyList())

    @Serializable
    data class ProficiencyEntry(val value: Int = 0, val proficiency: NamedRef = NamedRef())

    /** A `{index,name,url}` reference (conditions, proficiencies, equipment). */
    @Serializable
    data class NamedRef(val index: String = "", val name: String = "")

    /** A named block of rules text — a trait, action, or legendary action. */
    @Serializable
    data class NamedText(val name: String = "", val desc: String = "")

    // ---- Display-formatted accessors (mirror the flat 2024 model's fields) ----

    /** Senses are exposed by passive_perception as an Int, everything else as a string. */
    val passivePerception: Int get() = (senses["passive_perception"] as? JsonPrimitive)?.intOrNull
        ?: (10 + Math.floorDiv(wisdom - 10, 2))

    val armorClass: Int get() = armorClassEntries.firstOrNull()?.value ?: 10

    /** "(natural armor)" or "(leather armor, shield)" — the parenthetical after the AC number. */
    val armorDetail: String get() = armorClassEntries.firstOrNull()?.let { ac ->
        val worn = ac.armor.joinToString(", ") { it.name.lowercase() }
        worn.ifBlank { ac.type.takeIf { it != "dex" }?.let { "$it armor" }.orEmpty() }
    }.orEmpty()

    val speedDisplay: String get() = speed.entries.joinToString(", ") { (mode, v) ->
        // `walk`/`fly`/… carry a distance string; `hover` carries the boolean `true`.
        val value = (v as? JsonPrimitive)?.contentOrNull
        when {
            value == null || value == "true" -> mode
            mode == "walk" -> value
            else -> "$mode $value"
        }
    }

    val savesDisplay: String get() = proficiencies
        .filter { it.proficiency.index.startsWith("saving-throw") }
        .joinToString(", ") { "${it.proficiency.name.removePrefix("Saving Throw: ")} ${it.value.signed}" }

    val skillsDisplay: String get() = proficiencies
        .filter { it.proficiency.index.startsWith("skill") }
        .joinToString(", ") { "${it.proficiency.name.removePrefix("Skill: ")} ${it.value.signed}" }

    val sensesDisplay: String get() = senses.entries
        .filter { it.key != "passive_perception" }
        .joinToString(", ") { (k, v) -> "${k.replace('_', ' ')} ${(v as? JsonPrimitive)?.contentOrNull ?: v}" }

    val conditionImmunitiesDisplay: String get() = conditionImmunities.joinToString(", ") { it.name }

    val abilities: Abilities get() = Abilities(strength, dexterity, constitution, intelligence, wisdom, charisma)

    data class Abilities(val str: Int, val dex: Int, val con: Int, val int: Int, val wis: Int, val cha: Int)

    /** "1/4", "5", "30" — challenge-rating label with the fractional CRs spelled as fractions. */
    val crLabel: String get() = when (challengeRating) {
        0.125 -> "1/8"
        0.25 -> "1/4"
        0.5 -> "1/2"
        else -> if (challengeRating % 1.0 == 0.0) challengeRating.toInt().toString() else challengeRating.toString()
    }

    /** "Small humanoid, neutral evil" — the line under the name. */
    val subtitle: String get() = listOf(size, type).filter { it.isNotBlank() }.joinToString(" ") +
        alignment.takeIf { it.isNotBlank() }?.let { ", $it" }.orEmpty()

    private val Int.signed: String get() = if (this >= 0) "+$this" else "$this"
}

/** Read-only access to the bundled raw SRD monster catalogue (334 statblocks). */
object MonsterCatalog {
    private val json = Json { ignoreUnknownKeys = true }

    /** All monsters, sorted by challenge rating then name. */
    val all: List<Monster> by lazy {
        val text = javaClass.getResourceAsStream("/catalogues/Monsters/5e-SRD-Monsters.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("Missing bundled 2014 catalogue: 5e-SRD-Monsters.json")
        json.decodeFromString(ListSerializer(Monster.serializer()), text)
            .sortedWith(compareBy({ it.challengeRating }, { it.name }))
    }

    private val byId by lazy { all.associateBy { it.index } }
    fun monster(id: String): Monster? = byId[id]
}

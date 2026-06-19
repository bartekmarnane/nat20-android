package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.domain.Creature
import au.com.evonet.nat20.domain.Summon
import au.com.evonet.nat20.domain.SummonLifecycle
import au.com.evonet.nat20.domain.SummonOrigin
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

/**
 * The D&D 5e (2014) statblock carried inside a [Creature.rulesetPayload] (A18).
 * The domain [Creature] holds the runtime instance (current HP, conditions); this
 * is the ruleset-specific stat data the 2014 engine decodes on demand. Port of the
 * iOS `DnD5eCreaturePayload`.
 */
@Serializable
data class DnD5eCreaturePayload(
    val size: String = "",
    val type: String = "",
    val armorClass: Int = 10,
    val speed: String = "",
    val abilities: AbilityScores = AbilityScores(),
    val attacks: List<CreatureAttack> = emptyList(),
    val traits: List<String> = emptyList(),
    /** The catalogue template this was minted from (null for a homebrew creature). */
    val catalogueId: String? = null,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun encode(payload: DnD5eCreaturePayload): String = json.encodeToString(serializer(), payload)
        fun decode(text: String): DnD5eCreaturePayload = json.decodeFromString(serializer(), text)
    }
}

/** A creature's attack line — "Bite, +4 to hit, 1d6+2 piercing". */
@Serializable
data class CreatureAttack(
    val name: String,
    val toHit: Int,
    val reach: String = "5 ft.",
    val damage: String = "",
    val damageType: String = "",
) {
    val display: String get() = "$name, ${if (toHit >= 0) "+$toHit" else "$toHit"} to hit, reach $reach" +
        (damage.takeIf { it.isNotBlank() }?.let { ", $it ${damageType}".trimEnd() } ?: "")
}

/** What kind of secondary entity a template spawns — drives which library section + lifecycle it lands in. */
enum class CreatureKind { FAMILIAR, MOUNT, COMPANION, SUMMON }

/** A catalogue template for a familiar/mount/companion/summon. [makeCreature] mints a runtime [Creature]. */
data class CreatureTemplate(
    val id: String,
    val name: String,
    val kind: CreatureKind,
    val size: String,
    val type: String,
    val armorClass: Int,
    val maxHp: Int,
    val speed: String,
    val abilities: AbilityScores,
    val attacks: List<CreatureAttack> = emptyList(),
    val traits: List<String> = emptyList(),
) {
    fun statblock(): DnD5eCreaturePayload = DnD5eCreaturePayload(size, type, armorClass, speed, abilities, attacks, traits, id)

    fun makeCreature(customName: String? = null): Creature = Creature(
        id = UUID.randomUUID(),
        name = customName?.trim()?.takeIf { it.isNotEmpty() } ?: name,
        currentHp = maxHp,
        maxHp = maxHp,
        rulesetPayload = DnD5eCreaturePayload.encode(statblock()),
    )
}

/**
 * The bundled D&D 5e (2014) **familiar / mount / companion / summon** library
 * (A18). A hand-authored set of the classic forms with real SRD stats — enough
 * for the companion-app bar; homebrew creatures are built ad-hoc in the UI.
 */
object CreatureCatalog {
    private fun ab(str: Int, dex: Int, con: Int, int: Int, wis: Int, cha: Int) =
        AbilityScores(str, dex, con, int, wis, cha)

    /** Find Familiar forms (PHB roster). */
    val familiars: List<CreatureTemplate> = listOf(
        CreatureTemplate("owl", "Owl", CreatureKind.FAMILIAR, "Tiny", "beast", 11, 1, "5 ft., fly 60 ft.", ab(3, 13, 8, 2, 12, 7),
            listOf(CreatureAttack("Talons", 3, "5 ft.", "1 slashing", "slashing")), listOf("Flyby", "Keen Hearing and Sight")),
        CreatureTemplate("cat", "Cat", CreatureKind.FAMILIAR, "Tiny", "beast", 12, 2, "40 ft., climb 30 ft.", ab(3, 15, 10, 3, 12, 7),
            listOf(CreatureAttack("Claws", 0, "5 ft.", "1 slashing", "slashing")), listOf("Keen Smell")),
        CreatureTemplate("bat", "Bat", CreatureKind.FAMILIAR, "Tiny", "beast", 12, 1, "5 ft., fly 30 ft.", ab(2, 15, 8, 2, 12, 4),
            listOf(CreatureAttack("Bite", 0, "5 ft.", "1 piercing", "piercing")), listOf("Echolocation", "Keen Hearing")),
        CreatureTemplate("hawk", "Hawk", CreatureKind.FAMILIAR, "Tiny", "beast", 13, 1, "10 ft., fly 60 ft.", ab(5, 16, 8, 2, 14, 6),
            listOf(CreatureAttack("Talons", 5, "5 ft.", "1 slashing", "slashing")), listOf("Keen Sight")),
        CreatureTemplate("poisonous-snake", "Poisonous Snake", CreatureKind.FAMILIAR, "Tiny", "beast", 13, 2, "30 ft., swim 30 ft.", ab(2, 16, 11, 1, 10, 3),
            listOf(CreatureAttack("Bite", 5, "5 ft.", "1 + 2d4 poison", "poison"))),
        CreatureTemplate("raven", "Raven", CreatureKind.FAMILIAR, "Tiny", "beast", 12, 1, "10 ft., fly 50 ft.", ab(2, 14, 8, 2, 12, 6),
            listOf(CreatureAttack("Beak", 4, "5 ft.", "1 piercing", "piercing")), listOf("Mimicry")),
        CreatureTemplate("spider", "Spider", CreatureKind.FAMILIAR, "Tiny", "beast", 12, 1, "20 ft., climb 20 ft.", ab(2, 14, 8, 1, 10, 2),
            listOf(CreatureAttack("Bite", 4, "5 ft.", "1 poison", "poison")), listOf("Spider Climb", "Web Sense", "Web Walker")),
        CreatureTemplate("frog", "Frog", CreatureKind.FAMILIAR, "Tiny", "beast", 11, 1, "20 ft., swim 20 ft.", ab(1, 13, 8, 1, 8, 3),
            traits = listOf("Amphibious", "Standing Leap")),
    )

    /** Find Steed mounts (paladin's bonded mount). */
    val mounts: List<CreatureTemplate> = listOf(
        CreatureTemplate("warhorse", "Warhorse", CreatureKind.MOUNT, "Large", "beast", 11, 19, "60 ft.", ab(18, 12, 13, 2, 12, 7),
            listOf(CreatureAttack("Hooves", 6, "5 ft.", "2d6+4 bludgeoning", "bludgeoning")), listOf("Trampling Charge")),
        CreatureTemplate("pony", "Pony", CreatureKind.MOUNT, "Medium", "beast", 10, 11, "40 ft.", ab(15, 10, 13, 2, 11, 7),
            listOf(CreatureAttack("Hooves", 4, "5 ft.", "2d4+2 bludgeoning", "bludgeoning"))),
        CreatureTemplate("mastiff", "Mastiff", CreatureKind.MOUNT, "Medium", "beast", 12, 5, "40 ft.", ab(13, 14, 12, 3, 12, 7),
            listOf(CreatureAttack("Bite", 3, "5 ft.", "1d6+1 piercing", "piercing")), listOf("Keen Hearing and Smell")),
    )

    /** Beast Master companions + common conjured beasts. */
    val companions: List<CreatureTemplate> = listOf(
        CreatureTemplate("wolf", "Wolf", CreatureKind.COMPANION, "Medium", "beast", 13, 11, "40 ft.", ab(12, 15, 12, 3, 12, 6),
            listOf(CreatureAttack("Bite", 4, "5 ft.", "2d4+2 piercing", "piercing")), listOf("Keen Hearing and Smell", "Pack Tactics")),
        CreatureTemplate("boar", "Boar", CreatureKind.COMPANION, "Medium", "beast", 11, 11, "40 ft.", ab(13, 11, 12, 2, 9, 5),
            listOf(CreatureAttack("Tusk", 3, "5 ft.", "1d6+1 slashing", "slashing")), listOf("Charge", "Relentless")),
        CreatureTemplate("panther", "Panther", CreatureKind.COMPANION, "Medium", "beast", 12, 13, "50 ft., climb 40 ft.", ab(14, 15, 10, 3, 14, 7),
            listOf(CreatureAttack("Bite", 4, "5 ft.", "1d6+2 piercing", "piercing")), listOf("Keen Smell", "Pounce")),
        CreatureTemplate("giant-spider", "Giant Spider", CreatureKind.SUMMON, "Large", "beast", 14, 26, "30 ft., climb 30 ft.", ab(14, 16, 12, 2, 11, 4),
            listOf(CreatureAttack("Bite", 5, "5 ft.", "1d8+3 + 2d8 poison", "poison")), listOf("Spider Climb", "Web Walker")),
    )

    /** Common summon-spell stand-ins (Conjure Animals etc.). */
    val summons: List<CreatureTemplate> = companions.filter { it.kind == CreatureKind.SUMMON } + listOf(
        CreatureTemplate("brown-bear", "Brown Bear", CreatureKind.SUMMON, "Large", "beast", 11, 34, "40 ft., climb 30 ft.", ab(19, 10, 16, 2, 13, 7),
            listOf(CreatureAttack("Bite", 5, "5 ft.", "1d8+4 piercing", "piercing"), CreatureAttack("Claws", 5, "5 ft.", "2d6+4 slashing", "slashing")), listOf("Keen Smell")),
        CreatureTemplate("dire-wolf", "Dire Wolf", CreatureKind.SUMMON, "Large", "beast", 14, 37, "50 ft.", ab(17, 15, 15, 3, 12, 7),
            listOf(CreatureAttack("Bite", 5, "5 ft.", "2d6+3 piercing", "piercing")), listOf("Keen Hearing and Smell", "Pack Tactics")),
    )

    /** Every template, for lookup. */
    val all: List<CreatureTemplate> = familiars + mounts + companions + summons.distinctBy { it.id }
    fun template(id: String): CreatureTemplate? = all.firstOrNull { it.id == id }

    /**
     * Builds a [Summon] from a template — the UI supplies fresh ids + the spawn
     * timestamp (intents stay pure). [count] mints that many creatures (Conjure
     * Animals → 8 wolves); each gets an indexed name when there's more than one.
     */
    fun buildSummon(
        template: CreatureTemplate,
        origin: SummonOrigin,
        lifecycle: SummonLifecycle,
        spawnedAt: Instant,
        count: Int = 1,
        customName: String? = null,
        label: String? = null,
    ): Summon {
        val creatures = (1..maxOf(1, count)).map { i ->
            val baseName = customName?.trim()?.takeIf { it.isNotEmpty() } ?: template.name
            template.makeCreature(if (count > 1) "$baseName $i" else baseName)
        }
        return Summon(
            label = label ?: (customName?.takeIf { it.isNotBlank() }?.let { "$it (${template.name})" } ?: template.name),
            origin = origin,
            lifecycle = lifecycle,
            creatures = creatures,
            spawnedAt = spawnedAt,
        )
    }
}

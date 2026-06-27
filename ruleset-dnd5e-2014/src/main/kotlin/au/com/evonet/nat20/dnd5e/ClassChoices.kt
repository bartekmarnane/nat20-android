package au.com.evonet.nat20.dnd5e

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The deep class-catalogue choices the level-1-only creation flow used to skip
 * (A11): Sorcerer **Metamagic**, Warlock **Eldritch Invocations** + **Pact Boon**,
 * and Rogue/Bard **Expertise**. Each is a character-building selection (like
 * spells-known / fighting styles), so it's edited directly on the payload rather
 * than journaled. Mirrors the bundled SRD JSON under `catalogues/{Metamagic,
 * Invocations,PactBoons}`. The mechanical long tail of each option tracks A17.
 */

@Serializable
data class Metamagic(val id: String, val name: String, val description: String)

@Serializable
data class PactBoon(val id: String, val name: String, val description: String)

@Serializable
data class Invocation(
    val id: String,
    val name: String,
    val description: String,
    val prerequisite: InvocationPrerequisite? = null,
) {
    /** Whether a warlock of [warlockLevel] holding [pactBoon] and [knownSpells] may take this. */
    fun isAvailable(warlockLevel: Int, pactBoon: String?, knownSpells: Set<String>): Boolean {
        val pre = prerequisite ?: return true
        if (warlockLevel < pre.minimumWarlockLevel) return false
        if (pre.requiredPactBoon != null && pre.requiredPactBoon != pactBoon) return false
        if (pre.requiredSpell != null && pre.requiredSpell !in knownSpells) return false
        return true
    }
}

@Serializable
data class InvocationPrerequisite(
    val minimumWarlockLevel: Int = 1,
    val requiredSpell: String? = null,
    val requiredPactBoon: String? = null,
)

/** The 8 SRD Metamagic options (Sorcerer). */
object Metamagics {
    private val json = Json { ignoreUnknownKeys = true }
    val all: List<Metamagic> by lazy { load("/catalogues/Metamagic/Metamagic.json", Metamagic.serializer()) }
    private val byId by lazy { all.associateBy { it.id } }
    fun option(id: String): Metamagic? = byId[id]

    private fun <T> load(path: String, serializer: kotlinx.serialization.KSerializer<T>): List<T> {
        val text = javaClass.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: error("Missing bundled catalogue: $path")
        return json.decodeFromString(ListSerializer(serializer), text)
    }
}

/** The 4 SRD Pact Boons (Warlock, level 3). */
object PactBoons {
    private val json = Json { ignoreUnknownKeys = true }
    val all: List<PactBoon> by lazy {
        val text = javaClass.getResourceAsStream("/catalogues/PactBoons/PactBoons.json")?.bufferedReader()?.use { it.readText() }
            ?: error("Missing bundled catalogue: PactBoons.json")
        json.decodeFromString(ListSerializer(PactBoon.serializer()), text)
    }
    private val byId by lazy { all.associateBy { it.id } }
    fun boon(id: String): PactBoon? = byId[id]
}

/** The 32 SRD Eldritch Invocations (Warlock). */
object Invocations {
    private val json = Json { ignoreUnknownKeys = true }
    val all: List<Invocation> by lazy {
        val text = javaClass.getResourceAsStream("/catalogues/Invocations/Invocations.json")?.bufferedReader()?.use { it.readText() }
            ?: error("Missing bundled catalogue: Invocations.json")
        json.decodeFromString(ListSerializer(Invocation.serializer()), text)
    }
    private val byId by lazy { all.associateBy { it.id } }
    fun invocation(id: String): Invocation? = byId[id]
}

/**
 * The per-class allotments for the deep-catalogue choices, by the relevant class's
 * level (multiclass-aware — each draws from its own class's levels). All read off
 * the SRD class tables.
 */
object ClassChoiceAllotments {
    private fun levelOf(classes: List<ClassEntry>, classId: String): Int =
        classes.firstOrNull { it.classId.equals(classId, ignoreCase = true) }?.level ?: 0

    /** Sorcerer metamagic known: 2 at L3, +1 at L10, +1 at L17. */
    fun metamagicKnown(classes: List<ClassEntry>): Int = when (val s = levelOf(classes, "sorcerer")) {
        0, 1, 2 -> 0
        in 3..9 -> 2
        in 10..16 -> 3
        else -> if (s >= 17) 4 else 0
    }

    /** Warlock invocations known by warlock level (PHB table). */
    fun invocationsKnown(classes: List<ClassEntry>): Int = INVOCATIONS_BY_WARLOCK_LEVEL.getOrElse(levelOf(classes, "warlock")) { 0 }

    /** The Warlock gains a Pact Boon at level 3. */
    fun pactBoonAvailable(classes: List<ClassEntry>): Boolean = levelOf(classes, "warlock") >= 3

    /** Rogue: 2 Expertise at L1, +2 at L6. Bard: 2 at L3, +2 at L10. */
    fun expertiseSlots(classes: List<ClassEntry>): Int {
        val rogue = levelOf(classes, "rogue").let { when { it >= 6 -> 4; it >= 1 -> 2; else -> 0 } }
        val bard = levelOf(classes, "bard").let { when { it >= 10 -> 4; it >= 3 -> 2; else -> 0 } }
        return rogue + bard
    }

    val warlockLevel: (List<ClassEntry>) -> Int = { levelOf(it, "warlock") }

    // index = warlock level (0..20); value = invocations known.
    private val INVOCATIONS_BY_WARLOCK_LEVEL = listOf(
        0, 0, 2, 2, 2, 3, 3, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 8, 8, 8,
    )
}

// ── Payload accessors (earned counts + skills eligible for Expertise) ───────────

val DnD5ePayload.metamagicKnownCount: Int get() = ClassChoiceAllotments.metamagicKnown(classes)
val DnD5ePayload.invocationsKnownCount: Int get() = ClassChoiceAllotments.invocationsKnown(classes)
val DnD5ePayload.pactBoonAvailable: Boolean get() = ClassChoiceAllotments.pactBoonAvailable(classes)
val DnD5ePayload.expertiseSlots: Int get() = ClassChoiceAllotments.expertiseSlots(classes)
val DnD5ePayload.warlockLevel: Int get() = ClassChoiceAllotments.warlockLevel(classes)

/** Skills eligible for Expertise: those the character is proficient in. */
val DnD5ePayload.expertiseEligibleSkills: List<String> get() = effectiveSkillProficiencies

/** True when the character has Expertise in [skillId] (proficiency counts double). */
fun DnD5ePayload.hasExpertise(skillId: String): Boolean = skillId in expertiseSkills

/**
 * The proficiency-bonus multiplier for a skill check: 2 with Expertise, 1 when
 * merely proficient, 0 otherwise. Folds Expertise into the skill bonus math.
 */
fun DnD5ePayload.skillProficiencyMultiplier(skillId: String): Int = when {
    skillId in expertiseSkills && skillId in effectiveSkillProficiencies -> 2
    skillId in effectiveSkillProficiencies -> 1
    else -> 0
}

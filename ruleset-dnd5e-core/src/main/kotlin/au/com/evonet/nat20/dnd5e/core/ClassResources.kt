package au.com.evonet.nat20.dnd5e.core

import kotlinx.serialization.Serializable

/**
 * Edition-agnostic 5e class-resource machinery — the point pools (Ki, Sorcery
 * Points, Lay on Hands) and the use-counter features (Rage, Action Surge, …),
 * plus their max-by-level formulas and recovery cadence. Port of the iOS
 * `ClassResourceCatalog` + `ClassFeatureEffectCatalog` (recovery/max-uses parts;
 * the attached `ActiveEffect`s are deferred to A17).
 *
 * These are `ClassEntry`-free (it lives in the 2014 module): formulas take a
 * plain class level. The 2014 module's payload accessors resolve them against a
 * character's class list and inject the per-character caps (Bardic Inspiration's
 * CHA-based max, its L1-4/L5+ recovery split).
 */

/** When a class resource refreshes. */
@Serializable
enum class FeatureRecovery {
    /** Recovers on a short or long rest (Action Surge, Second Wind, Channel Divinity, Ki). */
    SHORT_REST,
    /** Recovers only on a long rest (Rage, Sorcery Points, Lay on Hands). */
    LONG_REST,
    /** Unlimited / at-will — no counter (the effect-only features, A17). */
    AT_WILL,
    /** Trigger-cleared (concentration-anchored features). */
    OTHER,
}

/** A stored use-counter for a single class feature: remaining uses + the recovery rule recorded at use time. */
@Serializable
data class FeatureUseEntry(
    val remaining: Int,
    val recovery: FeatureRecovery,
)

/** The point-pool resources (variable-cost spends): Ki, Sorcery Points, Lay on Hands. */
object ClassResourceCatalog {

    /** A point-pool definition. [maxFor] maps the character's level in [classId] to the pool size (0 = not online yet). */
    data class Pool(
        val id: String,
        val displayName: String,
        val abbreviation: String,
        val classId: String,
        val recovery: FeatureRecovery,
        val maxFor: (Int) -> Int,
    ) {
        fun maxValue(classLevel: Int): Int = maxOf(0, maxFor(classLevel))
    }

    val allPools: List<Pool> = listOf(
        Pool("ki", "Ki", "Ki", "monk", FeatureRecovery.SHORT_REST) { level -> if (level >= 2) level else 0 },
        Pool("sorcery-points", "Sorcery Points", "SP", "sorcerer", FeatureRecovery.LONG_REST) { level -> if (level >= 2) level else 0 },
        Pool("lay-on-hands", "Lay on Hands", "LoH", "paladin", FeatureRecovery.LONG_REST) { level -> if (level >= 1) level * 5 else 0 },
    )

    fun pool(id: String): Pool? = allPools.firstOrNull { it.id == id }

    /** Bardic Inspiration die faces by bard level: d6 → d8 (L5) → d10 (L10) → d12 (L15). */
    fun bardicInspirationDie(bardLevel: Int): Int = when {
        bardLevel <= 4 -> 6
        bardLevel <= 9 -> 8
        bardLevel <= 14 -> 10
        else -> 12
    }

    /** Rage damage bonus by barbarian level: +2 → +3 (L9) → +4 (L16). */
    fun rageDamageBonus(barbarianLevel: Int): Int = when {
        barbarianLevel <= 8 -> 2
        barbarianLevel <= 15 -> 3
        else -> 4
    }
}

/** The use-counter class features (fixed uses per rest): Rage, Action Surge, Second Wind, Bardic Inspiration, Channel Divinity. */
object ClassFeatureCatalog {

    /**
     * A use-counter feature definition. [grants] maps each granting class id to
     * the class level it comes online at. [maxUsesFor] gives the use cap by that
     * class's level — `null` means "uncomputable here" (Bardic Inspiration's
     * CHA-based cap, injected per-character) or "unlimited" (Rage at L20).
     */
    data class Feature(
        val id: String,
        val displayName: String,
        val grants: Map<String, Int>,
        val baseRecovery: FeatureRecovery,
        val maxUsesFor: (Int) -> Int?,
    )

    val allFeatures: List<Feature> = listOf(
        Feature("rage", "Rage", mapOf("barbarian" to 1), FeatureRecovery.LONG_REST) { level ->
            when {
                level <= 2 -> 2
                level <= 5 -> 3
                level <= 11 -> 4
                level <= 16 -> 5
                level <= 19 -> 6
                else -> null // L20: unlimited
            }
        },
        Feature("action-surge", "Action Surge", mapOf("fighter" to 2), FeatureRecovery.SHORT_REST) { level ->
            if (level >= 17) 2 else 1
        },
        Feature("second-wind", "Second Wind", mapOf("fighter" to 1), FeatureRecovery.SHORT_REST) { 1 },
        // Bardic Inspiration: cap = CHA mod (injected in -2014); recovery is long rest until bard L5.
        Feature("bardic-inspiration", "Bardic Inspiration", mapOf("bard" to 1), FeatureRecovery.LONG_REST) { null },
        Feature("channel-divinity", "Channel Divinity", mapOf("cleric" to 2, "paladin" to 3), FeatureRecovery.SHORT_REST) { level ->
            when {
                level <= 5 -> 1
                level <= 17 -> 2
                else -> 3
            }
        },
    )

    fun feature(id: String): Feature? = allFeatures.firstOrNull { it.id == id }
}

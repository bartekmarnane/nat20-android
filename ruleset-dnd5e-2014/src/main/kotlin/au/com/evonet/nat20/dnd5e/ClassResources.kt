package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.ClassFeatureCatalog
import au.com.evonet.nat20.dnd5e.core.ClassResourceCatalog
import au.com.evonet.nat20.dnd5e.core.FeatureRecovery

/**
 * The `ClassEntry`-aware resolution of the `-core` class-resource catalogues
 * against a character's class list, plus the per-character caps the catalogue
 * can't see (Bardic Inspiration's CHA-based max and its L1-4/L5+ recovery
 * split). Port of the iOS `DnD5ePayload` resource accessors.
 */

/** A point pool resolved for a character — current + max for the UI/picker. */
data class ResolvedResourcePool(
    val id: String,
    val displayName: String,
    val abbreviation: String,
    val recovery: FeatureRecovery,
    val current: Int,
    val max: Int,
)

/** A use-counter feature resolved for a character. [max] null = unlimited (Rage at L20). */
data class ResolvedFeature(
    val id: String,
    val displayName: String,
    val recovery: FeatureRecovery,
    val current: Int?,
    val max: Int?,
)

/** Catalogue max for a point pool at this character's class level, or null when it isn't online. */
fun DnD5ePayload.maxResource(poolID: String): Int? {
    val pool = ClassResourceCatalog.pool(poolID) ?: return null
    val entry = classes.firstOrNull { it.classId.lowercase() == pool.classId } ?: return null
    val max = pool.maxValue(entry.level)
    return if (max > 0) max else null
}

/** Current value of a point pool (stored spent-down value, or the max when untouched). Null when not online. */
fun DnD5ePayload.currentResource(poolID: String): Int? {
    val max = maxResource(poolID) ?: return null
    return resourcePools[poolID] ?: max
}

/** Point pools this character has online, each with its current + max. */
fun DnD5ePayload.availableResourcePools(): List<ResolvedResourcePool> =
    ClassResourceCatalog.allPools.mapNotNull { pool ->
        val max = maxResource(pool.id) ?: return@mapNotNull null
        ResolvedResourcePool(
            id = pool.id,
            displayName = pool.displayName,
            abbreviation = pool.abbreviation,
            recovery = pool.recovery,
            current = resourcePools[pool.id] ?: max,
            max = max,
        )
    }

/** Use-counter features this character has online, with per-character caps/recovery injected. */
fun DnD5ePayload.availableClassFeatures(): List<ResolvedFeature> =
    ClassFeatureCatalog.allFeatures.mapNotNull { feature ->
        // The highest class level among the granting classes that have brought it online.
        val classLevel = feature.grants.mapNotNull { (classId, minLevel) ->
            classes.firstOrNull { it.classId.lowercase() == classId && it.level >= minLevel }?.level
        }.maxOrNull() ?: return@mapNotNull null

        val (max, recovery) = if (feature.id == "bardic-inspiration") {
            val cap = maxOf(1, AbilityScores.modifier(abilityScores.charisma))
            val rec = if (classLevel >= 5) FeatureRecovery.SHORT_REST else FeatureRecovery.LONG_REST
            cap to rec
        } else {
            feature.maxUsesFor(classLevel) to feature.baseRecovery
        }

        val current = if (max == null) null else (classFeatureUses[feature.id]?.remaining ?: max)
        ResolvedFeature(feature.id, feature.displayName, recovery, current, max)
    }

/** True if this character has any class pools or use-counter features. */
fun DnD5ePayload.hasClassResources(): Boolean =
    availableResourcePools().isNotEmpty() || availableClassFeatures().isNotEmpty()

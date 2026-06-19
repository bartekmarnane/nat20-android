package au.com.evonet.nat20.pf2e.core

import kotlinx.serialization.Serializable

/**
 * A Pathfinder 2e condition afflicting a character, with its value where the
 * condition is *valued*. PF2e conditions are the other big departure from 5e:
 * many carry a live number (Frightened 2 = −2 status penalty to everything,
 * Clumsy 1, Drained 3, Dying 1…), so a PF2e character stores a list of these
 * rather than a `[String]`. The catalogue (which exist, rules text) lives in the
 * `:ruleset-pf2e` package; this is the shared structural value type. Port of the
 * iOS `PathfinderCore/ValuedCondition`.
 */
@Serializable
data class ValuedCondition(
    /** Condition slug, e.g. "frightened", "off-guard", "clumsy". */
    val id: String,
    /** Current value, or null for an unvalued condition (Off-Guard, Prone, Blinded). */
    val value: Int? = null,
) {
    /** True for conditions that carry a number. */
    val isValued: Boolean get() = value != null

    /** Display label, e.g. "Frightened 2" or "Prone". */
    fun label(displayName: String): String = if (value != null) "$displayName $value" else displayName
}

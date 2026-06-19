package au.com.evonet.nat20.pf2e.core

import kotlinx.serialization.Serializable

/**
 * The four-step outcome of a Pathfinder 2e check. Every d20 roll resolves to one
 * of four degrees, not 5e's binary hit/miss — and crit success/failure is
 * primarily a function of *beating the DC by 10* (or missing by 10), with the
 * natural-20/natural-1 rule a separate additional step (nat 20 bumps up one
 * degree, nat 1 bumps down one). Port of the iOS `PathfinderCore/DegreeOfSuccess`.
 */
@Serializable
enum class DegreeOfSuccess(val value: Int, val displayName: String) {
    CRITICAL_FAILURE(0, "Critical Failure"),
    FAILURE(1, "Failure"),
    SUCCESS(2, "Success"),
    CRITICAL_SUCCESS(3, "Critical Success");

    /** Bump one degree better, clamped at critical success. */
    fun steppedUp(): DegreeOfSuccess = fromValue(minOf(value + 1, CRITICAL_SUCCESS.value))

    /** Bump one degree worse, clamped at critical failure. */
    fun steppedDown(): DegreeOfSuccess = fromValue(maxOf(value - 1, CRITICAL_FAILURE.value))

    companion object {
        fun fromValue(value: Int): DegreeOfSuccess = entries.first { it.value == value }

        /**
         * Resolve a check from its final [total] against a [dc], applying the
         * beat-by-10 / miss-by-10 thresholds and then the natural-20/natural-1
         * step. Pass [naturalRoll] (the raw d20 face 1..20) to apply the nat step;
         * null for non-d20 resolutions.
         */
        fun resolve(total: Int, dc: Int, naturalRoll: Int? = null): DegreeOfSuccess {
            var degree = when {
                total >= dc + 10 -> CRITICAL_SUCCESS
                total >= dc -> SUCCESS
                total <= dc - 10 -> CRITICAL_FAILURE
                else -> FAILURE
            }
            when (naturalRoll) {
                20 -> degree = degree.steppedUp()
                1 -> degree = degree.steppedDown()
            }
            return degree
        }
    }
}

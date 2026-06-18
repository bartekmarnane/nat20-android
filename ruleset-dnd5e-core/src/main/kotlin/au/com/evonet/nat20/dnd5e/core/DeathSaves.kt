package au.com.evonet.nat20.dnd5e.core

import kotlinx.serialization.Serializable

/**
 * The 5e three-success / three-failure death-save tracker for a character
 * downed at 0 HP. Both counts are clamped to 0–3. Port of the iOS
 * `DnD5eCore/DeathSaves.swift`. Cleared on a long rest (and, on Android, when
 * healing lifts the character back above 0).
 */
@Serializable
data class DeathSaves(
    val successes: Int = 0,
    val failures: Int = 0,
) {
    init {
        require(successes in 0..3) { "successes out of range: $successes" }
        require(failures in 0..3) { "failures out of range: $failures" }
    }

    /** Three successes — the character is stable (unconscious but no longer dying). */
    val isStable: Boolean get() = successes >= 3

    /** Three failures — the character has died. */
    val isDead: Boolean get() = failures >= 3

    /** No marks recorded. */
    val isCleared: Boolean get() = successes == 0 && failures == 0

    companion object {
        val cleared = DeathSaves()

        /** Builds a tracker clamping both counts into 0–3 (mirrors the iOS init). */
        fun clamped(successes: Int, failures: Int): DeathSaves =
            DeathSaves(successes.coerceIn(0, 3), failures.coerceIn(0, 3))
    }
}

/** A single death-save outcome to record. */
@Serializable
enum class DeathSaveOutcome { SUCCESS, FAILURE, CLEAR }

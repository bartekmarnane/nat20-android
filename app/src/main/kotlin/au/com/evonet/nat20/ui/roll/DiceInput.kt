package au.com.evonet.nat20.ui.roll

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Where a roll's numbers come from — the app's RNG, or the player's own dice on
 * the table (A25).
 *
 * Testers asked for the second one: they roll physical dice and want the app to
 * record what actually happened, mainly so the journal stays truthful. Both
 * paths produce the same `RollResult`, so nothing downstream (intents, events,
 * the journal) can tell them apart — see `ManualRollEntry`.
 */
enum class DiceInput {
    /** Tap ROLL, the app rolls. The default. */
    APP,

    /** The player rolls real dice and enters the faces. */
    PHYSICAL;

    val label: String
        get() = when (this) {
            APP -> "App rolls"
            PHYSICAL -> "I roll my own"
        }
}

/**
 * The player's preferred roll source, provided once at the `NatApp` root from
 * `AppSettings`. Every `RollResultView` reads it to decide which mode to open
 * in; individual rolls can still be flipped in the widget itself.
 */
val LocalDiceInput = staticCompositionLocalOf { DiceInput.APP }

package au.com.evonet.nat20.ui.editor

import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * JSON codec for wizard state that outlives the composition.
 *
 * `allowStructuredMapKeys` is on because a couple of wizards key maps by a
 * data class (PF2e's `FeatSlotKey`); `ignoreUnknownKeys` keeps a restore from
 * blowing up if a catalogue type gains a field between the save and the
 * restore (an app update while the wizard sat backgrounded).
 */
val WizardJson: Json = Json {
    ignoreUnknownKeys = true
    allowStructuredMapKeys = true
}

/**
 * A [Saver] for any `@Serializable` value, for use as `rememberSaveable`'s
 * `stateSaver`. The wizards hold a lot of state that a `Bundle` can't take on
 * its own — ability scores, chosen-spell sets, per-level advancement choices,
 * inventory — so it round-trips as a JSON string instead.
 *
 * Losing that state to a rotation or a low-memory kill means re-walking the
 * whole character build from the top, which is why every wizard field that
 * feeds `build()` goes through here rather than plain `remember`.
 *
 * ```
 * var chosenSkills by rememberSaveable(stateSaver = jsonStateSaver<Set<String>>()) {
 *     mutableStateOf(emptySet())
 * }
 * ```
 */
inline fun <reified T> jsonStateSaver(): Saver<T, Any> {
    val serializer = serializer<T>()
    return Saver(
        save = { value -> WizardJson.encodeToString(serializer, value) },
        restore = { encoded -> WizardJson.decodeFromString(serializer, encoded as String) },
    )
}

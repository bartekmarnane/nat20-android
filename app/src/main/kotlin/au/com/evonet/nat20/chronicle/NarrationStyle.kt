package au.com.evonet.nat20.chronicle

/**
 * The voice the AI chronicler writes session recaps in (A9). [voice] is woven
 * into the system instruction; [label] shows in Settings. The default
 * [CHRONICLER] preserves the original archaic hand-written-journal tone.
 */
enum class NarrationStyle(val label: String, val voice: String) {
    CHRONICLER("Chronicler", "in a slightly archaic, hand-written-journal voice."),
    HEROIC("Heroic", "in a sweeping, triumphant heroic-saga voice."),
    GRIM("Grim", "in a dark, somber, foreboding voice."),
    WHIMSICAL("Whimsical", "in a light, witty, faintly humorous voice."),
}

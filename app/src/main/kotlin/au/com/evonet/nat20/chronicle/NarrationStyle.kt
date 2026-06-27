package au.com.evonet.nat20.chronicle

/**
 * How the journal renders a committed session — port of the iOS
 * `AppSettings.NarrationStyle`. [SIMPLE] shows plain template entries and never
 * calls the on-device model; [STORIED] upgrades them to AI-generated prose.
 * [label] / [blurb] show in Settings.
 */
enum class NarrationStyle(val label: String, val blurb: String) {
    SIMPLE(
        "Simple",
        "Journal entries read as plain facts — \"Took 5 slashing damage — goblin\".",
    ),
    STORIED(
        "Storied",
        "Journal entries read as atmospheric narratives — a short past-tense sentence drawn from your character's situation.",
    ),
}

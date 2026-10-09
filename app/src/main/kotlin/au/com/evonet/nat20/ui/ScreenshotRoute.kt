package au.com.evonet.nat20.ui

/**
 * DEBUG-only screenshot harness — the Android analogue of iOS's
 * `ScreenshotMode`. Launch the activity with a `shot` string extra:
 *
 *     adb shell am start -n au.com.evonet.nat20/.MainActivity --es shot stats
 *
 * `MainActivity` then skips the splash and onboarding (unless the shot *is*
 * onboarding) and `NatApp`'s router navigates straight to the screen, seeding
 * a campaign for the demo character where the shot needs one. One launch per
 * shot; `scripts/screenshots.sh` drives the loop. Release builds never set
 * [shot] (the activity only reads the extra when `BuildConfig.DEBUG`).
 */
object ScreenshotRoute {
    /** The shot being photographed this launch; null for a normal launch. */
    @Volatile
    var shot: String? = null

    val isActive: Boolean get() = shot != null
    val showsOnboarding: Boolean get() = shot == "onboarding"
    val opensActions: Boolean get() = shot == "actions"
    val opensLevelUp: Boolean get() = shot == "levelUp"

    /** Codex tab (2014 + 2024 shells share the order) the sheet opens on. */
    val initialTab: Int
        get() = when (shot) {
            "skills" -> 1
            "combat", "combat2024" -> 2
            "spells", "spells2024" -> 3
            "items", "items2024" -> 4
            "lore" -> 5
            else -> 0
        }

    /** Every shot name, in the order `scripts/screenshots.sh` captures them. */
    val all: List<String> = listOf(
        "roster", "stats", "skills", "combat", "spells", "items", "lore",
        "journal", "actions", "levelUp",
        "onboarding", "settings", "patron", "credits",
        "spellLibrary", "itemCatalog", "monsterCodex", "customCreatures",
        "create", "building", "editor", "characterSettings", "contentSources", "past",
        "sheet2024", "combat2024", "spells2024", "items2024", "journal2024", "sheetPF2e",
        // Deletes the seed for the rest of the install — keep it last.
        "emptyRoster",
    )
}

package au.com.evonet.nat20.store

import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.DnD5eRuleset
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.ClassEntry
import au.com.evonet.nat20.domain.Character
import java.time.Instant

/**
 * In-memory demo roster for the A4 UI shell. Mirrors the iOS `DemoCharacters`
 * seed so the list and read-only sheet have something to render before
 * persistence (A5) and character creation (A8) exist.
 *
 * When Room lands (A5) this seed moves behind the repository and becomes
 * `BuildConfig.DEBUG`-only (release installs start empty → onboarding, A12).
 */
object DemoCharacters {
    private val ruleset = DnD5eRuleset()
    private val seededAt: Instant = Instant.parse("2026-06-18T00:00:00Z")

    fun seed(): List<Character> = listOf(thorgar(), lyra(), kael())

    private fun character(name: String, payload: DnD5ePayload): Character =
        Character.new(name = name, ruleset = ruleset, payload = payload, timestamp = seededAt)

    /** Mountain dwarf fighter, lightly wounded — exercises current < max HP. */
    private fun thorgar() = character(
        name = "Thorgar Stonefist",
        payload = DnD5ePayload(
            race = "mountain-dwarf",
            classes = listOf(ClassEntry("fighter", 3, subclass = "champion")),
            abilityScores = AbilityScores(
                strength = 16, dexterity = 12, constitution = 16,
                intelligence = 9, wisdom = 11, charisma = 8,
            ),
            maxHp = 31,
            currentHp = 24,
            background = "soldier",
            selectedSkills = listOf("athletics", "intimidation", "perception", "survival"),
        ),
    )

    /** High elf wizard carrying temp HP — exercises the temp-HP vitals line. */
    private fun lyra() = character(
        name = "Lyra Brightwood",
        payload = DnD5ePayload(
            race = "high-elf",
            classes = listOf(ClassEntry("wizard", 2)),
            abilityScores = AbilityScores(
                strength = 8, dexterity = 16, constitution = 12,
                intelligence = 16, wisdom = 11, charisma = 10,
            ),
            maxHp = 14,
            currentHp = 14,
            temporaryHp = 4,
            background = "sage",
            selectedSkills = listOf("arcana", "history", "investigation", "insight"),
        ),
    )

    /** Level-1 human cleric at full health. */
    private fun kael() = character(
        name = "Brother Kael",
        payload = DnD5ePayload(
            race = "human",
            classes = listOf(ClassEntry("cleric", 1)),
            abilityScores = AbilityScores(
                strength = 13, dexterity = 10, constitution = 14,
                intelligence = 11, wisdom = 16, charisma = 12,
            ),
            maxHp = 10,
            currentHp = 10,
            background = "acolyte",
            selectedSkills = listOf("insight", "religion", "medicine", "persuasion"),
        ),
    )
}

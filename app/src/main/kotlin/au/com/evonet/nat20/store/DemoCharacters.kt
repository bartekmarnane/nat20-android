package au.com.evonet.nat20.store

import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.DnD5eRuleset
import au.com.evonet.nat20.dnd5e.InventoryItem
import au.com.evonet.nat20.dnd5e.withFullSpellSlots
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.dnd5e.ClassEntry
import au.com.evonet.nat20.dnd5e2024.ClassEntry2024
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Ruleset
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
    private val ruleset2024 = DnD5e2024Ruleset()
    private val seededAt: Instant = Instant.parse("2026-06-18T00:00:00Z")

    fun seed(): List<Character> = listOf(thorgar(), lyra(), kael(), nyx())

    private fun character(name: String, payload: DnD5ePayload): Character =
        Character.new(name = name, ruleset = ruleset, payload = payload, timestamp = seededAt)

    /** A D&D 5e (2024) character — proves the second ruleset renders alongside 2014 (A18). */
    private fun nyx(): Character = Character.new(
        name = "Nyx Veil",
        ruleset = ruleset2024,
        payload = DnD5e2024Payload(
            species = "elf",
            classes = listOf(ClassEntry2024("rogue", 4, subclass = "thief")),
            background = "criminal",
            abilityScores = AbilityScores(
                strength = 10, dexterity = 17, constitution = 13,
                intelligence = 14, wisdom = 12, charisma = 10,
            ),
            backgroundASI = mapOf(Ability.DEXTERITY to 2, Ability.INTELLIGENCE to 1),
            maxHp = 27,
            currentHp = 27,
            skillProficiencies = listOf("stealth", "sleightOfHand", "perception", "investigation"),
            originFeat = "alert",
            weaponMasteries = listOf("vex", "nick"),
        ),
        timestamp = seededAt,
    )

    // Catalogue helpers so the demo seed carries real equipment (drives AC + the Items tab).
    private fun weapon(id: String, equipped: Boolean = false): InventoryItem =
        DnD5eCatalog.weapon(id)!!.makeItem(equipped = equipped)
    private fun armor(id: String, equipped: Boolean = false): InventoryItem =
        DnD5eCatalog.armorPiece(id)!!.makeItem(equipped = equipped)
    private fun gear(id: String, equipped: Boolean = false, quantity: Int = 1): InventoryItem =
        DnD5eCatalog.gearPiece(id)!!.makeItem(equipped = equipped, quantity = quantity)

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
            // Chain mail (16, heavy) + shield (+2) → AC 18; longsword wielded.
            inventory = listOf(
                armor("chain-mail", equipped = true),
                gear("shield", equipped = true),
                weapon("longsword", equipped = true),
                weapon("handaxe"),
                gear("potion-of-healing", quantity = 2),
            ),
            coins = mapOf(Coin.GP to 42, Coin.SP to 8),
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
            // No armor (mage) → AC 10 + DEX(+3) = 13; a dagger for emergencies.
            inventory = listOf(
                weapon("dagger", equipped = true),
                weapon("quarterstaff"),
                gear("potion-of-healing"),
            ),
            coins = mapOf(Coin.GP to 15),
            // Wizard (known caster from her spellbook) — L2 → 3 first-level slots.
            cantripsKnown = listOf("fire-bolt", "mage-hand", "prestidigitation"),
            spellsKnown = mapOf("wizard" to listOf("magic-missile", "shield", "detect-magic", "sleep")),
        ).withFullSpellSlots(),
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
            // Scale mail (14, medium, DEX cap +2) + shield (+2); DEX +0 → AC 16.
            inventory = listOf(
                armor("scale-mail", equipped = true),
                gear("shield", equipped = true),
                weapon("mace", equipped = true),
                gear("potion-of-healing"),
            ),
            coins = mapOf(Coin.GP to 9, Coin.SP to 5, Coin.CP to 12),
            // Cleric (prepared caster) — L1 → 2 first-level slots; a daily prep list.
            cantripsKnown = listOf("sacred-flame", "guidance"),
            preparedSpells = mapOf("cleric" to listOf("cure-wounds", "bless", "guiding-bolt")),
        ).withFullSpellSlots(),
    )
}

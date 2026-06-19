package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.EffectSource

/**
 * Mechanical auto-apply for D&D 5e (2014) **race traits** (A19) — the 2014
 * analogue of [au.com.evonet.nat20.dnd5e2024.SpeciesTraits2024]. Centralises the
 * race-ID compares so every consumer (the folding layer, the damage intent, the
 * sheet) asks the same question:
 *
 * - **Innate damage resistances** (Tiefling → fire, Dwarven/Stout Resilience →
 *   poison, Aasimar → necrotic/radiant) fold into [effectiveDamageResistances].
 * - **Granted skill proficiencies** (Elf Keen Senses → Perception) fold into
 *   [effectiveSkillProficiencies].
 * - **Relentless Endurance** (Half-Orc) is a real consumer on the damage intent.
 * - **Halfling Luck / Savage Attacks** and the save-advantage hints surface as
 *   [reminders] on the sheet (advantage/reroll aren't flat numbers).
 *
 * Port of the iOS `RaceTraits`, keyed by the bundled SRD race ids.
 */
object RaceTraits {

    /** A surfaced, non-numeric trait reminder (advantage, reroll, immunity). */
    data class Reminder(val title: String, val detail: String)

    /** Damage types a race resists innately, lower-cased. */
    fun innateDamageResistances(race: String): List<String> = when (race.lowercase()) {
        "tiefling" -> listOf("fire")
        "hill-dwarf", "mountain-dwarf" -> listOf("poison")
        "stout-halfling" -> listOf("poison")
        "aasimar" -> listOf("necrotic", "radiant")
        else -> emptyList()
    }

    /** Passive [ActiveEffect]s a race always contributes (resistances). Folded via [allEffects]. */
    fun passiveEffects(race: String): List<ActiveEffect> = innateDamageResistances(race).map { type ->
        ActiveEffect(
            id = "passive:race:$race:resist-$type",
            name = resistanceTraitName(race),
            source = EffectSource.Feature("race:$race"),
            modifiers = listOf(EffectModifier.DamageResistance(type)),
            duration = EffectDuration.UntilCancelled,
        )
    }

    private fun resistanceTraitName(race: String): String = when (race.lowercase()) {
        "tiefling" -> "Hellish Resistance"
        "hill-dwarf", "mountain-dwarf" -> "Dwarven Resilience"
        "stout-halfling" -> "Stout Resilience"
        "aasimar" -> "Celestial Resistance"
        else -> "Resilience"
    }

    /** Skill proficiencies a race trait auto-grants (Elf Keen Senses → Perception). */
    fun grantedSkills(race: String): List<String> = when (race.lowercase()) {
        "high-elf", "wood-elf" -> listOf("perception")
        else -> emptyList()
    }

    /** Halfling Luck — reroll a natural 1 on any d20 test (both PHB subraces). */
    fun hasHalflingLuck(race: String): Boolean = race.lowercase() in setOf("lightfoot-halfling", "stout-halfling")

    /** Relentless Endurance (Half-Orc) — drop to 1 HP instead of 0, once per long rest. */
    fun hasRelentlessEndurance(race: String): Boolean = race.equals("half-orc", ignoreCase = true)

    /** Savage Attacks (Half-Orc) — roll an extra weapon damage die on a melee critical hit. */
    fun hasSavageAttacks(race: String): Boolean = race.equals("half-orc", ignoreCase = true)

    /** Non-numeric trait reminders surfaced on the sheet. */
    fun reminders(race: String): List<Reminder> = when (race.lowercase()) {
        "hill-dwarf", "mountain-dwarf" -> listOf(
            Reminder("Dwarven Resilience", "Resistance to poison damage and advantage on saving throws against poison."),
            Reminder("Stonecunning", "Add double proficiency to History checks about stonework."),
        )
        "high-elf", "wood-elf" -> listOf(
            Reminder("Fey Ancestry", "Advantage on saves against being Charmed; magic can't put you to sleep."),
            Reminder("Keen Senses", "Proficiency in Perception (auto-applied)."),
            Reminder("Trance", "4 hours of trance counts as a long rest's sleep."),
        )
        "lightfoot-halfling", "stout-halfling" -> listOf(
            Reminder("Lucky", "When you roll a 1 on a d20 attack, check, or save, reroll and use the new roll."),
            Reminder("Brave", "Advantage on saving throws against being Frightened."),
            Reminder("Halfling Nimbleness", "Move through the space of any creature larger than you."),
        ) + (if (race.equals("stout-halfling", ignoreCase = true)) listOf(Reminder("Stout Resilience", "Resistance to poison and advantage on poison saves.")) else emptyList())
        "half-orc" -> listOf(
            Reminder("Relentless Endurance", "When reduced to 0 HP but not killed outright, drop to 1 HP instead (once per long rest)."),
            Reminder("Savage Attacks", "Roll one extra weapon damage die on a melee critical hit."),
            Reminder("Menacing", "Proficiency in the Intimidation skill."),
        )
        "tiefling" -> listOf(
            Reminder("Hellish Resistance", "Resistance to fire damage."),
            Reminder("Infernal Legacy", "You know Thaumaturgy; gain innate spells as you level."),
        )
        "forest-gnome", "rock-gnome" -> listOf(
            Reminder("Gnome Cunning", "Advantage on Intelligence, Wisdom, and Charisma saving throws against magic."),
        )
        "half-elf" -> listOf(
            Reminder("Fey Ancestry", "Advantage on saves against being Charmed; magic can't put you to sleep."),
            Reminder("Skill Versatility", "Proficiency in two skills of your choice."),
        )
        "dragonborn" -> listOf(
            Reminder("Breath Weapon", "Exhale destructive energy (save for half) determined by your ancestry."),
            Reminder("Damage Resistance", "Resistance to the damage type of your draconic ancestry."),
        )
        "aasimar" -> listOf(
            Reminder("Celestial Resistance", "Resistance to necrotic and radiant damage."),
            Reminder("Healing Hands", "Once per long rest, touch to heal HP equal to your level."),
        )
        else -> emptyList()
    }
}

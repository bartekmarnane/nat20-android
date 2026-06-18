package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.EffectSource

/**
 * Mechanical auto-apply for D&D 5e (2024) **species traits** (A21). The catalogue
 * ([DnD5e2024Catalog.species]) carries traits as display strings; this object is
 * the single place that turns the ones with *rules effects* into folded modifiers:
 *
 * - **Damage resistances** (Dwarf → poison) fold into [effectiveDamageResistances]
 *   so [TakeDamage2024] halves them.
 * - **Granted skill proficiencies** (Elf Keen Senses → Perception) fold into
 *   [effectiveSkillProficiencies] so the Skills tab + passive Perception count them.
 * - **Save / d20 advantages** and other narrative riders surface as [reminders]
 *   on the sheet (advantage isn't a flat number, so it's a prompt, not a fold).
 * - **Relentless Endurance** (Orc) is a real consumer on the damage intent.
 *
 * Mirrors the iOS `TraitFeatEffects2024` + `SpeciesTrait` consumers, keyed by the
 * Android catalogue's species ids rather than separate trait slugs.
 */
object SpeciesTraits2024 {

    /** A surfaced, non-numeric trait reminder (advantage, immunity, at-will rider). */
    data class Reminder(val title: String, val detail: String)

    /** Passive [ActiveEffect]s a species always contributes (resistances). Folded via [allEffects]. */
    fun passiveEffects(speciesId: String): List<ActiveEffect> = resistances(speciesId).map { type ->
        ActiveEffect(
            id = "passive:species:$speciesId:resist-$type",
            name = "${speciesId.replaceFirstChar(Char::uppercase)} Resilience",
            source = EffectSource.Feature("species:$speciesId"),
            modifiers = listOf(EffectModifier.DamageResistance(type)),
            duration = EffectDuration.UntilCancelled,
        )
    }

    /** Damage types a species resists innately. */
    fun resistances(speciesId: String): List<String> = when (speciesId.lowercase()) {
        "dwarf" -> listOf("poison")
        else -> emptyList()
    }

    /** Skill proficiencies granted by a species trait (Elf Keen Senses → Perception). */
    fun grantedSkills(speciesId: String): List<String> = when (speciesId.lowercase()) {
        "elf" -> listOf("perception")
        else -> emptyList()
    }

    /** True for species with Relentless Endurance (drop to 1 HP instead of 0, once per long rest). */
    fun hasRelentlessEndurance(speciesId: String): Boolean = speciesId.equals("orc", ignoreCase = true)

    /** Non-numeric trait reminders surfaced on the sheet. */
    fun reminders(speciesId: String): List<Reminder> = when (speciesId.lowercase()) {
        "dwarf" -> listOf(
            Reminder("Dwarven Resilience", "Resistance to poison damage and advantage on saves against poison."),
            Reminder("Stonecunning", "Tremorsense 60 ft. while in contact with stone (bonus action, 10 min)."),
        )
        "elf" -> listOf(
            Reminder("Fey Ancestry", "Advantage on saving throws to avoid or end the Charmed condition."),
            Reminder("Trance", "Magic can't put you to sleep; 4 hours of trance equals a long rest."),
            Reminder("Keen Senses", "Proficiency in Perception (auto-applied)."),
        )
        "halfling" -> listOf(
            Reminder("Brave", "Advantage on saving throws to avoid or end the Frightened condition."),
            Reminder("Luck", "When you roll a 1 on a d20 test, you can reroll and must use the new roll."),
            Reminder("Halfling Nimbleness", "Move through the space of any creature larger than you."),
        )
        "gnome" -> listOf(
            Reminder("Gnomish Cunning", "Advantage on Intelligence, Wisdom, and Charisma saving throws."),
        )
        "orc" -> listOf(
            Reminder("Relentless Endurance", "When reduced to 0 HP but not killed outright, drop to 1 HP instead (once per long rest)."),
            Reminder("Adrenaline Rush", "Take the Dash action as a bonus action; gain temporary HP."),
        )
        "dragonborn" -> listOf(
            Reminder("Damage Resistance", "Resistance to the damage type of your draconic ancestry."),
            Reminder("Breath Weapon", "Replace an attack to exhale destructive energy (save for half)."),
        )
        "tiefling" -> listOf(
            Reminder("Fiendish Legacy", "Darkvision and innate spells from your chosen legacy."),
            Reminder("Otherworldly Presence", "You know the Thaumaturgy cantrip."),
        )
        "human" -> listOf(
            Reminder("Resourceful", "Gain Heroic Inspiration whenever you finish a long rest."),
            Reminder("Skillful", "Proficiency in one skill of your choice."),
            Reminder("Versatile", "Gain an Origin feat of your choice."),
        )
        else -> emptyList()
    }
}

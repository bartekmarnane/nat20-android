package au.com.evonet.nat20.dnd5e.core

import kotlinx.serialization.Serializable

/**
 * The 14 standard 5e conditions (Exhaustion is tracked separately — it's a
 * 0–6 level, not a binary flag). Port of the iOS conditions enum, trimmed to
 * the display name + a one-line effect summary. The payload stores conditions
 * as free-form strings so house-rule conditions ("Cursed", "Marked") work too;
 * this enum is the standard palette the picker offers.
 */
@Serializable
enum class Condition(val displayName: String, val summary: String) {
    BLINDED("Blinded", "Can't see; attacks against you have advantage, yours have disadvantage."),
    CHARMED("Charmed", "Can't attack the charmer, who has advantage on social checks with you."),
    DEAFENED("Deafened", "Can't hear; automatically fails hearing-based checks."),
    FRIGHTENED("Frightened", "Disadvantage while the source is in sight; can't move closer to it."),
    GRAPPLED("Grappled", "Speed 0; ends if the grappler is incapacitated or you're moved away."),
    INCAPACITATED("Incapacitated", "Can't take actions or reactions."),
    INVISIBLE("Invisible", "Unseen without aid; your attacks have advantage, attacks against you disadvantage."),
    PARALYZED("Paralyzed", "Incapacitated, can't move/speak; auto-fail STR/DEX saves; melee hits crit."),
    PETRIFIED("Petrified", "Turned to stone; incapacitated, resistance to all damage, immune to poison/disease."),
    POISONED("Poisoned", "Disadvantage on attack rolls and ability checks."),
    PRONE("Prone", "Can only crawl; disadvantage on attacks; melee against you has advantage, ranged disadvantage."),
    RESTRAINED("Restrained", "Speed 0; attacks against you have advantage, yours disadvantage; disadvantage on DEX saves."),
    STUNNED("Stunned", "Incapacitated, can't move; auto-fail STR/DEX saves; attacks against you have advantage."),
    UNCONSCIOUS("Unconscious", "Incapacitated and prone; drops what it holds; auto-fail STR/DEX saves; melee hits crit.");

    companion object {
        /** Resolves a (possibly homebrew) condition name to a standard [Condition], case-insensitively. */
        fun fromName(name: String): Condition? = entries.firstOrNull { it.displayName.equals(name.trim(), ignoreCase = true) }
    }
}

/**
 * The 5e (2014) exhaustion track: six cumulative levels, where level 6 is death.
 * Modelled as effect text per level (not a numeric penalty — 2014 exhaustion
 * isn't a flat −X like the 2024 rules).
 */
object Exhaustion {
    const val MAX: Int = 6

    fun clamp(level: Int): Int = level.coerceIn(0, MAX)

    fun isFatal(level: Int): Boolean = level >= MAX

    /** The cumulative effect *gained* at [level] (1–6); empty for 0. */
    fun effect(level: Int): String = when (level) {
        1 -> "Disadvantage on ability checks"
        2 -> "Speed halved"
        3 -> "Disadvantage on attack rolls and saving throws"
        4 -> "Hit point maximum halved"
        5 -> "Speed reduced to 0"
        6 -> "Death"
        else -> ""
    }
}

package au.com.evonet.nat20.pf2e

import kotlinx.serialization.Serializable

/**
 * Pathfinder 2e weapon + armor **runes** (A22). Companion-app scope: the
 * **potency** rune feeds the Strike's attack bonus / armor's AC item bonus, the
 * **striking** rune adds weapon damage dice, **resilient** a flat item bonus to
 * every save, and **property** runes ride along as named reminders (their
 * per-hit riders aren't auto-rolled). Values clamp 0–3; a piece's property slots
 * equal its potency. Port of the iOS `Runes`.
 */
@Serializable
data class WeaponRunes(
    /** Weapon potency (0–3) — item bonus to attack rolls. */
    val potency: Int = 0,
    /** Striking tier (0–3) — *extra* damage dice (total dice = 1 + striking). */
    val striking: Int = 0,
    val properties: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = potency == 0 && striking == 0 && properties.isEmpty()
    val propertySlots: Int get() = potency
}

@Serializable
data class ArmorRunes(
    /** Armor potency (0–3) — item bonus to AC. */
    val potency: Int = 0,
    /** Resilient tier (0–3) — item bonus to all saving throws. */
    val resilient: Int = 0,
    val properties: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = potency == 0 && resilient == 0 && properties.isEmpty()
    val propertySlots: Int get() = potency
}

/** A property rune — a named effect (reminder), with the level it unlocks + price for display. */
data class PropertyRune(val id: String, val name: String, val kind: RuneKind, val level: Int, val price: Int, val summary: String)

enum class RuneKind { WEAPON, ARMOR }

object Runes {
    /** The highest tier a fundamental rune reaches. */
    const val MAX_TIER: Int = 3

    fun weaponPotencyPrice(tier: Int): Int = intArrayOf(0, 35, 935, 8935)[tier.coerceIn(0, 3)]
    fun strikingPrice(tier: Int): Int = intArrayOf(0, 65, 1065, 31065)[tier.coerceIn(0, 3)]
    fun armorPotencyPrice(tier: Int): Int = intArrayOf(0, 160, 1060, 20560)[tier.coerceIn(0, 3)]
    fun resilientPrice(tier: Int): Int = intArrayOf(0, 340, 3440, 49440)[tier.coerceIn(0, 3)]

    /** A representative spread of property runes; full coverage is a content follow-up. */
    val propertyRunes: List<PropertyRune> = listOf(
        PropertyRune("flaming", "Flaming", RuneKind.WEAPON, 8, 500, "On a hit, deals 1d6 persistent fire damage."),
        PropertyRune("frost", "Frost", RuneKind.WEAPON, 8, 500, "On a hit, deals 1d6 cold damage."),
        PropertyRune("shock", "Shock", RuneKind.WEAPON, 8, 500, "On a hit, deals 1d6 electricity damage."),
        PropertyRune("ghost-touch", "Ghost Touch", RuneKind.WEAPON, 4, 75, "The weapon affects incorporeal creatures normally."),
        PropertyRune("returning", "Returning", RuneKind.WEAPON, 3, 55, "A thrown weapon flies back to your hand after a Strike."),
        PropertyRune("wounding", "Wounding", RuneKind.WEAPON, 7, 340, "On a hit, the target also takes 1d6 persistent bleed."),
        PropertyRune("fire-resistant", "Fire-Resistant", RuneKind.ARMOR, 8, 500, "Gain resistance to fire and to environmental heat."),
        PropertyRune("slick", "Slick", RuneKind.ARMOR, 5, 45, "Gain an item bonus to Acrobatics to Escape and Squeeze."),
        PropertyRune("shadow", "Shadow", RuneKind.ARMOR, 5, 55, "Gain an item bonus to Stealth checks."),
        PropertyRune("invisibility", "Invisibility", RuneKind.ARMOR, 8, 500, "Turn invisible for 1 minute, twice per day."),
    )

    fun propertyRune(id: String): PropertyRune? = propertyRunes.firstOrNull { it.id == id }
    fun propertyRunes(kind: RuneKind): List<PropertyRune> = propertyRunes.filter { it.kind == kind }
}

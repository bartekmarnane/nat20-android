package au.com.evonet.nat20.dnd5e2024

import kotlinx.serialization.Serializable

/**
 * The 2024 weapon + armor catalogues and the weapon-mastery system. These live
 * in the 2024 module (not `-core`) precisely because **weapon mastery** is the
 * edition's divergence point — the 2014 `WeaponProperties` has no mastery
 * concept. Port of the iOS `WeaponMastery2024.swift` + `Armor2024.swift`.
 */

/** The eight weapon-mastery properties introduced in D&D 5e (2024). Each weapon has exactly one. */
@Serializable
enum class WeaponMastery2024(val displayName: String, val summary: String) {
    CLEAVE("Cleave", "On a hit, deal damage to a second creature within reach."),
    GRAZE("Graze", "On a miss, still deal damage equal to your ability modifier."),
    NICK("Nick", "Make the extra Light-weapon attack as part of the Attack action."),
    PUSH("Push", "On a hit, push a Large-or-smaller target up to 10 feet away."),
    SAP("Sap", "On a hit, the target has Disadvantage on its next attack."),
    SLOW("Slow", "On a hit, reduce the target's Speed by 10 feet."),
    TOPPLE("Topple", "On a hit, force a Constitution save or knock the target Prone."),
    VEX("Vex", "On a hit, gain Advantage on your next attack against the target.");

    companion object {
        /** Case-insensitive lookup by id (`"vex"`); null for an unknown key. */
        fun from(id: String): WeaponMastery2024? = entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
    }
}

/** A 2024 weapon. The [mastery] property is the edition's divergence from 2014. */
data class Weapon2024(
    val id: String,
    val name: String,
    val category: Category,
    val damage: String,
    val mastery: WeaponMastery2024,
) {
    enum class Category { SIMPLE, MARTIAL }
}

/** Representative 2024 weapon catalogue with mastery properties (per the 2024 PHB weapon table). */
object Weapons2024 {
    val all: List<Weapon2024> = listOf(
        // Simple
        Weapon2024("dagger", "Dagger", Weapon2024.Category.SIMPLE, "1d4 piercing", WeaponMastery2024.NICK),
        Weapon2024("mace", "Mace", Weapon2024.Category.SIMPLE, "1d6 bludgeoning", WeaponMastery2024.SAP),
        Weapon2024("quarterstaff", "Quarterstaff", Weapon2024.Category.SIMPLE, "1d6 bludgeoning", WeaponMastery2024.TOPPLE),
        Weapon2024("spear", "Spear", Weapon2024.Category.SIMPLE, "1d6 piercing", WeaponMastery2024.SAP),
        Weapon2024("handaxe", "Handaxe", Weapon2024.Category.SIMPLE, "1d6 slashing", WeaponMastery2024.VEX),
        Weapon2024("shortbow", "Shortbow", Weapon2024.Category.SIMPLE, "1d6 piercing", WeaponMastery2024.VEX),
        // Martial
        Weapon2024("longsword", "Longsword", Weapon2024.Category.MARTIAL, "1d8 slashing", WeaponMastery2024.SAP),
        Weapon2024("shortsword", "Shortsword", Weapon2024.Category.MARTIAL, "1d6 piercing", WeaponMastery2024.VEX),
        Weapon2024("rapier", "Rapier", Weapon2024.Category.MARTIAL, "1d8 piercing", WeaponMastery2024.VEX),
        Weapon2024("greatsword", "Greatsword", Weapon2024.Category.MARTIAL, "2d6 slashing", WeaponMastery2024.GRAZE),
        Weapon2024("greataxe", "Greataxe", Weapon2024.Category.MARTIAL, "1d12 slashing", WeaponMastery2024.CLEAVE),
        Weapon2024("maul", "Maul", Weapon2024.Category.MARTIAL, "2d6 bludgeoning", WeaponMastery2024.TOPPLE),
        Weapon2024("warhammer", "Warhammer", Weapon2024.Category.MARTIAL, "1d8 bludgeoning", WeaponMastery2024.PUSH),
        Weapon2024("glaive", "Glaive", Weapon2024.Category.MARTIAL, "1d10 slashing", WeaponMastery2024.GRAZE),
        Weapon2024("halberd", "Halberd", Weapon2024.Category.MARTIAL, "1d10 slashing", WeaponMastery2024.CLEAVE),
        Weapon2024("longbow", "Longbow", Weapon2024.Category.MARTIAL, "1d8 piercing", WeaponMastery2024.SLOW),
    )

    fun weapon(id: String): Weapon2024? = all.firstOrNull { it.id == id }
}

/** How many weapon-mastery slots a class grants at a given level (2024 PHB). */
object WeaponMasteryProgression2024 {
    fun slots(classId: String, level: Int): Int = when (classId.lowercase()) {
        "fighter" -> when {
            level < 4 -> 3
            level < 10 -> 4
            level < 16 -> 5
            else -> 6
        }
        "barbarian", "paladin", "ranger", "rogue" -> 2
        else -> 0
    }
}

/** A 2024 armor. AC math follows the PHB table: Light adds full DEX, Medium caps DEX at +2, Heavy ignores DEX. */
data class Armor2024(
    val id: String,
    val name: String,
    val category: Category,
    val baseAC: Int,
) {
    enum class Category(val dexCap: Int?) {
        /** Uncapped DEX. */
        LIGHT(null),
        /** DEX capped at +2. */
        MEDIUM(2),
        /** DEX ignored. */
        HEAVY(0),
    }

    /** AC granted by wearing this armor, given the wearer's DEX modifier. */
    fun effectiveAC(dexModifier: Int): Int {
        val cap = category.dexCap ?: return baseAC + dexModifier
        return baseAC + minOf(dexModifier, cap)
    }
}

/** Representative 2024 armor catalogue. A shield is tracked separately on the payload (+2). */
object Armors2024 {
    val all: List<Armor2024> = listOf(
        // Light — base + full DEX
        Armor2024("padded", "Padded Armor", Armor2024.Category.LIGHT, 11),
        Armor2024("leather", "Leather Armor", Armor2024.Category.LIGHT, 11),
        Armor2024("studded-leather", "Studded Leather Armor", Armor2024.Category.LIGHT, 12),
        // Medium — base + DEX (max +2)
        Armor2024("hide", "Hide Armor", Armor2024.Category.MEDIUM, 12),
        Armor2024("chain-shirt", "Chain Shirt", Armor2024.Category.MEDIUM, 13),
        Armor2024("scale-mail", "Scale Mail", Armor2024.Category.MEDIUM, 14),
        Armor2024("breastplate", "Breastplate", Armor2024.Category.MEDIUM, 14),
        Armor2024("half-plate", "Half Plate Armor", Armor2024.Category.MEDIUM, 15),
        // Heavy — base only
        Armor2024("ring-mail", "Ring Mail", Armor2024.Category.HEAVY, 14),
        Armor2024("chain-mail", "Chain Mail", Armor2024.Category.HEAVY, 16),
        Armor2024("splint", "Splint Armor", Armor2024.Category.HEAVY, 17),
        Armor2024("plate", "Plate Armor", Armor2024.Category.HEAVY, 18),
    )

    fun armor(id: String): Armor2024? = all.firstOrNull { it.id == id }

    /** The shield's flat AC bonus. */
    const val SHIELD_BONUS: Int = 2
}

package au.com.evonet.nat20.pf2e

import kotlinx.serialization.Serializable

/**
 * Pathfinder 2e equipment models + catalogues (A22). Defense and weapon
 * proficiency are tracked **per category** (a Fighter is Expert in martial
 * weapons at level 1; a Wizard only Trained in unarmored), and the worn item's
 * category selects which rank feeds AC / the Strike. Mirrors the iOS `Armor`,
 * `Weapon`, `Shield` types, trimmed to the stats that drive the sheet. ORC content.
 */

@Serializable
enum class ArmorCategory(val displayName: String) {
    UNARMORED("Unarmored"), LIGHT("Light"), MEDIUM("Medium"), HEAVY("Heavy"),
}

@Serializable
enum class WeaponCategory(val displayName: String) { SIMPLE("Simple"), MARTIAL("Martial") }

/** Worn armor — its **item bonus** (acBonus) and **Dex cap** drive AC. */
data class PfArmor(
    val id: String, val name: String, val category: ArmorCategory, val acBonus: Int, val dexCap: Int, val summary: String = "",
    /** Price in gp + carried Bulk — drive the creation wizard's Load & Cost advisory. */
    val priceGp: Double = 0.0, val bulk: Double = 1.0,
)

/** A held shield — its raised circumstance bonus to AC + hardness/HP. */
data class PfShield(
    val id: String, val name: String, val raisedAcBonus: Int, val hardness: Int, val hp: Int,
    val priceGp: Double = 0.0, val bulk: Double = 1.0,
)

/** A weapon — the stats that drive the static Strike: damage die/type, finesse, ranged. */
data class PfWeapon(
    val id: String,
    val name: String,
    val category: WeaponCategory,
    val damageDie: String,
    val damageType: String,
    val finesse: Boolean = false,
    val ranged: Boolean = false,
    val traits: List<String> = emptyList(),
    /** Price in gp (PF2e prices are decimal — 2 sp = 0.2 gp) + carried Bulk. */
    val priceGp: Double = 0.0,
    val bulk: Double = Bulk.LIGHT,
) {
    /** Agile weapons take a reduced −4/−8 multiple-attack penalty instead of −5/−10. */
    val agile: Boolean get() = traits.any { it.equals("agile", ignoreCase = true) }
}

object PfArmors {
    val all: List<PfArmor> = listOf(
        PfArmor("leather", "Leather", ArmorCategory.LIGHT, 1, 4, "A light layer that barely slows you.", priceGp = 2.0, bulk = 1.0),
        PfArmor("studded-leather", "Studded Leather", ArmorCategory.LIGHT, 2, 3, "Leather reinforced with metal studs.", priceGp = 3.0, bulk = 1.0),
        PfArmor("chain-shirt", "Chain Shirt", ArmorCategory.LIGHT, 2, 3, "Interlocking rings worn under clothing.", priceGp = 5.0, bulk = 1.0),
        PfArmor("hide", "Hide", ArmorCategory.MEDIUM, 3, 2, "Thick layers of cured hide and fur.", priceGp = 2.0, bulk = 2.0),
        PfArmor("scale-mail", "Scale Mail", ArmorCategory.MEDIUM, 3, 2, "Overlapping metal scales.", priceGp = 4.0, bulk = 2.0),
        PfArmor("breastplate", "Breastplate", ArmorCategory.MEDIUM, 4, 1, "A metal chest plate over leather.", priceGp = 8.0, bulk = 2.0),
        PfArmor("chain-mail", "Chain Mail", ArmorCategory.HEAVY, 4, 1, "A full suit of interlocking rings.", priceGp = 6.0, bulk = 2.0),
        PfArmor("half-plate", "Half Plate", ArmorCategory.HEAVY, 5, 1, "Plate protecting torso and limbs.", priceGp = 18.0, bulk = 3.0),
        PfArmor("full-plate", "Full Plate", ArmorCategory.HEAVY, 6, 0, "A complete suit of articulated plate.", priceGp = 30.0, bulk = 4.0),
    )
    fun by(id: String): PfArmor? = all.firstOrNull { it.id == id }
}

object PfShields {
    val all: List<PfShield> = listOf(
        PfShield("buckler", "Buckler", 1, 3, 6, priceGp = 1.0, bulk = Bulk.LIGHT),
        PfShield("wooden-shield", "Wooden Shield", 2, 3, 12, priceGp = 1.0, bulk = 1.0),
        PfShield("steel-shield", "Steel Shield", 2, 5, 20, priceGp = 2.0, bulk = 1.0),
        PfShield("tower-shield", "Tower Shield", 2, 5, 20, priceGp = 10.0, bulk = 4.0),
    )
    fun by(id: String): PfShield? = all.firstOrNull { it.id == id }
}

object PfWeapons {
    val all: List<PfWeapon> = listOf(
        // Simple
        PfWeapon("dagger", "Dagger", WeaponCategory.SIMPLE, "1d4", "P", finesse = true, traits = listOf("Agile", "Finesse", "Thrown 10 ft."), priceGp = 0.2, bulk = Bulk.LIGHT),
        PfWeapon("club", "Club", WeaponCategory.SIMPLE, "1d6", "B", traits = listOf("Thrown 10 ft."), priceGp = 0.0, bulk = Bulk.LIGHT),
        PfWeapon("staff", "Staff", WeaponCategory.SIMPLE, "1d4", "B", traits = listOf("Two-Hand 1d8"), priceGp = 0.0, bulk = 1.0),
        PfWeapon("mace", "Mace", WeaponCategory.SIMPLE, "1d6", "B", traits = listOf("Shove"), priceGp = 1.0, bulk = 1.0),
        PfWeapon("spear", "Spear", WeaponCategory.SIMPLE, "1d6", "P", traits = listOf("Thrown 20 ft."), priceGp = 0.1, bulk = 1.0),
        PfWeapon("crossbow", "Crossbow", WeaponCategory.SIMPLE, "1d8", "P", ranged = true, traits = listOf("Range 120 ft.", "Reload 1"), priceGp = 3.0, bulk = 1.0),
        PfWeapon("shortbow", "Shortbow", WeaponCategory.SIMPLE, "1d6", "P", ranged = true, traits = listOf("Deadly d10", "Range 60 ft."), priceGp = 3.0, bulk = 1.0),
        // Martial
        PfWeapon("longsword", "Longsword", WeaponCategory.MARTIAL, "1d8", "S", traits = listOf("Versatile P"), priceGp = 1.0, bulk = 1.0),
        PfWeapon("rapier", "Rapier", WeaponCategory.MARTIAL, "1d6", "P", finesse = true, traits = listOf("Deadly d8", "Disarm", "Finesse"), priceGp = 2.0, bulk = 1.0),
        PfWeapon("shortsword", "Shortsword", WeaponCategory.MARTIAL, "1d6", "P", finesse = true, traits = listOf("Agile", "Finesse", "Versatile S"), priceGp = 0.9, bulk = Bulk.LIGHT),
        PfWeapon("greatsword", "Greatsword", WeaponCategory.MARTIAL, "1d12", "S", traits = listOf("Versatile P"), priceGp = 2.0, bulk = 2.0),
        PfWeapon("greataxe", "Greataxe", WeaponCategory.MARTIAL, "1d12", "S", traits = listOf("Sweep"), priceGp = 2.0, bulk = 2.0),
        PfWeapon("battle-axe", "Battle Axe", WeaponCategory.MARTIAL, "1d8", "S", traits = listOf("Sweep"), priceGp = 1.0, bulk = 1.0),
        PfWeapon("warhammer", "Warhammer", WeaponCategory.MARTIAL, "1d8", "B", traits = listOf("Shove"), priceGp = 1.0, bulk = 1.0),
        PfWeapon("longbow", "Longbow", WeaponCategory.MARTIAL, "1d8", "P", ranged = true, traits = listOf("Deadly d10", "Range 100 ft.", "Volley 30 ft."), priceGp = 6.0, bulk = 2.0),
    )
    fun by(id: String): PfWeapon? = all.firstOrNull { it.id == id }
}

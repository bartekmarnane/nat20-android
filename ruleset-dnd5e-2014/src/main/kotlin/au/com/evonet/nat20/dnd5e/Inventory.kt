package au.com.evonet.nat20.dnd5e

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * The inventory model for the A10 content slice — a flat list of [InventoryItem]
 * on the payload, each carrying an [equipped] flag that drives the AC calculation
 * ([ArmorClassCalculator]) and, later, the attack picker. Port of the iOS
 * `DnD5e/Inventory.swift`, trimmed to the fields this slice needs: the scroll /
 * wondrous-charge / effect-rider properties (save & skill bonuses, resistances)
 * arrive with active effects (A17).
 */

/** What kind of thing an item is — drives grouping, equip rules, and AC. */
@Serializable
enum class ItemKind {
    WEAPON, ARMOR, SHIELD, AMMUNITION, POTION, SCROLL, WONDROUS, TOOL, GEAR, TREASURE;

    /** Whether this kind can be worn/wielded (shows an equip toggle). */
    val isEquippable: Boolean get() = this == WEAPON || this == ARMOR || this == SHIELD || this == WONDROUS
}

/** Weapon-specific data, present only when [InventoryItem.kind] is [ItemKind.WEAPON]. */
@Serializable
data class WeaponProperties(
    val kind: Kind,
    val damageDice: String,
    val damageType: String,
    val properties: List<String> = emptyList(),
    /** Feet — ranged / thrown only. */
    val normalRange: Int? = null,
    val longRange: Int? = null,
) {
    /** Lowercase serial names match the bundled catalogue JSON (`"melee"`). */
    @Serializable
    enum class Kind {
        @SerialName("melee") MELEE,
        @SerialName("ranged") RANGED,
    }
}

/** Armor-specific data, present only when [InventoryItem.kind] is [ItemKind.ARMOR]. */
@Serializable
data class ArmorProperties(
    val kind: Kind,
    val baseAC: Int,
    /**
     * DEX bonus cap under this armor: `null` = uncapped (light), `2` = medium,
     * `0` = heavy (DEX ignored entirely).
     */
    val dexCap: Int? = null,
    val stealthDisadvantage: Boolean = false,
    val strengthRequirement: Int? = null,
) {
    /** Lowercase serial names match the bundled catalogue JSON (`"light"`). */
    @Serializable
    enum class Kind {
        @SerialName("light") LIGHT,
        @SerialName("medium") MEDIUM,
        @SerialName("heavy") HEAVY,
    }

    /**
     * Effective AC for a given DEX modifier under this armor's cap rule:
     * - **Light** (`dexCap == null`): full DEX, positive or negative.
     * - **Medium** (`dexCap == 2`): DEX capped at +2; a negative DEX still applies.
     * - **Heavy** (`dexCap == 0`): DEX ignored entirely.
     */
    fun effectiveAC(dexModifier: Int): Int {
        val dexContribution = when {
            dexCap == 0 -> 0
            dexCap != null -> minOf(dexCap, dexModifier)
            else -> dexModifier
        }
        return baseAC + dexContribution
    }
}

/**
 * A single carried item. Weapons/armor carry their typed sub-properties; mundane
 * gear, potions, and treasure leave them null. [equipped] only matters for
 * equippable kinds. [catalogueID] links back to the bundled SRD entry it came from.
 */
@Serializable
data class InventoryItem(
    val id: String,
    val name: String,
    val kind: ItemKind,
    val weapon: WeaponProperties? = null,
    val armor: ArmorProperties? = null,
    val quantity: Int = 1,
    val equipped: Boolean = false,
    val notes: String = "",
    val catalogueID: String? = null,
    val weight: Double? = null,
    /** Flat AC bonus from an equipped item (shield +2, cloak/ring of protection +1). */
    val acBonus: Int? = null,
) {
    companion object {
        /** Fresh random id for a newly-created item (catalogue `makeItem`, UI add). */
        fun newId(): String = UUID.randomUUID().toString()
    }
}

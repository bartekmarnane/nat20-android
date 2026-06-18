package au.com.evonet.nat20.dnd5e2024

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * The D&D 5e (2024) inventory model. Deliberately a **sibling** of the 2014
 * `InventoryItem`, not a shared `-core` type: the 2024 edition's divergence
 * point is **weapon mastery** (see [Weapon2024]), which the 2014 typed-weapon
 * model has no concept of — so iOS keeps a separate `Inventory2024.swift` and we
 * mirror that here rather than promoting the item model to `-core`.
 *
 * A line item is intentionally lightweight — a name, a quantity, a category for
 * grouping, and an optional note. Worn armor and a shield are tracked directly on
 * [DnD5e2024Payload] (`equippedArmor` / `hasShield`) and resolved against the
 * [Armors2024] catalogue for AC; weapons resolve against [Weapons2024] for
 * attacks + mastery. Port of the iOS `InventoryItem2024`.
 */
@Serializable
data class InventoryItem2024(
    val id: String,
    val name: String,
    val quantity: Int = 1,
    val kind: ItemKind2024 = ItemKind2024.GEAR,
    val note: String = "",
    /** Links back to the bundled catalogue entry it was added from (weapon/armor id). */
    val catalogueID: String? = null,
) {
    companion object {
        /** Fresh random id for a newly-created pack item. */
        fun newId(): String = UUID.randomUUID().toString()
    }
}

/** The five 2024 pack categories — drives grouping in the Items tab. */
@Serializable
enum class ItemKind2024 {
    @SerialName("weapon") WEAPON,
    @SerialName("armor") ARMOR,
    @SerialName("gear") GEAR,
    @SerialName("consumable") CONSUMABLE,
    @SerialName("treasure") TREASURE;

    val displayName: String get() = name.lowercase().replaceFirstChar(Char::uppercase)
}

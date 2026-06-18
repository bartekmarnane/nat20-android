package au.com.evonet.nat20.dnd5e

import kotlinx.serialization.Serializable

/**
 * Catalogue models for the bundled SRD equipment JSON under `Inventory/`, each
 * with a `makeItem()` that mints a fresh [InventoryItem] from the entry. Port of
 * the iOS `WeaponCatalog` / `ArmorCatalog` / gear loaders. The JSON is already
 * camelCase and pre-transformed by the iOS build pipeline, so these decode
 * directly (`ignoreUnknownKeys` drops `cost`-style display extras we don't model).
 */

@Serializable
data class WeaponCatalogueEntry(
    val id: String,
    val name: String,
    /** "simple" / "martial". */
    val category: String = "simple",
    val kind: WeaponProperties.Kind = WeaponProperties.Kind.MELEE,
    val damageDice: String = "",
    val damageType: String = "",
    val properties: List<String> = emptyList(),
    val normalRange: Int? = null,
    val longRange: Int? = null,
    val weight: Double? = null,
    val cost: String? = null,
) {
    fun makeItem(equipped: Boolean = false, quantity: Int = 1): InventoryItem = InventoryItem(
        id = InventoryItem.newId(),
        name = name,
        kind = ItemKind.WEAPON,
        weapon = WeaponProperties(
            kind = kind,
            damageDice = damageDice,
            damageType = damageType,
            properties = properties,
            normalRange = normalRange,
            longRange = longRange,
        ),
        quantity = quantity,
        equipped = equipped,
        catalogueID = id,
        weight = weight,
    )
}

@Serializable
data class ArmorCatalogueEntry(
    val id: String,
    val name: String,
    val kind: ArmorProperties.Kind = ArmorProperties.Kind.LIGHT,
    val baseAC: Int = 10,
    val dexCap: Int? = null,
    val stealthDisadvantage: Boolean = false,
    val strengthRequirement: Int? = null,
    val weight: Double? = null,
    val cost: String? = null,
) {
    fun makeItem(equipped: Boolean = false): InventoryItem = InventoryItem(
        id = InventoryItem.newId(),
        name = name,
        kind = ItemKind.ARMOR,
        armor = ArmorProperties(
            kind = kind,
            baseAC = baseAC,
            dexCap = dexCap,
            stealthDisadvantage = stealthDisadvantage,
            strengthRequirement = strengthRequirement,
        ),
        equipped = equipped,
        catalogueID = id,
        weight = weight,
    )
}

/** Shields, ammunition, potions, tools, and mundane gear share one JSON shape. */
@Serializable
data class GearCatalogueEntry(
    val id: String,
    val name: String,
    /** "shield" / "ammunition" / "potion" / "scroll" / "tool" / "gear". */
    val kind: String = "gear",
    val description: String = "",
    val weight: Double? = null,
    val cost: String? = null,
    /** Shields carry +2; null otherwise. */
    val acBonus: Int? = null,
) {
    /** Maps the gear's string kind to an [ItemKind] (unknown → [ItemKind.GEAR]). */
    val itemKind: ItemKind
        get() = when (kind) {
            "shield" -> ItemKind.SHIELD
            "ammunition" -> ItemKind.AMMUNITION
            "potion" -> ItemKind.POTION
            "scroll" -> ItemKind.SCROLL
            "tool" -> ItemKind.TOOL
            else -> ItemKind.GEAR
        }

    fun makeItem(equipped: Boolean = false, quantity: Int = 1): InventoryItem = InventoryItem(
        id = InventoryItem.newId(),
        name = name,
        kind = itemKind,
        quantity = quantity,
        equipped = equipped,
        notes = description,
        catalogueID = id,
        weight = weight,
        acBonus = acBonus,
    )
}

package au.com.evonet.nat20.dnd5e

/**
 * Seeds the creation wizard's Equipment step (A8): the primary class's
 * `starterEquipment` catalogue lines resolved into [InventoryItem]s, plus the
 * background's free-text gear lines, with the first weapon / armor / shield
 * auto-equipped. Port of the iOS wizard's starter-kit builder.
 */
object StartingEquipment {

    /**
     * Builds the level-1 starting kit for [classId] (+ optional [backgroundId]).
     *
     * - Each class `starterEquipment` id is resolved through the weapon → armor →
     *   gear catalogues (in that order) and minted via `makeItem`.
     * - The first weapon, first armor, and first shield come pre-equipped.
     * - Background `equipment` free-text lines become plain gear items.
     * - If nothing left the class kit wielding a weapon (unknown class, empty
     *   kit), an equipped dagger is added so AC/attack flows have a weapon.
     */
    fun seed(classId: String, backgroundId: String? = null): List<InventoryItem> {
        val items = mutableListOf<InventoryItem>()
        var hasWeapon = false
        var hasArmor = false
        var hasShield = false

        for (entry in DnD5eCatalog.characterClass(classId)?.starterEquipment.orEmpty()) {
            val quantity = maxOf(1, entry.quantity)
            val weapon = DnD5eCatalog.weapon(entry.id)
            val armor = if (weapon == null) DnD5eCatalog.armorPiece(entry.id) else null
            val gear = if (weapon == null && armor == null) DnD5eCatalog.gearPiece(entry.id) else null
            when {
                weapon != null -> {
                    items += weapon.makeItem(equipped = !hasWeapon, quantity = quantity)
                    hasWeapon = true
                }
                armor != null -> {
                    items += armor.makeItem(equipped = !hasArmor)
                    hasArmor = true
                }
                gear != null -> {
                    val equip = gear.itemKind == ItemKind.SHIELD && !hasShield
                    items += gear.makeItem(equipped = equip, quantity = quantity)
                    if (equip) hasShield = true
                }
                // Unresolvable id: skip rather than invent an item (kit stays usable).
            }
        }

        // Background gear is free text on the catalogue — carried as plain gear lines.
        for (line in backgroundId?.let(DnD5eCatalog::background)?.equipment.orEmpty()) {
            items += InventoryItem(id = InventoryItem.newId(), name = line, kind = ItemKind.GEAR)
        }

        if (!hasWeapon) {
            DnD5eCatalog.weapon(FALLBACK_WEAPON_ID)?.let { items += it.makeItem(equipped = true) }
        }
        return items
    }

    private const val FALLBACK_WEAPON_ID = "dagger"
}

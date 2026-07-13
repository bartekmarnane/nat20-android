package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.AcquireItem
import au.com.evonet.nat20.dnd5e.AdjustCoin
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.DropItem
import au.com.evonet.nat20.dnd5e.InventoryItem
import au.com.evonet.nat20.dnd5e.ItemKind
import au.com.evonet.nat20.dnd5e.UseItem
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.Diamond
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The codex **Items** tab, iOS order: coin tiles · INVENTORY head · Add Item
 * capsule · per-kind groups (canonical order) with diamond rows + seal equip
 * toggles · Other Proficiencies. Android keeps its adjust-coins, add/drop/use
 * and equip intents — row tap opens the detail/action dialog (iOS edits via a
 * sheet; pending #22).
 */
@Composable
internal fun ItemsPage(
    character: Character,
    payload: DnD5ePayload,
    onApplyIntent: (CharacterIntent) -> Unit,
    onSave: (Character) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var showAdd by remember { mutableStateOf(false) }
    var coinDialog by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<InventoryItem?>(null) }

    // Equip is a direct (unlogged) payload toggle in both phases — iOS never journals it.
    fun toggleEquip(item: InventoryItem) {
        onSave(character.copy(payload = payload.withEquipToggled(item)))
    }

    CodexPage {
        // Purse: five coin tiles, tap to adjust.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Coin.entries.forEach { coin ->
                CoinTile(coin.abbreviation, payload.coins[coin] ?: 0, Modifier.weight(1f)) { coinDialog = true }
            }
        }

        SectionHead("Inventory", top = 22.dp, bottom = 12.dp)
        CodexCapsuleButton("Add Item", onClick = { showAdd = true })

        if (payload.inventory.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            DashedNotice("Your pack is empty. Add gear from the SRD catalogue.")
        } else {
            KIND_ORDER.forEach { kind ->
                val items = payload.inventory.filter { it.kind == kind }
                if (items.isEmpty()) return@forEach
                KindHeader(kindLabel(kind), items.size)
                items.forEach { item ->
                    ItemRow(
                        item = item,
                        onTap = { detail = item },
                        onToggleEquip = { toggleEquip(item) },
                    )
                }
            }
        }

        val tools = payload.background.takeIf { it.isNotEmpty() }
            ?.let { DnD5eCatalog.background(it)?.toolProficiencies }
            .orEmpty()
        if (tools.isNotEmpty()) {
            SectionHead("Other Proficiencies", top = 22.dp, bottom = 12.dp)
            Text(
                tools.joinToString(" · "),
                fontFamily = EbGaramond,
                fontSize = 13.sp,
                color = palette.inkSoft,
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showAdd) {
        AddItemDialog(
            onPick = { onApplyIntent(AcquireItem(it)); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
    if (coinDialog) {
        CoinDialog(
            onAdjust = { coin, delta -> onApplyIntent(AdjustCoin(coin, delta)); coinDialog = false },
            onDismiss = { coinDialog = false },
        )
    }
    detail?.let { item ->
        ItemDetailDialog(
            item = item,
            onUse = useAction(item) { onApplyIntent(it); detail = null },
            onDrop = { onApplyIntent(DropItem(item.id, item.quantity)); detail = null },
            onToggleEquip = { toggleEquip(item); detail = null },
            onDismiss = { detail = null },
        )
    }
}

/** iOS canonical inventory-group order. */
private val KIND_ORDER = listOf(
    ItemKind.WEAPON, ItemKind.ARMOR, ItemKind.SHIELD, ItemKind.AMMUNITION,
    ItemKind.POTION, ItemKind.SCROLL, ItemKind.WONDROUS, ItemKind.TOOL,
    ItemKind.GEAR, ItemKind.TREASURE,
)

private fun kindLabel(kind: ItemKind): String = when (kind) {
    ItemKind.WEAPON -> "Weapons"
    ItemKind.ARMOR -> "Armor"
    ItemKind.SHIELD -> "Shields"
    ItemKind.AMMUNITION -> "Ammunition"
    ItemKind.POTION -> "Potions"
    ItemKind.SCROLL -> "Scrolls"
    ItemKind.WONDROUS -> "Wondrous Items"
    ItemKind.TOOL -> "Tools"
    ItemKind.GEAR -> "Gear"
    ItemKind.TREASURE -> "Treasure"
}

@Composable
private fun KindHeader(name: String, count: Int) {
    Text(
        name.uppercase() + if (count > 1) " ($count)" else "",
        fontFamily = Cinzel,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 2.5.sp,
        color = MaterialTheme.natPalette.accent,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

/** Use callback if the item is a consumable that should offer "Use"; null otherwise. */
private fun useAction(item: InventoryItem, apply: (CharacterIntent) -> Unit): (() -> Unit)? =
    if (item.kind == ItemKind.POTION || item.kind == ItemKind.SCROLL) {
        { apply(UseItem(item.id, healingRolled = healingFor(item))) }
    } else {
        null
    }

/** A flat 7 HP for a Potion of Healing (2d4+2 average); null for anything else. */
private fun healingFor(item: InventoryItem): Int? =
    if (item.catalogueID == "potion-of-healing") 7 else null

/** Inventory row: diamond · name ×qty · detail line · seal equip toggle. */
@Composable
private fun ItemRow(item: InventoryItem, onTap: () -> Unit, onToggleEquip: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (item.equipped) Modifier.background(palette.accent.copy(alpha = 0.06f)) else Modifier)
            .clickable(onClick = onTap)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Diamond(size = 5.dp, fill = palette.accent)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    fontFamily = Cormorant,
                    fontSize = 15.sp,
                    color = palette.ink,
                    maxLines = 1,
                )
                if (item.quantity > 1) {
                    Text(
                        " × ${item.quantity}",
                        fontFamily = ImFell,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp,
                        color = palette.inkMute,
                    )
                }
            }
            itemDetail(item)?.let {
                Text(
                    it,
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = 11.sp,
                    color = palette.inkMute,
                    maxLines = 1,
                )
            }
        }
        if (item.kind.isEquippable) {
            SealToggle(item.equipped, onToggleEquip)
        }
    }
}

/** Seal-check equip toggle: accent seal when worn, hairline circle when stowed. */
@Composable
private fun SealToggle(equipped: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (equipped) Modifier.background(palette.accent)
                else Modifier.border(1.dp, palette.inkMute, CircleShape),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (equipped) {
            Text("✓", fontSize = 12.sp, color = palette.cream)
        }
    }
}

/** A short descriptor under an item's name (damage for weapons, AC for armor). */
private fun itemDetail(item: InventoryItem): String? = when {
    item.weapon != null -> "${item.weapon!!.damageDice} ${item.weapon!!.damageType}"
    item.armor != null -> "AC ${item.armor!!.baseAC}${item.armor!!.dexCap?.let { if (it == 0) "" else " + DEX (max $it)" } ?: " + DEX"}"
    item.acBonus != null -> "+${item.acBonus} AC"
    else -> null
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

/** Row-tap detail + actions: equip/unequip, use (consumables), drop. */
@Composable
private fun ItemDetailDialog(
    item: InventoryItem,
    onUse: (() -> Unit)?,
    onDrop: () -> Unit,
    onToggleEquip: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(kindLabel(item.kind), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                itemDetail(item)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (item.quantity > 1) Text("Quantity: ${item.quantity}", style = MaterialTheme.typography.bodyMedium)
                if (item.notes.isNotBlank()) Text(item.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Row {
                if (item.kind.isEquippable) {
                    TextButton(onClick = onToggleEquip) { Text(if (item.equipped) "Unequip" else "Equip") }
                }
                onUse?.let { TextButton(onClick = it) { Text("Use") } }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDrop) { Text("Drop", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

private data class CatalogueChoice(val name: String, val category: String, val make: () -> InventoryItem)

@Composable
private fun AddItemDialog(onPick: (InventoryItem) -> Unit, onDismiss: () -> Unit) {
    val choices = remember {
        buildList {
            DnD5eCatalog.weapons.forEach { w -> add(CatalogueChoice(w.name, "Weapon") { w.makeItem() }) }
            DnD5eCatalog.armor.forEach { a -> add(CatalogueChoice(a.name, "Armor") { a.makeItem() }) }
            DnD5eCatalog.gear.forEach { g -> add(CatalogueChoice(g.name, "Gear") { g.makeItem() }) }
        }.sortedBy { it.name }
    }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) choices else choices.filter { it.name.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(filtered, key = { it.name }) { choice ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(choice.make()) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(choice.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                choice.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun CoinDialog(onAdjust: (Coin, Int) -> Unit, onDismiss: () -> Unit) {
    var coin by remember { mutableStateOf(Coin.GP) }
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust coins") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Coin.entries.forEach { c ->
                        FilterChip(selected = coin == c, onClick = { coin = c }, label = { Text(c.abbreviation) })
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit) },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = amount > 0, onClick = { onAdjust(coin, amount) }) { Text("Gain") }
        },
        dismissButton = {
            Row {
                TextButton(enabled = amount > 0, onClick = { onAdjust(coin, -amount) }) { Text("Spend") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

// ── Equip toggle (direct payload edit) ────────────────────────────────────────

/**
 * Flips [target]'s equipped flag. Equipping armor unequips other armor, and
 * equipping a shield unequips other shields — you benefit from only one of each.
 * Weapons (and wondrous items) equip independently.
 */
private fun DnD5ePayload.withEquipToggled(target: InventoryItem): DnD5ePayload {
    val nowEquipped = !target.equipped
    val exclusiveKind = nowEquipped && (target.kind == ItemKind.ARMOR || target.kind == ItemKind.SHIELD)
    val updated = inventory.map { item ->
        when {
            item.id == target.id -> item.copy(equipped = nowEquipped)
            exclusiveKind && item.kind == target.kind && item.equipped -> item.copy(equipped = false)
            else -> item
        }
    }
    return copy(inventory = updated)
}

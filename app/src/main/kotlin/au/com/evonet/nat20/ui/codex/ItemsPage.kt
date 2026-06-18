package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

/**
 * The codex **Items** tab (A10): coins, equipped gear, and the full inventory
 * grouped by kind. Editable in place — add from the SRD catalogues, equip /
 * unequip, drop, use a consumable, and adjust the purse. Add / drop / use / coin
 * reuse the tested 5e intents; equip is a direct payload toggle (mirrors the iOS
 * build-phase Items view, which never journals an equip change).
 */
@Composable
internal fun ItemsPage(
    character: Character,
    payload: DnD5ePayload,
    onApplyIntent: (CharacterIntent) -> Unit,
    onSave: (Character) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var coinDialog by remember { mutableStateOf(false) }

    fun applyIntent(intent: CharacterIntent) = onApplyIntent(intent)

    // Equip is a direct (unlogged) payload toggle in both phases — iOS never journals it.
    fun toggleEquip(item: InventoryItem) {
        onSave(character.copy(payload = payload.withEquipToggled(item)))
    }

    CodexPage {
        SectionCard("Coins") {
            val present = Coin.entries.filter { (payload.coins[it] ?: 0) > 0 }
            if (present.isEmpty()) {
                Text(
                    "No coins yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    present.forEach { coin ->
                        AssistChip(onClick = { coinDialog = true }, label = {
                            Text("${payload.coins[coin]} ${coin.abbreviation}")
                        })
                    }
                }
            }
            OutlinedButton(onClick = { coinDialog = true }) { Text("Adjust coins") }
        }

        val equipped = payload.inventory.filter { it.equipped }
        if (equipped.isNotEmpty()) {
            SectionCard("Equipped") {
                equipped.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ItemRow(item, onToggleEquip = { toggleEquip(item) }, onDrop = { applyIntent(DropItem(item.id, item.quantity)) }, onUse = useAction(item) { applyIntent(it) })
                }
            }
        }

        ItemGroup("Weapons", payload.inventory.filter { it.kind == ItemKind.WEAPON }, ::toggleEquip) { applyIntent(it) }
        ItemGroup("Armor & Shields", payload.inventory.filter { it.kind == ItemKind.ARMOR || it.kind == ItemKind.SHIELD }, ::toggleEquip) { applyIntent(it) }
        ItemGroup("Consumables", payload.inventory.filter { it.kind == ItemKind.POTION || it.kind == ItemKind.SCROLL }, ::toggleEquip) { applyIntent(it) }
        ItemGroup("Gear", payload.inventory.filter { it.kind in GEAR_KINDS }, ::toggleEquip) { applyIntent(it) }

        if (payload.inventory.isEmpty()) {
            Text(
                "Your pack is empty. Add gear from the SRD catalogue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add item")
        }
    }

    if (showAdd) {
        AddItemDialog(
            onPick = { applyIntent(AcquireItem(it)); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
    if (coinDialog) {
        CoinDialog(
            onAdjust = { coin, delta -> applyIntent(AdjustCoin(coin, delta)); coinDialog = false },
            onDismiss = { coinDialog = false },
        )
    }
}

private val GEAR_KINDS = setOf(ItemKind.GEAR, ItemKind.TOOL, ItemKind.AMMUNITION, ItemKind.WONDROUS, ItemKind.TREASURE)

/** Drop callback if the item is a consumable that should offer "Use"; null otherwise. */
private fun useAction(item: InventoryItem, apply: (CharacterIntent) -> Unit): (() -> Unit)? =
    if (item.kind == ItemKind.POTION || item.kind == ItemKind.SCROLL) {
        { apply(UseItem(item.id, healingRolled = healingFor(item))) }
    } else {
        null
    }

/** A flat 7 HP for a Potion of Healing (2d4+2 average); null for anything else. */
private fun healingFor(item: InventoryItem): Int? =
    if (item.catalogueID == "potion-of-healing") 7 else null

@Composable
private fun ItemGroup(
    title: String,
    items: List<InventoryItem>,
    onToggleEquip: (InventoryItem) -> Unit,
    onApply: (CharacterIntent) -> Unit,
) {
    if (items.isEmpty()) return
    SectionCard(title) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ItemRow(
                item = item,
                onToggleEquip = { onToggleEquip(item) },
                onDrop = { onApply(DropItem(item.id, item.quantity)) },
                onUse = useAction(item, onApply),
            )
        }
    }
}

@Composable
private fun ItemRow(
    item: InventoryItem,
    onToggleEquip: () -> Unit,
    onDrop: () -> Unit,
    onUse: (() -> Unit)?,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (item.kind.isEquippable) {
            Checkbox(checked = item.equipped, onCheckedChange = { onToggleEquip() })
        }
        Column(Modifier.weight(1f)) {
            Text(
                if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            itemDetail(item)?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        onUse?.let { TextButton(onClick = it) { Text("Use") } }
        TextButton(onClick = onDrop) { Text("Drop") }
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

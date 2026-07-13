package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import au.com.evonet.nat20.dnd5e.ArmorProperties
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.InventoryItem
import au.com.evonet.nat20.dnd5e.ItemKind
import au.com.evonet.nat20.dnd5e.ScrollProperties
import au.com.evonet.nat20.dnd5e.Spell
import au.com.evonet.nat20.dnd5e.WeaponProperties
import au.com.evonet.nat20.dnd5e.WondrousProperties
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.ui.editor.WizardChip
import au.com.evonet.nat20.ui.editor.WizardFieldLabel
import au.com.evonet.nat20.ui.editor.WizardPrimaryButton
import au.com.evonet.nat20.ui.editor.WizardSecondaryButton
import au.com.evonet.nat20.ui.editor.WizardSegmented
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.Diamond
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The 2014 codex **Add Item** / **Edit Item** sheets and the **Scroll Spell** picker
 * (parity #22), ported from iOS `AddItemSheet` / `EditItemSheet` / `ScrollSpellPicker`.
 * Both sheets are full-screen [Dialog]s on the parchment chrome (the `CustomRaceForm`
 * pattern) reusing the Wizard atoms + codex tokens.
 *
 * Divergence from iOS: the ScrollSpellPicker uses the **full** [DnD5eCatalog.spellLibrary]
 * — there is no `spells(in: sources)` accessor on the Android catalogue yet (payload
 * `enabledSources` is data-only), so the enabled-sources gate is deferred (PARITY #22).
 */

// ── Shared chrome ──────────────────────────────────────────────────────────────

/** CANCEL · centred two-line title · balancing spacer — the sheet header band. */
@Composable
private fun SheetHeader(overline: String, title: String, onCancel: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 4.dp),
    ) {
        Text(
            "CANCEL",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.ink,
            modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onCancel),
        )
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                overline.uppercase(),
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                color = palette.inkMute,
            )
            Text(
                title,
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                fontSize = 20.sp,
                color = palette.accent,
                maxLines = 1,
            )
        }
    }
}

/** Parchment search well: magnifier · italic Cormorant field · clear ×. */
@Composable
private fun SheetSearchField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tileStrong)
            .border(1.dp, palette.ink.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = palette.inkMute, modifier = Modifier.size(16.dp))
        Box(Modifier.weight(1f)) {
            WizardTextField(placeholder, value, onValueChange)
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Clear search",
                tint = palette.inkMute,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable(onClick = onClear).padding(2.dp),
            )
        }
    }
}

/** Small-caps accent section label used inside the custom card. */
@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontFamily = Cinzel,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 2.5.sp,
        color = MaterialTheme.natPalette.accent,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** The accent-outlined preview / custom card (accent@.06 fill, accent@.33 1.4dp stroke). */
@Composable
private fun SheetCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(palette.accent.copy(alpha = 0.06f), Color.Transparent)))
            .border(1.4.dp, palette.accent.copy(alpha = 0.33f), RoundedCornerShape(4.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/** −/+ quantity stepper row (Cormorant 20sp accent value). */
@Composable
private fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "QUANTITY",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.inkMute,
        )
        Spacer(Modifier.weight(1f))
        MiniStep("−", enabled = quantity > 1) { onChange(quantity - 1) }
        Text(
            "$quantity",
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = palette.accent,
            modifier = Modifier.widthIn(min = 36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        MiniStep("+", enabled = true) { onChange(quantity + 1) }
    }
}

/** A 30dp capsule −/+ button used by the stepper rows. */
@Composable
private fun MiniStep(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(palette.tileStrong)
            .border(1.dp, palette.accent.copy(alpha = if (enabled) 0.33f else 0.15f), CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = if (enabled) palette.ink else palette.inkMute,
        )
    }
}

/** Accent capsule primary CTA (dims + disables when [enabled] is false). */
@Composable
private fun SheetPrimaryCta(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(palette.accent.copy(alpha = if (enabled) 1f else 0.4f))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            color = palette.cream,
        )
    }
}

private fun String.parsedBonus(): Int? = trim().toIntOrNull()?.takeIf { it != 0 }
private fun parseCommaList(raw: String): List<String> =
    raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
private fun Int.signedLabel(): String = if (this >= 0) "+$this" else "$this"

/** The 13 canonical 5e damage types; anything else is homebrew "Other…". */
private val CANONICAL_DAMAGE_TYPES = listOf(
    "bludgeoning", "piercing", "slashing",
    "acid", "cold", "fire", "force",
    "lightning", "necrotic", "poison",
    "psychic", "radiant", "thunder",
)

// ── Add Item sheet ─────────────────────────────────────────────────────────────

private sealed interface AddSelection {
    data class Weapon(val entry: au.com.evonet.nat20.dnd5e.WeaponCatalogueEntry) : AddSelection
    data class Armor(val entry: au.com.evonet.nat20.dnd5e.ArmorCatalogueEntry) : AddSelection
    data class Gear(val entry: au.com.evonet.nat20.dnd5e.GearCatalogueEntry) : AddSelection
    data object Custom : AddSelection
}

/**
 * The Add-to-Inventory sheet: a search field over the SRD weapon/armor/gear
 * catalogues drives a 4-state flow — empty hint · match list · catalogue preview
 * (with a quantity stepper) · custom homebrew form (kind-gated sub-forms). Commit
 * mints an [InventoryItem] and hands it to [onAdd] (the caller dispatches AcquireItem).
 */
@Composable
internal fun AddItemSheet(onAdd: (InventoryItem) -> Unit, onDismiss: () -> Unit) {
    val palette = MaterialTheme.natPalette

    var searchText by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf<AddSelection?>(null) }
    var customKind by remember { mutableStateOf(ItemKind.GEAR) }
    var customName by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }
    var acBonusText by remember { mutableStateOf("") }
    var saveBonusText by remember { mutableStateOf("") }
    var attackBonusText by remember { mutableStateOf("") }

    // Weapon custom fields
    var weaponKindIndex by remember { mutableIntStateOf(0) } // 0 melee, 1 ranged
    var weaponDamageDice by remember { mutableStateOf("1d8") }
    var weaponDamageType by remember { mutableStateOf("slashing") }
    var weaponDamageTypeCustom by remember { mutableStateOf(false) }
    var weaponPropertiesText by remember { mutableStateOf("") }
    var weaponNormalRange by remember { mutableStateOf("") }
    var weaponLongRange by remember { mutableStateOf("") }

    // Armor custom fields
    var armorKindIndex by remember { mutableIntStateOf(0) } // 0 light, 1 medium, 2 heavy
    var armorBaseAC by remember { mutableStateOf("11") }
    var armorDexCap by remember { mutableStateOf("") }
    var armorStealth by remember { mutableStateOf(false) }
    var armorStrengthReq by remember { mutableStateOf("") }

    // Scroll + wondrous custom fields
    var scrollSelection by remember { mutableStateOf<ScrollProperties?>(null) }
    var showScrollPicker by remember { mutableStateOf(false) }
    var wondrousCurrent by remember { mutableStateOf("") }
    var wondrousMax by remember { mutableStateOf("") }
    var wondrousRecharge by remember { mutableStateOf("1d6+1") }

    val query = searchText.trim().lowercase()
    val filteredWeapons = if (query.isEmpty()) emptyList() else DnD5eCatalog.weapons.filter { it.name.lowercase().contains(query) }
    val filteredArmor = if (query.isEmpty()) emptyList() else DnD5eCatalog.armor.filter { it.name.lowercase().contains(query) }
    val filteredGear = if (query.isEmpty()) emptyList() else DnD5eCatalog.gear.filter { it.name.lowercase().contains(query) }
    val matchCount = filteredWeapons.size + filteredArmor.size + filteredGear.size

    val showsAcBonus = customKind == ItemKind.SHIELD || customKind == ItemKind.WONDROUS || customKind == ItemKind.ARMOR
    val showsSaveBonus = customKind == ItemKind.WONDROUS || customKind == ItemKind.ARMOR || customKind == ItemKind.SHIELD
    val showsAttackBonus = customKind == ItemKind.WEAPON

    fun onSearchChange(newValue: String) {
        searchText = newValue
        if (newValue.isNotEmpty() && selection != null) selection = null
        customName = newValue
        quantity = 1
    }

    fun selectKind(kind: ItemKind) {
        customKind = kind
        if (kind != ItemKind.SCROLL) scrollSelection = null
        if (kind != ItemKind.WONDROUS) {
            wondrousCurrent = ""; wondrousMax = ""; wondrousRecharge = "1d6+1"
        }
    }

    fun commitCustom() {
        val name = customName.trim()
        if (name.isEmpty()) return
        val weapon = if (customKind == ItemKind.WEAPON) {
            val ranged = weaponKindIndex == 1
            WeaponProperties(
                kind = if (ranged) WeaponProperties.Kind.RANGED else WeaponProperties.Kind.MELEE,
                damageDice = weaponDamageDice.trim().ifEmpty { "1d4" },
                damageType = weaponDamageType.trim().ifEmpty { "bludgeoning" },
                properties = parseCommaList(weaponPropertiesText),
                normalRange = if (ranged) weaponNormalRange.trim().toIntOrNull() else null,
                longRange = if (ranged) weaponLongRange.trim().toIntOrNull() else null,
            )
        } else {
            null
        }
        val armor = if (customKind == ItemKind.ARMOR) {
            ArmorProperties(
                kind = when (armorKindIndex) {
                    1 -> ArmorProperties.Kind.MEDIUM
                    2 -> ArmorProperties.Kind.HEAVY
                    else -> ArmorProperties.Kind.LIGHT
                },
                baseAC = armorBaseAC.trim().toIntOrNull() ?: 10,
                dexCap = armorDexCap.trim().takeIf { it.isNotEmpty() }?.toIntOrNull(),
                stealthDisadvantage = armorStealth,
                strengthRequirement = armorStrengthReq.trim().takeIf { it.isNotEmpty() }?.toIntOrNull(),
            )
        } else {
            null
        }
        val wondrous = if (customKind == ItemKind.WONDROUS) {
            val max = wondrousMax.trim().toIntOrNull() ?: 0
            if (max > 0) {
                WondrousProperties(
                    currentCharges = wondrousCurrent.trim().toIntOrNull() ?: max,
                    maxCharges = max,
                    rechargeDice = wondrousRecharge.trim(),
                ).normalized
            } else {
                null
            }
        } else {
            null
        }
        onAdd(
            InventoryItem(
                id = InventoryItem.newId(),
                name = name,
                kind = customKind,
                weapon = weapon,
                armor = armor,
                scroll = if (customKind == ItemKind.SCROLL) scrollSelection else null,
                wondrous = wondrous,
                quantity = quantity,
                notes = notes.trim(),
                acBonus = if (showsAcBonus) acBonusText.parsedBonus() else null,
                saveBonus = if (showsSaveBonus) saveBonusText.parsedBonus() else null,
                attackBonus = if (showsAttackBonus) attackBonusText.parsedBonus() else null,
            ),
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(palette.parchment).statusBarsPadding()) {
            SheetHeader("Add to Inventory", "What seeks the pack?", onDismiss)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 22.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SheetSearchField(searchText, "Longsword, Plate, Healing Potion, Rope…", ::onSearchChange) {
                    searchText = ""; selection = null; customName = ""
                }

                when {
                    selection != null -> when (val sel = selection) {
                        is AddSelection.Weapon -> CataloguePreview(
                            kindLabel = "${sel.entry.category.replaceFirstChar { it.uppercase() }} Weapon",
                            name = sel.entry.name,
                            summary = weaponSummary(sel.entry.damageDice, sel.entry.damageType, sel.entry.properties, sel.entry.normalRange, sel.entry.longRange),
                            quantity = quantity,
                            onQuantity = { quantity = it },
                            onChange = { selection = null },
                            onCommit = { onAdd(sel.entry.makeItem(equipped = false, quantity = quantity)) },
                        )
                        is AddSelection.Armor -> CataloguePreview(
                            kindLabel = "${sel.entry.kind.name.lowercase().replaceFirstChar { it.uppercase() }} Armor",
                            name = sel.entry.name,
                            summary = armorSummary(sel.entry.baseAC, sel.entry.dexCap, sel.entry.stealthDisadvantage, sel.entry.strengthRequirement),
                            quantity = quantity,
                            onQuantity = { quantity = it },
                            onChange = { selection = null },
                            onCommit = { onAdd(sel.entry.makeItem(equipped = false)) },
                        )
                        is AddSelection.Gear -> CataloguePreview(
                            kindLabel = sel.entry.itemKind.displayName,
                            name = sel.entry.name,
                            summary = sel.entry.description,
                            quantity = quantity,
                            onQuantity = { quantity = it },
                            onChange = { selection = null },
                            onCommit = { onAdd(sel.entry.makeItem(quantity = quantity)) },
                        )
                        AddSelection.Custom, null -> CustomCard(
                            customName, { customName = it },
                            customKind, ::selectKind,
                            weaponKindIndex, { weaponKindIndex = it },
                            weaponDamageDice, { weaponDamageDice = it },
                            weaponDamageType, { weaponDamageType = it },
                            weaponDamageTypeCustom, { weaponDamageTypeCustom = it },
                            weaponPropertiesText, { weaponPropertiesText = it },
                            weaponNormalRange, { weaponNormalRange = it },
                            weaponLongRange, { weaponLongRange = it },
                            armorKindIndex, { armorKindIndex = it },
                            armorBaseAC, { armorBaseAC = it },
                            armorDexCap, { armorDexCap = it },
                            armorStealth, { armorStealth = it },
                            armorStrengthReq, { armorStrengthReq = it },
                            scrollSelection, { showScrollPicker = true }, { scrollSelection = null },
                            wondrousCurrent, { wondrousCurrent = it },
                            wondrousMax, { wondrousMax = it },
                            wondrousRecharge, { wondrousRecharge = it },
                            acBonusText, { acBonusText = it }, showsAcBonus,
                            saveBonusText, { saveBonusText = it }, showsSaveBonus,
                            attackBonusText, { attackBonusText = it }, showsAttackBonus,
                            notes, { notes = it },
                            quantity, { quantity = it },
                        ) { commitCustom() }
                    }
                    query.isEmpty() -> EmptyHint()
                    matchCount == 0 -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SheetSectionLabel("No matches — adding as custom")
                        CustomCard(
                            customName, { customName = it },
                            customKind, ::selectKind,
                            weaponKindIndex, { weaponKindIndex = it },
                            weaponDamageDice, { weaponDamageDice = it },
                            weaponDamageType, { weaponDamageType = it },
                            weaponDamageTypeCustom, { weaponDamageTypeCustom = it },
                            weaponPropertiesText, { weaponPropertiesText = it },
                            weaponNormalRange, { weaponNormalRange = it },
                            weaponLongRange, { weaponLongRange = it },
                            armorKindIndex, { armorKindIndex = it },
                            armorBaseAC, { armorBaseAC = it },
                            armorDexCap, { armorDexCap = it },
                            armorStealth, { armorStealth = it },
                            armorStrengthReq, { armorStrengthReq = it },
                            scrollSelection, { showScrollPicker = true }, { scrollSelection = null },
                            wondrousCurrent, { wondrousCurrent = it },
                            wondrousMax, { wondrousMax = it },
                            wondrousRecharge, { wondrousRecharge = it },
                            acBonusText, { acBonusText = it }, showsAcBonus,
                            saveBonusText, { saveBonusText = it }, showsSaveBonus,
                            attackBonusText, { attackBonusText = it }, showsAttackBonus,
                            notes, { notes = it },
                            quantity, { quantity = it },
                        ) { commitCustom() }
                    }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SheetSectionLabel("Matches ($matchCount)")
                        filteredWeapons.forEach { entry ->
                            MatchRow(entry.name, "Weapon · ${entry.damageDice} ${entry.damageType}") { selection = AddSelection.Weapon(entry) }
                        }
                        filteredArmor.forEach { entry ->
                            MatchRow(entry.name, "${entry.kind.name.lowercase().replaceFirstChar { it.uppercase() }} armor · AC ${entry.baseAC}") { selection = AddSelection.Armor(entry) }
                        }
                        filteredGear.forEach { entry ->
                            MatchRow(entry.name, entry.itemKind.displayName) { selection = AddSelection.Gear(entry) }
                        }
                        Text(
                            "NONE OF THESE — ADD AS CUSTOM",
                            fontFamily = Cinzel,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = palette.inkMute,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectKind(ItemKind.GEAR)
                                    customName = searchText
                                    selection = AddSelection.Custom
                                }
                                .padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showScrollPicker) {
        ScrollSpellPicker(
            onPick = { spell -> scrollSelection = ScrollProperties(spell.index, spell.name, spell.level); showScrollPicker = false },
            onDismiss = { showScrollPicker = false },
        )
    }
}

@Composable
private fun EmptyHint() {
    val palette = MaterialTheme.natPalette
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
        SheetSectionLabelMute("Start typing")
        Text(
            "Search across SRD weapons, armor, and gear. If nothing matches, you can add a homebrew item by the same name.",
            fontFamily = EbGaramond,
            fontSize = 13.sp,
            color = palette.inkSoft,
        )
    }
}

@Composable
private fun SheetSectionLabelMute(text: String) {
    Text(
        text.uppercase(),
        fontFamily = Cinzel,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 2.5.sp,
        color = MaterialTheme.natPalette.inkMute,
    )
}

@Composable
private fun MatchRow(label: String, sublabel: String, onTap: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(3.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Diamond(size = 5.dp, fill = palette.accent)
        Column(Modifier.weight(1f)) {
            Text(label, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.ink, maxLines = 1)
            Text(sublabel, fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 11.sp, color = palette.inkMute, maxLines = 1)
        }
        Text("›", fontFamily = Cormorant, fontSize = 18.sp, color = palette.inkMute)
    }
}

@Composable
private fun CataloguePreview(
    kindLabel: String,
    name: String,
    summary: String,
    quantity: Int,
    onQuantity: (Int) -> Unit,
    onChange: () -> Unit,
    onCommit: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    SheetCard {
        Text(kindLabel.uppercase(), fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.accent)
        Text(name, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic, fontSize = 20.sp, color = palette.ink)
        if (summary.isNotEmpty()) {
            Text(summary, fontFamily = EbGaramond, fontSize = 13.sp, color = palette.inkSoft)
        }
        QuantityStepper(quantity, onQuantity)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "CHANGE",
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = palette.inkMute,
                modifier = Modifier.clickable(onClick = onChange),
            )
            Spacer(Modifier.weight(1f))
            Box(Modifier.widthIn(min = 160.dp)) {
                SheetPrimaryCta("Add to Inventory", onClick = onCommit)
            }
        }
    }
}

private fun weaponSummary(dice: String, type: String, properties: List<String>, normal: Int?, long: Int?): String {
    val bits = mutableListOf("$dice $type")
    if (properties.isNotEmpty()) bits.add(properties.joinToString(" · "))
    if (normal != null && long != null) bits.add("range $normal/$long ft")
    return bits.joinToString(" — ")
}

private fun armorSummary(baseAC: Int, dexCap: Int?, stealth: Boolean, strReq: Int?): String {
    val bits = mutableListOf("Base AC $baseAC")
    bits.add(
        when {
            dexCap == null -> "DEX uncapped"
            dexCap == 0 -> "DEX ignored"
            else -> "DEX cap +$dexCap"
        },
    )
    if (stealth) bits.add("stealth disadvantage")
    if (strReq != null) bits.add("STR $strReq+")
    return bits.joinToString(" · ")
}

// The custom-item card carries a lot of state; it's threaded explicitly rather than
// bundled so the parent's `remember`s stay the single source of truth across the flow.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomCard(
    name: String, onName: (String) -> Unit,
    kind: ItemKind, onKind: (ItemKind) -> Unit,
    weaponKindIndex: Int, onWeaponKind: (Int) -> Unit,
    weaponDamageDice: String, onWeaponDamageDice: (String) -> Unit,
    weaponDamageType: String, onWeaponDamageType: (String) -> Unit,
    weaponDamageTypeCustom: Boolean, onWeaponDamageTypeCustom: (Boolean) -> Unit,
    weaponPropertiesText: String, onWeaponProperties: (String) -> Unit,
    weaponNormalRange: String, onWeaponNormalRange: (String) -> Unit,
    weaponLongRange: String, onWeaponLongRange: (String) -> Unit,
    armorKindIndex: Int, onArmorKind: (Int) -> Unit,
    armorBaseAC: String, onArmorBaseAC: (String) -> Unit,
    armorDexCap: String, onArmorDexCap: (String) -> Unit,
    armorStealth: Boolean, onArmorStealth: (Boolean) -> Unit,
    armorStrengthReq: String, onArmorStrengthReq: (String) -> Unit,
    scrollSelection: ScrollProperties?, onChooseScroll: () -> Unit, onClearScroll: () -> Unit,
    wondrousCurrent: String, onWondrousCurrent: (String) -> Unit,
    wondrousMax: String, onWondrousMax: (String) -> Unit,
    wondrousRecharge: String, onWondrousRecharge: (String) -> Unit,
    acBonusText: String, onAcBonus: (String) -> Unit, showsAcBonus: Boolean,
    saveBonusText: String, onSaveBonus: (String) -> Unit, showsSaveBonus: Boolean,
    attackBonusText: String, onAttackBonus: (String) -> Unit, showsAttackBonus: Boolean,
    notes: String, onNotes: (String) -> Unit,
    quantity: Int, onQuantity: (Int) -> Unit,
    onCommit: () -> Unit,
) {
    SheetCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardFieldLabel("Name", required = true)
            WizardTextField("Item name", name, onName)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardFieldLabel("Kind")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                ItemKind.entries.forEach { k ->
                    WizardChip(label = k.displayName, selected = kind == k) { onKind(k) }
                }
            }
        }

        when (kind) {
            ItemKind.WEAPON -> WeaponCustomFields(
                weaponKindIndex, onWeaponKind,
                weaponDamageDice, onWeaponDamageDice,
                weaponDamageType, onWeaponDamageType,
                weaponDamageTypeCustom, onWeaponDamageTypeCustom,
                weaponPropertiesText, onWeaponProperties,
                weaponNormalRange, onWeaponNormalRange,
                weaponLongRange, onWeaponLongRange,
            )
            ItemKind.ARMOR -> ArmorCustomFields(
                armorKindIndex, onArmorKind,
                armorBaseAC, onArmorBaseAC,
                armorDexCap, onArmorDexCap,
                armorStealth, onArmorStealth,
                armorStrengthReq, onArmorStrengthReq,
            )
            ItemKind.SCROLL -> ScrollCustomFields(scrollSelection, onChooseScroll, onClearScroll)
            ItemKind.WONDROUS -> WondrousCustomFields(
                wondrousCurrent, onWondrousCurrent,
                wondrousMax, onWondrousMax,
                wondrousRecharge, onWondrousRecharge,
            )
            else -> Unit
        }

        if (showsAcBonus) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WizardFieldLabel("AC bonus", hint = acBonusHint(kind))
                WizardTextField(if (kind == ItemKind.SHIELD) "2" else "0", acBonusText, onAcBonus)
            }
        }
        if (showsSaveBonus) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WizardFieldLabel("Save bonus", hint = "applies to all saves")
                WizardTextField("0", saveBonusText, onSaveBonus)
            }
        }
        if (showsAttackBonus) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WizardFieldLabel("Attack bonus", hint = "e.g. +1 magic weapon")
                WizardTextField("0", attackBonusText, onAttackBonus)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardFieldLabel("Notes")
            WizardTextField("Special abilities, source, attunement…", notes, onNotes, multiline = true, lineLimit = 3)
        }
        QuantityStepper(quantity, onQuantity)
        SheetPrimaryCta("Add to Inventory", enabled = name.trim().isNotEmpty(), onClick = onCommit)
    }
}

private fun acBonusHint(kind: ItemKind): String = when (kind) {
    ItemKind.SHIELD -> "shields are usually +2"
    ItemKind.WONDROUS -> "e.g. +1 cloak of protection"
    ItemKind.ARMOR -> "for magical +X armor"
    else -> ""
}

@Composable
private fun WeaponCustomFields(
    kindIndex: Int, onKind: (Int) -> Unit,
    damageDice: String, onDamageDice: (String) -> Unit,
    damageType: String, onDamageType: (String) -> Unit,
    damageTypeCustom: Boolean, onDamageTypeCustom: (Boolean) -> Unit,
    propertiesText: String, onProperties: (String) -> Unit,
    normalRange: String, onNormalRange: (String) -> Unit,
    longRange: String, onLongRange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SheetSectionLabel("Weapon properties")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardFieldLabel("Type")
            WizardSegmented(listOf("Melee", "Ranged"), kindIndex, onKind)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardFieldLabel("Damage dice")
            WizardTextField("1d8", damageDice, onDamageDice)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardFieldLabel("Damage type")
            DamageTypePicker(damageType, damageTypeCustom, onDamageType, onDamageTypeCustom)
            if (damageTypeCustom) {
                WizardTextField("e.g. radiant fire", damageType, onDamageType)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardFieldLabel("Properties", hint = "comma-separated")
            WizardTextField("finesse, versatile (1d10), light", propertiesText, onProperties)
        }
        if (kindIndex == 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    WizardFieldLabel("Normal range (ft)")
                    WizardTextField("80", normalRange, onNormalRange)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    WizardFieldLabel("Long range (ft)")
                    WizardTextField("320", longRange, onLongRange)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DamageTypePicker(
    damageType: String,
    custom: Boolean,
    onDamageType: (String) -> Unit,
    onCustom: (Boolean) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        CANONICAL_DAMAGE_TYPES.forEach { type ->
            val active = !custom && damageType == type
            WizardChip(label = type, selected = active) { onCustom(false); onDamageType(type) }
        }
        val otherActive = custom || damageType !in CANONICAL_DAMAGE_TYPES
        WizardChip(label = "Other…", selected = otherActive) {
            onCustom(true)
            if (damageType in CANONICAL_DAMAGE_TYPES) onDamageType("")
        }
    }
}

@Composable
private fun ArmorCustomFields(
    kindIndex: Int, onKind: (Int) -> Unit,
    baseAC: String, onBaseAC: (String) -> Unit,
    dexCap: String, onDexCap: (String) -> Unit,
    stealth: Boolean, onStealth: (Boolean) -> Unit,
    strengthReq: String, onStrengthReq: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SheetSectionLabel("Armor properties")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WizardFieldLabel("Category")
            WizardSegmented(listOf("Light", "Medium", "Heavy"), kindIndex, onKind)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WizardFieldLabel("Base AC")
                WizardTextField("14", baseAC, onBaseAC)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WizardFieldLabel("DEX cap", hint = dexCapHint(kindIndex))
                WizardTextField(dexCapPlaceholder(kindIndex), dexCap, onDexCap)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WizardFieldLabel("STR req.", hint = "optional")
                WizardTextField("13", strengthReq, onStrengthReq)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WizardFieldLabel("Stealth")
                ToggleTile(stealth, if (stealth) "Disadvantage" else "Normal", onStealth)
            }
        }
    }
}

private fun dexCapHint(kindIndex: Int): String = when (kindIndex) {
    1 -> "usually 2"
    2 -> "0 = ignored"
    else -> "uncapped"
}

private fun dexCapPlaceholder(kindIndex: Int): String = when (kindIndex) {
    1 -> "2"
    2 -> "0"
    else -> "blank for uncapped"
}

@Composable
private fun ToggleTile(on: Boolean, label: String, onToggle: (Boolean) -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tileStrong)
            .border(1.dp, palette.ink.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .clickable { onToggle(!on) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontFamily = Cormorant, fontSize = 13.sp, color = palette.ink, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .then(if (on) Modifier.background(palette.accent) else Modifier.border(1.dp, palette.inkMute, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (on) Text("✓", fontSize = 11.sp, color = palette.cream)
        }
    }
}

@Composable
private fun ScrollCustomFields(scroll: ScrollProperties?, onChoose: () -> Unit, onClear: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SheetSectionLabel("Scroll spell")
        ScrollSpellRow(scroll, onChoose, onClear)
        Text(
            "Optional — leave unset for narration-only scrolls.",
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 11.sp,
            color = palette.inkMute,
        )
    }
}

@Composable
private fun ScrollSpellRow(scroll: ScrollProperties?, onChoose: () -> Unit, onClear: () -> Unit) {
    val palette = MaterialTheme.natPalette
    if (scroll != null) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(palette.tile)
                .border(1.dp, palette.accent.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(scroll.spellName, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.ink)
                Text(
                    if (scroll.spellLevel == 0) "Cantrip" else "Level ${scroll.spellLevel}",
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = 11.sp,
                    color = palette.inkMute,
                )
            }
            Text("CHANGE", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.accent, modifier = Modifier.clickable(onClick = onChoose))
            Icon(
                Icons.Filled.Close,
                contentDescription = "Clear scroll spell",
                tint = palette.danger,
                modifier = Modifier.size(22.dp).clip(CircleShape).border(1.dp, palette.danger.copy(alpha = 0.33f), CircleShape).clickable(onClick = onClear).padding(4.dp),
            )
        }
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .dashedBorder(palette.accent.copy(alpha = 0.4f), 3.dp)
                .clickable(onClick = onChoose)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("✦ CHOOSE SPELL", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.accent)
        }
    }
}

@Composable
private fun WondrousCustomFields(
    current: String, onCurrent: (String) -> Unit,
    max: String, onMax: (String) -> Unit,
    recharge: String, onRecharge: (String) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SheetSectionLabel("Charges")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WizardFieldLabel("Current")
                WizardTextField("0", current, onCurrent)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WizardFieldLabel("Max")
                WizardTextField("0", max, onMax)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            WizardFieldLabel("Recharge dice", hint = "e.g. 1d6+1")
            WizardTextField("1d6+1", recharge, onRecharge)
        }
        Text(
            "Leave Max at 0 for non-charged wondrous items (cloaks, eyes of the eagle, …).",
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 11.sp,
            color = palette.inkMute,
        )
    }
}

// ── Edit Item sheet ────────────────────────────────────────────────────────────

/**
 * The Edit-Item sheet: name / kind / quantity / equipped / bonus steppers, read-only
 * weapon & armor summaries, scroll + wondrous editors, notes, and a collapsible
 * Advanced Effects editor. Save commits a **direct** payload replace via [onSave]
 * (the caller uses `withItemReplaced`, unjournaled); Delete removes it. Use/Drop stay
 * reachable in a small action row.
 */
@Composable
internal fun EditItemSheet(
    item: InventoryItem,
    onSave: (InventoryItem) -> Unit,
    onDelete: () -> Unit,
    onUse: (() -> Unit)?,
    onDrop: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var draft by remember { mutableStateOf(item) }
    var advancedExpanded by remember {
        mutableStateOf(
            item.saveBonusByAbility.isNotEmpty() || item.skillBonus.isNotEmpty() ||
                item.damageResistances.isNotEmpty() || item.damageImmunities.isNotEmpty() ||
                item.conditionImmunities.isNotEmpty() || item.advantageOn.isNotEmpty(),
        )
    }
    var showScrollPicker by remember { mutableStateOf(false) }

    val supportsEquipped = draft.kind == ItemKind.WEAPON || draft.kind == ItemKind.ARMOR || draft.kind == ItemKind.SHIELD
    val supportsAcBonus = draft.kind == ItemKind.SHIELD || draft.kind == ItemKind.WONDROUS || draft.kind == ItemKind.ARMOR
    val supportsSaveBonus = draft.kind == ItemKind.WONDROUS || draft.kind == ItemKind.ARMOR || draft.kind == ItemKind.SHIELD
    val supportsAttackBonus = draft.kind == ItemKind.WEAPON

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(palette.parchment).statusBarsPadding()) {
            SheetHeader("Edit Item", item.name, onDismiss)

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    WizardFieldLabel("Name")
                    WizardTextField("Item name", draft.name, { draft = draft.copy(name = it) })
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        WizardFieldLabel("Kind")
                        Text(
                            draft.kind.displayName,
                            fontFamily = Cormorant,
                            fontSize = 15.sp,
                            color = palette.inkSoft,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.tile)
                                .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        WizardFieldLabel("Quantity")
                        QuantityStepperTile(draft.quantity) { draft = draft.copy(quantity = it) }
                    }
                }

                if (supportsEquipped) {
                    ToggleTile(draft.equipped, "Equipped") { draft = draft.copy(equipped = it) }
                }

                if (supportsAcBonus) {
                    BonusStepper("AC bonus", acBonusHint(draft.kind), draft.acBonus ?: 0, min = -5, max = 10) {
                        draft = draft.copy(acBonus = it.takeIf { v -> v != 0 })
                    }
                }
                if (supportsSaveBonus) {
                    BonusStepper("Save bonus", "applies to all saves", draft.saveBonus ?: 0, min = -10, max = 10) {
                        draft = draft.copy(saveBonus = it.takeIf { v -> v != 0 })
                    }
                }
                if (supportsAttackBonus) {
                    BonusStepper("Attack bonus", "added to attacks with this weapon", draft.attackBonus ?: 0, min = -10, max = 10) {
                        draft = draft.copy(attackBonus = it.takeIf { v -> v != 0 })
                    }
                }

                draft.weapon?.let { w ->
                    PropertyCard("Weapon") {
                        Text(w.damageLine, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.ink)
                        if (w.properties.isNotEmpty()) {
                            Text(w.properties.joinToString(" · "), fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = palette.inkMute)
                        }
                        if (w.normalRange != null && w.longRange != null) {
                            Text("Range ${w.normalRange} / ${w.longRange} ft", fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = palette.inkMute)
                        }
                    }
                }
                draft.armor?.let { a ->
                    PropertyCard("Armor") {
                        Text("Base AC ${a.baseAC} · ${a.kind.name.lowercase().replaceFirstChar { it.uppercase() }}", fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.ink)
                        Text(
                            listOfNotNull(
                                when {
                                    a.dexCap == null -> "DEX uncapped"
                                    a.dexCap == 0 -> "DEX ignored"
                                    else -> "DEX cap +${a.dexCap}"
                                },
                                if (a.stealthDisadvantage) "Stealth disadvantage" else null,
                                a.strengthRequirement?.let { "STR $it+" },
                            ).joinToString(" · "),
                            fontFamily = ImFell,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            color = palette.inkMute,
                        )
                    }
                }

                if (draft.kind == ItemKind.SCROLL) {
                    PropertyCard("Scroll Spell") {
                        ScrollSpellRow(draft.scroll, { showScrollPicker = true }) { draft = draft.copy(scroll = null) }
                    }
                }

                if (draft.kind == ItemKind.WONDROUS) {
                    PropertyCard("Charges") {
                        ToggleTile(draft.wondrous != null, "Track charges") { on ->
                            draft = draft.copy(
                                wondrous = if (on) draft.wondrous ?: WondrousProperties(7, 7, "1d6+1") else null,
                            )
                        }
                        draft.wondrous?.let { w ->
                            BonusStepper("Max charges", "", w.maxCharges, min = 0, max = 50, signed = false) { newMax ->
                                draft = draft.copy(wondrous = w.copy(maxCharges = newMax, currentCharges = minOf(w.currentCharges, newMax)).normalized)
                            }
                            BonusStepper("Current", "", w.currentCharges, min = 0, max = w.maxCharges, signed = false) { newCur ->
                                draft = draft.copy(wondrous = w.copy(currentCharges = newCur.coerceIn(0, w.maxCharges)))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                WizardFieldLabel("Recharge dice", hint = "e.g. 1d6+1 · blank = none")
                                WizardTextField("1d6+1", w.rechargeDice, { draft = draft.copy(wondrous = w.copy(rechargeDice = it)) })
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    WizardFieldLabel("Notes")
                    WizardTextField("Magical bonuses, attunement, where it came from…", draft.notes, { draft = draft.copy(notes = it) }, multiline = true, lineLimit = 4)
                }

                AdvancedEffectsSection(
                    expanded = advancedExpanded,
                    onToggle = { advancedExpanded = !advancedExpanded },
                    draft = draft,
                    onDraft = { draft = it },
                )

                // Use / Drop action row (kept reachable, per parity note).
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onUse != null) {
                        CodexButton("Use", modifier = Modifier.weight(1f)) { onUse() }
                    }
                    CodexButton("Drop", modifier = Modifier.weight(1f), danger = true) { onDrop() }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Footer: Delete + Save.
            val hairline = palette.ink.copy(alpha = 0.13f)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(palette.cream.copy(alpha = 0.5f), palette.cream.copy(alpha = 0.98f), palette.cream.copy(alpha = 0.98f)),
                        ),
                    )
                    .drawBehind { drawRect(color = hairline, size = Size(size.width, 1.dp.toPx())) }
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WizardSecondaryButton("Delete") { onDelete() }
                Spacer(Modifier.weight(1f))
                WizardPrimaryButton("Save", enabled = draft.name.trim().isNotEmpty()) {
                    onSave(draft.copy(name = draft.name.trim()))
                }
            }
        }
    }

    if (showScrollPicker) {
        ScrollSpellPicker(
            onPick = { spell -> draft = draft.copy(scroll = ScrollProperties(spell.index, spell.name, spell.level)); showScrollPicker = false },
            onDismiss = { showScrollPicker = false },
        )
    }
}

@Composable
private fun QuantityStepperTile(quantity: Int, onChange: (Int) -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        MiniStep("−", enabled = quantity > 1) { onChange(quantity - 1) }
        Text(
            "$quantity",
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = palette.accent,
            modifier = Modifier.widthIn(min = 36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        MiniStep("+", enabled = true) { onChange(quantity + 1) }
    }
}

@Composable
private fun BonusStepper(
    label: String,
    hint: String,
    value: Int,
    min: Int,
    max: Int,
    signed: Boolean = true,
    onChange: (Int) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label.uppercase(), fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.inkMute)
            if (hint.isNotEmpty()) {
                Text(hint, fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 11.sp, color = palette.inkMute)
            }
        }
        MiniStep("−", enabled = value > min) { onChange((value - 1).coerceAtLeast(min)) }
        Text(
            if (signed) value.signedLabel() else "$value",
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = if (value == 0) palette.inkMute else palette.accent,
            modifier = Modifier.widthIn(min = 36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        MiniStep("+", enabled = value < max) { onChange((value + 1).coerceAtMost(max)) }
    }
}

@Composable
private fun PropertyCard(label: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .border(1.dp, palette.accent.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label.uppercase(), fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.accent)
        content()
    }
}

// ── Advanced effects editor ────────────────────────────────────────────────────

@Composable
private fun AdvancedEffectsSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    draft: InventoryItem,
    onDraft: (InventoryItem) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ADVANCED EFFECTS", fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.accent, modifier = Modifier.weight(1f))
            Text(if (expanded) "▲" else "▼", fontSize = 10.sp, color = palette.accent)
        }
        if (expanded) {
            // Per-ability save grid.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SAVE BONUSES (PER ABILITY)", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.inkMute)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Ability.entries.forEach { ability ->
                        val current = draft.saveBonusByAbility[ability] ?: 0
                        AbilitySaveCell(ability.abbreviation, current, Modifier.weight(1f)) { delta ->
                            val next = ((draft.saveBonusByAbility[ability] ?: 0) + delta).coerceIn(-5, 10)
                            val map = draft.saveBonusByAbility.toMutableMap()
                            if (next == 0) map.remove(ability) else map[ability] = next
                            onDraft(draft.copy(saveBonusByAbility = map))
                        }
                    }
                }
            }

            // Skill bonuses.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SKILL BONUSES", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.inkMute)
                draft.skillBonus.entries.sortedBy { skillName(it.key) }.forEach { (skillId, value) ->
                    SkillBonusRow(skillName(skillId), value, onChange = { delta ->
                        val next = (value + delta).coerceIn(-5, 10)
                        val map = draft.skillBonus.toMutableMap()
                        if (next == 0) map.remove(skillId) else map[skillId] = next
                        onDraft(draft.copy(skillBonus = map))
                    }, onRemove = {
                        onDraft(draft.copy(skillBonus = draft.skillBonus - skillId))
                    })
                }
                AddSkillBonusMenu(taken = draft.skillBonus.keys) { skillId ->
                    onDraft(draft.copy(skillBonus = draft.skillBonus + (skillId to 1)))
                }
            }

            ResistanceField("Damage resistances", draft.damageResistances, "fire, poison") { onDraft(draft.copy(damageResistances = it)) }
            ResistanceField("Damage immunities", draft.damageImmunities, "necrotic") { onDraft(draft.copy(damageImmunities = it)) }
            ResistanceField("Condition immunities", draft.conditionImmunities, "frightened, charmed") { onDraft(draft.copy(conditionImmunities = it)) }
            ResistanceField("Advantage on", draft.advantageOn, "Perception checks, saves vs charm") { onDraft(draft.copy(advantageOn = it)) }
        }
    }
}

private fun skillName(id: String): String = DnD5eCatalog.skill(id)?.name ?: id.replaceFirstChar { it.uppercase() }

@Composable
private fun AbilitySaveCell(abbrev: String, value: Int, modifier: Modifier, onDelta: (Int) -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(3.dp))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(abbrev.uppercase(), fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.5.sp, color = palette.inkMute)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("−", fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = palette.ink, modifier = Modifier.size(18.dp).clickable { onDelta(-1) }, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(value.signedLabel(), fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (value == 0) palette.inkMute else palette.accent, modifier = Modifier.widthIn(min = 22.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("+", fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = palette.ink, modifier = Modifier.size(18.dp).clickable { onDelta(1) }, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SkillBonusRow(name: String, value: Int, onChange: (Int) -> Unit, onRemove: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(3.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(name, fontFamily = Cormorant, fontSize = 13.sp, color = palette.ink, modifier = Modifier.weight(1f))
        MiniStep("−", enabled = true) { onChange(-1) }
        Text(value.signedLabel(), fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.accent, modifier = Modifier.widthIn(min = 28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        MiniStep("+", enabled = true) { onChange(1) }
        Icon(
            Icons.Filled.Close,
            contentDescription = "Remove $name bonus",
            tint = palette.danger,
            modifier = Modifier.size(22.dp).clip(CircleShape).border(1.dp, palette.danger.copy(alpha = 0.33f), CircleShape).clickable(onClick = onRemove).padding(4.dp),
        )
    }
}

@Composable
private fun AddSkillBonusMenu(taken: Set<String>, onAdd: (String) -> Unit) {
    val palette = MaterialTheme.natPalette
    var expanded by remember { mutableStateOf(false) }
    val available = DnD5eCatalog.skills.filter { it.id !in taken }
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .dashedBorder(palette.accent.copy(alpha = 0.4f), 3.dp)
                .then(if (available.isNotEmpty()) Modifier.clickable { expanded = true } else Modifier)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (available.isEmpty()) "ALL SKILLS ALREADY BONUSED" else "+ ADD SKILL BONUS",
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = if (available.isEmpty()) palette.inkMute else palette.accent,
            )
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            available.forEach { skill ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(skill.name) },
                    onClick = { onAdd(skill.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ResistanceField(label: String, values: List<String>, placeholder: String, onChange: (List<String>) -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.inkMute)
        WizardTextField(placeholder, values.joinToString(", "), { onChange(parseCommaList(it)) })
    }
}

// ── Scroll Spell picker ────────────────────────────────────────────────────────

/**
 * A minimal spell picker for scroll items — search over the full SRD
 * [DnD5eCatalog.spellLibrary], grouped by level. No cast/prepare affordances.
 * Divergence: not gated by `enabledSources` (no such catalogue accessor yet).
 */
@Composable
private fun ScrollSpellPicker(onPick: (Spell) -> Unit, onDismiss: () -> Unit) {
    val palette = MaterialTheme.natPalette
    var searchText by remember { mutableStateOf("") }
    val query = searchText.trim().lowercase()
    val filtered = remember(query) {
        val pool = DnD5eCatalog.spellLibrary
        if (query.isEmpty()) pool else pool.filter { it.name.lowercase().contains(query) }
    }
    val grouped = filtered.groupBy { it.level }.toSortedMap()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(palette.parchment).statusBarsPadding()) {
            SheetHeader("Scroll Spell", "Which spell does it cast?", onDismiss)
            Box(Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) {
                SheetSearchField(searchText, "Fireball, Cure Wounds, Magic Missile…", { searchText = it }) { searchText = "" }
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (grouped.isEmpty()) {
                    item {
                        Text(
                            "No spells match.",
                            fontFamily = Cormorant,
                            fontStyle = FontStyle.Italic,
                            fontSize = 15.sp,
                            color = palette.inkSoft,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                grouped.forEach { (level, spells) ->
                    item(key = "head-$level") {
                        Text(
                            if (level == 0) "CANTRIPS (${spells.size})" else "LEVEL $level",
                            fontFamily = Cinzel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 2.5.sp,
                            color = palette.accent,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(spells, key = { it.index }) { spell ->
                        ScrollSpellPickRow(spell) { onPick(spell) }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ScrollSpellPickRow(spell: Spell, onTap: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(3.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(spell.name, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.ink, maxLines = 1)
            val classes = spell.classNames.joinToString(", ")
            Text(
                if (classes.isEmpty()) spell.schoolName else "${spell.schoolName} · $classes",
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = palette.inkMute,
                maxLines = 1,
            )
        }
        Text("›", fontFamily = Cormorant, fontSize = 18.sp, color = palette.inkMute)
    }
}

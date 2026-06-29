package au.com.evonet.nat20.ui.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e2024.Armors2024
import au.com.evonet.nat20.dnd5e2024.Gears2024
import au.com.evonet.nat20.dnd5e2024.Weapons2024
import au.com.evonet.nat20.pf2e.PfArmors
import au.com.evonet.nat20.pf2e.PfShields
import au.com.evonet.nat20.pf2e.PfWeapons
import au.com.evonet.nat20.pf2e.RuneKind
import au.com.evonet.nat20.pf2e.Runes
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The unified Item Catalog (matching iOS): one reference browser with ruleset
 * tabs — D&D 5e 2014, D&D 5e 2024, Pathfinder 2e — each with category chips
 * (Weapons / Armor / …) over parchment-styled rows (name + italic meta + cost +
 * chevron), ported from the iOS `ItemCatalogView` family.
 */
@Composable
fun ItemCatalogShell(onBack: () -> Unit) {
    ReferenceTabShell(
        eyebrow = "Catalogue",
        title = "Item Catalog",
        onBack = onBack,
        tabs = listOf(
            ReferenceTab("5e 2014") { ItemCatalogBody2014() },
            ReferenceTab("5e 2024") { ItemCatalog2024Body() },
            ReferenceTab("PF2e") { ItemCatalogPfBody() },
        ),
    )
}

private data class ItemRowModel(
    val id: String,
    val name: String,
    val subtitle: String,
    val trailing: String?,
    val detailLines: List<Pair<String, String>>,
    val description: String,
)

// ── The three ruleset bodies ────────────────────────────────────────────────────

@Composable
fun ItemCatalogBody2014() {
    ItemCatalogBody(listOf("Weapons", "Armor", "Gear")) { category, q ->
        when (category) {
            "Weapons" -> DnD5eCatalog.weapons.filter { matches(it.name, q) }.map { w ->
                ItemRowModel(
                    w.id, w.name,
                    listOf(w.category.replaceFirstChar(Char::uppercase), listOf(w.damageDice, w.damageType).filter { it.isNotBlank() }.joinToString(" ")).filter { it.isNotBlank() }.joinToString(" · "),
                    w.cost,
                    buildList {
                        add("Type" to "${w.category.replaceFirstChar(Char::uppercase)} · ${w.kind.name.lowercase()}")
                        add("Damage" to listOf(w.damageDice, w.damageType).filter { it.isNotBlank() }.joinToString(" "))
                        val range = listOfNotNull(w.normalRange, w.longRange)
                        if (range.isNotEmpty()) add("Range" to range.joinToString("/") + " ft.")
                        if (w.properties.isNotEmpty()) add("Properties" to w.properties.joinToString(", "))
                    },
                    "",
                )
            }
            "Armor" -> DnD5eCatalog.armor.filter { matches(it.name, q) }.map { a ->
                ItemRowModel(
                    a.id, a.name,
                    "${a.kind.name.lowercase().replaceFirstChar(Char::uppercase)} · Base AC ${a.baseAC}",
                    a.cost,
                    buildList {
                        val dexRule = when (a.dexCap) { null -> "+ full DEX"; 0 -> "DEX ignored"; else -> "+ DEX (max +${a.dexCap})" }
                        add("Armor Class" to "${a.baseAC}  ·  $dexRule")
                        a.strengthRequirement?.let { add("Strength" to "$it") }
                        if (a.stealthDisadvantage) add("Stealth" to "Disadvantage")
                    },
                    "",
                )
            }
            else -> DnD5eCatalog.gear.filter { matches(it.name, q) }.map { g ->
                ItemRowModel(g.id, g.name, g.kind.replaceFirstChar(Char::uppercase), g.cost, emptyList(), g.description)
            }
        }
    }
}

@Composable
fun ItemCatalog2024Body() {
    ItemCatalogBody(listOf("Weapons", "Armor", "Gear")) { category, q ->
        when (category) {
            "Weapons" -> Weapons2024.all.filter { matches(it.name, q) }.map { w ->
                ItemRowModel(
                    w.id, w.name,
                    "${w.category.name.lowercase().replaceFirstChar(Char::uppercase)} · ${w.damage} · ${w.mastery.displayName}",
                    null,
                    listOf(
                        "Category" to w.category.name.lowercase().replaceFirstChar(Char::uppercase),
                        "Damage" to w.damage,
                        "Mastery" to w.mastery.displayName,
                    ),
                    w.mastery.summary,
                )
            }
            "Armor" -> Armors2024.all.filter { matches(it.name, q) }.map { a ->
                val dexRule = when (a.category.dexCap) { null -> "+ full DEX"; 0 -> "DEX ignored"; else -> "+ DEX (max +${a.category.dexCap})" }
                ItemRowModel(
                    a.id, a.name,
                    "${a.category.name.lowercase().replaceFirstChar(Char::uppercase)} · Base AC ${a.baseAC}",
                    null,
                    listOf("Armor Class" to "${a.baseAC}  ·  $dexRule"),
                    "",
                )
            }
            else -> Gears2024.all.filter { matches(it.name, q) }.map { g ->
                ItemRowModel(
                    g.id, g.name,
                    g.acBonus?.let { "Shield · +$it AC" } ?: g.cost,
                    g.cost,
                    buildList {
                        g.acBonus?.let { add("AC Bonus" to "+$it") }
                        add("Cost" to g.cost)
                        g.weight?.let { add("Weight" to "$it lb") }
                    },
                    g.description,
                )
            }
        }
    }
}

@Composable
fun ItemCatalogPfBody() {
    ItemCatalogBody(listOf("Weapons", "Armor", "Shields", "Runes")) { category, q ->
        when (category) {
            "Weapons" -> PfWeapons.all.filter { matches(it.name, q) }.map { w ->
                ItemRowModel(
                    w.id, w.name,
                    "${w.category.displayName} · ${w.damageDie} ${w.damageType}" + if (w.ranged) " · Ranged" else "",
                    null,
                    buildList {
                        add("Category" to w.category.displayName)
                        add("Damage" to "${w.damageDie} ${w.damageType}")
                        if (w.traits.isNotEmpty()) add("Traits" to w.traits.joinToString(", "))
                    },
                    "",
                )
            }
            "Armor" -> PfArmors.all.filter { matches(it.name, q) }.map { a ->
                ItemRowModel(
                    a.id, a.name,
                    "${a.category.displayName} · +${a.acBonus} AC · Dex cap +${a.dexCap}",
                    null,
                    listOf("Category" to a.category.displayName, "AC Bonus" to "+${a.acBonus}", "Dex Cap" to "+${a.dexCap}"),
                    a.summary,
                )
            }
            "Shields" -> PfShields.all.filter { matches(it.name, q) }.map { s ->
                ItemRowModel(
                    s.id, s.name,
                    "Raised +${s.raisedAcBonus} AC · Hardness ${s.hardness}",
                    null,
                    listOf("Raised AC" to "+${s.raisedAcBonus}", "Hardness" to "${s.hardness}", "HP" to "${s.hp}"),
                    "",
                )
            }
            else -> Runes.propertyRunes.filter { matches(it.name, q) }.map { r ->
                ItemRowModel(
                    "rune-${r.id}", r.name,
                    "${if (r.kind == RuneKind.WEAPON) "Weapon" else "Armor"} rune · Level ${r.level}",
                    "${r.price} gp",
                    listOf(
                        "Kind" to if (r.kind == RuneKind.WEAPON) "Weapon" else "Armor",
                        "Level" to "${r.level}",
                        "Price" to "${r.price} gp",
                    ),
                    r.summary,
                )
            }
        }
    }
}

private fun matches(name: String, q: String): Boolean = q.isBlank() || name.contains(q, ignoreCase = true)

// ── Shared body: category chips + search + flat list ────────────────────────────

@Composable
private fun ItemCatalogBody(categories: List<String>, rowsFor: (String, String) -> List<ItemRowModel>) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.first()) }
    var detail by remember { mutableStateOf<ItemRowModel?>(null) }
    val rows = remember(category, query) { rowsFor(category, query.trim()) }

    Column(Modifier.fillMaxSize()) {
        ReferencePillRow {
            categories.forEach { c -> ReferencePill(c, c == category) { category = c } }
        }
        ReferenceSearchField(query, { query = it }, "Search by name", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(rows, key = { it.id }) { model -> ItemListRow(model) { detail = model } }
        }
    }

    detail?.let { ItemDetailDialog(it) { detail = null } }
}

@Composable
private fun ItemListRow(model: ItemRowModel, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(model.name, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = palette.ink, maxLines = 1)
                Text(model.subtitle, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = palette.inkSoft, maxLines = 1)
            }
            model.trailing?.takeIf { it.isNotBlank() }?.let { cost ->
                Text(cost.uppercase(), fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.5.sp, color = palette.accentBrown)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.inkMute)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.ink.copy(alpha = 0.12f)))
    }
}

@Composable
private fun ItemDetailDialog(model: ItemRowModel, onDismiss: () -> Unit) {
    val palette = MaterialTheme.natPalette
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(model.name, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = palette.accent) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(model.subtitle, fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = palette.inkMute)
                model.detailLines.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                    Text("$label: $value", fontFamily = Cormorant, fontSize = 15.sp, color = palette.inkSoft)
                }
                if (model.description.isNotBlank()) {
                    Text(model.description, fontFamily = EbGaramond, fontSize = 15.sp, color = palette.ink)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

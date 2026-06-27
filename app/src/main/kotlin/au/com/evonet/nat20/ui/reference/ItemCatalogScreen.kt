package au.com.evonet.nat20.ui.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5eCatalog

/**
 * Read-only Item Catalog for D&D 5e (2014): the bundled SRD weapon / armor / gear
 * tables. Sibling of the 2024 [ItemCatalog2024Screen], reading the
 * [DnD5eCatalog] equipment entries. Attribution: SRD 5.1 / 5e-database (CC BY 4.0).
 */
private enum class ItemCat2014(val label: String) { WEAPONS("Weapons"), ARMOR("Armor"), GEAR("Gear") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCatalogScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ItemCat2014.WEAPONS) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Item Catalog") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ItemCat2014.entries) { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val q = query.trim()
                when (category) {
                    ItemCat2014.WEAPONS -> items(
                        DnD5eCatalog.weapons.filter { q.isBlank() || it.name.contains(q, ignoreCase = true) },
                        key = { it.id },
                    ) { w ->
                        ItemCard(w.name, "${w.category.replaceFirstChar(Char::uppercase)} weapon · ${w.kind.name.lowercase()}") {
                            MetaLine("Damage", listOf(w.damageDice, w.damageType).filter { it.isNotBlank() }.joinToString(" "))
                            val range = listOfNotNull(w.normalRange, w.longRange)
                            if (range.isNotEmpty()) MetaLine("Range", range.joinToString("/") + " ft.")
                            if (w.properties.isNotEmpty()) MetaLine("Properties", w.properties.joinToString(", "))
                        }
                    }
                    ItemCat2014.ARMOR -> items(
                        DnD5eCatalog.armor.filter { q.isBlank() || it.name.contains(q, ignoreCase = true) },
                        key = { it.id },
                    ) { a ->
                        val dexRule = when (a.dexCap) { null -> "+ full DEX"; 0 -> "DEX ignored"; else -> "+ DEX (max +${a.dexCap})" }
                        ItemCard(a.name, "${a.kind.name.lowercase().replaceFirstChar(Char::uppercase)} armor") {
                            MetaLine("Armor Class", "${a.baseAC}  ·  $dexRule")
                            a.strengthRequirement?.let { MetaLine("Strength", "$it") }
                            if (a.stealthDisadvantage) MetaLine("Stealth", "Disadvantage")
                        }
                    }
                    ItemCat2014.GEAR -> items(
                        DnD5eCatalog.gear.filter { q.isBlank() || it.name.contains(q, ignoreCase = true) },
                        key = { it.id },
                    ) { g ->
                        ItemCard(g.name, g.kind.replaceFirstChar(Char::uppercase)) {
                            g.acBonus?.let { MetaLine("AC Bonus", "+$it") }
                            if (g.description.isNotBlank()) Text(g.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(title: String, subtitle: String, body: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            body()
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    if (value.isBlank()) return
    Text("$label: $value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

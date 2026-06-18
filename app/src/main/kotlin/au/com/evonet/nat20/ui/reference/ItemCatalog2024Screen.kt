package au.com.evonet.nat20.ui.reference

import androidx.compose.foundation.clickable
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
import au.com.evonet.nat20.dnd5e2024.Armors2024
import au.com.evonet.nat20.dnd5e2024.WeaponMastery2024
import au.com.evonet.nat20.dnd5e2024.Weapons2024

/**
 * Read-only Item Catalog (A21): the 2024 weapon table (with mastery properties)
 * and armor table (with the DEX-cap rule and base AC). Browsable reference for
 * the Reference codex. Port of the iOS `ItemCatalog2024View`.
 */
private enum class ItemCategory(val label: String) { WEAPONS("Weapons"), ARMOR("Armor"), MASTERIES("Masteries") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCatalog2024Screen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ItemCategory.WEAPONS) }

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
                items(ItemCategory.entries) { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                }
            }
            if (category != ItemCategory.MASTERIES) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (category) {
                    ItemCategory.WEAPONS -> items(
                        Weapons2024.all.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) },
                        key = { it.id },
                    ) { w ->
                        ItemCard(w.name, "${w.category.name.lowercase().replaceFirstChar(Char::uppercase)} weapon · ${w.damage}") {
                            Text("Mastery: ${w.mastery.displayName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(w.mastery.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ItemCategory.ARMOR -> items(
                        Armors2024.all.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) },
                        key = { it.id },
                    ) { a ->
                        val dexRule = when (a.category.dexCap) { null -> "+ full DEX"; 0 -> "DEX ignored"; else -> "+ DEX (max +${a.category.dexCap})" }
                        ItemCard(a.name, "${a.category.name.lowercase().replaceFirstChar(Char::uppercase)} armor") {
                            Text("Base AC ${a.baseAC}  ·  $dexRule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ItemCategory.MASTERIES -> items(WeaponMastery2024.entries.toList(), key = { it.name }) { m ->
                        ItemCard(m.displayName, "Weapon mastery property") {
                            Text(m.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

package au.com.evonet.nat20.ui.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.pf2e.PfArmors
import au.com.evonet.nat20.pf2e.PfShields
import au.com.evonet.nat20.pf2e.PfWeapons

/**
 * Read-only Pathfinder 2e **Item Catalog** (A22): the weapon, armor, and shield
 * tables (with their PF2e-defining stats — damage die + traits, AC bonus + Dex
 * cap, raised shield bonus + hardness). Mirrors the 2024 Item Catalog. ORC content.
 */
private enum class PfItemCategory(val label: String) { WEAPONS("Weapons"), ARMOR("Armor"), SHIELDS("Shields") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathfinderItemCatalogScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PfItemCategory.WEAPONS) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Item Catalog") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PfItemCategory.entries) { c -> FilterChip(category == c, { category = c }, label = { Text(c.label) }) }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it }, label = { Text("Search") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val q = query.trim()
                when (category) {
                    PfItemCategory.WEAPONS -> items(PfWeapons.all.filter { q.isBlank() || it.name.contains(q, ignoreCase = true) }, key = { it.id }) { w ->
                        ItemCard(w.name, "${w.category.displayName} · ${w.damageDie} ${w.damageType}${if (w.ranged) " · Ranged" else ""}") {
                            if (w.traits.isNotEmpty()) Text(w.traits.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    PfItemCategory.ARMOR -> items(PfArmors.all.filter { q.isBlank() || it.name.contains(q, ignoreCase = true) }, key = { it.id }) { a ->
                        ItemCard(a.name, "${a.category.displayName} · AC +${a.acBonus} · Dex cap +${a.dexCap}") {
                            if (a.summary.isNotBlank()) Text(a.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    PfItemCategory.SHIELDS -> items(PfShields.all.filter { q.isBlank() || it.name.contains(q, ignoreCase = true) }, key = { it.id }) { s ->
                        ItemCard(s.name, "Raised AC +${s.raisedAcBonus} · Hardness ${s.hardness} · ${s.hp} HP") {}
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
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            body()
        }
    }
}

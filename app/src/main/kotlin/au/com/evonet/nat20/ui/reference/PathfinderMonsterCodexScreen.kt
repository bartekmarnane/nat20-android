package au.com.evonet.nat20.ui.reference

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.pf2e.PFMonster
import au.com.evonet.nat20.pf2e.PFMonsterCatalog

/**
 * Read-only Pathfinder 2e **Monster Core** codex (A22): the ORC bestiary (445
 * creatures), searchable and filterable by level, with expandable statblocks.
 * Mirrors the 2024 Monster Codex. Attribution: PF2e Remaster Monster Core, ORC
 * License, via Archives of Nethys.
 */
/** Chromeless Pathfinder 2e Monster Core body for the [ReferenceTabShell]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathfinderMonsterCodexBody() {
    var query by remember { mutableStateOf("") }
    var levelFilter by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }

    val monsters = remember(query, levelFilter) {
        PFMonsterCatalog.all.filter { m ->
            (levelFilter == null || m.level == levelFilter) &&
                (query.isBlank() || m.name.contains(query.trim(), ignoreCase = true) || m.traits.any { it.contains(query.trim(), ignoreCase = true) })
        }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Search ${PFMonsterCatalog.all.size} creatures") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LevelFilters) { lvl ->
                FilterChip(levelFilter == lvl, { levelFilter = if (levelFilter == lvl) null else lvl }, label = { Text("Lv $lvl") })
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(monsters, key = { it.id }) { m -> MonsterRow(m, expanded == m.id) { expanded = if (expanded == m.id) null else m.id } }
        }
    }
}

@Composable
private fun MonsterRow(monster: PFMonster, expanded: Boolean, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(monster.name, style = MaterialTheme.typography.titleMedium)
                    Text(monster.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("AC ${monster.ac} · ${monster.hp} HP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    if (monster.description.isNotBlank()) Text(monster.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(cleanStatblock(monster.statblock), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Light Markdown → plain text: drop the bold/italic markers, keep the line structure. */
private fun cleanStatblock(markdown: String): String =
    markdown.replace("**", "").replace("---", "──────────")

/** Level filter chips spanning the Monster Core's range. */
private val LevelFilters: List<Int> = listOf(-1, 0, 1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 18, 20, 25)

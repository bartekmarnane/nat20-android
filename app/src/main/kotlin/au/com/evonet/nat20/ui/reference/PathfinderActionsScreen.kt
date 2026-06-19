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
import au.com.evonet.nat20.pf2e.PfAction
import au.com.evonet.nat20.pf2e.PfActions

/**
 * Read-only Pathfinder 2e **Actions** glossary (A22): the three-action economy
 * as static reference data — the Basic Actions + common Skill Actions with their
 * action cost (◆ / ◆◆ / ◆◆◆ / ◇ / ↺), traits, and summaries. Not a turn tracker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathfinderActionsScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var skillsOnly by remember { mutableStateOf<Boolean?>(null) } // null = all; true = skill; false = basic

    val actions = remember(query, skillsOnly) {
        PfActions.all.filter { a ->
            (skillsOnly == null || a.skill == skillsOnly) &&
                (query.isBlank() || a.name.contains(query.trim(), ignoreCase = true) || a.traits.any { it.contains(query.trim(), ignoreCase = true) })
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Actions") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, label = { Text("Search actions") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(skillsOnly == null, { skillsOnly = null }, label = { Text("All") }) }
                item { FilterChip(skillsOnly == false, { skillsOnly = false }, label = { Text("Basic") }) }
                item { FilterChip(skillsOnly == true, { skillsOnly = true }, label = { Text("Skill") }) }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(actions, key = { it.id }) { ActionRow(it) }
            }
        }
    }
}

@Composable
private fun ActionRow(action: PfAction) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(action.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(action.cost.glyph, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (action.traits.isNotEmpty()) Text(action.traits.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(action.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

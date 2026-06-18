package au.com.evonet.nat20.ui.reference

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.Spell

/**
 * Read-only Spell Library (A10): the SRD 5.1 catalogue, searchable and
 * level-filterable, with expandable rows. Port of the iOS `SpellLibraryView`
 * (browse mode). Reachable from Settings → Reference and the codex Spells tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellLibraryScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var levelFilter by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }

    val spells = remember(query, levelFilter) {
        DnD5eCatalog.spellLibrary.filter { spell ->
            (levelFilter == null || spell.level == levelFilter) &&
                (query.isBlank() || spell.name.contains(query.trim(), ignoreCase = true))
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Spell Library") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search spells") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LevelFilters) { level ->
                    FilterChip(
                        selected = levelFilter == level,
                        onClick = { levelFilter = if (levelFilter == level) null else level },
                        label = { Text(levelChipLabel(level)) },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(spells, key = { it.index }) { spell ->
                    SpellRow(
                        spell = spell,
                        expanded = expanded == spell.index,
                        onToggle = { expanded = if (expanded == spell.index) null else spell.index },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpellRow(spell: Spell, expanded: Boolean, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(spell.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${spell.levelLabel} · ${spell.schoolName}" +
                    listOfNotNull(
                        if (spell.concentration) "Concentration" else null,
                        if (spell.ritual) "Ritual" else null,
                    ).joinToString("") { " · $it" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    MetaLine("Casting Time", spell.castingTime)
                    MetaLine("Range", spell.range)
                    MetaLine("Components", spell.components.joinToString(", "))
                    MetaLine("Duration", spell.duration)
                    if (spell.classNames.isNotEmpty()) MetaLine("Classes", spell.classNames.joinToString(", "))
                    Text(spell.description, style = MaterialTheme.typography.bodyMedium)
                    if (spell.higherLevelText.isNotEmpty()) {
                        Text(
                            "At Higher Levels. ${spell.higherLevelText}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    if (value.isBlank()) return
    Text(
        buildString { append(label).append(": ").append(value) },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val LevelFilters: List<Int> = (0..9).toList()

private fun levelChipLabel(level: Int): String = if (level == 0) "Cantrip" else "Lv $level"

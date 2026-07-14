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
@Composable
fun PathfinderMonsterCodexBody() {
    var query by remember { mutableStateOf("") }
    var levelFilter by remember { mutableStateOf<Int?>(null) }

    val monsters = remember(query, levelFilter) {
        PFMonsterCatalog.all.filter { m ->
            (levelFilter == null || m.level == levelFilter) &&
                (query.isBlank() || m.name.contains(query.trim(), ignoreCase = true) || m.traits.any { it.contains(query.trim(), ignoreCase = true) })
        }.map { it.toView() }
    }

    MonsterCodexList(
        query = query,
        onQuery = { query = it },
        hint = "Search ${PFMonsterCatalog.all.size} creatures",
        monsters = monsters,
        filterPills = {
            LevelFilters.forEach { lvl ->
                ReferencePill("Lv $lvl", levelFilter == lvl) { levelFilter = if (levelFilter == lvl) null else lvl }
            }
        },
    )
}

private fun PFMonster.toView() = MonsterView(
    id = id,
    name = name,
    subtitle = subtitle,
    badge = "AC $ac · $hp HP",
    meta = emptyList(),
    abilities = emptyList(),
    blocks = buildList {
        if (description.isNotBlank()) add("Description" to listOf("" to description))
        add("Statblock" to listOf("" to cleanStatblock(statblock)))
    },
)

/** Light Markdown → plain text: drop the bold/italic markers, keep the line structure. */
private fun cleanStatblock(markdown: String): String =
    markdown.replace("**", "").replace("---", "──────────")

/** Level filter chips spanning the Monster Core's range. */
private val LevelFilters: List<Int> = listOf(-1, 0, 1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 18, 20, 25)

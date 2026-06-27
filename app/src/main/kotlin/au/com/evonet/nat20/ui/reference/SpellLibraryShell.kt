package au.com.evonet.nat20.ui.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Catalog
import au.com.evonet.nat20.pf2e.PfSpells
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.DiamondShape
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The unified Spell Library (matching iOS): one reference browser with ruleset
 * tabs — D&D 5e 2014, D&D 5e 2024, Pathfinder 2e — each a parchment-styled,
 * level-grouped spell list with a school monogram, component badges, and section
 * headers, ported from the iOS `SpellLibraryView` / `CodexGroupedList`.
 */
@Composable
fun SpellLibraryShell(onBack: () -> Unit) {
    ReferenceTabShell(
        eyebrow = "Catalogue",
        title = "Spell Library",
        onBack = onBack,
        tabs = listOf(
            ReferenceTab("5e 2014") { SpellLibraryBody() },
            ReferenceTab("5e 2024") { SpellLibrary2024Body() },
            ReferenceTab("PF2e") { SpellLibraryPfBody() },
        ),
    )
}

// ── Shared row model + per-ruleset mapping ──────────────────────────────────────

private data class SpellBadge(val letter: String, val filled: Boolean)

private data class SpellRowModel(
    val id: String,
    val name: String,
    val meta: String,
    val monogram: String,
    val badges: List<SpellBadge>,
    val group: Int,
    val detailLines: List<Pair<String, String>>,
    val description: String,
)

private fun groupLabel(level: Int, pathfinder: Boolean): String = when {
    level == 0 -> "Cantrips"
    pathfinder -> "Rank $level"
    else -> ordinal(level) + " Level"
}

private fun ordinal(n: Int): String = when (n) {
    1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${n}th"
}

// ── The three ruleset bodies ────────────────────────────────────────────────────

@Composable
fun SpellLibraryBody() {
    val all = remember {
        DnD5eCatalog.spellLibrary.sortedWith(compareBy({ it.level }, { it.name })).map { s ->
            SpellRowModel(
                id = s.index,
                name = s.name,
                meta = listOf(s.schoolName, s.castingTime, s.range).filter { it.isNotBlank() }.joinToString(" · "),
                monogram = s.schoolName.firstOrNull()?.uppercase() ?: "•",
                badges = listOf(
                    SpellBadge("V", "V" in s.components),
                    SpellBadge("S", "S" in s.components),
                    SpellBadge("M", "M" in s.components),
                ),
                group = s.level,
                detailLines = buildList {
                    add("Casting Time" to s.castingTime)
                    add("Range" to s.range)
                    add("Components" to s.components.joinToString(", "))
                    add("Duration" to s.duration)
                    if (s.classNames.isNotEmpty()) add("Classes" to s.classNames.joinToString(", "))
                },
                description = s.description + s.higherLevelText.takeIf { it.isNotBlank() }?.let { "\n\nAt Higher Levels. $it" }.orEmpty(),
            )
        }
    }
    GroupedSpellList(all, pathfinder = false, searchHint = "Search by name or school")
}

@Composable
fun SpellLibrary2024Body() {
    val all = remember {
        // The 2024 SRD catalogue doesn't carry V/S/M components; 93% of its spells
        // share an id with the 2014 SRD (which does), so reuse those. The ~25
        // 2024-only spells fall back to the common Verbal+Somatic pattern.
        val components2014 = DnD5eCatalog.spellLibrary.associate { it.index to it.components }
        DnD5e2024Catalog.spellLibrary.sortedWith(compareBy({ it.level }, { it.name })).map { s ->
            val components = components2014[s.id] ?: listOf("V", "S")
            SpellRowModel(
                id = s.id,
                name = s.name,
                meta = listOfNotNull(
                    s.school.replaceFirstChar(Char::uppercase).takeIf { it.isNotBlank() },
                    if (s.concentration) "Concentration" else null,
                    if (s.ritual) "Ritual" else null,
                ).joinToString(" · "),
                monogram = s.school.firstOrNull()?.uppercase() ?: "•",
                badges = listOf(
                    SpellBadge("V", "V" in components),
                    SpellBadge("S", "S" in components),
                    SpellBadge("M", "M" in components),
                ),
                group = s.level,
                detailLines = buildList {
                    if (s.school.isNotBlank()) add("School" to s.school.replaceFirstChar(Char::uppercase))
                    add("Components" to components.joinToString(", "))
                    if (s.classNames.isNotEmpty()) add("Classes" to s.classNames.joinToString(", "))
                },
                description = s.description,
            )
        }
    }
    GroupedSpellList(all, pathfinder = false, searchHint = "Search by name or school")
}

@Composable
fun SpellLibraryPfBody() {
    val all = remember {
        PfSpells.all.sortedWith(compareBy({ it.rank }, { it.name })).map { s ->
            SpellRowModel(
                id = s.id,
                name = s.name,
                meta = listOfNotNull(
                    s.traditions.joinToString("/") { it.name.lowercase().replaceFirstChar(Char::uppercase) }.takeIf { it.isNotBlank() },
                    "${s.actions} action".let { if (s.actions == "1") it else "$it" },
                ).joinToString(" · "),
                monogram = s.traditions.firstOrNull()?.name?.firstOrNull()?.uppercase() ?: "✦",
                badges = emptyList(),
                group = s.rank,
                detailLines = buildList {
                    add("Traditions" to s.traditions.joinToString(", ") { it.name.lowercase().replaceFirstChar(Char::uppercase) })
                    add("Actions" to s.actions)
                    if (s.traits.isNotEmpty()) add("Traits" to s.traits.joinToString(", "))
                },
                description = s.summary,
            )
        }
    }
    GroupedSpellList(all, pathfinder = true, searchHint = "Search by name or tradition")
}

// ── The grouped list (search + filter pills + sectioned rows) ────────────────────

@Composable
private fun GroupedSpellList(models: List<SpellRowModel>, pathfinder: Boolean, searchHint: String) {
    var query by remember { mutableStateOf("") }
    var levelFilter by remember { mutableStateOf<Int?>(null) }
    var detail by remember { mutableStateOf<SpellRowModel?>(null) }

    val filtered = remember(models, query, levelFilter) {
        models.filter { m ->
            (levelFilter == null || m.group == levelFilter) &&
                (query.isBlank() || m.name.contains(query.trim(), ignoreCase = true) || m.meta.contains(query.trim(), ignoreCase = true))
        }
    }
    val grouped = remember(filtered) { filtered.groupBy { it.group }.toSortedMap() }
    val levels = remember(models) { models.map { it.group }.distinct().sorted() }

    Column(Modifier.fillMaxSize()) {
        ReferenceSearchField(query, { query = it }, searchHint, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        FilterPillRow(levels, levelFilter, pathfinder, onSelect = { levelFilter = it })
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            grouped.forEach { (level, rows) ->
                item(key = "h$level") { SpellGroupHeader(groupLabel(level, pathfinder), rows.size) }
                items(rows, key = { it.id }) { model ->
                    SpellListRow(model) { detail = model }
                }
            }
        }
    }

    detail?.let { SpellDetailDialog(it, onDismiss = { detail = null }) }
}

@Composable
private fun ReferenceSearchField(query: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.natPalette
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = palette.inkMute, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f).padding(start = 8.dp)) {
            if (query.isEmpty()) {
                Text(hint, fontFamily = Cormorant, fontSize = 17.sp, color = palette.inkMute)
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = Cormorant, fontSize = 17.sp, color = palette.ink),
                cursorBrush = Brush.verticalGradient(listOf(palette.accent, palette.accent)),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Filled.Close, contentDescription = "Clear", tint = palette.inkMute,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onChange("") },
            )
        }
    }
}

@Composable
private fun FilterPillRow(levels: List<Int>, selected: Int?, pathfinder: Boolean, onSelect: (Int?) -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill("All", selected == null) { onSelect(null) }
        levels.forEach { lvl ->
            val label = when { lvl == 0 -> "Cantrip"; pathfinder -> "R $lvl"; else -> "Lv $lvl" }
            FilterPill(label, selected == lvl) { onSelect(if (selected == lvl) null else lvl) }
        }
    }
}

@Composable
private fun FilterPill(label: String, active: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (active) palette.accent else palette.tile)
            .border(1.dp, if (active) palette.accent else palette.ink.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            color = if (active) palette.cream else palette.ink,
        )
    }
}

@Composable
private fun SpellGroupHeader(title: String, count: Int) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(6.dp).clip(DiamondShape).background(palette.accent))
        Text(title.uppercase(), fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 3.sp, color = palette.accent)
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(palette.accent.copy(alpha = 0.4f), Color.Transparent))))
        Text("$count", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.inkMute)
    }
}

@Composable
private fun SpellListRow(model: SpellRowModel, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(palette.tile).border(1.dp, palette.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(model.monogram, fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = palette.accent)
            }
            Column(Modifier.weight(1f)) {
                Text(model.name, fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 19.sp, color = palette.ink, maxLines = 1)
                Text(model.meta, fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkMute, maxLines = 1)
            }
            if (model.badges.isNotEmpty()) SpellBadges(model.badges)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.inkMute, modifier = Modifier.size(18.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.ink.copy(alpha = 0.08f)))
    }
}

@Composable
private fun SpellBadges(badges: List<SpellBadge>) {
    val palette = MaterialTheme.natPalette
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        badges.forEach { badge ->
            Box(
                Modifier
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(if (badge.filled) palette.accent else Color.Transparent)
                    .border(1.dp, if (badge.filled) palette.accent else palette.inkMute.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    badge.letter,
                    fontFamily = Cinzel,
                    fontWeight = if (badge.filled) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 9.sp,
                    color = if (badge.filled) palette.cream else palette.inkMute,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SpellDetailDialog(model: SpellRowModel, onDismiss: () -> Unit) {
    val palette = MaterialTheme.natPalette
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(model.name, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = palette.accent) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(model.meta, fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = palette.inkMute)
                model.detailLines.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                    Text("$label: $value", fontFamily = Cormorant, fontSize = 15.sp, color = palette.inkSoft)
                }
                if (model.description.isNotBlank()) {
                    Text(model.description, fontFamily = au.com.evonet.nat20.ui.theme.EbGaramond, fontSize = 15.sp, color = palette.ink)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

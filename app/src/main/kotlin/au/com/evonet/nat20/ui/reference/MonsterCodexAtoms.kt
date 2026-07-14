package au.com.evonet.nat20.ui.reference

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * A ruleset-agnostic view of one bestiary statblock, so the three monster
 * codices (2014 / 2024 / PF2e) share one parchment renderer.
 */
data class MonsterView(
    val id: String,
    val name: String,
    val subtitle: String,
    val badge: String,
    val meta: List<Pair<String, String>>,
    val abilities: List<Pair<String, Int>>,
    val blocks: List<Pair<String, List<Pair<String, String>>>>,
)

/**
 * The shared parchment bestiary body: a rounded search field, a caller-supplied
 * filter-pill row, and a scrolling list of expandable statblock cards. Replaces
 * the per-edition stock-Material lists (parity #43).
 */
@Composable
fun MonsterCodexList(
    query: String,
    onQuery: (String) -> Unit,
    hint: String,
    monsters: List<MonsterView>,
    filterPills: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        ReferenceSearchField(query, onQuery, hint, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        ReferencePillRow { filterPills() }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(monsters, key = { it.id }) { m ->
                StatblockCard(m, expanded == m.id) { expanded = if (expanded == m.id) null else m.id }
            }
        }
    }
}

@Composable
private fun StatblockCard(m: MonsterView, expanded: Boolean, onToggle: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(m.name, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = palette.accent)
                if (m.subtitle.isNotBlank()) {
                    Text(m.subtitle, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkSoft)
                }
            }
            if (m.badge.isNotBlank()) {
                Text(m.badge, fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.5.sp, color = palette.accent)
            }
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(palette.accent.copy(alpha = 0.2f)))
                if (m.abilities.isNotEmpty()) AbilityRow(m.abilities)
                m.meta.filter { it.second.isNotBlank() }.forEach { (label, value) -> MetaLine(label, value) }
                m.blocks.filter { it.second.isNotEmpty() }.forEach { (title, entries) -> TextBlock(title, entries) }
            }
        }
    }
}

@Composable
private fun AbilityRow(abilities: List<Pair<String, Int>>) {
    val palette = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        abilities.forEach { (label, score) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, fontFamily = Cinzel, fontSize = 10.sp, letterSpacing = 1.sp, color = palette.inkMute)
                Text("$score", fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.ink)
                val mod = Math.floorDiv(score - 10, 2)
                Text(if (mod >= 0) "+$mod" else "$mod", fontFamily = EbGaramond, fontSize = 11.sp, color = palette.inkMute)
            }
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    val palette = MaterialTheme.natPalette
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label:", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 1.sp, color = palette.accent)
        Text(value, fontFamily = Cormorant, fontSize = 14.sp, color = palette.ink)
    }
}

@Composable
private fun TextBlock(title: String, entries: List<Pair<String, String>>) {
    val palette = MaterialTheme.natPalette
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title.uppercase(), fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.accent)
        entries.forEach { (name, desc) ->
            Text(
                buildString { if (name.isNotBlank()) append("$name. "); append(desc) },
                fontFamily = EbGaramond,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = palette.inkSoft,
            )
        }
    }
}

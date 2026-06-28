package au.com.evonet.nat20.ui.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.DiamondShape
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * Shared chrome for the reference browsers (Spell Library / Item Catalog),
 * ported from the iOS `ReferenceCodexChrome`: a capsule search field, capsule
 * filter/category pills, and a sectioned-list header with a fading rule + count.
 */

/** Capsule search field with a magnifier and a clear button (iOS `ReferenceCodexSearchField`). */
@Composable
internal fun ReferenceSearchField(query: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
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

/** A single capsule filter/category pill: accent fill + cream Cinzel when active (iOS `ReferenceCodexChip`). */
@Composable
internal fun ReferencePill(label: String, active: Boolean, onClick: () -> Unit) {
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

/** A horizontally-scrolling row of [ReferencePill]s. */
@Composable
internal fun ReferencePillRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

/** A grouped-list section header: diamond + small-caps label + fading rule + count (iOS `ReferenceCodexSectionHeader`). */
@Composable
internal fun ReferenceSectionHeader(title: String, count: Int) {
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

package au.com.evonet.nat20.ui.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.natPalette

/** One ruleset's slice of a reference browser — a labelled tab + its chromeless content. */
class ReferenceTab(val label: String, val content: @Composable () -> Unit)

/**
 * Chrome for a Settings reference browser (Spell Library, Item Catalog, Monster
 * Codex) with a ruleset tab bar along the top — a port of the iOS
 * `ReferenceTabShell`. The shell owns the back button, eyebrow + title, the
 * ornamental divider, and the ruleset tab switcher; each tab supplies a
 * chromeless catalogue body.
 */
@Composable
fun ReferenceTabShell(
    eyebrow: String,
    title: String,
    tabs: List<ReferenceTab>,
    onBack: () -> Unit,
) {
    var selection by remember { mutableStateOf(tabs.firstOrNull()?.label.orEmpty()) }
    val palette = MaterialTheme.natPalette

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Header: back circle on the left, centred eyebrow + title.
        Box(Modifier.fillMaxWidth().padding(top = 8.dp, start = 22.dp, end = 22.dp)) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(palette.tileStrong)
                    .border(1.dp, palette.accent, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = palette.accent)
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(eyebrow.uppercase(), fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 3.sp, color = palette.inkMute)
                Text(title, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = palette.ink)
            }
        }

        OrnamentalDivider(Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 8.dp), opacity = 0.4f)

        // Ruleset tab pills.
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEach { tab ->
                val active = tab.label == selection
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (active) palette.accent else palette.tile)
                        .border(1.dp, if (active) palette.accent else palette.ink.copy(alpha = 0.25f), CircleShape)
                        .clickable { selection = tab.label }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        tab.label.uppercase(),
                        fontFamily = Cinzel,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        color = if (active) palette.cream else palette.ink,
                    )
                }
            }
        }

        Box(Modifier.fillMaxSize().padding(top = 4.dp)) {
            (tabs.firstOrNull { it.label == selection } ?: tabs.firstOrNull())?.content?.invoke()
        }
    }
}

package au.com.evonet.nat20.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared parchment-chrome controls, ported from the iOS `DnD5eUICore` /
 * `SettingsView` components (PickerSection / SegmentedChoice / referenceTile),
 * so the settings, picker, and codex screens share one styled vocabulary.
 */

/** A section header: small-caps Cinzel accent label + a fading hairline rule (iOS `PickerSection`). */
@Composable
fun SectionLabel(label: String, modifier: Modifier = Modifier) {
    val accent = LocalNatPalette.current.accent
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 12.sp,
            letterSpacing = 3.sp,
            color = accent,
        )
        Box(
            Modifier
                .padding(start = 10.dp)
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.27f), Color.Transparent))),
        )
    }
}

/**
 * Pill-segmented chooser (iOS `SegmentedChoice`): capsule segments, the active
 * one filled accent with cream small-caps Cinzel, the rest a parchment tile with
 * a hairline. [equalWeight] lays the segments edge-to-edge across the full width
 * (Appearance); otherwise they flow at intrinsic width and wrap (Narration).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SegmentedPills(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    equalWeight: Boolean = true,
) {
    if (equalWeight) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                Pill(label(option), option == selected, Modifier.weight(1f)) { onSelect(option) }
            }
        }
    } else {
        FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                Pill(label(option), option == selected) { onSelect(option) }
            }
        }
    }
}

@Composable
private fun Pill(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = LocalNatPalette.current
    Box(
        modifier
            .clip(CircleShape)
            .background(if (active) palette.accent else palette.tile)
            .border(1.dp, if (active) palette.accent else palette.ink.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            fontFamily = Cinzel,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            color = if (active) palette.cream else palette.ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * A tappable settings/reference row tile (iOS `referenceTile`): Cormorant title +
 * italic subtitle on a parchment tile with a hairline border and a chevron.
 */
@Composable
fun SettingsTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true,
) {
    val palette = LocalNatPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = palette.ink)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkSoft)
            }
        }
        if (showChevron) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.inkMute)
        }
    }
}

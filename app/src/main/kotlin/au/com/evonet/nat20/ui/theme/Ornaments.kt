package au.com.evonet.nat20.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A rhombus, used for the codex/roster flourishes (iOS `DiamondShape`). */
val DiamondShape: Shape = object : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): Outline = Outline.Generic(
        Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height / 2f)
            close()
        },
    )
}

@Composable
fun Diamond(size: Dp, fill: Color) {
    Box(Modifier.size(size).clip(DiamondShape).background(fill))
}

/**
 * Thin gradient rule with a centred diamond (iOS `OrnamentalDivider`) — sits
 * between the hero and content, and in section headers throughout the codex.
 */
@Composable
fun OrnamentalDivider(modifier: Modifier = Modifier, opacity: Float = 0.4f) {
    val accent = LocalNatPalette.current.accent
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = opacity), Color.Transparent))),
        )
        Box(Modifier.padding(horizontal = 8.dp).size(8.dp).clip(DiamondShape).background(accent.copy(alpha = opacity)))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = opacity), Color.Transparent))),
        )
    }
}

/**
 * The bordered drop-cap initial (58×70): hairline outer rect, accent-gradient
 * inner rect with an accent border and a cream inner glow, Cormorant italic
 * letter — shared by the roster cards and the 2014 codex hero row (iOS
 * `DropCap`). Do not fork per-screen copies of this.
 */
@Composable
fun DropCapSquare(letter: String) {
    val palette = LocalNatPalette.current
    Box(Modifier.size(width = 58.dp, height = 70.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 58.dp, height = 70.dp)
                .border(0.8.dp, palette.accent.copy(alpha = 0.53f)),
        )
        Box(
            Modifier
                .size(width = 52.dp, height = 64.dp)
                .background(
                    Brush.linearGradient(listOf(palette.accent.copy(alpha = 0.20f), palette.accent.copy(alpha = 0.067f))),
                )
                .border(1.4.dp, palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter,
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                color = palette.accent,
            )
            // Inset cream highlight ring inside the accent border (iOS "inner glow").
            Box(
                Modifier
                    .matchParentSize()
                    .border(2.dp, Color(0xFFFFFADC).copy(alpha = 0.6f)),
            )
        }
    }
}

/**
 * Three diamonds with hairline rules between them — the flourish under the
 * welcome title on the roster (iOS `Rosette`).
 */
@Composable
fun Rosette(size: Dp = 16.dp) {
    val accent = LocalNatPalette.current.accent
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = size * 0.7f, height = size * 0.3f).clip(DiamondShape).background(accent.copy(alpha = 0.55f)))
        Spacer(Modifier.width(size * 0.18f))
        Box(Modifier.width(size * 0.45f).height(1.dp).background(accent.copy(alpha = 0.5f)))
        Spacer(Modifier.width(size * 0.06f))
        Box(Modifier.size(width = size * 0.9f, height = size * 0.7f).clip(DiamondShape).background(accent))
        Spacer(Modifier.width(size * 0.06f))
        Box(Modifier.width(size * 0.45f).height(1.dp).background(accent.copy(alpha = 0.5f)))
        Spacer(Modifier.width(size * 0.18f))
        Box(Modifier.size(width = size * 0.7f, height = size * 0.3f).clip(DiamondShape).background(accent.copy(alpha = 0.55f)))
    }
}

package au.com.evonet.nat20.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import au.com.evonet.nat20.R

/**
 * A4 theme: a light ink-on-parchment palette plus the two display typefaces
 * already bundled in `res/font`. This is deliberately modest — the hi-fi
 * parchment codex (textured ground, gold rules, dark candlelit variant) is the
 * A7e build. Here we just give the shell some character so the list/sheet don't
 * read as raw Material defaults.
 */

private val Cinzel = FontFamily(Font(R.font.cinzel))
private val EbGaramond = FontFamily(
    Font(R.font.eb_garamond),
    Font(R.font.eb_garamond_italic, style = FontStyle.Italic),
)

private val InkOnParchment = lightColorScheme(
    primary = Color(0xFF8A6D2F),
    onPrimary = Color(0xFFFBF6E9),
    secondary = Color(0xFF6B5836),
    background = Color(0xFFF4ECD8),
    onBackground = Color(0xFF2B2118),
    surface = Color(0xFFF8F1DE),
    onSurface = Color(0xFF2B2118),
    surfaceVariant = Color(0xFFE9DDC1),
    onSurfaceVariant = Color(0xFF5A4B33),
    outline = Color(0xFFB8A475),
)

private fun natTypography(): Typography {
    val base = Typography()
    return base.copy(
        headlineLarge = base.headlineLarge.copy(fontFamily = Cinzel),
        headlineMedium = base.headlineMedium.copy(fontFamily = Cinzel),
        headlineSmall = base.headlineSmall.copy(fontFamily = Cinzel),
        titleLarge = base.titleLarge.copy(fontFamily = Cinzel),
        titleMedium = base.titleMedium.copy(fontFamily = Cinzel),
        bodyLarge = base.bodyLarge.copy(fontFamily = EbGaramond),
        bodyMedium = base.bodyMedium.copy(fontFamily = EbGaramond),
        labelLarge = base.labelLarge.copy(fontFamily = EbGaramond),
        labelMedium = base.labelMedium.copy(fontFamily = EbGaramond),
    )
}

@Composable
fun Nat20Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InkOnParchment,
        typography = natTypography(),
        content = content,
    )
}

package au.com.evonet.nat20.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import au.com.evonet.nat20.R

/**
 * The 5e ink-on-parchment design system, ported from the iOS `DnD5eUICore`
 * `Theme`/`ParchmentBackground` (the canonical spec). Light = ink-on-parchment;
 * dark = the candlelit gold-on-black variant. Every token resolves at theme
 * build time from the `darkTheme` flag (Compose has no UIKit dynamic colours),
 * so the whole app re-tints when appearance flips.
 *
 * Material 3's `ColorScheme` only has a handful of slots, so the full 14-token
 * palette + the translucent tile/track surfaces live on [NatPalette], provided
 * through [LocalNatPalette] and read via `MaterialTheme.natPalette`.
 */

// ── Palette ────────────────────────────────────────────────────────────────────

/** The full design palette (iOS `Theme.Palette`), resolved for one appearance. */
data class NatPalette(
    val parchment: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkMute: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentGold: Color,
    val accentBrown: Color,
    val accentBlood: Color,
    val cream: Color,
    val danger: Color,
    /** Translucent warm surface tints layered over the parchment ground. */
    val tile: Color,
    val tileStrong: Color,
    /** HP-bar / progress empty track. */
    val track: Color,
)

private val LightPalette = NatPalette(
    parchment = Color(0xFFE8D6A8),
    ink = Color(0xFF2A1A08),
    inkSoft = Color(0xFF5A4422),
    inkMute = Color(0xFF8A7146),
    accent = Color(0xFF7A3B1D),
    accentDeep = Color(0xFF5C2E15),
    accentGold = Color(0xFF8E6A2A),
    accentBrown = Color(0xFF3E2B18),
    accentBlood = Color(0xFF6E1A1A),
    cream = Color(0xFFF5E6C4),
    danger = Color(0xFF7A1F1F),
    tile = Color(red = 1f, green = 250f / 255f, blue = 225f / 255f, alpha = 0.45f),
    tileStrong = Color(red = 1f, green = 250f / 255f, blue = 225f / 255f, alpha = 0.62f),
    track = Color(red = 60f / 255f, green = 30f / 255f, blue = 10f / 255f, alpha = 0.14f),
)

private val DarkPalette = NatPalette(
    parchment = Color(0xFF0E0B07),
    ink = Color(0xFFE8D7AD),
    inkSoft = Color(0xFFB59C6B),
    inkMute = Color(0xFF97855A),
    accent = Color(0xFFC7A24E),
    accentDeep = Color(0xFF9B7C32),
    accentGold = Color(0xFFCBAA5C),
    accentBrown = Color(0xFF5A4A30),
    accentBlood = Color(0xFF9E3A30),
    cream = Color(0xFF1B1408),
    danger = Color(0xFFA8453A),
    tile = Color(red = 1f, green = 244f / 255f, blue = 214f / 255f, alpha = 0.05f),
    tileStrong = Color(red = 1f, green = 244f / 255f, blue = 214f / 255f, alpha = 0.10f),
    track = Color(red = 1f, green = 245f / 255f, blue = 220f / 255f, alpha = 0.12f),
)

val LocalNatPalette = staticCompositionLocalOf { LightPalette }

/** Read the full parchment palette anywhere inside [Nat20Theme]. */
val MaterialTheme.natPalette: NatPalette
    @Composable @ReadOnlyComposable get() = LocalNatPalette.current

// Material colour slots derived from the palette, so stock Material components
// (dialogs, text fields, chips, scaffolds) already read in-theme.
private fun NatPalette.materialColors(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = accent, onPrimary = cream,
        secondary = accentGold, onSecondary = cream,
        tertiary = accentBlood, onTertiary = cream,
        background = parchment, onBackground = ink,
        surface = Color(0xFF221C14), onSurface = ink,
        surfaceVariant = Color(0xFF2A2114), onSurfaceVariant = inkSoft,
        // Container roles (used by chips, FABs) — warm, never the baseline purple.
        primaryContainer = Color(0xFF2E2614), onPrimaryContainer = ink,
        secondaryContainer = Color(0xFF332B19), onSecondaryContainer = ink,
        tertiaryContainer = Color(0xFF35221C), onTertiaryContainer = ink,
        // Override every container role so stray Material Cards read warm, not the baseline purple.
        surfaceBright = Color(0xFF3A2F1C), surfaceDim = Color(0xFF0E0B07),
        surfaceContainerLowest = Color(0xFF0A0805), surfaceContainerLow = Color(0xFF181309),
        surfaceContainer = Color(0xFF1F190F), surfaceContainerHigh = Color(0xFF2A2114),
        surfaceContainerHighest = Color(0xFF342A19),
        outline = inkMute, outlineVariant = accentBrown,
        error = danger, onError = cream,
    )
} else {
    lightColorScheme(
        primary = accent, onPrimary = cream,
        secondary = accentGold, onSecondary = cream,
        tertiary = accentBlood, onTertiary = cream,
        background = parchment, onBackground = ink,
        surface = cream, onSurface = ink,
        surfaceVariant = Color(0xFFEFE2BC), onSurfaceVariant = inkSoft,
        primaryContainer = Color(0xFFEAD6A6), onPrimaryContainer = ink,
        secondaryContainer = Color(0xFFE3CF9C), onSecondaryContainer = ink,
        tertiaryContainer = Color(0xFFE9C9B0), onTertiaryContainer = ink,
        surfaceBright = Color(0xFFF5E6C4), surfaceDim = Color(0xFFDCC9A0),
        surfaceContainerLowest = Color(0xFFF8EFD6), surfaceContainerLow = Color(0xFFF3E8C9),
        surfaceContainer = Color(0xFFEEE2BC), surfaceContainerHigh = Color(0xFFE8DAB0),
        surfaceContainerHighest = Color(0xFFE2D3A4),
        outline = inkMute, outlineVariant = accentGold,
        error = danger, onError = cream,
    )
}

// ── Typefaces (iOS `Theme.Typeface` roles) ──────────────────────────────────────

/** Cinzel — small-caps section labels, level names, button text. */
val Cinzel = FontFamily(
    Font(R.font.cinzel, FontWeight.Normal),
    Font(R.font.cinzel, FontWeight.Bold),
)

/** Cormorant Garamond — character names, big numbers, body display (variable weights). */
val Cormorant = FontFamily(
    Font(R.font.cormorant_garamond, FontWeight.Normal),
    Font(R.font.cormorant_garamond, FontWeight.Medium),
    Font(R.font.cormorant_garamond, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic),
)

/** EB Garamond — body paragraphs. */
val EbGaramond = FontFamily(
    Font(R.font.eb_garamond, FontWeight.Normal),
    Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
)

/** IM Fell English — mottos, journal italics, small captions. */
val ImFell = FontFamily(
    Font(R.font.im_fell_english_regular, FontWeight.Normal),
    Font(R.font.im_fell_english_italic, FontWeight.Normal, FontStyle.Italic),
)

/** IM Fell English SC — small-caps variant for captions. */
val ImFellSmallCaps = FontFamily(Font(R.font.im_fell_english_sc_regular))

private fun natTypography(): Typography {
    val base = Typography()
    return base.copy(
        // Display / titles → Cinzel (the engraved small-caps display face).
        displayLarge = base.displayLarge.copy(fontFamily = Cinzel),
        displayMedium = base.displayMedium.copy(fontFamily = Cinzel),
        displaySmall = base.displaySmall.copy(fontFamily = Cinzel),
        headlineLarge = base.headlineLarge.copy(fontFamily = Cinzel),
        headlineMedium = base.headlineMedium.copy(fontFamily = Cinzel),
        headlineSmall = base.headlineSmall.copy(fontFamily = Cinzel),
        // Titles / names → Cormorant (character names, big numbers).
        titleLarge = base.titleLarge.copy(fontFamily = Cormorant, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = Cormorant, fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontFamily = Cormorant, fontWeight = FontWeight.Medium),
        // Body → EB Garamond.
        bodyLarge = base.bodyLarge.copy(fontFamily = EbGaramond),
        bodyMedium = base.bodyMedium.copy(fontFamily = EbGaramond),
        bodySmall = base.bodySmall.copy(fontFamily = EbGaramond),
        // Labels (section headers, chips, buttons) → Cinzel small-caps feel.
        labelLarge = base.labelLarge.copy(fontFamily = Cinzel),
        labelMedium = base.labelMedium.copy(fontFamily = Cinzel),
        labelSmall = base.labelSmall.copy(fontFamily = ImFellSmallCaps),
    )
}

@Composable
fun Nat20Theme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalNatPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.materialColors(darkTheme),
            typography = natTypography(),
            content = content,
        )
    }
}

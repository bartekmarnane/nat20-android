package au.com.evonet.nat20.ui.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.DiamondShape
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.SectionLabel
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * Shared chrome for the full-screen action follow-up pickers (parity #19,
 * port of iOS `PickerShell`): a full-screen dialog on the parchment ground
 * with a CANCEL affordance left, a centred kicker/title stack, an ornamental
 * divider, a scrollable body (h22 gutters), and an optional footer pinned to
 * the bottom over a cream gradient. Pickers with keyboard-heavy bodies put
 * their commit button inline at the end of the body instead of the footer.
 */
@Composable
internal fun ActionPickerShell(
    kicker: String,
    title: String,
    onCancel: () -> Unit,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(palette.parchment)
                .statusBarsPadding()
                .imePadding(),
        ) {
            // ── Top nav: CANCEL left over a centred kicker/title stack ──
            Box(Modifier.fillMaxWidth().padding(top = 8.dp, start = 22.dp, end = 22.dp)) {
                Text(
                    "CANCEL",
                    fontFamily = Cinzel,
                    fontSize = 11.sp,
                    letterSpacing = 2.5.sp,
                    color = palette.inkSoft,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onCancel)
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                )
                Column(
                    Modifier.align(Alignment.Center).padding(horizontal = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        kicker.uppercase(),
                        fontFamily = Cinzel,
                        fontSize = 11.sp,
                        letterSpacing = 3.sp,
                        color = palette.inkMute,
                        maxLines = 1,
                    )
                    Text(
                        title,
                        fontFamily = Cormorant,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = palette.ink,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }

            OrnamentalDivider(
                Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 4.dp),
                opacity = 0.4f,
            )

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 24.dp),
            ) {
                content()
            }

            if (footer != null) {
                val hairline = palette.ink.copy(alpha = 0.13f)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .drawBehind { drawRect(color = hairline, size = Size(size.width, 1.dp.toPx())) }
                        .background(
                            Brush.verticalGradient(
                                listOf(palette.cream.copy(alpha = 0.5f), palette.cream.copy(alpha = 0.98f)),
                            ),
                        )
                        .navigationBarsPadding()
                        .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 10.dp),
                ) {
                    footer()
                }
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

// ── Shared picker atoms ────────────────────────────────────────────────────────

/** Small-caps Cinzel section label + fading accent rule between picker blocks (iOS `PickerSection`). */
@Composable
internal fun PickerSection(label: String, top: Dp = 18.dp, bottom: Dp = 8.dp) {
    SectionLabel(label, Modifier.padding(top = top, bottom = bottom))
}

/** Muted Cormorant-italic helper paragraph under a section or field (iOS `pickerHelpText`). */
@Composable
internal fun PickerHelpText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontFamily = Cormorant,
        fontStyle = FontStyle.Italic,
        fontSize = 15.sp,
        color = MaterialTheme.natPalette.inkSoft,
        modifier = modifier,
    )
}

/** Serif capsule chip (damage types, class dice): Cormorant 13sp, filled when active. */
@Composable
internal fun PickerChip(
    text: String,
    active: Boolean,
    tone: Color = MaterialTheme.natPalette.accent,
    onClick: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (active) tone else palette.tile)
            .border(1.dp, if (active) tone else palette.ink.copy(alpha = 0.2f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text,
            fontFamily = Cormorant,
            fontSize = 13.sp,
            color = if (active) palette.cream else palette.ink,
            maxLines = 1,
        )
    }
}

/** Small-caps capsule chip (conditions, outcome judgments): Cinzel 11sp, filled when active. */
@Composable
internal fun SmallCapsChip(
    text: String,
    active: Boolean,
    tone: Color = MaterialTheme.natPalette.accent,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Box(
        modifier
            .clip(CircleShape)
            .background(if (active) tone else palette.tile)
            .border(1.dp, tone.copy(alpha = if (active) 1f else 0.4f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            fontFamily = Cinzel,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = if (active) palette.cream else tone,
            maxLines = 1,
        )
    }
}

/** Centred ceremonial glyph for confirm-style pickers (iOS hero glyph: rings + diamond + dot). */
@Composable
internal fun PickerHeroGlyph() {
    val palette = MaterialTheme.natPalette
    Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(68.dp)
                .border(0.8.dp, palette.accent.copy(alpha = 0.55f), CircleShape),
        )
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(palette.accent.copy(alpha = 0.067f))
                .border(1.2.dp, palette.accent, CircleShape),
        )
        Box(Modifier.size(38.dp).border(1.2.dp, palette.accent, DiamondShape))
        Box(Modifier.size(8.dp).clip(CircleShape).background(palette.accent))
    }
}

/** "This will" bullet row: small diamond + Cormorant 13sp on a parchment tile. */
@Composable
internal fun EffectBulletRow(text: String) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(4.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.13f), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(DiamondShape).background(palette.accent))
        Spacer(Modifier.size(10.dp))
        Text(text, fontFamily = Cormorant, fontSize = 13.sp, color = palette.ink)
    }
}

/** Rounded parchment row container shared by picker list rows. */
@Composable
internal fun PickerRow(
    modifier: Modifier = Modifier,
    borderTone: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.tile)
            .border(1.dp, borderTone ?: palette.ink.copy(alpha = 0.13f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        content = content,
    )
}

/** Vertical space helper so pickers don't sprinkle raw Spacers. */
@Composable
internal fun PickerGap(height: Dp) {
    Spacer(Modifier.height(height))
}

// ── Outcome + defeated atoms (slice B, iOS `OutcomeChipRow` / `DefeatedToggle`) ─

/** One selectable outcome in an [OutcomeChipRow]. */
internal data class OutcomeOption<T>(val value: T, val label: String, val tone: Color)

/**
 * Equal-width row of small-caps outcome chips (Failure=danger / Unjudged=accent
 * / Success=accentGold on iOS; attack pickers reuse it for Miss/Hit/Critical).
 */
@Composable
internal fun <T> OutcomeChipRow(
    selection: T,
    options: List<OutcomeOption<T>>,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            SmallCapsChip(
                option.label,
                active = selection == option.value,
                tone = option.tone,
                modifier = Modifier.weight(1f),
            ) { onSelect(option.value) }
        }
    }
}

/** "Was the target defeated?" two-chip toggle (iOS `DefeatedToggle`). */
@Composable
internal fun DefeatedToggle(defeated: Boolean, onChange: (Boolean) -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SmallCapsChip("Still standing", active = !defeated, tone = palette.accent, modifier = Modifier.weight(1f)) {
            onChange(false)
        }
        SmallCapsChip("Defeated", active = defeated, tone = palette.danger, modifier = Modifier.weight(1f)) {
            onChange(true)
        }
    }
}

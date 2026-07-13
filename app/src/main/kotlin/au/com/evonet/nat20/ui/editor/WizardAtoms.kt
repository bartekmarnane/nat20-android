package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The wizard's shared step-body atoms — the port of iOS `DnD5eUICore`'s
 * `Wizard*` controls (field labels, stat cells, the level stepper, segmented
 * pickers, parchment text fields, chip pickers, quote blocks, detail-card
 * chrome). [WizardShell] carries the chrome; these carry the step content.
 * Package-public so the 2024 / PF2e wizards restyle onto the same atoms.
 */

// ── Field label ───────────────────────────────────────────────────────────────

/** Small-caps field caption with optional required star and trailing italic hint. */
@Composable
fun WizardFieldLabel(label: String, required: Boolean = false, hint: String? = null) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.5.sp,
            color = palette.inkMute,
        )
        if (required) {
            Text("*", fontFamily = Cinzel, fontSize = 11.sp, color = palette.accent, modifier = Modifier.padding(start = 2.dp))
        }
        if (hint != null) {
            Spacer(Modifier.weight(1f))
            Text(
                hint,
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = palette.inkMute,
                maxLines = 1,
            )
        }
    }
}

// ── Stat cell ─────────────────────────────────────────────────────────────────

/** A small labelled fact tile (Bonus / Size / Speed / Skills … on detail cards). */
@Composable
fun WizardStatCell(label: String, value: String, danger: Boolean = false, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.natPalette
    val border = (if (danger) palette.danger else palette.accent).copy(alpha = 0.2f)
    Column(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .background(palette.tile)
            .border(1.dp, border, RoundedCornerShape(3.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.inkMute,
        )
        Text(
            value,
            fontFamily = Cormorant,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = if (danger) palette.danger else palette.ink,
        )
    }
}

// ── Level stepper ─────────────────────────────────────────────────────────────

/** −/+ stepper row with an italic leading label and a big accent value (1–20). */
@Composable
fun WizardLevelStepper(label: String, value: Int, range: IntRange = 1..20, onChange: (Int) -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tileStrong)
            .border(1.dp, palette.ink.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = palette.inkSoft,
            modifier = Modifier.weight(1f),
        )
        StepperButton("−", enabled = value > range.first) { onChange(value - 1) }
        Text(
            "$value",
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = palette.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 56.dp),
        )
        StepperButton("+", enabled = value < range.last) { onChange(value + 1) }
    }
}

/** A 32×32 tile-filled −/+ button (shared by the level stepper and mini steppers). */
@Composable
fun StepperButton(glyph: String, enabled: Boolean = true, size: Int = 32, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .graphicsLayer(alpha = if (enabled) 1f else 0.35f)
            .size(size.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = palette.ink)
    }
}

// ── Segmented control ─────────────────────────────────────────────────────────

/** Radio segmented control: small-caps segments, active = cream on accent. */
@Composable
fun WizardSegmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
    ) {
        options.forEachIndexed { index, option ->
            val active = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .background(if (active) palette.accent else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 7.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.uppercase(),
                    fontFamily = Cinzel,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = if (active) palette.cream else palette.ink,
                    maxLines = 1,
                )
            }
        }
    }
}

// ── Text field ────────────────────────────────────────────────────────────────

/** Parchment text field: Cormorant on a tileStrong well (italic when multiline). */
@Composable
fun WizardTextField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    multiline: Boolean = false,
    lineLimit: Int = 3,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.natPalette
    val style = TextStyle(
        fontFamily = Cormorant,
        fontSize = 16.sp,
        fontStyle = if (multiline) FontStyle.Italic else FontStyle.Normal,
        color = palette.ink,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        singleLine = !multiline,
        minLines = if (multiline) lineLimit else 1,
        maxLines = if (multiline) Int.MAX_VALUE else 1,
        cursorBrush = SolidColor(palette.accent),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tileStrong)
            .border(1.dp, palette.ink.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = style.copy(color = palette.inkMute.copy(alpha = 0.8f)))
                }
                inner()
            }
        },
    )
}

// ── Chips ─────────────────────────────────────────────────────────────────────

/** A single capsule chip (shared by the single- and multi-select pickers). */
@Composable
fun WizardChip(
    label: String,
    selected: Boolean,
    large: Boolean = false,
    enabled: Boolean = true,
    dimmed: Boolean = false,
    strongFill: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .graphicsLayer(alpha = if (dimmed) 0.35f else 1f)
            .clip(CircleShape)
            .background(if (selected) palette.accent else if (strongFill) palette.tileStrong else palette.tile)
            .border(1.dp, if (selected) palette.accent else palette.ink.copy(alpha = 0.2f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = if (large) 13.dp else 11.dp,
                vertical = if (large) 7.dp else 6.dp,
            ),
    ) {
        Text(
            label,
            fontFamily = Cormorant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = if (large) 15.sp else 13.sp,
            color = if (selected) palette.cream else palette.ink,
            maxLines = 1,
        )
    }
}

/** Wrap-flow single-select chip picker (races, backgrounds, alignments-style lists). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> WizardChipsPicker(
    items: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    large: Boolean = false,
    onPick: (T) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
    ) {
        items.forEach { item ->
            WizardChip(label = label(item), selected = isSelected(item), large = large) { onPick(item) }
        }
    }
}

/** Multi-select chips with a quota: unselected chips dim + disable once it's met. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> WizardMultiSelectChips(
    items: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    quotaFull: Boolean,
    enabled: (T) -> Boolean = { true },
    onToggle: (T) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
    ) {
        items.forEach { item ->
            val selected = isSelected(item)
            val blocked = !enabled(item) || (!selected && quotaFull)
            WizardChip(
                label = label(item),
                selected = selected,
                enabled = !blocked,
                dimmed = blocked,
                strongFill = true,
            ) { onToggle(item) }
        }
    }
}

// ── Selection dot ─────────────────────────────────────────────────────────────

/** 18dp stroked circle with an accent core when selected (spell rows, edition tiles). */
@Composable
fun WizardSelectionDot(selected: Boolean) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .size(18.dp)
            .border(1.4.dp, palette.accent.copy(alpha = if (selected) 1f else 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(palette.accent))
        }
    }
}

// ── Quote block ───────────────────────────────────────────────────────────────

/** Trait/feature quote block: optional name, italic Cormorant body, tile fill. */
@Composable
fun WizardQuoteBlock(title: String?, body: String) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .padding(10.dp),
    ) {
        if (!title.isNullOrEmpty()) {
            Text(
                title,
                fontFamily = Cormorant,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = palette.ink,
            )
        }
        if (body.isNotEmpty()) {
            Text(
                body,
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = palette.inkSoft,
            )
        }
    }
}

// ── Detail card ───────────────────────────────────────────────────────────────

/** Race/class/background detail chrome: accent gradient fill + accent hairline. */
@Composable
fun WizardDetailCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(listOf(palette.accent.copy(alpha = 0.06f), Color.Transparent)),
            )
            .border(1.4.dp, palette.accent.copy(alpha = 0.33f), RoundedCornerShape(4.dp))
            .padding(14.dp),
        content = content,
    )
}

// ── Sub-section card ──────────────────────────────────────────────────────────

/** Advancements-style sub-card: Cormorant title + italic counter over a tile. */
@Composable
fun WizardSubSectionCard(
    title: String,
    counter: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                fontFamily = Cormorant,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = palette.ink,
                modifier = Modifier.weight(1f),
            )
            if (counter != null) {
                Text(
                    counter,
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    color = palette.inkMute,
                )
            }
        }
        content()
    }
}

// ── Review atoms (shared by every ruleset's Review step) ─────────────────────

/** Review section header: small-caps accent title, fading rule, optional pencil-jump button. */
@Composable
fun ReviewSectionHeader(title: String, onEdit: (() -> Unit)? = null) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            color = palette.accent,
        )
        Box(
            Modifier
                .padding(horizontal = 10.dp)
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(palette.accent.copy(alpha = 0.33f), Color.Transparent))),
        )
        if (onEdit != null) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(palette.tileStrong)
                    .border(1.dp, palette.accent.copy(alpha = 0.4f), CircleShape)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit $title",
                    tint = palette.accent,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

/** A single labelled Review fact line ("—" when empty). */
@Composable
fun ReviewLine(label: String, value: String) {
    val palette = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.inkMute,
            modifier = Modifier.width(110.dp).padding(top = 2.dp),
        )
        if (value.isEmpty()) {
            Text("—", fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = palette.inkMute)
        } else {
            Text(value, fontFamily = Cormorant, fontSize = 15.sp, color = palette.ink)
        }
    }
}

/** The bordered drop-cap initial from the roster hero, reused on Review steps. */
@Composable
fun ReviewDropCap(letter: String) {
    val palette = MaterialTheme.natPalette
    Box(Modifier.size(width = 58.dp, height = 70.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 58.dp, height = 70.dp).border(0.8.dp, palette.accent.copy(alpha = 0.53f)))
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
            Box(Modifier.matchParentSize().border(2.dp, Color(0xFFFFFADC).copy(alpha = 0.6f)))
        }
    }
}

/** A background-granted skill: locked accent-gold capsule, not toggleable. */
@Composable
fun GrantedSkillChip(label: String) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .clip(CircleShape)
            .background(palette.accentGold.copy(alpha = 0.10f))
            .border(1.dp, palette.accentGold.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = "Granted by background",
            tint = palette.accentGold,
            modifier = Modifier.size(10.dp),
        )
        Text(label, fontFamily = Cormorant, fontSize = 13.sp, color = palette.ink)
    }
}

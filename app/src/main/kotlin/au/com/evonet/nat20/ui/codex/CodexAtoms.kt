package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.core.ACOverrideFormula
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.Diamond
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The 2014 codex atom vocabulary (parity #13–18), ported 1:1 from the iOS
 * `DnD5eUICore` page atoms: SectionHead fading rules + naked content instead of
 * card-everything, StatChips, the HP block + bar, concentric ability medallions,
 * save cells, slot/coin tiles, quote and feature blocks, dashed rules/notices.
 * Tokens follow the port spec (Cinzel 11sp small-caps labels, Cormorant values,
 * radius 3 chips/tiles · 4 buttons/notices · 8 minion cards, accent hairlines).
 */

// ── Section chrome ───────────────────────────────────────────────────────────

/** Fading-rule section header: rule → TITLE (Cinzel 11sp, tracking 4) → rule. */
@Composable
internal fun SectionHead(title: String, top: Dp = 24.dp, bottom: Dp = 12.dp) {
    val accent = MaterialTheme.natPalette.accent
    Row(
        Modifier.fillMaxWidth().padding(top = top, bottom = bottom),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.weight(1f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.4f)))),
        )
        Text(
            title.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 4.sp,
            color = accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Box(
            Modifier.weight(1f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.4f), Color.Transparent))),
        )
    }
}

/** Dashed hairline rule between list rows (skills), [1.5, 2] accent@.13. */
@Composable
internal fun DashedRule(alpha: Float = 0.13f) {
    val color = MaterialTheme.natPalette.accent.copy(alpha = alpha)
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color,
            Offset(0f, center.y),
            Offset(size.width, center.y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 2.dp.toPx()), 0f),
        )
    }
}

/** Solid hairline row rule (attacks table), accent@.2. */
@Composable
internal fun HairlineRule(alpha: Float = 0.2f) {
    val color = MaterialTheme.natPalette.accent.copy(alpha = alpha)
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}

/** Rounded-rect dashed border ([4, 3]) for empty-state notices. */
internal fun Modifier.dashedBorder(color: Color, radius: Dp, strokeWidth: Dp = 1.dp): Modifier =
    drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f),
            ),
        )
    }

/** Dashed empty-state notice (tile fill, r4, dashed accent@.33, IM Fell italic). */
@Composable
internal fun DashedNotice(text: String) {
    val p = MaterialTheme.natPalette
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(p.tile)
            .dashedBorder(p.accent.copy(alpha = 0.33f), 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = p.inkMute,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

// ── Chips & tiles ────────────────────────────────────────────────────────────

/** Label-over-value stat chip (Init/AC/Prof/Speed rows). */
@Composable
internal fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    Column(
        modifier
            .clip(shape)
            .background(p.tile)
            .border(1.dp, p.accent.copy(alpha = if (highlight) 0.45f else 0.2f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.5.sp,
            color = p.inkMute,
            maxLines = 1,
        )
        Text(
            value,
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = p.accent,
            maxLines = 1,
        )
    }
}

/** Full-width small-caps stroked action button (Browse spells / Rest / Level Up …). */
@Composable
internal fun CodexButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val p = MaterialTheme.natPalette
    val tone = if (danger) p.danger else p.accent
    val shape = RoundedCornerShape(3.dp)
    Row(
        modifier
            .clip(shape)
            .background(p.tile)
            .border(1.dp, tone.copy(alpha = if (enabled) 1f else 0.4f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.5.sp,
            color = tone,
            maxLines = 1,
        )
    }
}

/** Full-width capsule CTA (plus glyph + ADD ITEM). */
@Composable
internal fun CodexCapsuleButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(p.tileStrong)
            .border(1.2.dp, p.accent.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("+", fontFamily = Cormorant, fontSize = 15.sp, color = p.accent)
        Spacer(Modifier.width(6.dp))
        Text(
            text.uppercase(),
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.5.sp,
            color = p.accent,
        )
    }
}

/** Coin tile: ABBREV over amount, five across the purse row. */
@Composable
internal fun CoinTile(abbrev: String, amount: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    Column(
        modifier
            .clip(shape)
            .background(p.tile)
            .border(1.dp, p.accent.copy(alpha = 0.2f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            abbrev.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = p.inkMute,
        )
        Text(
            amount.toString(),
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = if (amount > 0) p.accent else p.inkMute,
        )
    }
}

/** Spell-slot tile: "LVL n" over remaining-of-total dots. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SlotTile(level: Int, remaining: Int, total: Int, onClick: (() -> Unit)? = null) {
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    Column(
        Modifier
            .width(56.dp)
            .clip(shape)
            .background(p.tile)
            .border(1.dp, p.accent.copy(alpha = 0.2f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            "LVL $level",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = p.inkMute,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .then(
                            if (i < remaining) Modifier.background(p.accent)
                            else Modifier.border(1.dp, p.accent.copy(alpha = 0.4f), CircleShape),
                        ),
                )
            }
        }
    }
}

// ── HP ───────────────────────────────────────────────────────────────────────

/** HIT POINTS header + gradient bar; compact (Stats) or large (Combat) chrome. */
@Composable
internal fun HPBlock(current: Int, max: Int, temporary: Int, compact: Boolean) {
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (compact) p.tile else p.tileStrong)
            .border(1.dp, p.accent.copy(alpha = if (compact) 0.2f else 0.33f), shape)
            .padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 10.dp else 14.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "HIT POINTS",
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = if (compact) 2.5.sp else 3.sp,
                color = p.inkMute,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.weight(1f))
            if (temporary > 0) {
                Box(
                    Modifier
                        .padding(end = 8.dp)
                        .border(1.dp, p.accent.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        "+$temporary temp",
                        fontFamily = Cormorant,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp,
                        color = p.accent,
                    )
                }
            }
            Text(
                current.toString(),
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 22.sp else 28.sp,
                color = p.accent,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                " / $max",
                fontFamily = Cormorant,
                fontSize = 13.sp,
                color = p.inkSoft.copy(alpha = 0.6f),
                modifier = Modifier.alignByBaseline(),
            )
        }
        HPBar(current, max, height = if (compact) 7.dp else 8.dp)
    }
}

@Composable
internal fun HPBar(current: Int, max: Int, height: Dp) {
    val p = MaterialTheme.natPalette
    val pct = if (max > 0) (current.toFloat() / max).coerceIn(0f, 1f) else 0f
    val shape = RoundedCornerShape(2.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(p.track)
            .border(1.dp, p.accent.copy(alpha = 0.2f), shape),
    ) {
        if (pct > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct)
                    .background(
                        Brush.horizontalGradient(listOf(p.accent.copy(alpha = 0.67f), p.accent)),
                    ),
            )
        }
    }
}

// ── Abilities & saves ────────────────────────────────────────────────────────

/** Concentric-ring ability medallion (100dp): abbrev / score / modifier. */
@Composable
internal fun AbilityMedallion(
    abbrev: String,
    score: Int,
    modifierValue: Int,
    onClick: (() -> Unit)? = null,
) {
    val p = MaterialTheme.natPalette
    Box(
        Modifier
            .size(100.dp)
            .then(
                if (onClick != null) Modifier.clip(CircleShape).clickable(onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(100.dp)) {
            val c = center
            // Outer whisper ring Ø92.
            drawCircle(
                p.accent.copy(alpha = 0.53f * 0.55f),
                radius = 46.dp.toPx(),
                center = c,
                style = Stroke(0.8.dp.toPx()),
            )
            // Tile disc Ø84 + accent ring.
            drawCircle(p.tile, radius = 42.dp.toPx(), center = c)
            drawCircle(
                p.accent.copy(alpha = 0.53f),
                radius = 42.dp.toPx(),
                center = c,
                style = Stroke(1.dp.toPx()),
            )
            // Dashed inner circle Ø72.
            drawCircle(
                p.accent.copy(alpha = 0.45f),
                radius = 36.dp.toPx(),
                center = c,
                style = Stroke(
                    0.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(1.dp.toPx(), 1.5.dp.toPx()),
                        0f,
                    ),
                ),
            )
            // Compass dots on the main ring.
            val r = 42.dp.toPx()
            listOf(
                Offset(c.x, c.y - r), Offset(c.x + r, c.y),
                Offset(c.x, c.y + r), Offset(c.x - r, c.y),
            ).forEach { drawCircle(p.accent, radius = 1.6.dp.toPx(), center = it) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                abbrev.uppercase(),
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 2.5.sp,
                color = p.inkMute,
            )
            Text(
                score.toString(),
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                color = p.ink,
            )
            Text(
                modifierValue.signed(),
                fontFamily = EbGaramond,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = p.inkMute,
            )
        }
    }
}

/** One saving-throw cell (six across): prof dot / ABBREV / bonus. */
@Composable
internal fun SaveCell(
    abbrev: String,
    bonus: Int,
    proficient: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    Column(
        modifier
            .clip(shape)
            .background(p.tile)
            .border(1.dp, p.accent.copy(alpha = 0.2f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        if (proficient) {
            Box(Modifier.size(3.dp).clip(CircleShape).background(p.accent))
        } else {
            Spacer(Modifier.size(3.dp))
        }
        Text(
            abbrev.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = p.inkMute,
        )
        Text(
            bonus.signed(),
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = p.ink,
        )
    }
}

/** Skill-row proficiency pip: ring untrained / filled proficient / diamond expertise. */
@Composable
internal fun SkillPip(proficient: Boolean, expertise: Boolean) {
    val p = MaterialTheme.natPalette
    Box(Modifier.size(10.dp), contentAlignment = Alignment.Center) {
        when {
            expertise -> Diamond(size = 10.dp, fill = p.accent)
            proficient -> Box(Modifier.size(7.dp).clip(CircleShape).background(p.accent))
            else -> Box(Modifier.size(7.dp).border(1.dp, p.accent.copy(alpha = 0.33f), CircleShape))
        }
    }
}

// ── Prose blocks ─────────────────────────────────────────────────────────────

/** Feature entry: small-caps accent title over an EB Garamond body. */
@Composable
internal fun FeatureBlock(title: String, body: String) {
    val p = MaterialTheme.natPalette
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = p.accent,
        )
        if (body.isNotBlank()) {
            Text(
                body,
                fontFamily = EbGaramond,
                fontSize = 12.sp,
                color = p.inkSoft,
            )
        }
    }
}

/** Lore quote block: leading accent bar, small-caps title, italic Cormorant text. */
@Composable
internal fun QuoteBlock(title: String, text: String) {
    val p = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 5.dp)) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(p.accent.copy(alpha = 0.53f)))
        Column(Modifier.padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title.uppercase(),
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = p.accent,
            )
            Text(
                text,
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = p.ink,
            )
        }
    }
}

/** Identity grid cell: small-caps label over a Cormorant value ("—" when empty). */
@Composable
internal fun IdentityCell(label: String, value: String?, modifier: Modifier = Modifier) {
    val p = MaterialTheme.natPalette
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = p.inkMute,
        )
        Text(
            value?.takeIf { it.isNotBlank() } ?: "—",
            fontFamily = Cormorant,
            fontSize = 15.sp,
            color = p.ink,
        )
    }
}

// ── Inspiration ──────────────────────────────────────────────────────────────

/** Gradient inspiration banner shown while the DM's reroll is held. */
@Composable
internal fun InspirationBanner(modifier: Modifier = Modifier) {
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(p.accent.copy(alpha = 0.13f), Color.Transparent)))
            .border(1.dp, p.accent.copy(alpha = 0.4f), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Diamond(size = 10.dp, fill = p.accent)
        Text(
            "INSPIRATION",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            color = p.accent,
            modifier = Modifier.padding(start = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            "granted — spend before your next rest",
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = p.inkSoft,
        )
    }
}

// ── Active-effect chips ──────────────────────────────────────────────────────

/** Buff / debuff / mixed tint per the codex palette (accent / danger / gold). */
@Composable
internal fun ActiveEffect.codexTone(): Color {
    val p = MaterialTheme.natPalette
    val negative = modifiers.any {
        (it is EffectModifier.AcBonus && it.value < 0) || (it is EffectModifier.AttackBonus && it.value < 0) ||
            (it is EffectModifier.DamageBonus && it.value < 0) || (it is EffectModifier.SaveBonus && it.value < 0) ||
            (it is EffectModifier.AbilityDelta && it.value < 0) || it is EffectModifier.Condition
    }
    val positive = modifiers.any {
        (it is EffectModifier.AcBonus && it.value > 0) || it is EffectModifier.AcOverride ||
            (it is EffectModifier.AttackBonus && it.value > 0) || (it is EffectModifier.DamageBonus && it.value > 0) ||
            (it is EffectModifier.SaveBonus && it.value > 0) || it is EffectModifier.DamageResistance ||
            it is EffectModifier.AdvantageOn
    }
    return when {
        negative && positive -> p.accentGold
        negative -> p.danger
        else -> p.accent
    }
}

/** Compact impact label for an effect modifier ("+2 AC", "resist fire", …). */
internal fun EffectModifier.impactLabel(): String = when (this) {
    is EffectModifier.AcBonus -> "${value.signed()} AC"
    is EffectModifier.AcOverride -> when (val f = formula) {
        is ACOverrideFormula.BaseDex -> "AC = ${f.base} + DEX"
        is ACOverrideFormula.BaseDexAbility -> "AC = ${f.base} + DEX + ${f.ability.abbreviation}"
    }
    is EffectModifier.AbilityDelta -> "${value.signed()} ${ability.abbreviation}"
    is EffectModifier.AbilitySet -> "${ability.abbreviation} = $value"
    is EffectModifier.SaveBonus -> "${value.signed()} ${ability?.abbreviation ?: "all"} save"
    is EffectModifier.AttackBonus -> "${value.signed()} attack"
    is EffectModifier.DamageBonus -> "${value.signed()} damage"
    is EffectModifier.SkillBonus -> "${value.signed()} ${if (skillId == "__any__") "one" else skillId} skill"
    is EffectModifier.DamageResistance -> "resist $type"
    is EffectModifier.AdvantageOn -> "adv: $descriptor"
    is EffectModifier.Condition -> name
    is EffectModifier.FreeText -> text
}

/** Diamond-glyph effect chip: NAME · impact, tinted by buff/debuff/mixed tone. */
@Composable
internal fun CodexEffectChip(effect: ActiveEffect, onClick: () -> Unit) {
    val tone = effect.codexTone()
    val p = MaterialTheme.natPalette
    val shape = RoundedCornerShape(3.dp)
    Row(
        Modifier
            .clip(shape)
            .background(tone.copy(alpha = 0.08f))
            .border(1.dp, tone.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Diamond(size = 6.dp, fill = tone)
        Text(
            effect.name.uppercase(),
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = tone,
            modifier = Modifier.padding(start = 6.dp),
        )
        val impact = effect.modifiers.joinToString(" · ") { it.impactLabel() }.takeIf { it.isNotBlank() }
        if (impact != null) {
            Text(
                " · $impact",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = p.inkSoft,
                maxLines = 1,
            )
        }
        (effect.duration as? EffectDuration.Rounds)?.let { r ->
            Text(
                "  ${r.count} rd",
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = tone,
            )
        }
    }
}

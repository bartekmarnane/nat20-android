package au.com.evonet.nat20.ui.roll

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.core.DiceRoller
import au.com.evonet.nat20.dnd5e.core.Keep
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollResult
import au.com.evonet.nat20.dnd5e.core.RollSpec
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** The forest-green / wine-red tints for advantage / disadvantage kept dice. */
private val AdvantageGreen = Color(0xFF246B29)
private val DisadvantageRed = Color(0xFF8C1A1A)

private enum class Phase { IDLE, ROLLING, SETTLED }
private enum class Mode { DISADVANTAGE, NORMAL, ADVANTAGE }

/**
 * The die-roll animation primitive (A16): a ROLL button that tumbles the dice,
 * settles on the result, streams in the bonus chips while a total ticker counts
 * up, and flourishes on a natural 20 / 1. Port of the iOS `RollResultView`.
 *
 * [baseSpec] is the normal roll (usually `1d20`); when [allowAdvantageToggle]
 * and it's a d20, a Disadvantage / Normal / Advantage selector swaps in the
 * 2d20-keep variant and re-rolls. [onSettled] fires with the final [RollResult].
 */
@Composable
fun RollResultView(
    baseSpec: RollSpec,
    bonuses: List<RollBonus> = emptyList(),
    allowAdvantageToggle: Boolean = true,
    luckyReroll: Boolean = false,
    onSettled: (RollResult) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var mode by remember { mutableStateOf(Mode.NORMAL) }
    var phase by remember { mutableStateOf(Phase.IDLE) }
    var rollKey by remember { mutableIntStateOf(0) }
    var displayed by remember { mutableStateOf(baseSpec.dieList()) }
    var result by remember { mutableStateOf<RollResult?>(null) }
    var revealedBonuses by remember { mutableIntStateOf(0) }
    var wasLucky by remember { mutableStateOf(false) }

    val canToggle = allowAdvantageToggle && baseSpec.faces == 20 && baseSpec.count == 1
    val spec = if (canToggle) mode.specFor(baseSpec) else baseSpec

    LaunchedEffect(rollKey) {
        if (rollKey == 0) return@LaunchedEffect
        // Halfling Lucky: a natural 1 on a d20 is rerolled once, and the new roll must be used.
        val first = DiceRoller.roll(spec, bonuses, Random)
        val rolled = if (luckyReroll && first.naturalD20 == 1) DiceRoller.roll(spec, bonuses, Random) else first
        wasLucky = rolled !== first
        phase = Phase.ROLLING
        revealedBonuses = 0
        val frames = 16
        for (i in 1..frames) {
            displayed = List(spec.count) { Random.nextInt(1, spec.faces + 1) }
            val progress = i.toFloat() / frames
            val eased = 1f - (1f - progress).pow(3) // easeOutCubic — intervals stretch as it settles
            delay((25 + eased * 95).toLong())
        }
        displayed = rolled.dice
        phase = Phase.SETTLED
        result = rolled
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        for (b in bonuses.indices) {
            delay(300)
            revealedBonuses = b + 1
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onSettled(rolled)
    }

    fun reroll() {
        result = null
        rollKey++
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            spec.displayNotation.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (canToggle && phase != Phase.ROLLING) {
            ModeSelector(mode) { picked -> mode = picked; if (phase == Phase.SETTLED) reroll() }
        }

        if (phase == Phase.IDLE) {
            RollButton(onClick = { reroll() })
        } else {
            DiceRow(displayed = displayed, spec = spec, settled = phase == Phase.SETTLED, mode = if (canToggle) mode else Mode.NORMAL)
            if (phase == Phase.SETTLED) {
                result?.let { Flourish(it) }
                if (wasLucky) {
                    Text("Halfling Lucky — rerolled a 1", style = MaterialTheme.typography.labelSmall, color = AdvantageGreen)
                }
                TotalLine(result, revealedBonuses)
                if (bonuses.isNotEmpty()) BonusChips(bonuses, revealedBonuses)
                RerollButton(onClick = { reroll() })
            }
        }
    }
}

@Composable
private fun RollButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text("ROLL", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun DiceRow(displayed: List<Int>, spec: RollSpec, settled: Boolean, mode: Mode) {
    val kept = if (settled) spec.keptIndices(displayed) else displayed.indices.toSet()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        displayed.forEachIndexed { i, value ->
            DieFace(value = value, kept = i in kept, settled = settled, mode = mode)
        }
    }
}

@Composable
private fun DieFace(value: Int, kept: Boolean, settled: Boolean, mode: Mode) {
    val scale by animateFloatAsState(
        targetValue = if (settled && kept) 1f else if (settled) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "dieScale",
    )
    val tint = when {
        !settled || !kept -> MaterialTheme.colorScheme.onSurface
        mode == Mode.ADVANTAGE -> AdvantageGreen
        mode == Mode.DISADVANTAGE -> DisadvantageRed
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        Modifier
            .size(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint.copy(alpha = if (settled && !kept) 0.25f else 1f),
        )
    }
}

@Composable
private fun TotalLine(result: RollResult?, revealedBonuses: Int) {
    val keptSum = result?.keptSum ?: 0
    val bonusPart = result?.bonuses?.take(revealedBonuses)?.sumOf { it.value } ?: 0
    val flat = if (result?.bonuses.isNullOrEmpty()) (result?.modifier ?: 0) else bonusPart
    val total by animateIntAsState(targetValue = keptSum + flat, label = "total")
    Text(
        "◆  $total  ◆",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun BonusChips(bonuses: List<RollBonus>, revealed: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        bonuses.forEachIndexed { i, bonus ->
            AnimatedVisibility(
                visible = i < revealed,
                enter = slideInHorizontally { it / 2 } + fadeIn(),
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${bonus.label} ${bonus.sign}${bonus.magnitude}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Flourish(result: RollResult) {
    val label = when {
        result.isNatural20 -> "CRIT!"
        result.isNatural1 -> "FUMBLE"
        else -> null
    } ?: return
    Box(contentAlignment = Alignment.Center) {
        if (result.isNatural20) CritBurst()
        AnimatedVisibility(visible = true, enter = scaleIn(spring(dampingRatio = 0.4f)) + fadeIn()) {
            Text(
                label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = if (result.isNatural20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** A one-shot radial spark burst behind a natural-20 (A16 crit flourish polish). */
@Composable
private fun CritBurst() {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val p by animateFloatAsState(targetValue = if (started) 1f else 0f, animationSpec = tween(700), label = "critBurst")
    val color = MaterialTheme.colorScheme.primary
    val sparks = 12
    Canvas(Modifier.fillMaxWidth().height(44.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension * 0.95f
        for (i in 0 until sparks) {
            val angle = (2.0 * Math.PI * i / sparks).toFloat()
            val r = maxR * p
            val pos = Offset(center.x + cos(angle) * r, center.y + sin(angle) * r)
            drawCircle(color = color.copy(alpha = (1f - p).coerceIn(0f, 1f)), radius = (1f - p) * 6f + 1f, center = pos)
        }
    }
}

@Composable
private fun RerollButton(onClick: () -> Unit) {
    Text(
        "Roll again",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { onClick() }.padding(4.dp),
    )
}

@Composable
private fun ModeSelector(mode: Mode, onPick: (Mode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Mode.entries.forEach { option ->
            val active = option == mode
            val tint = when (option) {
                Mode.ADVANTAGE -> AdvantageGreen
                Mode.DISADVANTAGE -> DisadvantageRed
                Mode.NORMAL -> MaterialTheme.colorScheme.primary
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) tint else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPick(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val Mode.label: String
    get() = when (this) {
        Mode.DISADVANTAGE -> "Disadv."
        Mode.NORMAL -> "Normal"
        Mode.ADVANTAGE -> "Advantage"
    }

private fun Mode.specFor(base: RollSpec): RollSpec = when (this) {
    Mode.NORMAL -> base
    Mode.ADVANTAGE -> RollSpec.advantage(base.faces, base.modifier)
    Mode.DISADVANTAGE -> RollSpec.disadvantage(base.faces, base.modifier)
}

private fun RollSpec.dieList(): List<Int> = List(count) { faces } // resting faces before first roll

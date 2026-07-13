package au.com.evonet.nat20.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.R
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.Diamond
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.LocalNatPalette
import au.com.evonet.nat20.ui.theme.Nat20MarkD20Iso
import au.com.evonet.nat20.ui.theme.Nat20Wordmark
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import kotlinx.coroutines.launch

/**
 * The first-run onboarding flow (A12): four skippable, swipeable parchment
 * pages — Welcome / Characters / Campaigns / The Seal — a faithful port of the
 * iOS `OnboardingView`: small-caps SKIP pill, ruled section label, Cormorant
 * italic display title, brand-mark vignettes, EB Garamond body, diamond page
 * dots, and the filled small-caps CONTINUE button. The Characters and
 * Campaigns pages carry AI-aware copy when on-device AI is available.
 * [onComplete] persists the completion flag and dismisses.
 */
@Composable
fun OnboardingScreen(aiAvailable: Boolean, onComplete: () -> Unit) {
    val palette = LocalNatPalette.current
    val pages = onboardingPages(aiAvailable)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    fun advance() {
        if (isLast) onComplete() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }

    Column(Modifier.fillMaxSize()) {
        // Top bar — SKIP, available on every page.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                "SKIP",
                fontFamily = Cinzel,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = palette.inkMute,
                modifier = Modifier
                    .border(1.dp, palette.inkMute.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                    .clickable(onClick = onComplete)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }

        // Swipeable page content between the fixed chrome.
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
            PageContent(pages[index])
        }

        // Fixed footer — diamond page dots + CONTINUE.
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                pages.indices.forEach { i ->
                    val active = i == pagerState.currentPage
                    Diamond(
                        size = if (active) 9.dp else 5.dp,
                        fill = palette.accent.copy(alpha = if (active) 1f else 0.3f),
                    )
                }
            }
            PrimaryActionButton("Continue") { advance() }
        }
    }
}

// ── Page body ─────────────────────────────────────────────────────────────────

@Composable
private fun PageContent(page: OnboardingPage) {
    val palette = LocalNatPalette.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp).widthIn(max = 460.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        // Section label flanked by short rules.
        Row(Modifier.width(200.dp), verticalAlignment = Alignment.CenterVertically) {
            LabelRule(Modifier.weight(1f))
            Text(
                page.label.uppercase(),
                fontFamily = Cinzel,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                color = palette.inkMute,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            LabelRule(Modifier.weight(1f))
        }

        Text(
            page.title,
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            color = palette.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp),
        )

        Spacer(Modifier.weight(1f))

        page.art()

        Spacer(Modifier.weight(1f))

        Text(
            page.body,
            fontFamily = EbGaramond,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = palette.inkSoft,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun LabelRule(modifier: Modifier) {
    Box(modifier.height(1.dp).background(LocalNatPalette.current.accent.copy(alpha = 0.45f)))
}

// ── Shared vignette frame ─────────────────────────────────────────────────────

/**
 * The thin double-ruled parchment box the vignettes sit inside (iOS
 * `codexPlate`): tile fill, 1dp accent rule, and a fainter rule inset 4dp.
 */
@Composable
private fun Modifier.codexPlate(): Modifier {
    val palette = LocalNatPalette.current
    val outer = palette.accent.copy(alpha = 0.45f)
    val innerRule = palette.accent.copy(alpha = 0.25f)
    return background(palette.tile).drawBehind {
        drawRect(color = outer, style = Stroke(width = 1.dp.toPx()))
        inset(4.dp.toPx()) {
            drawRect(color = innerRule, style = Stroke(width = 0.6.dp.toPx()))
        }
    }
}

// ── 1. Welcome — brand lockup ─────────────────────────────────────────────────

@Composable
private fun WelcomeArt() {
    val palette = LocalNatPalette.current
    Box(Modifier.size(width = 200.dp, height = 188.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Nat20MarkD20Iso(size = 78.dp, fg = palette.accent)
            Nat20Wordmark(size = 24.sp, color = palette.accent, withDiamonds = false)
        }
    }
}

// ── 2. Characters — the die ───────────────────────────────────────────────────

@Composable
private fun CharactersArt() {
    val palette = LocalNatPalette.current
    Box(Modifier.size(width = 240.dp, height = 188.dp), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Icon(
            painterResource(R.drawable.die),
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(108.dp),
        )
    }
}

// ── 3. Campaigns — the live journal ───────────────────────────────────────────

/**
 * Mirrors the real campaign journal: a roman-numeral Session header flanked by
 * fading rules, then two timed entries with event glyphs, divided by a dotted
 * rule, all on a codex plate.
 */
@Composable
private fun CampaignsArt(ai: Boolean) {
    val palette = LocalNatPalette.current
    val entries = if (ai) {
        listOf(
            JournalEntry("07:28", Glyph.BURST, "A goblin's blade nicked Sparks' arm — a scratch that wounded his pride far more than his flesh."),
            JournalEntry("07:31", Glyph.SPARKLE, "With a flick of the wrist he loosed a bolt of fire, and the goblin was unmade in ash."),
        )
    } else {
        listOf(
            JournalEntry("07:28", Glyph.BURST, "Took 4 slashing damage — goblin."),
            JournalEntry("07:31", Glyph.SPARKLE, "Cast Fire Bolt — goblin defeated."),
        )
    }

    Column(
        Modifier.width(320.dp).codexPlate().padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SessionRule(Modifier.weight(1f), clearOnLeft = true)
                Text(
                    "SESSION II",
                    fontFamily = Cinzel,
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                    color = palette.accent,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                SessionRule(Modifier.weight(1f), clearOnLeft = false)
            }
            Text(
                "3rd of Flamerule",
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = palette.inkSoft,
            )
        }

        Column {
            JournalRow(entries[0])
            JournalDottedRule()
            JournalRow(entries[1])
        }
    }
}

private data class JournalEntry(val time: String, val glyph: Glyph, val text: String)
private enum class Glyph { BURST, SPARKLE }

@Composable
private fun SessionRule(modifier: Modifier, clearOnLeft: Boolean) {
    val accent = LocalNatPalette.current.accent.copy(alpha = 0.4f)
    val colors = if (clearOnLeft) listOf(Color.Transparent, accent) else listOf(accent, Color.Transparent)
    Box(modifier.height(1.dp).background(Brush.horizontalGradient(colors)))
}

@Composable
private fun JournalRow(entry: JournalEntry) {
    val palette = LocalNatPalette.current
    Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            entry.time,
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = palette.inkMute,
            modifier = Modifier.width(34.dp),
        )
        Box(Modifier.width(14.dp).padding(top = 2.dp), contentAlignment = Alignment.Center) {
            when (entry.glyph) {
                Glyph.BURST -> BurstGlyph(palette.accent)
                Glyph.SPARKLE -> SparkleGlyph(palette.accent)
            }
        }
        Text(
            entry.text,
            fontFamily = Cormorant,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            color = palette.ink,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun JournalDottedRule() {
    val ink = LocalNatPalette.current.ink.copy(alpha = 0.2f)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 56.dp)
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color = ink,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 2.5.dp.toPx())),
                )
            },
    )
}

/** An eight-spoke burst (stands in for the iOS `burst.fill` damage glyph). */
@Composable
private fun BurstGlyph(color: Color) {
    Canvas(Modifier.size(11.dp)) {
        val c = center
        val rOuter = size.minDimension / 2
        val rInner = rOuter * 0.45f
        repeat(8) { i ->
            val angle = Math.toRadians(i * 45.0)
            val dir = Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat())
            drawLine(
                color = color,
                start = c + dir * rInner,
                end = c + dir * rOuter,
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawCircle(color = color, radius = rInner * 0.55f, center = c)
    }
}

/** A four-point sparkle (stands in for the iOS `sparkles` cast glyph). */
@Composable
private fun SparkleGlyph(color: Color) {
    Canvas(Modifier.size(11.dp)) {
        val c = center
        val r = size.minDimension / 2
        val waist = r * 0.22f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(c.x, c.y - r)
            quadraticBezierTo(c.x + waist, c.y - waist, c.x + r, c.y)
            quadraticBezierTo(c.x + waist, c.y + waist, c.x, c.y + r)
            quadraticBezierTo(c.x - waist, c.y + waist, c.x - r, c.y)
            quadraticBezierTo(c.x - waist, c.y - waist, c.x, c.y - r)
            close()
        }
        drawPath(path, color = color)
    }
}

// ── 4. The Seal — a sealed character sheet ────────────────────────────────────

@Composable
private fun SealArt() {
    val palette = LocalNatPalette.current
    Box(Modifier.padding(end = 14.dp, bottom = 16.dp), contentAlignment = Alignment.BottomEnd) {
        Column(
            Modifier.size(width = 220.dp, height = 150.dp).codexPlate().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Sera Vane",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = palette.ink,
            )
            Text(
                "FIGHTER · LEVEL 5",
                fontFamily = Cinzel,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                color = palette.inkMute,
            )
            Column(
                Modifier.padding(top = 8.dp, end = 36.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                repeat(3) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.inkSoft.copy(alpha = 0.3f)))
                }
            }
        }
        WaxSeal(56.dp, Modifier.offset(x = 14.dp, y = 16.dp))
    }
}

/** A round wax seal stamped with an italic "20". */
@Composable
private fun WaxSeal(size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val palette = LocalNatPalette.current
    Box(
        modifier
            .size(size)
            .shadow(3.dp, CircleShape, spotColor = palette.accentBrown, ambientColor = palette.accentBrown)
            .drawBehind {
                // Off-centre highlight on the wax (iOS: centre 0.38/0.34, radius 0.75×).
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.accent, palette.accentDeep),
                        center = Offset(this.size.width * 0.38f, this.size.height * 0.34f),
                        radius = this.size.width * 0.75f,
                    ),
                    radius = this.size.minDimension / 2,
                )
            }
            .padding(4.dp)
            .border(1.5.dp, palette.accentBrown.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "20",
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.42f).sp,
            color = palette.cream,
        )
    }
}

// ── Page data (AI-aware) ──────────────────────────────────────────────────────

private data class OnboardingPage(val label: String, val title: String, val body: String, val art: @Composable () -> Unit)

private fun onboardingPages(ai: Boolean): List<OnboardingPage> = listOf(
    OnboardingPage(
        label = "Welcome",
        title = "Every legend starts as a blank page.",
        body = "Nat20 keeps your characters, campaigns, and stories in one codex. A few minutes from now, you'll have a hero.",
        art = { WelcomeArt() },
    ),
    OnboardingPage(
        label = "Characters",
        title = if (ai) "Forge a hero by hand — or conjure one whole." else "Forge a hero, choice by choice.",
        body = if (ai) {
            "Build by hand, step by step, or let Nat20 conjure a ready-to-play hero in moments — across all three rulesets: 5th Edition (2014), 5th Edition (2024), and Pathfinder 2e. Until a campaign begins, every detail is yours to change."
        } else {
            "Build your hero by hand, choice by choice, across all three rulesets: 5th Edition (2014), 5th Edition (2024), and Pathfinder 2e. Until a campaign begins, every detail is yours to change."
        },
        art = { CharactersArt() },
    ),
    OnboardingPage(
        label = "Campaigns",
        title = if (ai) "Play on, and the story writes itself." else "Play on, and the journal keeps the tale.",
        body = if (ai) {
            "Start a campaign and your actions become journal entries — damage taken, treasure found, nights survived. Entry by entry, they bind into a storied chronicle of the whole adventure."
        } else {
            "Start a campaign and your actions become journal entries — damage taken, treasure found, nights survived — kept in order, session by session, for the whole adventure."
        },
        art = { CampaignsArt(ai) },
    ),
    OnboardingPage(
        label = "The Seal",
        title = "When the adventure begins, the ink dries.",
        body = "Once your character enters a campaign, the sheet is sealed — no edits until the campaign ends. Change happens the honest way: through play.",
        art = { SealArt() },
    ),
)

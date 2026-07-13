package au.com.evonet.nat20.ui.codex

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.ui.identity.CharacterPortraitFrame
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.Diamond
import au.com.evonet.nat20.ui.theme.NatPalette
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.natPalette
import kotlinx.coroutines.launch

/**
 * The payload-agnostic codex chrome (parity #12 / #24): the full iOS
 * `CodexShellView` scaffold — nav row (back circle, diamond-flanked current-tab
 * title, Act/Edit pill), hero row (rect drop cap + name + subtitle lines),
 * campaign region (Start Adventuring / journal bar), ornamental divider — over a
 * caller-owned [HorizontalPager] with the diamond-indicator tab bar pinned below.
 *
 * Both the 2014 [CodexShellView] and the 2024 `Codex2024ShellView` render through
 * this one scaffold; only the per-tab page bodies differ. The caller owns the
 * [PagerState] (so it can programmatically scroll to a tab, e.g. from an actions
 * layer) and supplies the tab labels, hero copy, campaign phase, and page bodies.
 */
@Composable
fun CodexScaffold(
    pagerState: PagerState,
    tabLabels: List<String>,
    heroName: String,
    heroSubtitleLines: List<String>,
    portraitData: ByteArray?,
    fallbackLetter: String,
    editableHero: Boolean,
    onEditIdentity: () -> Unit,
    inCampaign: Boolean,
    campaignName: String?,
    hasPastAdventures: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStartCampaign: () -> Unit,
    onEndCampaign: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPastAdventures: () -> Unit,
    modifier: Modifier = Modifier,
    /** In-campaign Act pill target; null ⇒ no pill (edition without an actions layer yet). */
    onAct: (() -> Unit)? = null,
    /**
     * When false the diamond tab bar is hidden and the body is a single page
     * ([pageContent] called with page 0) — the PF2e single-scroll sheet (#34).
     * Defaults true so the 2014/2024 paged codices are unaffected.
     */
    showTabBar: Boolean = true,
    pageContent: @Composable (page: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        TopNavRow(
            title = tabLabels[pagerState.currentPage].uppercase(),
            inCampaign = inCampaign,
            onAct = onAct,
            onBack = onBack,
            onEdit = onEdit,
        )
        HeroRow(
            name = heroName,
            subtitleLines = heroSubtitleLines,
            portraitData = portraitData,
            fallbackLetter = fallbackLetter,
            editable = editableHero,
            onEditIdentity = onEditIdentity,
        )
        CampaignRegion(
            inCampaign = inCampaign,
            campaignName = campaignName,
            hasPastAdventures = hasPastAdventures,
            onStartCampaign = onStartCampaign,
            onEndCampaign = onEndCampaign,
            onOpenJournal = onOpenJournal,
            onOpenPastAdventures = onOpenPastAdventures,
        )
        OrnamentalDivider(Modifier.padding(horizontal = 22.dp, vertical = 20.dp))

        if (showTabBar) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page -> pageContent(page) }

            CodexTabBar(
                tabs = tabLabels,
                selected = pagerState.currentPage,
                onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
            )
        } else {
            // Single-scroll body (PF2e #34): no pager, no tab bar.
            Box(Modifier.weight(1f).fillMaxWidth()) { pageContent(0) }
        }
    }
}

// ── Top nav row ────────────────────────────────────────────────────────────────

@Composable
private fun TopNavRow(
    title: String,
    inCampaign: Boolean,
    onAct: (() -> Unit)?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, start = 22.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Back: chevron in a hairline-stroked circle.
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(palette.tileStrong)
                .border(1.dp, palette.accent, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = palette.accent,
                modifier = Modifier.size(18.dp),
            )
        }

        // Centre: diamond-flanked CURRENT TAB title, synced with the pager.
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Diamond(size = 5.dp, fill = palette.accent)
            Text(
                title,
                fontFamily = Cinzel,
                fontSize = 12.sp,
                letterSpacing = 5.sp,
                color = palette.accent,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Diamond(size = 5.dp, fill = palette.accent)
        }

        // Trailing: Act pill in a campaign (when the edition has an actions layer),
        // Edit pill while building. (iOS also shows a gear → character settings here;
        // omitted until the #40 Character Settings screen exists.)
        when {
            inCampaign && onAct != null -> ActPill(palette, onAct)
            !inCampaign -> EditPill(palette, onEdit)
            else -> Spacer(Modifier.size(0.dp))
        }
    }
}

@Composable
private fun ActPill(palette: NatPalette, onAct: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(palette.accent.copy(alpha = 0.067f))
            .border(1.dp, palette.accent.copy(alpha = 0.53f), shape)
            .clickable(onClick = onAct)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Drawn circle + plus glyph (iOS draws this rather than using an SF symbol).
        val accent = palette.accent
        Canvas(Modifier.size(13.dp)) {
            val stroke = 1.2.dp.toPx()
            drawCircle(color = accent, radius = size.minDimension / 2f - stroke / 2f, style = Stroke(stroke))
            val r = size.minDimension * 0.26f
            drawLine(accent, Offset(center.x - r, center.y), Offset(center.x + r, center.y), stroke)
            drawLine(accent, Offset(center.x, center.y - r), Offset(center.x, center.y + r), stroke)
        }
        Text(
            "ACT",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.accent,
        )
    }
}

@Composable
private fun EditPill(palette: NatPalette, onEdit: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(palette.tileStrong)
            .border(1.dp, palette.accent.copy(alpha = 0.33f), shape)
            .clickable(onClick = onEdit)
            .padding(vertical = 8.dp, horizontal = 14.dp),
    ) {
        Text(
            "EDIT",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.accent,
        )
    }
}

// ── Hero row ───────────────────────────────────────────────────────────────────

@Composable
private fun HeroRow(
    name: String,
    subtitleLines: List<String>,
    portraitData: ByteArray?,
    fallbackLetter: String,
    editable: Boolean,
    onEditIdentity: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (editable) Modifier.clickable(onClick = onEditIdentity) else Modifier)
            .padding(top = 16.dp, start = 22.dp, end = 22.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CharacterPortraitFrame(
            portraitData = portraitData,
            fallbackLetter = fallbackLetter,
        )
        Column(Modifier.weight(1f)) {
            Text(
                name,
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                color = palette.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitleLines.forEach { line ->
                Text(
                    line,
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = palette.inkSoft,
                    maxLines = 1,
                )
            }
        }
    }
}

// ── Campaign region ────────────────────────────────────────────────────────────

@Composable
private fun CampaignRegion(
    inCampaign: Boolean,
    campaignName: String?,
    hasPastAdventures: Boolean,
    onStartCampaign: () -> Unit,
    onEndCampaign: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPastAdventures: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 14.dp, start = 22.dp, end = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val palette = MaterialTheme.natPalette
        if (inCampaign) {
            CampaignJournalBar(palette, campaignName ?: "Campaign", onOpenJournal, onEndCampaign)
        } else {
            StartAdventuringButton(palette, onStartCampaign)
            if (hasPastAdventures) {
                PastAdventuresRow(palette, onOpenPastAdventures)
            }
        }
    }
}

@Composable
private fun StartAdventuringButton(palette: NatPalette, onStartCampaign: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        palette.accent.copy(alpha = 0.2f),
                        palette.accent.copy(alpha = 0.1f),
                        palette.accent.copy(alpha = 0.2f),
                    ),
                ),
            )
            .border(1.5.dp, palette.accent, shape)
            .clickable(onClick = onStartCampaign)
            .padding(vertical = 12.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Diamond(size = 8.dp, fill = palette.accent)
        Text(
            "START ADVENTURING",
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 4.sp,
            color = palette.accent,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Diamond(size = 8.dp, fill = palette.accent)
    }
}

@Composable
private fun PastAdventuresRow(palette: NatPalette, onOpenPastAdventures: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, palette.ink.copy(alpha = 0.45f), shape)
            .clickable(onClick = onOpenPastAdventures)
            .padding(vertical = 9.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        BookGlyph(palette.ink)
        Spacer(Modifier.width(8.dp))
        Text(
            "PAST ADVENTURES",
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            color = palette.ink,
            maxLines = 1,
        )
    }
}

/** Small drawn open-book glyph (no book icon in the core Material set). */
@Composable
private fun BookGlyph(color: Color) {
    Canvas(Modifier.size(width = 13.dp, height = 11.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.1.dp.toPx()
        val path = Path().apply {
            moveTo(w / 2f, h * 0.18f)
            cubicTo(w * 0.36f, h * 0.02f, w * 0.12f, h * 0.02f, 0f, h * 0.12f)
            lineTo(0f, h * 0.88f)
            cubicTo(w * 0.12f, h * 0.78f, w * 0.36f, h * 0.78f, w / 2f, h * 0.94f)
            cubicTo(w * 0.64f, h * 0.78f, w * 0.88f, h * 0.78f, w, h * 0.88f)
            lineTo(w, h * 0.12f)
            cubicTo(w * 0.88f, h * 0.02f, w * 0.64f, h * 0.02f, w / 2f, h * 0.18f)
        }
        drawPath(path, color = color, style = Stroke(stroke))
        drawLine(color, Offset(w / 2f, h * 0.18f), Offset(w / 2f, h * 0.94f), stroke)
    }
}

@Composable
private fun CampaignJournalBar(
    palette: NatPalette,
    campaignName: String,
    onOpenJournal: () -> Unit,
    onEndCampaign: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            campaignName,
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            color = palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // Android-only: End stays reachable from the bar until the campaign
        // settings screen (#37) can own it — iOS ends campaigns from settings.
        Text(
            "End",
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = palette.inkMute,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onEndCampaign)
                .padding(horizontal = 6.dp, vertical = 6.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .border(1.2.dp, palette.accent, RoundedCornerShape(4.dp))
                .clickable(onClick = onOpenJournal)
                .padding(vertical = 6.dp, horizontal = 12.dp),
        ) {
            Text(
                "JOURNAL",
                fontFamily = Cinzel,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                color = palette.accent,
            )
        }
    }
}

// ── Tab bar ────────────────────────────────────────────────────────────────────

@Composable
private fun CodexTabBar(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val palette = MaterialTheme.natPalette
    val hairline = palette.accent.copy(alpha = 0.2f)
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(palette.cream.copy(alpha = 0.6f), palette.cream.copy(alpha = 0.96f)),
                ),
            )
            .drawBehind { drawRect(color = hairline, size = Size(size.width, 1.dp.toPx())) }
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        tabs.forEachIndexed { index, tab ->
            val active = index == selected
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(8.dp), contentAlignment = Alignment.Center) {
                    if (active) {
                        Diamond(size = 8.dp, fill = palette.accent)
                    } else {
                        Box(
                            Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(palette.inkMute.copy(alpha = 0.5f)),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    tab.uppercase(),
                    fontFamily = Cinzel,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = if (active) palette.accent else palette.inkMute,
                    maxLines = 1,
                )
            }
        }
    }
}

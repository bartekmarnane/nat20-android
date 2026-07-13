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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.ui.actions.ActionRoute
import au.com.evonet.nat20.ui.actions.DnD5eActionsLayer
import au.com.evonet.nat20.ui.slugToTitle
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.Diamond
import au.com.evonet.nat20.ui.theme.DropCapSquare
import au.com.evonet.nat20.ui.theme.NatPalette
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.natPalette
import kotlinx.coroutines.launch

/**
 * The persistent in-character codex (A7e / parity #12): the full iOS
 * `CodexShellView` chrome — nav row (back circle, diamond-flanked current-tab
 * title, phase pill), hero row (rect drop cap + name/race/level), campaign
 * region (Start Adventuring / journal bar), ornamental divider — over a
 * swipeable 6-tab pager (Stats / Skills / Combat / Spells / Items / Lore)
 * with the diamond-indicator tab bar pinned below. The shell owns ALL chrome
 * for the 2014 sheet; `CharacterSheetScreen` draws no Scaffold around it.
 */
private enum class CodexTab(val title: String) {
    STATS("Stats"), SKILLS("Skills"), COMBAT("Combat"),
    SPELLS("Spells"), ITEMS("Items"), LORE("Lore"),
}

@Composable
fun CodexShellView(
    character: Character,
    activeCampaign: Campaign?,
    hasPastAdventures: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStartCampaign: () -> Unit,
    onEndCampaign: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPastAdventures: () -> Unit,
    onBrowseSpells: () -> Unit,
    onApplyIntent: (CharacterIntent) -> Unit,
    onSave: (Character) -> Unit,
    modifier: Modifier = Modifier,
) {
    val payload = character.payload as? DnD5ePayload ?: return
    val inCampaign = character.phase is CharacterPhase.InCampaign
    val tabs = CodexTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    var levelingUp by remember { mutableStateOf(false) }
    // Actions layer (parity #19): the Act pill opens the grouped sheet; tiles
    // whose mechanic already has a codex surface route back into it here.
    var showActions by remember { mutableStateOf(false) }
    var attacking by remember { mutableStateOf(false) }
    var managingDeathSaves by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().statusBarsPadding()) {
        TopNavRow(
            title = tabs[pagerState.currentPage].title.uppercase(),
            inCampaign = inCampaign,
            onBack = onBack,
            onEdit = onEdit,
            onAct = { showActions = true },
        )
        HeroRow(character, payload)
        CampaignRegion(
            inCampaign = inCampaign,
            campaignName = activeCampaign?.name,
            hasPastAdventures = hasPastAdventures,
            onStartCampaign = onStartCampaign,
            onEndCampaign = onEndCampaign,
            onOpenJournal = onOpenJournal,
            onOpenPastAdventures = onOpenPastAdventures,
        )
        OrnamentalDivider(Modifier.padding(horizontal = 22.dp, vertical = 20.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            when (tabs[page]) {
                CodexTab.STATS -> StatsPage(character, payload, onApplyIntent, onSave) { levelingUp = true }
                CodexTab.SKILLS -> SkillsPage(payload, onApplyIntent)
                CodexTab.COMBAT -> CombatPage(character, payload, onApplyIntent)
                CodexTab.SPELLS -> SpellsPage(character, payload, onBrowseSpells, onApplyIntent, onSave)
                CodexTab.ITEMS -> ItemsPage(character, payload, onApplyIntent, onSave)
                CodexTab.LORE -> LorePage(character, payload, onApplyIntent)
            }
        }

        CodexTabBar(
            tabs = tabs,
            selected = pagerState.currentPage,
            onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
        )
    }

    if (levelingUp) {
        LevelUpWizard(payload, onApplyIntent, onDismiss = { levelingUp = false })
    }

    DnD5eActionsLayer(
        payload = payload,
        showSheet = showActions && inCampaign,
        onDismissSheet = { showActions = false },
        onApplyIntent = onApplyIntent,
        onRoute = { route ->
            when (route) {
                ActionRoute.ATTACK -> attacking = true
                ActionRoute.DEATH_SAVES -> managingDeathSaves = true
                ActionRoute.LEVEL_UP -> levelingUp = true
                ActionRoute.STATS_TAB -> scope.launch { pagerState.animateScrollToPage(CodexTab.STATS.ordinal) }
                ActionRoute.SKILLS_TAB -> scope.launch { pagerState.animateScrollToPage(CodexTab.SKILLS.ordinal) }
                ActionRoute.COMBAT_TAB -> scope.launch { pagerState.animateScrollToPage(CodexTab.COMBAT.ordinal) }
                ActionRoute.SPELLS_TAB -> scope.launch { pagerState.animateScrollToPage(CodexTab.SPELLS.ordinal) }
                ActionRoute.ITEMS_TAB -> scope.launch { pagerState.animateScrollToPage(CodexTab.ITEMS.ordinal) }
                ActionRoute.LORE_TAB -> scope.launch { pagerState.animateScrollToPage(CodexTab.LORE.ordinal) }
            }
        },
    )

    if (attacking) {
        AttackSheet(payload, onApplyIntent, onDismiss = { attacking = false })
    }
    if (managingDeathSaves) {
        DeathSavesDialog(payload, onApplyIntent, onDismiss = { managingDeathSaves = false })
    }
}

// ── Top nav row ────────────────────────────────────────────────────────────────

@Composable
private fun TopNavRow(
    title: String,
    inCampaign: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAct: () -> Unit,
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

        // Trailing: Act pill in a campaign, Edit pill while building.
        // (iOS also shows a gear → character settings here; omitted until the
        // #40 Character Settings screen exists — no destination to wire.)
        if (inCampaign) ActPill(palette, onAct) else EditPill(palette, onEdit)
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
private fun HeroRow(character: Character, payload: DnD5ePayload) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, start = 22.dp, end = 22.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DropCapSquare(character.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
        Column(Modifier.weight(1f)) {
            Text(
                character.name,
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                color = palette.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                heroRaceLine(payload),
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = palette.inkSoft,
                maxLines = 1,
            )
            Text(
                heroClassAndLevelLine(payload),
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = palette.inkSoft,
                maxLines = 1,
            )
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
    val palette = MaterialTheme.natPalette
    Column(
        Modifier.fillMaxWidth().padding(top = 14.dp, start = 22.dp, end = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
private fun CodexTabBar(tabs: List<CodexTab>, selected: Int, onSelect: (Int) -> Unit) {
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
                    tab.title.uppercase(),
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

// ── Hero copy helpers ──────────────────────────────────────────────────────────

private fun heroRaceLine(payload: DnD5ePayload): String =
    payload.race.takeIf { it.isNotEmpty() }?.slugToTitle() ?: "Unknown race"

private fun heroClassAndLevelLine(payload: DnD5ePayload): String {
    val primary = (payload.classes.firstOrNull()?.classId ?: payload.characterClass)
        .takeIf { it.isNotEmpty() }?.slugToTitle() ?: "Adventurer"
    return "Level ${payload.level} $primary"
}

/** Proficiency bonus shown in a few pages; centralised here. */
internal fun DnD5ePayload.proficiency(): Int = Proficiency.bonus(level)

package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.ui.actions.ActionRoute
import au.com.evonet.nat20.ui.actions.DnD5eActionsLayer
import au.com.evonet.nat20.ui.identity.IdentitySheet
import au.com.evonet.nat20.ui.slugToTitle
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * The persistent in-character 2014 codex (A7e / parity #12): the shared
 * payload-agnostic [CodexScaffold] (nav row, hero, campaign region, tab bar,
 * ornamental divider) wrapped around the six 2014 tab bodies. The shell owns
 * ALL chrome for the 2014 sheet; `CharacterSheetScreen` draws no Scaffold around
 * it. The Act pill opens the #19 grouped actions layer.
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
    onOpenCharacterSettings: () -> Unit,
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
    // In-campaign only: tapping the hero opens the name/portrait identity sheet.
    var editingIdentity by remember { mutableStateOf(false) }
    // Actions layer (parity #19): the Act pill opens the grouped sheet; most
    // tiles mount their own full-screen pickers inside the layer (slice B),
    // the few remaining tab-hosted mechanics route back into the shell here.
    var showActions by remember { mutableStateOf(false) }

    CodexScaffold(
        pagerState = pagerState,
        tabLabels = tabs.map { it.title },
        heroName = character.name,
        heroSubtitleLines = listOf(heroRaceLine(payload), heroClassAndLevelLine(payload)),
        portraitData = character.portraitData,
        fallbackLetter = character.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        editableHero = inCampaign,
        onEditIdentity = { editingIdentity = true },
        inCampaign = inCampaign,
        campaignName = activeCampaign?.name,
        hasPastAdventures = hasPastAdventures,
        onBack = onBack,
        onEdit = onEdit,
        onAct = { showActions = true },
        onStartCampaign = onStartCampaign,
        onEndCampaign = onEndCampaign,
        onOpenJournal = onOpenJournal,
        onOpenPastAdventures = onOpenPastAdventures,
        onOpenSettings = onOpenCharacterSettings,
        modifier = modifier,
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

    if (levelingUp) {
        LevelUpWizard(payload, onApplyIntent, onDismiss = { levelingUp = false })
    }

    if (editingIdentity) {
        IdentitySheet(
            initialName = character.name,
            initialPortrait = character.portraitData,
            onCancel = { editingIdentity = false },
            onCommit = { newName, portrait ->
                // Cosmetic metadata: save directly (unjournaled), not via an intent.
                onSave(character.copy(name = newName, portraitData = portrait, updatedAt = Instant.now()))
                editingIdentity = false
            },
        )
    }

    DnD5eActionsLayer(
        payload = payload,
        showSheet = showActions && inCampaign,
        onDismissSheet = { showActions = false },
        onApplyIntent = onApplyIntent,
        onRoute = { route ->
            when (route) {
                ActionRoute.LEVEL_UP -> levelingUp = true
                ActionRoute.SPELLS_TAB -> scope.launch { pagerState.animateScrollToPage(CodexTab.SPELLS.ordinal) }
                ActionRoute.ITEMS_TAB -> scope.launch { pagerState.animateScrollToPage(CodexTab.ITEMS.ordinal) }
            }
        },
    )
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

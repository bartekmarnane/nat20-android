package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.slugToTitle
import kotlinx.coroutines.launch

/**
 * The persistent in-character codex (A7e): a hero header over a swipeable
 * 6-tab pager (Stats / Skills / Combat / Spells / Items / Lore). Replaces the
 * plain `DnD5eSheetView`; port of the iOS `CodexShellView`. Pages render what
 * the current payload supports — Spells/Items are empty states until inventory
 * and spell prep land (A10).
 */
private enum class CodexTab(val title: String) {
    STATS("Stats"), SKILLS("Skills"), COMBAT("Combat"),
    SPELLS("Spells"), ITEMS("Items"), LORE("Lore"),
}

@Composable
fun CodexShellView(
    character: Character,
    onBrowseSpells: () -> Unit,
    onApplyIntent: (CharacterIntent) -> Unit,
    onSave: (Character) -> Unit,
    modifier: Modifier = Modifier,
) {
    val payload = character.payload as? DnD5ePayload ?: return
    val tabs = CodexTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    var levelingUp by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        HeroHeader(character, payload)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            when (tabs[page]) {
                CodexTab.STATS -> StatsPage(payload) { levelingUp = true }
                CodexTab.SKILLS -> SkillsPage(payload)
                CodexTab.COMBAT -> CombatPage(character, payload, onApplyIntent)
                CodexTab.SPELLS -> SpellsPage(character, payload, onBrowseSpells, onApplyIntent, onSave)
                CodexTab.ITEMS -> ItemsPage(character, payload, onApplyIntent, onSave)
                CodexTab.LORE -> LorePage(character, payload)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        CodexTabBar(
            tabs = tabs,
            selected = pagerState.currentPage,
            onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
        )
    }

    if (levelingUp) {
        LevelUpWizard(payload, onApplyIntent, onDismiss = { levelingUp = false })
    }
}

@Composable
private fun HeroHeader(character: Character, payload: DnD5ePayload) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DropCap(character.name.firstOrNull()?.uppercaseChar() ?: '?')
        Column(Modifier.weight(1f)) {
            Text(
                character.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                heroSubtitle(payload),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DropCap(initial: Char) {
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun CodexTabBar(tabs: List<CodexTab>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { index, tab ->
            val active = index == selected
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    tab.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun heroSubtitle(payload: DnD5ePayload): String {
    val race = payload.race.takeIf { it.isNotEmpty() }?.slugToTitle()
    val classes = payload.classes
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" / ") { "${it.classId.slugToTitle()} ${it.level}" }
        ?: "Level ${payload.level}"
    return listOfNotNull(race, classes).joinToString(" · ")
}

/** Proficiency bonus shown in a few pages; centralised here. */
internal fun DnD5ePayload.proficiency(): Int = Proficiency.bonus(level)

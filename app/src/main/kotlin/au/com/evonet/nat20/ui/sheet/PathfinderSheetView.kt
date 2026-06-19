package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.pf2e.PathfinderConditions
import au.com.evonet.nat20.pf2e.PathfinderPayload
import au.com.evonet.nat20.pf2e.PfAddNote
import au.com.evonet.nat20.pf2e.PfAdjustHeroPoints
import au.com.evonet.nat20.pf2e.PfApplyCondition
import au.com.evonet.nat20.pf2e.PfClearCondition
import au.com.evonet.nat20.pf2e.PfGainTempHp
import au.com.evonet.nat20.pf2e.PfHeal
import au.com.evonet.nat20.pf2e.PfSetDying
import au.com.evonet.nat20.pf2e.PfTakeDamage
import au.com.evonet.nat20.pf2e.armorClass
import au.com.evonet.nat20.pf2e.classDcValue
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save
import au.com.evonet.nat20.pf2e.modifier
import au.com.evonet.nat20.pf2e.perceptionBonus
import au.com.evonet.nat20.pf2e.saveBonus
import au.com.evonet.nat20.pf2e.skillBonus
import au.com.evonet.nat20.ui.slugToTitle
import kotlinx.coroutines.launch

/**
 * The Pathfinder 2e (Remaster) character sheet — the foundation slice (A22).
 * Interactive Stats / Skills / Combat / Lore tabs typed to [PathfinderPayload],
 * surfacing what makes PF2e PF2e: per-statistic proficiency ranks (U/T/E/M/L),
 * the dying/wounded track, Hero Points, and valued conditions. Spells, equipment,
 * feats, and the action economy are follow-up slices.
 */
private enum class PfTab(val title: String) { STATS("Stats"), SKILLS("Skills"), COMBAT("Combat"), LORE("Lore") }

@Composable
fun PathfinderSheetView(character: Character, onApplyIntent: (CharacterIntent) -> Unit, modifier: Modifier = Modifier) {
    val payload = character.payload as? PathfinderPayload ?: return
    val tabs = PfTab.entries
    val pager = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(character.name, style = MaterialTheme.typography.headlineSmall)
            Text(pfHeroLine(payload), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Pathfinder 2e (Remaster)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            when (tabs[page]) {
                PfTab.STATS -> PfStats(payload)
                PfTab.SKILLS -> PfSkills(payload)
                PfTab.COMBAT -> PfCombat(payload, onApplyIntent)
                PfTab.LORE -> PfLore(character, payload, onApplyIntent)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { i, tab ->
                val active = i == pager.currentPage
                Text(
                    tab.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).clickable { scope.launch { pager.animateScrollToPage(i) } }.padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PfStats(payload: PathfinderPayload) {
    PfPage {
        PfCard("Abilities") {
            PfAbility.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { a ->
                        PfMedallion(a.abbreviation, payload.abilityScores.score(a), Modifier.weight(1f))
                    }
                }
            }
        }
        PfCard("Defenses & Perception") {
            PfStatRow("Armor Class", payload.armorClass.toString())
            PfProfRow("Perception", payload.perceptionBonus, payload.perception)
            Save.entries.forEach { s -> PfProfRow(s.displayName, payload.saveBonus(s), payload.saves[s] ?: Proficiency.UNTRAINED) }
            PfStatRow("Class DC", payload.classDcValue.toString())
        }
    }
}

@Composable
private fun PfSkills(payload: PathfinderPayload) {
    PfPage {
        PfCard("Skills") {
            PfSkill.entries.forEachIndexed { i, skill ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                val rank = payload.skills[skill] ?: Proficiency.UNTRAINED
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(rank.letter, color = if (rank == Proficiency.UNTRAINED) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(skill.displayName, Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                    Text(skill.ability.abbreviation, Modifier.padding(end = 12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(payload.skillBonus(skill).signedPf(), fontWeight = FontWeight.Bold)
                }
            }
        }
        if (payload.loreSkills.isNotEmpty()) {
            PfCard("Lore") {
                payload.loreSkills.forEach { (subtype, rank) ->
                    PfProfRow(subtype, payload.modifier(PfAbility.INTELLIGENCE) + rank.bonus(payload.level), rank)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PfCombat(payload: PathfinderPayload, onApplyIntent: (CharacterIntent) -> Unit) {
    var amount by remember { mutableStateOf<PfAmount?>(null) }
    var addCond by remember { mutableStateOf(false) }
    PfPage {
        PfCard("Hit Points") {
            PfStatRow("Current / Max", "${payload.currentHp} / ${payload.maxHp}" + if (payload.temporaryHp > 0) " (+${payload.temporaryHp})" else "")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { amount = PfAmount.DAMAGE }) { Text("Damage") }
                OutlinedButton(onClick = { amount = PfAmount.HEAL }) { Text("Heal") }
                OutlinedButton(onClick = { amount = PfAmount.TEMP }) { Text("Temp HP") }
            }
        }
        if (payload.dying > 0 || payload.wounded > 0 || payload.currentHp == 0) {
            PfCard("Dying & Wounded") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Dying ${payload.dying} / ${PathfinderPayload.DYING_MAX}", style = MaterialTheme.typography.bodyMedium, color = if (payload.isDying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        if (payload.wounded > 0) Text("Wounded ${payload.wounded}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(enabled = payload.dying > 0, onClick = { onApplyIntent(PfSetDying(payload.dying - 1)) }) { Text("Recover") }
                    TextButton(enabled = payload.dying < PathfinderPayload.DYING_MAX, onClick = { onApplyIntent(PfSetDying(payload.dying + 1)) }) { Text("+1") }
                }
            }
        }
        PfCard("Hero Points") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("●".repeat(payload.heroPoints) + "○".repeat((PathfinderPayload.HERO_POINT_MAX - payload.heroPoints).coerceAtLeast(0)), Modifier.weight(1f), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                TextButton(enabled = payload.heroPoints > 0, onClick = { onApplyIntent(PfAdjustHeroPoints(-1)) }) { Text("Spend") }
                TextButton(enabled = payload.heroPoints < PathfinderPayload.HERO_POINT_MAX, onClick = { onApplyIntent(PfAdjustHeroPoints(1)) }) { Text("Gain") }
            }
        }
        PfCard("Conditions") {
            if (payload.conditions.isEmpty()) Text("No conditions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                payload.conditions.forEach { c ->
                    AssistChip(onClick = { onApplyIntent(PfClearCondition(c.id)) }, label = { Text(c.label(PathfinderConditions.displayName(c.id))) }, trailingIcon = { Text("✕", style = MaterialTheme.typography.labelMedium) })
                }
            }
            OutlinedButton(onClick = { addCond = true }) { Text("Add condition") }
        }
    }
    amount?.let { kind ->
        PfAmountDialog(kind.label, onConfirm = { n ->
            onApplyIntent(when (kind) { PfAmount.DAMAGE -> PfTakeDamage(n); PfAmount.HEAL -> PfHeal(n); PfAmount.TEMP -> PfGainTempHp(n) })
            amount = null
        }, onDismiss = { amount = null })
    }
    if (addCond) PfConditionPicker(payload.conditions.map { it.id }.toSet(), onPick = { id, value -> onApplyIntent(PfApplyCondition(id, value)); addCond = false }, onDismiss = { addCond = false })
}

@Composable
private fun PfLore(character: Character, payload: PathfinderPayload, onApplyIntent: (CharacterIntent) -> Unit) {
    var note by remember { mutableStateOf<Boolean>(false) }
    PfPage {
        PfCard("Identity") {
            PfStatRow("Name", character.name)
            PfStatRow("Ancestry", listOfNotNull(payload.heritage?.slugToTitle(), payload.ancestry.takeIf { it.isNotEmpty() }?.slugToTitle()).joinToString(" ").ifEmpty { "—" })
            PfStatRow("Background", payload.background?.slugToTitle() ?: "—")
            PfStatRow("Class", payload.className.takeIf { it.isNotEmpty() }?.slugToTitle()?.let { "$it ${payload.level}" } ?: "Level ${payload.level}")
            payload.keyAbility?.let { PfStatRow("Key Ability", it.displayName) }
            payload.alignmentOrEdicts?.let { PfStatRow("Edicts / Anathema", it) }
        }
        OutlinedButton(onClick = { note = true }) { Text("Add a note") }
    }
    if (note) PfNoteDialog(onConfirm = { onApplyIntent(PfAddNote(it)); note = false }, onDismiss = { note = false })
}

// ── atoms ──────────────────────────────────────────────────────────────────────

@Composable
private fun PfPage(content: @Composable () -> Unit) =
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }

@Composable
private fun PfCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun PfStatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PfProfRow(label: String, bonus: Int, rank: Proficiency) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(rank.letter, color = if (rank == Proficiency.UNTRAINED) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(label, Modifier.weight(1f).padding(start = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(bonus.signedPf(), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PfMedallion(label: String, score: Int, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(PfAbilityScores.modifier(score).signedPf(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(score.toString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private enum class PfAmount(val label: String) { DAMAGE("Damage"), HEAL("Heal"), TEMP("Temp HP") }

@Composable
private fun PfAmountDialog(title: String, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var n by remember { mutableIntStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(enabled = n > 1, onClick = { n-- }) { Text("−", style = MaterialTheme.typography.headlineSmall) }
                Text("$n", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { n++ }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(n) }) { Text(title) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PfConditionPicker(active: Set<String>, onPick: (String, Int?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add condition") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PathfinderConditions.all.filter { it.id !in active }.forEach { c ->
                    AssistChip(onClick = { onPick(c.id, if (c.valued) 1 else null) }, label = { Text(c.displayName + if (c.valued) " 1" else "") })
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun PfNoteDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a note") },
        text = { OutlinedTextField(text, { text = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun pfHeroLine(payload: PathfinderPayload): String {
    val ancestry = payload.ancestry.takeIf { it.isNotEmpty() }?.slugToTitle()
    val cls = payload.className.takeIf { it.isNotEmpty() }?.slugToTitle()?.let { "$it ${payload.level}" } ?: "Level ${payload.level}"
    return listOfNotNull(ancestry, cls).joinToString(" · ")
}

private fun Int.signedPf(): String = if (this >= 0) "+$this" else "$this"

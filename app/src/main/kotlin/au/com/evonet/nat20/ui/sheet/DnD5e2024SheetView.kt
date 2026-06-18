package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.dnd5e2024.Exhaustion2024
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.slugToTitle

/**
 * A minimal read-only sheet for D&D 5e (2024) characters (A18 first slice). It
 * proves the multi-ruleset seam end-to-end — a 2024 character renders, persists,
 * and dispatches off `rulesetId` — using only the shared `-core` types. The full
 * 6-tab 2024 codex + creation wizard are follow-up slices.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DnD5e2024SheetView(character: Character, modifier: Modifier = Modifier) {
    val payload = character.payload as? DnD5e2024Payload ?: return
    val scores = payload.effectiveAbilityScores
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(character.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                heroLine(payload),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("5th Edition (2024)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }

        Section("Abilities") {
            Ability.entries.chunked(3).forEach { rowAbilities ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowAbilities.forEach { ability ->
                        Medallion(ability.abbreviation, scores.score(ability), Modifier.weight(1f))
                    }
                }
            }
        }

        Section("Vitals") {
            Stat("Hit Points", "${payload.currentHp} / ${payload.maxHp}" + if (payload.temporaryHp > 0) " (+${payload.temporaryHp})" else "")
            Stat("Armor Class", (10 + AbilityScores.modifier(scores.score(Ability.DEXTERITY))).toString())
            Stat("Initiative", AbilityScores.modifier(scores.score(Ability.DEXTERITY)).let { if (it >= 0) "+$it" else "$it" })
            Stat("Hit Dice", "${payload.currentHitDice} / ${payload.maxHitDice}")
            if (payload.exhaustionLevel > 0) {
                Stat("Exhaustion", "Level ${payload.exhaustionLevel} (${Exhaustion2024.d20Modifier(payload.exhaustionLevel)} d20, −${Exhaustion2024.speedPenaltyFeet(payload.exhaustionLevel)} ft)")
            }
            if (payload.hasInspiration) Stat("Heroic Inspiration", "Yes")
        }

        if (payload.activeConditions.isNotEmpty()) {
            Section("Conditions") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    payload.activeConditions.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                }
            }
        }

        Section("Note") {
            Text(
                "The full 2024 codex (spells, items, weapon mastery, level-up) is in progress; this view confirms the edition renders on the shared engine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun heroLine(payload: DnD5e2024Payload): String {
    val species = payload.species.takeIf { it.isNotEmpty() }?.slugToTitle()
    val cls = payload.classes.takeIf { it.isNotEmpty() }
        ?.joinToString(" / ") { "${it.classId.slugToTitle()} ${it.level}" }
        ?: "Level ${payload.level}"
    return listOfNotNull(species, cls).joinToString(" · ")
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun Medallion(label: String, score: Int, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(AbilityScores.modifier(score).let { if (it >= 0) "+$it" else "$it" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(score.toString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

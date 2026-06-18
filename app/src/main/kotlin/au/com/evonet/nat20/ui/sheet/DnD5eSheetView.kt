package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.slugToTitle

/**
 * Read-only D&D 5e sheet body for A4: Identity, Vitals, and Abilities, read
 * straight off the [DnD5ePayload]. The full codex (6 tabbed pages, actions,
 * effects) is the A7e build; this proves the per-ruleset dispatch + payload
 * read-out end to end.
 */
@Composable
fun DnD5eSheetView(character: Character, modifier: Modifier = Modifier) {
    val payload = character.payload as? DnD5ePayload ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IdentitySection(payload)
        VitalsSection(payload)
        AbilitiesSection(payload)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun IdentitySection(payload: DnD5ePayload) {
    SectionCard("Identity") {
        val race = payload.race.takeIf { it.isNotEmpty() }?.slugToTitle() ?: "—"
        StatLine("Race", race)
        val classes = payload.classes
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" / ") { "${it.classId.slugToTitle()} ${it.level}" }
            ?: "—"
        StatLine("Class", classes)
        StatLine("Level", payload.level.toString())
        StatLine("Proficiency", Proficiency.bonus(payload.level).signed())
    }
}

@Composable
private fun VitalsSection(payload: DnD5ePayload) {
    SectionCard("Vitals") {
        StatLine("Hit Points", "${payload.currentHp} / ${payload.maxHp}")
        if (payload.temporaryHp > 0) {
            StatLine("Temporary HP", "+${payload.temporaryHp}")
        }
    }
}

@Composable
private fun AbilitiesSection(payload: DnD5ePayload) {
    SectionCard("Abilities") {
        // A fixed 3-wide grid built from plain Rows — no LazyVerticalGrid, so it
        // composes cleanly inside the page's verticalScroll.
        Ability.entries.chunked(3).forEach { rowAbilities ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowAbilities.forEach { ability ->
                    AbilityMedallion(ability, payload.abilityScores, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AbilityMedallion(ability: Ability, scores: AbilityScores, modifier: Modifier = Modifier) {
    val score = scores.score(ability)
    val mod = AbilityScores.modifier(score)
    Card(modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                ability.abbreviation,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                mod.signed(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(score.toString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
    }
}

/** "+3" / "-1" / "+0" — 5e always shows the sign on a modifier. */
private fun Int.signed(): String = if (this >= 0) "+$this" else "$this"

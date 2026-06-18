package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.slugToTitle

// ── Pages ────────────────────────────────────────────────────────────────────

@Composable
internal fun StatsPage(payload: DnD5ePayload) {
    CodexPage {
        SectionCard("Abilities") {
            Ability.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { ability ->
                        AbilityMedallion(ability, payload.abilityScores, Modifier.weight(1f))
                    }
                }
            }
        }
        SectionCard("Proficiency & Senses") {
            StatLine("Proficiency Bonus", payload.proficiency().signed())
            StatLine("Passive Perception", (10 + payload.abilityScores.modifier(Ability.WISDOM)).toString())
        }
        SectionCard("Saving Throws") {
            // Proficient saves are class-driven (not modelled yet) — show the raw
            // ability modifier for each; proficiency dots arrive with creation (A8).
            Ability.entries.forEach { ability ->
                StatLine(ability.abbreviation, payload.abilityScores.modifier(ability).signed())
            }
        }
    }
}

@Composable
internal fun SkillsPage(payload: DnD5ePayload) {
    CodexPage {
        SectionCard("Skills") {
            Text(
                "Skill proficiencies aren't chosen yet — values are the base ability modifier. Proficiency lands with character creation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Skills5e.forEachIndexed { index, skill ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(skill.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        skill.ability.abbreviation,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(
                        payload.abilityScores.modifier(skill.ability).signed(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CombatPage(payload: DnD5ePayload) {
    val dexMod = payload.abilityScores.modifier(Ability.DEXTERITY)
    CodexPage {
        SectionCard("Hit Points") {
            StatLine("Current / Max", "${payload.currentHp} / ${payload.maxHp}")
            if (payload.temporaryHp > 0) StatLine("Temporary", "+${payload.temporaryHp}")
        }
        SectionCard("Defense & Movement") {
            // No armor modelled yet → unarmored AC = 10 + DEX; speed defaults to 30.
            StatLine("Armor Class", (10 + dexMod).toString())
            StatLine("Initiative", dexMod.signed())
            StatLine("Speed", "30 ft")
        }
        SectionCard("Hit Dice") {
            payload.classes.forEach { entry ->
                StatLine(entry.classId.slugToTitle(), "${entry.level}d${DnD5eClasses.hitDie(entry.classId)}")
            }
            if (payload.classes.isEmpty()) {
                Text("—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun LorePage(character: Character, payload: DnD5ePayload) {
    CodexPage {
        SectionCard("Identity") {
            StatLine("Name", character.name)
            StatLine("Race", payload.race.takeIf { it.isNotEmpty() }?.slugToTitle() ?: "—")
            StatLine(
                "Class",
                payload.classes.takeIf { it.isNotEmpty() }
                    ?.joinToString(" / ") { "${it.classId.slugToTitle()} ${it.level}" }
                    ?: "—",
            )
            StatLine("Level", payload.level.toString())
        }
        SectionCard("Backstory") {
            Text(
                "Background, alignment, and backstory arrive with the character-creation wizard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Shared atoms ─────────────────────────────────────────────────────────────

@Composable
private fun CodexPage(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
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
private fun AbilityMedallion(ability: Ability, scores: AbilityScores, modifier: Modifier = Modifier) {
    val score = scores.score(ability)
    Card(modifier) {
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
                AbilityScores.modifier(score).signed(),
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
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

/** "+3" / "-1" / "+0" — 5e always shows the sign on a modifier. */
internal fun Int.signed(): String = if (this >= 0) "+$this" else "$this"

/** The 18 SRD skills and their governing ability — a display stopgap until the
 *  skill catalogue + per-character proficiencies are modelled (A8/A10). */
private data class Skill5e(val label: String, val ability: Ability)

private val Skills5e: List<Skill5e> = listOf(
    Skill5e("Acrobatics", Ability.DEXTERITY),
    Skill5e("Animal Handling", Ability.WISDOM),
    Skill5e("Arcana", Ability.INTELLIGENCE),
    Skill5e("Athletics", Ability.STRENGTH),
    Skill5e("Deception", Ability.CHARISMA),
    Skill5e("History", Ability.INTELLIGENCE),
    Skill5e("Insight", Ability.WISDOM),
    Skill5e("Intimidation", Ability.CHARISMA),
    Skill5e("Investigation", Ability.INTELLIGENCE),
    Skill5e("Medicine", Ability.WISDOM),
    Skill5e("Nature", Ability.INTELLIGENCE),
    Skill5e("Perception", Ability.WISDOM),
    Skill5e("Performance", Ability.CHARISMA),
    Skill5e("Persuasion", Ability.CHARISMA),
    Skill5e("Religion", Ability.INTELLIGENCE),
    Skill5e("Sleight of Hand", Ability.DEXTERITY),
    Skill5e("Stealth", Ability.DEXTERITY),
    Skill5e("Survival", Ability.WISDOM),
)

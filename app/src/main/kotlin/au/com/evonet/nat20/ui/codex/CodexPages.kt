package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.slugToTitle

// ── Pages ────────────────────────────────────────────────────────────────────

@Composable
internal fun StatsPage(payload: DnD5ePayload) {
    val prof = Proficiency.bonus(payload.level)
    val proficientSaves = payload.primaryClass()?.savingThrowAbilities()?.toSet().orEmpty()
    val perceptionProficient = "perception" in payload.selectedSkills
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
            StatLine("Proficiency Bonus", prof.signed())
            val passive = 10 + payload.abilityScores.modifier(Ability.WISDOM) + if (perceptionProficient) prof else 0
            StatLine("Passive Perception", passive.toString())
        }
        SectionCard("Saving Throws") {
            Ability.entries.forEach { ability ->
                val proficient = ability in proficientSaves
                val mod = payload.abilityScores.modifier(ability) + if (proficient) prof else 0
                ProficiencyLine(ability.abbreviation, mod.signed(), proficient)
            }
        }
    }
}

@Composable
internal fun SkillsPage(payload: DnD5ePayload) {
    val prof = Proficiency.bonus(payload.level)
    CodexPage {
        SectionCard("Skills") {
            DnD5eCatalog.skills.forEachIndexed { index, skill ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                val proficient = skill.id in payload.selectedSkills
                val mod = payload.abilityScores.modifier(skill.ability) + if (proficient) prof else 0
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ProficiencyDot(proficient)
                    Text(skill.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    Text(
                        skill.ability.abbreviation,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(mod.signed(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
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
internal fun SpellsPage(payload: DnD5ePayload, onBrowseSpells: () -> Unit) {
    val caster = payload.primaryClass()?.isCaster == true
    CodexPage {
        if (caster) {
            SectionCard("Spellcasting") {
                Text(
                    "Known and prepared spells arrive with a later step. For now, browse the full spell list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onBrowseSpells) { Text("Browse Spell Library") }
            }
        } else {
            SectionCard("Spellcasting") {
                Text(
                    "${payload.primaryClass()?.name ?: "This class"} isn't a spellcaster.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onBrowseSpells) { Text("Browse Spell Library") }
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
            StatLine("Background", payload.background.takeIf { it.isNotEmpty() }?.let { DnD5eCatalog.background(it)?.name ?: it.slugToTitle() } ?: "—")
            StatLine("Level", payload.level.toString())
        }
        SectionCard("Backstory") {
            Text(
                "Alignment and backstory prose arrive with a later content step.",
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

/** The character's primary (first) class, resolved through the catalogue. */
private fun DnD5ePayload.primaryClass() =
    classes.firstOrNull()?.classId?.let(DnD5eCatalog::characterClass)

@Composable
private fun ProficiencyLine(label: String, value: String, proficient: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ProficiencyDot(proficient)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProficiencyDot(proficient: Boolean) {
    Text(
        if (proficient) "●" else "○",
        style = MaterialTheme.typography.bodySmall,
        color = if (proficient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
    )
}

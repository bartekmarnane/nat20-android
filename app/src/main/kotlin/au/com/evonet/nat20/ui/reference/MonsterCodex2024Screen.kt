package au.com.evonet.nat20.ui.reference

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e2024.Monster2024
import au.com.evonet.nat20.dnd5e2024.MonsterCatalog2024

/**
 * Read-only Monster Codex (A21): the SRD 5.2 monster catalogue (330 statblocks),
 * searchable and filterable by challenge rating, with expandable statblocks.
 * Port of the iOS `MonsterCodex2024View`. Attribution: SRD 5.2 (CC BY 4.0).
 */
/** Chromeless D&D 5e (2024) bestiary body for the [ReferenceTabShell]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonsterCodex2024Body() {
    var query by remember { mutableStateOf("") }
    var crFilter by remember { mutableStateOf<Double?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }

    val monsters = remember(query, crFilter) {
        MonsterCatalog2024.all.filter { m ->
            (crFilter == null || m.challengeRating == crFilter) &&
                (query.isBlank() || m.name.contains(query.trim(), ignoreCase = true) || m.type.contains(query.trim(), ignoreCase = true))
        }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search ${MonsterCatalog2024.all.size} monsters") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CrFilters) { cr ->
                FilterChip(
                    selected = crFilter == cr,
                    onClick = { crFilter = if (crFilter == cr) null else cr },
                    label = { Text("CR ${crLabel(cr)}") },
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(monsters, key = { it.id }) { monster ->
                MonsterRow(monster, expanded == monster.id) { expanded = if (expanded == monster.id) null else monster.id }
            }
        }
    }
}

@Composable
private fun MonsterRow(monster: Monster2024, expanded: Boolean, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(monster.name, style = MaterialTheme.typography.titleMedium)
                    Text(monster.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("CR ${monster.crLabel}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    MetaLine("Armor Class", "${monster.armorClass}" + monster.armorDetail.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty())
                    MetaLine("Hit Points", "${monster.hitPoints}" + monster.hitDice.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty())
                    MetaLine("Speed", monster.speed)
                    AbilityGrid(monster.abilities)
                    MetaLine("Saving Throws", monster.savesDisplay)
                    MetaLine("Skills", monster.skillsDisplay)
                    MetaLine("Resistances", monster.damageResistances)
                    MetaLine("Immunities", listOf(monster.damageImmunities, monster.conditionImmunities).filter { it.isNotBlank() }.joinToString("; "))
                    MetaLine("Vulnerabilities", monster.damageVulnerabilities)
                    MetaLine("Senses", listOf(monster.sensesDisplay, "passive Perception ${monster.passivePerception}").filter { it.isNotBlank() }.joinToString(", "))
                    MetaLine("Languages", monster.languages)
                    NamedTextBlock("Traits", monster.traits)
                    NamedTextBlock("Actions", monster.actions)
                    NamedTextBlock("Bonus Actions", monster.bonusActions)
                    NamedTextBlock("Reactions", monster.reactions)
                    NamedTextBlock("Legendary Actions", monster.legendaryActions)
                }
            }
        }
    }
}

@Composable
private fun AbilityGrid(a: Monster2024.Abilities) {
    val pairs = listOf("STR" to a.str, "DEX" to a.dex, "CON" to a.con, "INT" to a.int, "WIS" to a.wis, "CHA" to a.cha)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        pairs.forEach { (label, score) ->
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$score", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                val mod = Math.floorDiv(score - 10, 2)
                Text(if (mod >= 0) "+$mod" else "$mod", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NamedTextBlock(title: String, entries: List<Monster2024.NamedText>) {
    if (entries.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        entries.forEach { e ->
            Text(
                buildAnnotatedNamed(e.name, e.desc),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun buildAnnotatedNamed(name: String, desc: String): String = "$name. $desc"

@Composable
private fun MetaLine(label: String, value: String) {
    if (value.isBlank()) return
    Text("$label: $value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A representative ladder of challenge ratings for the filter chips. */
private val CrFilters: List<Double> = listOf(0.0, 0.125, 0.25, 0.5, 1.0, 2.0, 3.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0)

private fun crLabel(cr: Double): String = when (cr) {
    0.125 -> "1/8"
    0.25 -> "1/4"
    0.5 -> "1/2"
    else -> if (cr % 1.0 == 0.0) cr.toInt().toString() else cr.toString()
}

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
@Composable
fun MonsterCodex2024Body() {
    var query by remember { mutableStateOf("") }
    var crFilter by remember { mutableStateOf<Double?>(null) }

    val monsters = remember(query, crFilter) {
        MonsterCatalog2024.all.filter { m ->
            (crFilter == null || m.challengeRating == crFilter) &&
                (query.isBlank() || m.name.contains(query.trim(), ignoreCase = true) || m.type.contains(query.trim(), ignoreCase = true))
        }.map { it.toView() }
    }

    MonsterCodexList(
        query = query,
        onQuery = { query = it },
        hint = "Search ${MonsterCatalog2024.all.size} monsters",
        monsters = monsters,
        filterPills = {
            CrFilters.forEach { cr ->
                ReferencePill("CR ${crLabel(cr)}", crFilter == cr) { crFilter = if (crFilter == cr) null else cr }
            }
        },
    )
}

private fun Monster2024.toView() = MonsterView(
    id = id,
    name = name,
    subtitle = subtitle,
    badge = "CR $crLabel",
    meta = listOf(
        "Armor Class" to "$armorClass" + armorDetail.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty(),
        "Hit Points" to "$hitPoints" + hitDice.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty(),
        "Speed" to speed,
        "Saving Throws" to savesDisplay,
        "Skills" to skillsDisplay,
        "Resistances" to damageResistances,
        "Immunities" to listOf(damageImmunities, conditionImmunities).filter { it.isNotBlank() }.joinToString("; "),
        "Vulnerabilities" to damageVulnerabilities,
        "Senses" to listOf(sensesDisplay, "passive Perception $passivePerception").filter { it.isNotBlank() }.joinToString(", "),
        "Languages" to languages,
    ),
    abilities = listOf("STR" to abilities.str, "DEX" to abilities.dex, "CON" to abilities.con, "INT" to abilities.int, "WIS" to abilities.wis, "CHA" to abilities.cha),
    blocks = listOf(
        "Traits" to traits.map { it.name to it.desc },
        "Actions" to actions.map { it.name to it.desc },
        "Bonus Actions" to bonusActions.map { it.name to it.desc },
        "Reactions" to reactions.map { it.name to it.desc },
        "Legendary Actions" to legendaryActions.map { it.name to it.desc },
    ),
)

/** A representative ladder of challenge ratings for the filter chips. */
private val CrFilters: List<Double> = listOf(0.0, 0.125, 0.25, 0.5, 1.0, 2.0, 3.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0)

private fun crLabel(cr: Double): String = when (cr) {
    0.125 -> "1/8"
    0.25 -> "1/4"
    0.5 -> "1/2"
    else -> if (cr % 1.0 == 0.0) cr.toInt().toString() else cr.toString()
}

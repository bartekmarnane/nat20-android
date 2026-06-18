package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.ClassEntry
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.DnD5eRuleset
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.domain.Character
import java.time.Instant

/**
 * Minimal D&D 5e create/edit form (A6). Deliberately a plain form — the
 * parchment multi-step creation wizard is A8 (iOS step 8), which replaces this
 * exactly as iOS deleted its step-6 form. Used for both create (`existing` =
 * null) and edit. The editor owns the ruleset-specific payload, building the
 * whole [Character] and handing it back via [onSave].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnD5eEditorScreen(
    existing: Character?,
    onSave: (Character) -> Unit,
    onCancel: () -> Unit,
) {
    val source = existing?.payload as? DnD5ePayload
    val primaryClass = source?.classes?.firstOrNull()

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var race by rememberSaveable { mutableStateOf(source?.race ?: "") }
    var classId by rememberSaveable { mutableStateOf(primaryClass?.classId ?: "") }
    var level by rememberSaveable { mutableIntStateOf(primaryClass?.level ?: 1) }
    var maxHp by rememberSaveable { mutableIntStateOf(source?.maxHp ?: 8) }
    var scores by remember { mutableStateOf(source?.abilityScores ?: AbilityScores()) }

    fun build(): Character {
        val classes = classId.trim()
            .takeIf { it.isNotEmpty() }
            ?.let { listOf(ClassEntry(classId = it, level = level)) }
            ?: emptyList()
        val payload = DnD5ePayload(
            race = race.trim(),
            classes = classes,
            abilityScores = scores,
            maxHp = maxHp,
            // Build-phase edit: keep current HP but never above the new max.
            currentHp = minOf(source?.currentHp ?: maxHp, maxHp),
            temporaryHp = source?.temporaryHp ?: 0,
        )
        return if (existing == null) {
            Character.new(name.trim(), DnD5eRuleset(), payload, Instant.now())
        } else {
            existing.copy(name = name.trim(), payload = payload, updatedAt = Instant.now())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New Character" else "Edit Character") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
                actions = {
                    TextButton(enabled = name.isNotBlank(), onClick = { onSave(build()) }) {
                        Text("Save")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = race,
                onValueChange = { race = it },
                label = { Text("Race") },
                placeholder = { Text("e.g. mountain-dwarf") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = classId,
                onValueChange = { classId = it },
                label = { Text("Class") },
                placeholder = { Text("e.g. fighter") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
            )

            StepperRow("Level", level, 1..20) { level = it }
            StepperRow("Max HP", maxHp, 1..999) { maxHp = it }

            Text(
                "ABILITIES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Ability.entries.forEach { ability ->
                StepperRow(ability.abbreviation, scores.score(ability), AbilityScores.VALID_RANGE) {
                    scores = scores.with(ability, it)
                }
            }
        }
    }
}

/** A label + value with −/+ buttons, clamped to [range]. */
@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = { if (value > range.first) onChange(value - 1) },
            enabled = value > range.first,
            modifier = Modifier.size(44.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) { Text("−") }
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp).width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        OutlinedButton(
            onClick = { if (value < range.last) onChange(value + 1) },
            enabled = value < range.last,
            modifier = Modifier.size(44.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) { Text("+") }
    }
}

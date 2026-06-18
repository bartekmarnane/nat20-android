package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.AdjustExhaustion
import au.com.evonet.nat20.dnd5e.ApplyCondition
import au.com.evonet.nat20.dnd5e.ClearCondition
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.core.Condition
import au.com.evonet.nat20.dnd5e.core.Exhaustion
import au.com.evonet.nat20.domain.CharacterIntent

/**
 * The Combat-tab **Conditions** section (A7f-2): active-condition chips (tap to
 * clear), an exhaustion track with ± steppers, and an add-condition picker
 * (the 14 standard conditions + a custom field for house rules). All edits
 * route through the shared `onApplyIntent`, so they journal in a campaign.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ConditionsSection(payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }

    SectionCard("Conditions") {
        if (payload.activeConditions.isEmpty()) {
            Text(
                "No conditions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                payload.activeConditions.forEach { name ->
                    AssistChip(
                        onClick = { onApplyIntent(ClearCondition(name)) },
                        label = { Text(name) },
                        trailingIcon = { Text("✕", style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        }

        // Exhaustion track.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Exhaustion ${payload.exhaustionLevel} / ${Exhaustion.MAX}", style = MaterialTheme.typography.bodyMedium)
                if (payload.exhaustionLevel > 0) {
                    Text(
                        Exhaustion.effect(payload.exhaustionLevel),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(enabled = payload.exhaustionLevel > 0, onClick = { onApplyIntent(AdjustExhaustion(-1)) }) {
                Text("−", style = MaterialTheme.typography.headlineSmall)
            }
            TextButton(enabled = payload.exhaustionLevel < Exhaustion.MAX, onClick = { onApplyIntent(AdjustExhaustion(1)) }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }

        OutlinedButton(onClick = { showAdd = true }) { Text("Add condition") }
    }

    if (showAdd) {
        AddConditionDialog(
            active = payload.activeConditions,
            onApply = { name -> onApplyIntent(ApplyCondition(name)); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddConditionDialog(active: List<String>, onApply: (String) -> Unit, onDismiss: () -> Unit) {
    var custom by remember { mutableStateOf("") }
    val activeLower = remember(active) { active.map { it.lowercase() }.toSet() }
    val available = remember(active) { Condition.entries.filter { it.displayName.lowercase() !in activeLower } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add condition") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    available.forEach { condition ->
                        FilterChip(
                            selected = false,
                            onClick = { onApply(condition.displayName) },
                            label = { Text(condition.displayName) },
                        )
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text("Custom condition") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (custom.isNotBlank()) {
                    TextButton(onClick = { onApply(custom.trim()) }, modifier = Modifier.padding(start = 4.dp)) {
                        Text("Add \"${custom.trim()}\"", fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

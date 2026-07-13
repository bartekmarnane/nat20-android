package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.AdjustExhaustion
import au.com.evonet.nat20.dnd5e.ApplyCondition
import au.com.evonet.nat20.dnd5e.ClearCondition
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.effectImposedConditions
import au.com.evonet.nat20.dnd5e.core.Condition
import au.com.evonet.nat20.dnd5e.core.Exhaustion
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.editor.StepperButton
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The Combat-tab **Conditions** extra (A7f-2, Android interaction layer —
 * pending #19): active-condition chips (tap to clear), an exhaustion track with
 * ± steppers, and an add-condition picker. Chrome restyled to SectionHead +
 * codex chips (parity #15). All edits journal in a campaign.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConditionsSection(payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit) {
    val palette = MaterialTheme.natPalette
    var showAdd by remember { mutableStateOf(false) }

    // Conditions imposed by an active effect (Greater Invisibility → Invisible); they clear
    // with their effect, so they show as read-only chips rather than tap-to-clear.
    val imposed = payload.effectImposedConditions.filterNot { c -> payload.activeConditions.any { it.equals(c, ignoreCase = true) } }

    SectionHead("Conditions", top = 22.dp, bottom = 10.dp)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (payload.activeConditions.isEmpty() && imposed.isEmpty()) {
            DashedNotice("No conditions.")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                payload.activeConditions.forEach { name ->
                    ConditionChip(name, trailing = "✕", tone = palette.danger) { onApplyIntent(ClearCondition(name)) }
                }
                imposed.forEach { name ->
                    ConditionChip(name, trailing = "⟲", tone = palette.inkMute, onClick = null)
                }
            }
        }

        // Exhaustion track.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Exhaustion ${payload.exhaustionLevel} / ${Exhaustion.MAX}",
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = palette.ink,
                )
                if (payload.exhaustionLevel > 0) {
                    Text(
                        Exhaustion.effect(payload.exhaustionLevel),
                        fontFamily = ImFell,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp,
                        color = palette.inkMute,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepperButton("−", enabled = payload.exhaustionLevel > 0) { onApplyIntent(AdjustExhaustion(-1)) }
                StepperButton("+", enabled = payload.exhaustionLevel < Exhaustion.MAX) { onApplyIntent(AdjustExhaustion(1)) }
            }
        }

        CodexButton("Add condition", onClick = { showAdd = true })
    }

    if (showAdd) {
        AddConditionDialog(
            active = payload.activeConditions,
            onApply = { name -> onApplyIntent(ApplyCondition(name)); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
}

/** Tinted condition chip; tap clears (danger) or is inert (effect-imposed). */
@Composable
private fun ConditionChip(name: String, trailing: String, tone: androidx.compose.ui.graphics.Color, onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(3.dp)
    Row(
        Modifier
            .clip(shape)
            .border(1.dp, tone.copy(alpha = 0.45f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(name, fontFamily = Cormorant, fontSize = 13.sp, color = tone)
        Text(trailing, fontSize = 11.sp, color = tone.copy(alpha = 0.8f))
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

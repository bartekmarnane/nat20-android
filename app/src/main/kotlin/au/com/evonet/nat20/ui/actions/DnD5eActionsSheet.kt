package au.com.evonet.nat20.ui.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.AddNote
import au.com.evonet.nat20.dnd5e.GainTempHp
import au.com.evonet.nat20.dnd5e.Heal
import au.com.evonet.nat20.dnd5e.LongRest
import au.com.evonet.nat20.dnd5e.ShortRest
import au.com.evonet.nat20.dnd5e.TakeDamage
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.NoteKind

/** The quick in-campaign actions; the detailed pickers live on the codex tabs. */
private enum class Act(val label: String) {
    DAMAGE("Damage"),
    HEAL("Heal"),
    TEMP_HP("Temp HP"),
    SHORT_REST("Short Rest"),
    LONG_REST("Long Rest"),
    NOTE("Note"),
}

/**
 * In-campaign Actions sheet (A7b). A modal bottom sheet of action tiles; each
 * opens a small picker, then emits a [CharacterIntent] via [onAct] (the caller
 * runs it through `applyIntent`, which mutates the character and logs it). This
 * is the minimal play loop — the hi-fi parchment Actions sheet + full action
 * catalogue land at A7e/A7f.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnD5eActionsSheet(onDismiss: () -> Unit, onAct: (CharacterIntent) -> Unit) {
    var selected by remember { mutableStateOf<Act?>(null) }

    fun emit(intent: CharacterIntent) {
        onAct(intent)
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val act = selected) {
                null -> {
                    Text("Actions", style = MaterialTheme.typography.titleLarge)
                    Act.entries.forEach { entry ->
                        // Rests are immediate (no picker); everything else opens a sub-picker.
                        ActionTile(entry.label) {
                            when (entry) {
                                Act.SHORT_REST -> emit(ShortRest())
                                Act.LONG_REST -> emit(LongRest())
                                else -> selected = entry
                            }
                        }
                    }
                }

                Act.DAMAGE, Act.HEAL, Act.TEMP_HP ->
                    AmountPicker(
                        title = act.label,
                        onBack = { selected = null },
                        onConfirm = { amount -> emit(amountIntent(act, amount)) },
                    )

                Act.SHORT_REST, Act.LONG_REST -> Unit // handled immediately from the tile tap

                Act.NOTE ->
                    NotePicker(
                        onBack = { selected = null },
                        onConfirm = { text, kind -> emit(AddNote(text, kind)) },
                    )
            }
        }
    }
}

private fun amountIntent(act: Act, amount: Int): CharacterIntent = when (act) {
    Act.DAMAGE -> TakeDamage(amount)
    Act.HEAL -> Heal(amount)
    Act.TEMP_HP -> GainTempHp(amount)
    Act.NOTE, Act.SHORT_REST, Act.LONG_REST -> error("$act has no amount picker")
}

@Composable
private fun ActionTile(label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun AmountPicker(title: String, onBack: () -> Unit, onConfirm: (Int) -> Unit) {
    var amount by remember { mutableIntStateOf(1) }
    Text(title, style = MaterialTheme.typography.titleLarge)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = { if (amount > 1) amount-- },
            enabled = amount > 1,
            modifier = Modifier.size(48.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) { Text("−") }
        Text(
            amount.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(96.dp),
        )
        OutlinedButton(
            onClick = { amount++ },
            modifier = Modifier.size(48.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) { Text("+") }
    }
    PickerButtons(onBack = onBack, confirmLabel = title, onConfirm = { onConfirm(amount) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotePicker(onBack: () -> Unit, onConfirm: (String, NoteKind?) -> Unit) {
    var text by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf<NoteKind?>(null) }
    Text("Note", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("What happened?") },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NoteKind.NARRATIVE.forEach { option ->
            FilterChip(
                selected = kind == option,
                onClick = { kind = if (kind == option) null else option },
                label = { Text(option.label) },
            )
        }
    }
    PickerButtons(
        onBack = onBack,
        confirmLabel = "Add Note",
        confirmEnabled = text.isNotBlank(),
        onConfirm = { onConfirm(text, kind) },
    )
}

@Composable
private fun PickerButtons(
    onBack: () -> Unit,
    confirmLabel: String,
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f),
        ) { Text(confirmLabel) }
    }
}

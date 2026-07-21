package au.com.evonet.nat20.ui.roll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollResult
import au.com.evonet.nat20.dnd5e.core.RollSpec

/**
 * A modal wrapper around [RollResultView] for one-off rolls (checks, saves,
 * death saves, hit dice). The dialog stays open after settling so the player can
 * read the result, roll again, or correct a hand-entered value.
 *
 * Callers that **journal** an outcome must use [onCommit], not [onSettled]: the
 * confirm button then reads "Record", stays disabled until the dice have landed,
 * and fires exactly once. Rolling again — or re-entering a physical roll after a
 * typo (A25) — settles repeatedly, so journalling from [onSettled] would write a
 * second entry every time. [onSettled] is for display-only reactions.
 */
@Composable
fun RollDialog(
    title: String,
    spec: RollSpec,
    bonuses: List<RollBonus> = emptyList(),
    allowAdvantageToggle: Boolean = true,
    luckyReroll: Boolean = false,
    onSettled: (RollResult) -> Unit = {},
    onCommit: ((RollResult) -> Unit)? = null,
    commitLabel: String = "Record",
    onDismiss: () -> Unit,
) {
    var settled by remember { mutableStateOf<RollResult?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RollResultView(
                    baseSpec = spec,
                    bonuses = bonuses,
                    allowAdvantageToggle = allowAdvantageToggle,
                    luckyReroll = luckyReroll,
                    onSettled = { result -> settled = result; onSettled(result) },
                    onReset = { settled = null },
                )
            }
        },
        confirmButton = {
            if (onCommit == null) {
                TextButton(onClick = onDismiss) { Text("Done") }
            } else {
                val result = settled
                TextButton(
                    enabled = result != null,
                    onClick = { result?.let(onCommit); onDismiss() },
                ) { Text(commitLabel) }
            }
        },
        dismissButton = if (onCommit == null) null else {
            { TextButton(onClick = onDismiss) { Text("Cancel") } }
        },
    )
}

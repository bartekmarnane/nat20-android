package au.com.evonet.nat20.ui.roll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollResult
import au.com.evonet.nat20.dnd5e.core.RollSpec

/**
 * A modal wrapper around [RollResultView] for one-off rolls (checks, saves,
 * death saves, hit dice). [onSettled] fires with the result; consumers that
 * journal an outcome (death save, hit die) apply their intent there. The dialog
 * stays open after settling so the player can read the result and roll again.
 */
@Composable
fun RollDialog(
    title: String,
    spec: RollSpec,
    bonuses: List<RollBonus> = emptyList(),
    allowAdvantageToggle: Boolean = true,
    onSettled: (RollResult) -> Unit = {},
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RollResultView(
                    baseSpec = spec,
                    bonuses = bonuses,
                    allowAdvantageToggle = allowAdvantageToggle,
                    onSettled = onSettled,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectSource
import au.com.evonet.nat20.dnd5e.core.RestKind

/**
 * The active-effect detail dialog (A17), shown when an effect chip is tapped on
 * the Stats tab's Effects strip: source, duration, and typed-modifier list plus
 * a cancel affordance. The former Combat-tab **Active Effects** management
 * section (concentration save, ally-cast receive, round advance) moved into the
 * Act sheet (parity #19 slice C): concentration saves chain off the Take-damage
 * picker, ally-cast is the "Ally's spell" tile, and Rounds-bound effects are
 * cancelled here (matching iOS, which has no round tracker).
 */
@Composable
internal fun EffectDetailDialog(effect: ActiveEffect, onCancelEffect: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(effect.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Source: ${effect.source.label()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Duration: ${effect.duration.label()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                effect.modifiers.forEach { Text("• ${it.impactLabel()}", style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = onCancelEffect) { Text("Cancel effect", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

// ── Display helpers ───────────────────────────────────────────────────────────

private fun EffectSource.label(): String = when (this) {
    is EffectSource.Spell -> "Spell"
    is EffectSource.Item -> "Item"
    is EffectSource.Feature -> "Class feature"
    EffectSource.Custom -> "Applied effect"
    is EffectSource.ExternalCaster -> "Cast by $name"
}

private fun EffectDuration.label(): String = when (this) {
    EffectDuration.UntilCancelled -> "Until cancelled"
    EffectDuration.Concentration -> "Concentration"
    is EffectDuration.UntilRest -> if (rest == RestKind.SHORT) "Until a short rest" else "Until a long rest"
    is EffectDuration.Rounds -> "$count round${if (count == 1) "" else "s"}"
}

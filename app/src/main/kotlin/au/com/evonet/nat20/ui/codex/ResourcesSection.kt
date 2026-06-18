package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.LongRest
import au.com.evonet.nat20.dnd5e.ResolvedResourcePool
import au.com.evonet.nat20.dnd5e.ShortRest
import au.com.evonet.nat20.dnd5e.SpendResource
import au.com.evonet.nat20.dnd5e.UseClassFeature
import au.com.evonet.nat20.dnd5e.availableClassFeatures
import au.com.evonet.nat20.dnd5e.availableResourcePools
import au.com.evonet.nat20.dnd5e.hasClassResources
import au.com.evonet.nat20.domain.CharacterIntent

/**
 * The Combat-tab **Rest** and **Class Resources** sections (A10). Rest is
 * universal (a barbarian needs it as much as a wizard); resources show point
 * pools (Ki/Sorcery Points/Lay on Hands) and use-counter features (Rage/Action
 * Surge/…), each spendable in place. All edits reuse the tested intents.
 */

@Composable
internal fun RestSection(onApplyIntent: (CharacterIntent) -> Unit) {
    SectionCard("Rest") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onApplyIntent(ShortRest()) }) { Text("Short rest") }
            OutlinedButton(onClick = { onApplyIntent(LongRest()) }) { Text("Long rest") }
        }
    }
}

@Composable
internal fun ClassResourcesSection(payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit) {
    if (!payload.hasClassResources()) return
    var spendPool by remember { mutableStateOf<ResolvedResourcePool?>(null) }

    fun apply(intent: CharacterIntent) = onApplyIntent(intent)

    SectionCard("Class Resources") {
        val pools = payload.availableResourcePools()
        val features = payload.availableClassFeatures()
        pools.forEachIndexed { i, pool ->
            if (i > 0) ResourceDivider()
            ResourceRow(
                name = pool.displayName,
                value = "${pool.current}/${pool.max}",
                actionLabel = "Spend",
                enabled = pool.current > 0,
                onAction = { spendPool = pool },
            )
        }
        features.forEachIndexed { i, feature ->
            if (i > 0 || pools.isNotEmpty()) ResourceDivider()
            ResourceRow(
                name = feature.displayName,
                value = if (feature.max == null) "∞" else "${feature.current}/${feature.max}",
                actionLabel = "Use",
                enabled = feature.max == null || (feature.current ?: 0) > 0,
                onAction = {
                    apply(UseClassFeature(feature.id, feature.displayName, feature.recovery, usesRemaining = feature.current))
                },
            )
        }
    }

    spendPool?.let { pool ->
        SpendResourceDialog(
            pool = pool,
            onSpend = { amount -> apply(SpendResource(pool.id, amount)); spendPool = null },
            onDismiss = { spendPool = null },
        )
    }
}

@Composable
private fun ResourceRow(name: String, value: String, actionLabel: String, enabled: Boolean, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 8.dp),
        )
        TextButton(enabled = enabled, onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun ResourceDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

@Composable
private fun SpendResourceDialog(pool: ResolvedResourcePool, onSpend: (Int) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spend ${pool.displayName}") },
        text = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(enabled = amount > 1, onClick = { amount-- }) { Text("−", style = MaterialTheme.typography.headlineSmall) }
                Text("$amount", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                TextButton(enabled = amount < pool.current, onClick = { amount++ }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
                Text("of ${pool.current}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onSpend(amount.coerceAtMost(pool.current)) }) { Text("Spend") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

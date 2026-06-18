package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.dnd5e.DnD5eRuleset
import au.com.evonet.nat20.ui.actions.DnD5eActionsSheet

/**
 * The character sheet host: universal chrome + per-ruleset body dispatch (the
 * iOS `CharacterSheetView` pattern). A7b makes the chrome **phase-aware**:
 * building → Edit + Start Campaign; in a campaign → Actions + Journal, with an
 * active-campaign banner and End in context.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    character: Character,
    activeCampaign: Campaign?,
    hasPastAdventures: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStartCampaign: (String) -> Unit,
    onEndCampaign: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPastAdventures: () -> Unit,
    onApplyIntent: (CharacterIntent) -> Unit,
) {
    val inCampaign = character.phase is CharacterPhase.InCampaign
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(character.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    if (inCampaign) {
                        TextButton(onClick = { showActions = true }) { Text("Actions") }
                        TextButton(onClick = onOpenJournal) { Text("Journal") }
                    } else {
                        if (hasPastAdventures) {
                            TextButton(onClick = onOpenPastAdventures) { Text("Past") }
                        }
                        TextButton(onClick = onEdit) { Text("Edit") }
                        TextButton(onClick = { showStart = true }) { Text("Start") }
                    }
                },
            )
        },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (inCampaign && activeCampaign != null) {
                ActiveCampaignBanner(activeCampaign.name, onEnd = { showEnd = true })
            }
            when (character.rulesetId) {
                DnD5eRuleset.RULESET_ID -> DnD5eSheetView(character, Modifier.fillMaxSize())
                else -> UnsupportedRuleset(character.rulesetId)
            }
        }
    }

    if (showStart) {
        StartCampaignDialog(
            defaultName = "${character.name}'s Adventure",
            onDismiss = { showStart = false },
            onConfirm = { name -> showStart = false; onStartCampaign(name) },
        )
    }

    if (showEnd) {
        AlertDialog(
            onDismissRequest = { showEnd = false },
            title = { Text("End campaign?") },
            text = { Text("${character.name} returns to building. The journal is kept in Past Adventures.") },
            confirmButton = {
                TextButton(onClick = { showEnd = false; onEndCampaign() }) { Text("End") }
            },
            dismissButton = { TextButton(onClick = { showEnd = false }) { Text("Cancel") } },
        )
    }

    if (showActions && activeCampaign != null) {
        DnD5eActionsSheet(
            onDismiss = { showActions = false },
            onAct = onApplyIntent,
        )
    }
}

@Composable
private fun ActiveCampaignBanner(name: String, onEnd: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "In campaign · $name",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEnd) {
                Text("End", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartCampaignDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start a campaign") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Campaign name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) },
            ) { Text("Start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun UnsupportedRuleset(rulesetId: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            "No sheet for ruleset \"$rulesetId\" yet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

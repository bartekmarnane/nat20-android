package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import au.com.evonet.nat20.domain.PartyMember
import au.com.evonet.nat20.ui.campaign.CampaignSetupScreen
import au.com.evonet.nat20.dnd5e.DnD5eRuleset
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Ruleset
import au.com.evonet.nat20.pf2e.PathfinderRuleset
import au.com.evonet.nat20.ui.codex.CodexShellView

/**
 * The character sheet host: per-ruleset body dispatch (the iOS
 * `CharacterSheetView` pattern). The 2014 (parity #12) and 2024 (parity #24)
 * codices each own their own parchment chrome via the shared `CodexScaffold`
 * (nav row, hero, campaign region, tab bar), so the host renders them bare and
 * only keeps the Start/End campaign dialogs. PF2e (and any unsupported ruleset)
 * still uses the phase-aware Material Scaffold: building → Edit + Start
 * Campaign; in a campaign → Journal, with an active-campaign banner + End.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    character: Character,
    activeCampaign: Campaign?,
    hasPastAdventures: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStartCampaign: (String, List<PartyMember>) -> Unit,
    onRenameCampaign: (String) -> Unit,
    onUpdateParty: (List<PartyMember>) -> Unit,
    onLeaveCampaign: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPastAdventures: () -> Unit,
    onOpenCharacterSettings: () -> Unit,
    onBrowseSpells: () -> Unit,
    onApplyIntent: (CharacterIntent) -> Unit,
    onSave: (Character) -> Unit,
) {
    val inCampaign = character.phase is CharacterPhase.InCampaign
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    if (character.rulesetId == DnD5eRuleset.RULESET_ID) {
        // 2014: the shell IS the chrome (it also hosts the #19 actions layer
        // behind its Act pill); dialogs stay hosted here below.
        CodexShellView(
            character = character,
            activeCampaign = activeCampaign,
            hasPastAdventures = hasPastAdventures,
            onBack = onBack,
            onEdit = onEdit,
            onStartCampaign = { showStart = true },
            onEndCampaign = { showEnd = true },
            onOpenJournal = onOpenJournal,
            onOpenPastAdventures = onOpenPastAdventures,
            onOpenCharacterSettings = onOpenCharacterSettings,
            onBrowseSpells = onBrowseSpells,
            onApplyIntent = onApplyIntent,
            onSave = onSave,
            modifier = Modifier.fillMaxSize(),
        )
    } else if (character.rulesetId == DnD5e2024Ruleset.RULESET_ID) {
        // 2024 (parity #24–30): owns the shared parchment chrome via CodexScaffold,
        // exactly like the 2014 shell. The Act pill / actions layer is slice #31;
        // dialogs (Start / End campaign) stay hosted here below.
        Codex2024ShellView(
            character = character,
            activeCampaign = activeCampaign,
            hasPastAdventures = hasPastAdventures,
            onBack = onBack,
            onEdit = onEdit,
            onStartCampaign = { showStart = true },
            onEndCampaign = { showEnd = true },
            onOpenJournal = onOpenJournal,
            onOpenPastAdventures = onOpenPastAdventures,
            onOpenCharacterSettings = onOpenCharacterSettings,
            onApplyIntent = onApplyIntent,
            onSave = onSave,
            modifier = Modifier.fillMaxSize(),
        )
    } else if (character.rulesetId == PathfinderRuleset.RULESET_ID) {
        // PF2e (parity #34–35): single read-only scroll on the shared parchment
        // chrome (no tab bar), with the Act pill opening the PF2e actions layer.
        PathfinderSheetView(
            character = character,
            activeCampaign = activeCampaign,
            hasPastAdventures = hasPastAdventures,
            onBack = onBack,
            onEdit = onEdit,
            onStartCampaign = { showStart = true },
            onEndCampaign = { showEnd = true },
            onOpenJournal = onOpenJournal,
            onOpenPastAdventures = onOpenPastAdventures,
            onOpenCharacterSettings = onOpenCharacterSettings,
            onApplyIntent = onApplyIntent,
            onSave = onSave,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(character.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    actions = {
                        if (inCampaign) {
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
                UnsupportedRuleset(character.rulesetId)
            }
        }
    }

    // Start Adventuring → the full campaign setup screen (create mode).
    if (showStart) {
        CampaignSetupScreen(
            create = true,
            initialName = "${character.name}'s Adventure",
            onCancel = { showStart = false },
            onBegin = { name, party -> showStart = false; onStartCampaign(name, party) },
        )
    }

    // The in-campaign "End"/settings affordance opens the setup screen in edit
    // mode (rename + party roster + Leave); iOS also leaves from settings.
    if (showEnd && activeCampaign != null) {
        CampaignSetupScreen(
            create = false,
            initialName = activeCampaign.name,
            initialParty = activeCampaign.party,
            onCancel = { showEnd = false },
            onRename = onRenameCampaign,
            onPartyChange = onUpdateParty,
            onLeave = { showEnd = false; onLeaveCampaign() },
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

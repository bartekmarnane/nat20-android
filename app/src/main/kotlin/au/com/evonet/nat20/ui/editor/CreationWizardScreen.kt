package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5eRuleset
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Ruleset
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.pf2e.PathfinderRuleset
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The unified character-creation flow (iOS `CharacterCreationWizard`): a single
 * shell whose first step picks the ruleset, then hands off to that edition's
 * wizard hosted in the same [EditorShell] chrome with `stepOffset = 1`, so the
 * diamond rail reads as one continuous journey. Replaces the old
 * EditionChooser dialog + three separately-chromed wizard routes.
 *
 * Re-entering the ruleset step resets the edition wizard's draft (iOS keeps a
 * draft per edition — deferred).
 */
@Composable
fun CreationWizardScreen(onSave: (Character) -> Unit, onCancel: () -> Unit) {
    var edition by rememberSaveable { mutableStateOf<String?>(null) }
    var onRulesetStep by rememberSaveable { mutableStateOf(true) }
    val chosen = edition

    if (onRulesetStep || chosen == null) {
        RulesetStep(
            edition = chosen,
            onPick = { edition = it },
            onContinue = { onRulesetStep = false },
            onCancel = onCancel,
        )
    } else {
        val backToRuleset = { onRulesetStep = true }
        when (chosen) {
            DnD5eRuleset.RULESET_ID -> DnD5eWizardScreen(
                existing = null,
                onSave = onSave,
                onCancel = onCancel,
                stepOffset = 1,
                onExitFirstStep = backToRuleset,
            )
            DnD5e2024Ruleset.RULESET_ID -> DnD5e2024WizardScreen(
                onSave = onSave,
                onCancel = onCancel,
                stepOffset = 1,
                onExitFirstStep = backToRuleset,
            )
            PathfinderRuleset.RULESET_ID -> PathfinderWizardScreen(
                onSave = onSave,
                onCancel = onCancel,
                stepOffset = 1,
                onExitFirstStep = backToRuleset,
            )
            else -> RulesetStep(
                edition = null,
                onPick = { edition = it },
                onContinue = { onRulesetStep = false },
                onCancel = onCancel,
            )
        }
    }
}

/** Step 1 — pick the ruleset (iOS `RulesetStepMerged`). */
@Composable
private fun RulesetStep(
    edition: String?,
    onPick: (String) -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    // All three edition wizards open with 7 steps (their conditional steps appear
    // as choices are made), so a chosen edition previews 1 + 7 total diamonds.
    val stepCount = if (edition == null) 1 else 8
    EditorShell(
        kicker = "Step 1 of $stepCount",
        title = "New Character",
        stepCount = stepCount,
        currentIndex = 0,
        onBack = onCancel,
        onJump = null,
        footer = {
            WizardSecondaryButton("Cancel", onCancel)
            Spacer(Modifier.weight(1f))
            WizardPrimaryButton("Continue", enabled = edition != null, onClick = onContinue)
        },
    ) {
        WizardStepSection("Ruleset", "Each character runs on its own ruleset — pick one to begin.")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EditionChoiceRow(
                name = "5th Edition (2014)",
                blurb = "The classic fifth-edition rules — the broadest catalogue of races, classes, and spells.",
                selected = edition == DnD5eRuleset.RULESET_ID,
            ) { onPick(DnD5eRuleset.RULESET_ID) }
            EditionChoiceRow(
                name = "5th Edition (2024)",
                blurb = "The revised 2024 rules — species, backgrounds with ability boosts, and weapon masteries.",
                selected = edition == DnD5e2024Ruleset.RULESET_ID,
            ) { onPick(DnD5e2024Ruleset.RULESET_ID) }
            EditionChoiceRow(
                name = "Pathfinder 2e",
                blurb = "Paizo's d20 — three actions, four degrees of success, deep customisation.",
                selected = edition == PathfinderRuleset.RULESET_ID,
            ) { onPick(PathfinderRuleset.RULESET_ID) }
        }
        // iOS also shows a Content Sources disclosure under the selected 2014 row — deferred until the source-catalogue port.
    }
}

/** One selectable ruleset tile: selection dot + name + one-line blurb. */
@Composable
private fun EditionChoiceRow(name: String, blurb: String, selected: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(4.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) palette.tileStrong else palette.tile)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) palette.accent else palette.ink.copy(alpha = 0.2f),
                shape,
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .border(1.4.dp, palette.accent.copy(alpha = if (selected) 1f else 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(palette.accent))
            }
        }
        Column {
            Text(name, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = palette.ink)
            Spacer(Modifier.height(3.dp))
            Text(blurb, fontFamily = EbGaramond, fontSize = 13.sp, color = palette.inkSoft)
        }
    }
}

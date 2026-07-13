package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.AdvanceRound
import au.com.evonet.nat20.dnd5e.ApplyEffect
import au.com.evonet.nat20.dnd5e.CancelEffect
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.EndConcentration
import au.com.evonet.nat20.dnd5e.RollConcentrationSave
import au.com.evonet.nat20.dnd5e.SpellEffectCatalog
import au.com.evonet.nat20.dnd5e.savingThrowBonus
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.EffectSource
import au.com.evonet.nat20.dnd5e.core.RestKind
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The Combat-tab **Active Effects** extra (A17, Android interaction layer —
 * pending #19): a concentration banner with Save/End, chips for every live
 * effect (tap → detail + cancel), free-text riders, round advance and ally-cast
 * receive. Chrome restyled to SectionHead + codex chips (parity #15).
 */
@Composable
internal fun EffectsSection(payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit) {
    val palette = MaterialTheme.natPalette
    var detail by remember { mutableStateOf<ActiveEffect?>(null) }
    var savingFocus by remember { mutableStateOf<String?>(null) }
    var receivingAlly by remember { mutableStateOf(false) }
    val hasRoundBound = payload.activeEffects.any { it.duration is EffectDuration.Rounds }

    SectionHead("Active Effects", top = 22.dp, bottom = 10.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        payload.concentratingOn?.let { focus ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Concentrating on $focus",
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontSize = 15.sp,
                    color = palette.accent,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { savingFocus = focus }) { Text("Save") }
                TextButton(onClick = { onApplyIntent(EndConcentration()) }) { Text("End") }
            }
        }
        if (payload.activeEffects.isEmpty()) {
            DashedNotice("No lasting effects.")
        } else {
            EffectStrip(payload.activeEffects) { detail = it }
        }
        // Free-text rules the engine doesn't model mechanically (e.g. "Double speed, extra action").
        val freeText = payload.activeEffects.flatMap { e -> e.modifiers.filterIsInstance<EffectModifier.FreeText>().map { e.name to it.text } }
        freeText.forEach { (name, text) ->
            Text(
                "• $name: $text",
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = palette.inkMute,
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasRoundBound) {
                CodexButton("Advance round", Modifier.weight(1f), onClick = { onApplyIntent(AdvanceRound()) })
            }
            CodexButton("Receive ally's spell", Modifier.weight(1f), onClick = { receivingAlly = true })
        }
    }

    if (receivingAlly) {
        AllySpellDialog(onApply = { spellId, caster ->
            SpellEffectCatalog.allyCast(spellId, caster)?.let { onApplyIntent(ApplyEffect(it)) }
            receivingAlly = false
        }, onDismiss = { receivingAlly = false })
    }

    detail?.let { e ->
        EffectDetailDialog(
            effect = e,
            onCancelEffect = { onApplyIntent(CancelEffect(e.id)); detail = null },
            onDismiss = { detail = null },
        )
    }

    savingFocus?.let { focus ->
        ConcentrationSaveDialog(
            payload = payload,
            focus = focus,
            onApplyIntent = onApplyIntent,
            onDismiss = { savingFocus = null },
        )
    }
}

/**
 * Rolls a concentration check (CON save) against a DC the player sets from the
 * triggering damage (the damage event surfaces it: max of 10 and half the
 * damage). A failed roll auto-ends concentration via [RollConcentrationSave];
 * the result resolves once so a re-roll can't double-break it.
 */
@Composable
private fun ConcentrationSaveDialog(
    payload: DnD5ePayload,
    focus: String,
    onApplyIntent: (CharacterIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var dc by remember { mutableIntStateOf(10) }
    var maintained by remember { mutableStateOf<Boolean?>(null) }
    val conBonus = payload.savingThrowBonus(Ability.CONSTITUTION)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Concentration — $focus") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Save DC", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { if (dc > 10) dc-- }, enabled = dc > 10 && maintained == null) { Text("−") }
                    Text("$dc", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                    OutlinedButton(onClick = { dc++ }, enabled = maintained == null) { Text("+") }
                }
                Text(
                    "DC is the higher of 10 and half the damage taken.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RollResultView(
                    baseSpec = RollSpec.d(1, 20),
                    bonuses = listOf(RollBonus("CON", conBonus)),
                    onSettled = { result ->
                        if (maintained == null) {
                            val d20 = result.naturalD20 ?: return@RollResultView
                            maintained = (d20 + conBonus) >= dc
                            onApplyIntent(RollConcentrationSave(d20, dc))
                        }
                    },
                )
                maintained?.let { ok ->
                    Text(
                        if (ok) "Concentration holds." else "Concentration broken.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (ok) palette.accent else palette.danger,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/**
 * Picks a spell an ally cast on the character (A17 ally-cast effects): a buff from
 * the TARGET_PICKED catalogue + the caster's name → an [ApplyEffect] carrying an
 * ExternalCaster source (the ally holds any concentration, not this character).
 */
@Composable
private fun AllySpellDialog(onApply: (String, String) -> Unit, onDismiss: () -> Unit) {
    var caster by remember { mutableStateOf("") }
    var spellId by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive an ally's spell") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = caster,
                    onValueChange = { caster = it },
                    label = { Text("Caster (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SpellEffectCatalog.targetable.forEach { (id, template) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { spellId = id }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (spellId == id) "●" else "○", color = MaterialTheme.colorScheme.primary)
                        Text("  ${template.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = spellId != null, onClick = { onApply(spellId!!, caster) }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Effect detail: source, duration, modifiers, and a cancel affordance. */
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

package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.EndConcentration
import au.com.evonet.nat20.dnd5e.CancelEffect
import au.com.evonet.nat20.dnd5e.RollConcentrationSave
import au.com.evonet.nat20.dnd5e.savingThrowBonus
import au.com.evonet.nat20.dnd5e.core.ACOverrideFormula
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

private val BuffGreen = Color(0xFF246B29)
private val DebuffRed = Color(0xFF8C1A1A)

/**
 * The Combat-tab **Active Effects** section (A17): a concentration banner with
 * an End button, plus chips for every live effect (green buff / red debuff /
 * gold mixed). Tapping a chip opens its detail — source, duration, full modifier
 * list — with a Cancel affordance. All edits journal in-campaign.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EffectsSection(payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit) {
    if (payload.concentratingOn == null && payload.activeEffects.isEmpty()) return
    var detail by remember { mutableStateOf<ActiveEffect?>(null) }
    var savingFocus by remember { mutableStateOf<String?>(null) }

    SectionCard("Active Effects") {
        payload.concentratingOn?.let { focus ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Concentrating on $focus", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                TextButton(onClick = { savingFocus = focus }) { Text("Save") }
                TextButton(onClick = { onApplyIntent(EndConcentration()) }) { Text("End") }
            }
        }
        if (payload.activeEffects.isEmpty()) {
            Text("No lasting effects.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                payload.activeEffects.forEach { e -> EffectChip(e) { detail = e } }
            }
        }
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
                        color = if (ok) BuffGreen else DebuffRed,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun EffectChip(effect: ActiveEffect, onClick: () -> Unit) {
    val tone = effect.tone()
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tone.copy(alpha = 0.16f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(effect.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = tone)
        val summary = effect.modifiers.joinToString(" · ") { it.shortLabel() }.takeIf { it.isNotBlank() }
        if (summary != null) {
            Text("  $summary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EffectDetailDialog(effect: ActiveEffect, onCancelEffect: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(effect.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Source: ${effect.source.label()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Duration: ${effect.duration.label()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                effect.modifiers.forEach { Text("• ${it.shortLabel()}", style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = onCancelEffect) { Text("Cancel effect", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

// ── Display helpers ───────────────────────────────────────────────────────────

@Composable
private fun ActiveEffect.tone(): Color {
    val negative = modifiers.any {
        (it is EffectModifier.AcBonus && it.value < 0) || (it is EffectModifier.AttackBonus && it.value < 0) ||
            (it is EffectModifier.DamageBonus && it.value < 0) || (it is EffectModifier.SaveBonus && it.value < 0) ||
            (it is EffectModifier.AbilityDelta && it.value < 0) || it is EffectModifier.Condition
    }
    val positive = modifiers.any {
        (it is EffectModifier.AcBonus && it.value > 0) || it is EffectModifier.AcOverride ||
            (it is EffectModifier.AttackBonus && it.value > 0) || (it is EffectModifier.DamageBonus && it.value > 0) ||
            (it is EffectModifier.SaveBonus && it.value > 0) || it is EffectModifier.DamageResistance || it is EffectModifier.AdvantageOn
    }
    return when {
        negative && positive -> MaterialTheme.colorScheme.primary // mixed → gold
        negative -> DebuffRed
        else -> BuffGreen
    }
}

private fun EffectModifier.shortLabel(): String = when (this) {
    is EffectModifier.AcBonus -> "${value.signs()} AC"
    is EffectModifier.AcOverride -> when (val f = formula) {
        is ACOverrideFormula.BaseDex -> "AC = ${f.base} + DEX"
        is ACOverrideFormula.BaseDexAbility -> "AC = ${f.base} + DEX + ${f.ability.abbreviation}"
    }
    is EffectModifier.AbilityDelta -> "${value.signs()} ${ability.abbreviation}"
    is EffectModifier.AbilitySet -> "${ability.abbreviation} = $value"
    is EffectModifier.SaveBonus -> "${value.signs()} ${ability?.abbreviation ?: "all"} save"
    is EffectModifier.AttackBonus -> "${value.signs()} attack"
    is EffectModifier.DamageBonus -> "${value.signs()} damage"
    is EffectModifier.SkillBonus -> "${value.signs()} ${if (skillId == "__any__") "one" else skillId} skill"
    is EffectModifier.DamageResistance -> "resist $type"
    is EffectModifier.AdvantageOn -> "adv: $descriptor"
    is EffectModifier.Condition -> name
    is EffectModifier.FreeText -> text
}

private fun Int.signs(): String = if (this >= 0) "+$this" else "$this"

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

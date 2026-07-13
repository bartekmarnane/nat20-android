package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DismissSummon
import au.com.evonet.nat20.dnd5e.DnD5eCreaturePayload
import au.com.evonet.nat20.dnd5e.SetCreatureHp
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.Creature
import au.com.evonet.nat20.domain.Summon
import au.com.evonet.nat20.domain.SummonLifecycle
import au.com.evonet.nat20.domain.SummonOrigin
import au.com.evonet.nat20.ui.editor.StepperButton
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The Lore-tab **Minions** section (A18, moved from Combat per iOS parity #18):
 * familiars / mounts / beast companions / summons as expandable cards — header
 * name + summary + chevron, expanded statblock with a per-creature HP stepper
 * and a confirmed Dismiss. View + dismiss only (parity #19 slice C): summoning
 * happens through the Act sheet's summon pickers, matching iOS where the Lore
 * page never summons. Reads the cross-ruleset `Character.summons`.
 */
@Composable
internal fun MinionsSection(character: Character, onApplyIntent: (CharacterIntent) -> Unit) {
    SectionHead("Minions", top = 22.dp, bottom = 10.dp)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (character.summons.isEmpty()) {
            DashedNotice("No familiars, mounts, or summons. Summon one from the Act menu while adventuring.")
        } else {
            character.summons.forEach { summon -> MinionCard(summon, onApplyIntent) }
        }
    }
}

/** Expandable minion card: tile r8 hairline; expanded = statblock + dismiss. */
@Composable
private fun MinionCard(summon: Summon, onApplyIntent: (CharacterIntent) -> Unit) {
    val palette = MaterialTheme.natPalette
    var expanded by remember { mutableStateOf(false) }
    var confirmingDismiss by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.15f), shape)
            .clickable { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    summon.label,
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = palette.ink,
                )
                Text(
                    lifecycleLabel(summon.lifecycle) + originLabel(summon.origin),
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = palette.inkSoft,
                )
            }
            Text(
                if (expanded) "▾" else "▸",
                fontSize = 13.sp,
                color = palette.inkMute,
            )
        }
        if (expanded) {
            summon.creatures.forEach { creature -> CreatureBlock(summon, creature, onApplyIntent) }
            CodexButton("Dismiss", danger = true, onClick = { confirmingDismiss = true })
        }
    }
    if (confirmingDismiss) {
        AlertDialog(
            onDismissRequest = { confirmingDismiss = false },
            title = { Text("Dismiss ${summon.label}?") },
            text = { Text("The creature departs; this is recorded in the journal while adventuring.") },
            confirmButton = {
                TextButton(onClick = { onApplyIntent(DismissSummon(summon.id)); confirmingDismiss = false }) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmingDismiss = false }) { Text("Cancel") } },
        )
    }
}

/** One creature inside an expanded minion card: HP stepper + statblock lines. */
@Composable
private fun CreatureBlock(summon: Summon, creature: Creature, onApplyIntent: (CharacterIntent) -> Unit) {
    val palette = MaterialTheme.natPalette
    val stat = remember(creature.rulesetPayload) { runCatching { DnD5eCreaturePayload.decode(creature.rulesetPayload) }.getOrNull() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        DashedRule()
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                creature.name,
                fontFamily = Cormorant,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = palette.ink,
                modifier = Modifier.weight(1f),
            )
            StepperButton("−", enabled = creature.currentHp > 0, size = 26) {
                onApplyIntent(SetCreatureHp(summon.id, creature.id, creature.currentHp - 1))
            }
            Text(
                "${creature.currentHp}/${creature.maxHp}",
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = palette.accent,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            StepperButton("+", enabled = creature.currentHp < creature.maxHp, size = 26) {
                onApplyIntent(SetCreatureHp(summon.id, creature.id, creature.currentHp + 1))
            }
        }
        stat?.let { s ->
            Text(
                "${s.size} ${s.type}  ·  AC ${s.armorClass}  ·  ${s.speed}",
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = palette.inkMute,
            )
            if (s.attacks.isNotEmpty()) {
                Text(
                    s.attacks.joinToString("\n") { it.display },
                    fontFamily = EbGaramond,
                    fontSize = 12.sp,
                    color = palette.inkSoft,
                )
            }
            if (s.traits.isNotEmpty()) {
                Text(
                    s.traits.joinToString(", "),
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = 11.sp,
                    color = palette.accent,
                )
            }
        }
    }
}

private fun lifecycleLabel(l: SummonLifecycle): String = when (l) {
    is SummonLifecycle.Persistent -> "Persistent"
    is SummonLifecycle.Concentration -> "Concentration"
    is SummonLifecycle.Timed -> "Timed"
    is SummonLifecycle.ControlReassertion -> "Controlled"
}

private fun originLabel(o: SummonOrigin): String = when (o) {
    is SummonOrigin.Spell -> " · ${o.name}"
    is SummonOrigin.ClassFeature -> " · ${o.featureName}"
    is SummonOrigin.RacialTrait -> " · ${o.name}"
    is SummonOrigin.Item -> ""
    SummonOrigin.Manual -> ""
}

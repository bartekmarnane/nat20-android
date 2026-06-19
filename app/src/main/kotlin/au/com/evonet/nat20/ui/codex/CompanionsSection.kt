package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.CreatureCatalog
import au.com.evonet.nat20.dnd5e.CreatureKind
import au.com.evonet.nat20.dnd5e.DismissSummon
import au.com.evonet.nat20.dnd5e.DnD5eCreaturePayload
import au.com.evonet.nat20.dnd5e.SetCreatureHp
import au.com.evonet.nat20.dnd5e.SummonCreature
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.Creature
import au.com.evonet.nat20.domain.Summon
import au.com.evonet.nat20.domain.SummonLifecycle
import au.com.evonet.nat20.domain.SummonOrigin
import java.time.Instant

/**
 * Companions & summons (A18): the character's familiars / mounts / beast
 * companions / summoned creatures, each a statblock card with a per-creature HP
 * stepper + dismiss. A summon picker mints new ones from [CreatureCatalog] (or a
 * homebrew creature). Reads the cross-ruleset `Character.summons`.
 */
@Composable
internal fun CompanionsSection(character: Character, onApplyIntent: (CharacterIntent) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    SectionCard("Companions & Summons") {
        if (character.summons.isEmpty()) {
            Text("No familiars, mounts, or summons.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            character.summons.forEachIndexed { i, summon ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                SummonCard(summon, onApplyIntent)
            }
        }
        OutlinedButton(onClick = { picking = true }) { Text("Summon a creature") }
    }
    if (picking) SummonPickerDialog(onSummon = { onApplyIntent(it); picking = false }, onDismiss = { picking = false })
}

@Composable
private fun SummonCard(summon: Summon, onApplyIntent: (CharacterIntent) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(summon.label, fontWeight = FontWeight.Medium)
                Text(lifecycleLabel(summon.lifecycle) + originLabel(summon.origin), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onApplyIntent(DismissSummon(summon.id)) }) { Text("Dismiss") }
        }
        summon.creatures.forEach { creature -> CreatureRow(summon.id, creature, onApplyIntent) }
    }
}

@Composable
private fun CreatureRow(summonId: java.util.UUID, creature: Creature, onApplyIntent: (CharacterIntent) -> Unit) {
    val stat = remember(creature.rulesetPayload) { runCatching { DnD5eCreaturePayload.decode(creature.rulesetPayload) }.getOrNull() }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(creature.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                TextButton(onClick = { onApplyIntent(SetCreatureHp(summonId, creature.id, creature.currentHp - 1)) }, enabled = creature.currentHp > 0) { Text("−") }
                Text("${creature.currentHp}/${creature.maxHp}", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { onApplyIntent(SetCreatureHp(summonId, creature.id, creature.currentHp + 1)) }, enabled = creature.currentHp < creature.maxHp) { Text("+") }
            }
            stat?.let { s ->
                Text("${s.size} ${s.type}  ·  AC ${s.armorClass}  ·  ${s.speed}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (s.attacks.isNotEmpty()) Text(s.attacks.joinToString("\n") { it.display }, style = MaterialTheme.typography.labelSmall)
                if (s.traits.isNotEmpty()) Text(s.traits.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummonPickerDialog(onSummon: (SummonCreature) -> Unit, onDismiss: () -> Unit) {
    var kind by remember { mutableStateOf(CreatureKind.FAMILIAR) }
    var templateId by remember { mutableStateOf<String?>(null) }
    var customName by remember { mutableStateOf("") }
    var count by remember { mutableIntStateOf(1) }
    val pool = remember(kind) { CreatureCatalog.all.filter { it.kind == kind } }
    val template = templateId?.let(CreatureCatalog::template)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Summon a creature") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CreatureKind.entries.forEach { k ->
                        FilterChip(kind == k, { kind = k; templateId = null }, label = { Text(kindLabel(k)) })
                    }
                }
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(pool, key = { it.id }) { t ->
                        Row(Modifier.fillMaxWidth().clickable { templateId = t.id }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (templateId == t.id) "●" else "○", color = if (templateId == t.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(t.name, style = MaterialTheme.typography.bodyLarge)
                                Text("${t.size} ${t.type} · AC ${t.armorClass} · ${t.maxHp} HP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                OutlinedTextField(customName, { customName = it }, label = { Text("Name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (kind == CreatureKind.SUMMON) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Count", Modifier.weight(1f))
                        TextButton(enabled = count > 1, onClick = { count-- }) { Text("−") }
                        Text("$count", fontWeight = FontWeight.Bold)
                        TextButton(enabled = count < 8, onClick = { count++ }) { Text("+") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = template != null, onClick = {
                val t = template!!
                val origin = when (t.kind) {
                    CreatureKind.FAMILIAR -> SummonOrigin.Spell("Find Familiar", 1)
                    CreatureKind.MOUNT -> SummonOrigin.Spell("Find Steed", 2)
                    CreatureKind.COMPANION -> SummonOrigin.ClassFeature("Ranger", "Animal Companion")
                    CreatureKind.SUMMON -> SummonOrigin.Spell("Conjure Animals", 3)
                }
                val lifecycle = when (t.kind) {
                    CreatureKind.FAMILIAR, CreatureKind.MOUNT, CreatureKind.COMPANION -> SummonLifecycle.Persistent
                    CreatureKind.SUMMON -> SummonLifecycle.Concentration(au.com.evonet.nat20.domain.GameDuration.Hours(1))
                }
                val summon = CreatureCatalog.buildSummon(t, origin, lifecycle, Instant.now(), count = if (t.kind == CreatureKind.SUMMON) count else 1, customName = customName)
                onSummon(SummonCreature(summon))
            }) { Text("Summon") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun kindLabel(k: CreatureKind): String = when (k) {
    CreatureKind.FAMILIAR -> "Familiars"
    CreatureKind.MOUNT -> "Mounts"
    CreatureKind.COMPANION -> "Companions"
    CreatureKind.SUMMON -> "Summons"
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

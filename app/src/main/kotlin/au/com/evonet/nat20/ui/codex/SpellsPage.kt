package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import au.com.evonet.nat20.dnd5e.CastSpell
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.ExpendSpellSlot
import au.com.evonet.nat20.dnd5e.PrepareSpell
import au.com.evonet.nat20.dnd5e.Spell
import au.com.evonet.nat20.dnd5e.UnprepareSpell
import au.com.evonet.nat20.dnd5e.castableSpellIDs
import au.com.evonet.nat20.dnd5e.core.CastingProgression
import au.com.evonet.nat20.dnd5e.isSpellcaster
import au.com.evonet.nat20.dnd5e.maxPactSlots
import au.com.evonet.nat20.dnd5e.pactSlotLevel
import au.com.evonet.nat20.dnd5e.spellcastingClasses
import au.com.evonet.nat20.dnd5e.totalCurrentSlots
import au.com.evonet.nat20.dnd5e.totalMaxSlots
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.slugToTitle

/**
 * The codex **Spells** tab (A10): a live spell-slot tracker (regular + warlock
 * pact, merged for display), the known cantrips, and the prepared/known spell
 * list — all castable in place, with short/long rest to recover slots. Add
 * spells from the SRD library; prepared casters (Wizard/Cleric/Druid/Paladin)
 * fill `preparedSpells`, known casters fill `spellsKnown`.
 *
 * Non-casters get a "browse the library" affordance only.
 */
@Composable
internal fun SpellsPage(
    character: Character,
    payload: DnD5ePayload,
    onBrowseSpells: () -> Unit,
    onApplyIntent: (CharacterIntent) -> Unit,
    onSave: (Character) -> Unit,
) {
    if (!payload.isSpellcaster) {
        CodexPage {
            SectionCard("Spellcasting") {
                Text(
                    "${payload.classes.firstOrNull()?.classId?.slugToTitle() ?: "This class"} isn't a spellcaster.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onBrowseSpells) { Text("Browse Spell Library") }
            }
        }
        return
    }

    val primaryClass = payload.spellcastingClasses.first().classId
    val prepared = CastingProgression.usesPreparation(primaryClass)
    var addLevel by remember { mutableStateOf<Int?>(null) } // null = closed; 0 = cantrip; ≥1 = leveled

    fun applyIntent(intent: CharacterIntent) = onApplyIntent(intent)

    // Learning/forgetting cantrips & known spells is a direct (unlogged) list edit.
    fun editPayload(transform: (DnD5ePayload) -> DnD5ePayload) {
        onSave(character.copy(payload = transform(payload)))
    }

    val castable = payload.castableSpellIDs
    val cantrips = payload.cantripsKnown.mapNotNull(::spell).sortedBy { it.name }
    val leveled = (if (prepared) payload.preparedSpells[primaryClass] else payload.spellsKnown[primaryClass])
        .orEmpty()
        .mapNotNull(::spell)
        .sortedWith(compareBy({ it.level }, { it.name }))

    CodexPage {
        SlotTrackerCard(payload, onCast = { lvl -> applyIntent(ExpendSpellSlot(lvl)) })

        SectionCard("Cantrips") {
            if (cantrips.isEmpty()) {
                EmptyHint("No cantrips known yet.")
            } else {
                cantrips.forEachIndexed { i, s ->
                    if (i > 0) ThinDivider()
                    SpellRow(
                        spell = s,
                        canCast = true,
                        onCast = { applyIntent(CastSpell(s.index, s.name, 0, 0)) },
                        onRemove = { editPayload { it.copy(cantripsKnown = it.cantripsKnown - s.index) } },
                    )
                }
            }
            OutlinedButton(onClick = { addLevel = 0 }) { Text("Add cantrip") }
        }

        SectionCard(if (prepared) "Prepared — ${primaryClass.slugToTitle()}" else "Known — ${primaryClass.slugToTitle()}") {
            if (leveled.isEmpty()) {
                EmptyHint(if (prepared) "Nothing prepared." else "No spells known yet.")
            } else {
                leveled.forEachIndexed { i, s ->
                    if (i > 0) ThinDivider()
                    val hasSlot = (payload.totalCurrentSlots[s.level] ?: 0) > 0
                    SpellRow(
                        spell = s,
                        canCast = hasSlot,
                        onCast = { applyIntent(CastSpell(s.index, s.name, s.level, s.level)) },
                        onRemove = {
                            if (prepared) {
                                applyIntent(UnprepareSpell(s.index, s.name, primaryClass))
                            } else {
                                editPayload { p ->
                                    p.copy(spellsKnown = p.spellsKnown + (primaryClass to (p.spellsKnown[primaryClass].orEmpty() - s.index)))
                                }
                            }
                        },
                    )
                }
            }
            OutlinedButton(onClick = { addLevel = 1 }) { Text(if (prepared) "Prepare a spell" else "Learn a spell") }
        }

        OutlinedButton(onClick = onBrowseSpells, modifier = Modifier.fillMaxWidth()) {
            Text("Browse full Spell Library")
        }
    }

    addLevel?.let { lvl ->
        AddSpellDialog(
            cantrips = lvl == 0,
            alreadyHave = if (lvl == 0) payload.cantripsKnown.toSet() else castable,
            onPick = { picked ->
                if (lvl == 0) {
                    editPayload { it.copy(cantripsKnown = (it.cantripsKnown + picked.index).distinct()) }
                } else if (prepared) {
                    applyIntent(PrepareSpell(picked.index, picked.name, primaryClass))
                } else {
                    editPayload { p ->
                        p.copy(spellsKnown = p.spellsKnown + (primaryClass to (p.spellsKnown[primaryClass].orEmpty() + picked.index).distinct()))
                    }
                }
                addLevel = null
            },
            onDismiss = { addLevel = null },
        )
    }
}

@Composable
private fun SlotTrackerCard(payload: DnD5ePayload, onCast: (Int) -> Unit) {
    val maxSlots = payload.totalMaxSlots
    SectionCard("Spell Slots") {
        if (maxSlots.isEmpty()) {
            EmptyHint("No spell slots at this level. (Rest on the Combat tab.)")
        } else {
            maxSlots.keys.sorted().forEach { level ->
                val max = maxSlots[level] ?: 0
                val remaining = payload.totalCurrentSlots[level] ?: 0
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        levelLabel(level, payload),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "●".repeat(remaining) + "○".repeat((max - remaining).coerceAtLeast(0)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    TextButton(enabled = remaining > 0, onClick = { onCast(level) }) { Text("Use") }
                }
            }
            Text(
                "Recover slots with a rest on the Combat tab.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SpellRow(spell: Spell, canCast: Boolean, onCast: () -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(spell.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "${spell.levelLabel} · ${spell.schoolName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(enabled = canCast, onClick = onCast) { Text("Cast") }
        TextButton(onClick = onRemove) { Text("✕") }
    }
}

@Composable
private fun AddSpellDialog(
    cantrips: Boolean,
    alreadyHave: Set<String>,
    onPick: (Spell) -> Unit,
    onDismiss: () -> Unit,
) {
    val pool = remember(cantrips) {
        DnD5eCatalog.spellLibrary.filter { if (cantrips) it.level == 0 else it.level >= 1 }
    }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        pool.filter { it.index !in alreadyHave && (query.isBlank() || it.name.contains(query, ignoreCase = true)) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cantrips) "Add a cantrip" else "Add a spell") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(filtered, key = { it.index }) { s ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(s) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(s.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                s.levelLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ThinDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

/** A slot-level label, marking the warlock pact level for clarity. */
private fun levelLabel(level: Int, payload: DnD5ePayload): String {
    val base = "Level $level"
    return if (payload.maxPactSlots > 0 && level == payload.pactSlotLevel) "$base (incl. pact)" else base
}

private val spellIndex: Map<String, Spell> by lazy { DnD5eCatalog.spellLibrary.associateBy { it.index } }
private fun spell(id: String): Spell? = spellIndex[id]

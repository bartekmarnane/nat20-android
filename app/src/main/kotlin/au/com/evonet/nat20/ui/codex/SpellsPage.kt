package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.CastSpell
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.ExpendSpellSlot
import au.com.evonet.nat20.dnd5e.PrepareSpell
import au.com.evonet.nat20.dnd5e.Spell
import au.com.evonet.nat20.dnd5e.Spellcasting
import au.com.evonet.nat20.dnd5e.UnprepareSpell
import au.com.evonet.nat20.dnd5e.castableSpellIDs
import au.com.evonet.nat20.dnd5e.core.CastingProgression
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.dnd5e.effectiveAbilityScores
import au.com.evonet.nat20.dnd5e.isSpellcaster
import au.com.evonet.nat20.dnd5e.maxPactSlots
import au.com.evonet.nat20.dnd5e.pactSlotLevel
import au.com.evonet.nat20.dnd5e.spellcastingClasses
import au.com.evonet.nat20.dnd5e.totalCurrentSlots
import au.com.evonet.nat20.dnd5e.totalMaxSlots
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.slugToTitle
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.Diamond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The codex **Spells** tab, iOS order: casting triplet chips · browse button ·
 * Spell Slots tiles · Cantrip pills · Prepared/Known rows. Android keeps its
 * cast / prepare / expend interactions on the tiles, pills, and rows (iOS casts
 * from the actions layer — pending #19); non-casters get the iOS empty state.
 */
@Composable
internal fun SpellsPage(
    character: Character,
    payload: DnD5ePayload,
    onBrowseSpells: () -> Unit,
    onApplyIntent: (CharacterIntent) -> Unit,
    onSave: (Character) -> Unit,
) {
    val palette = MaterialTheme.natPalette

    if (!payload.isSpellcaster) {
        NoSpellsEmptyState(
            subtitle = "${payload.classes.firstOrNull()?.classId?.slugToTitle() ?: "This class"} channels no magic — the catalogue is still open to the curious.",
            onBrowseSpells = onBrowseSpells,
        )
        return
    }

    val casters = payload.spellcastingClasses
    val primaryClass = casters.first().classId
    val prepared = CastingProgression.usesPreparation(primaryClass)
    var addLevel by remember { mutableStateOf<Int?>(null) } // null = closed; 0 = cantrip; ≥1 = leveled
    var spellAction by remember { mutableStateOf<SpellAction?>(null) }

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
        // Casting triplet: Ability / Save DC / Spell Atk (per class if multiclass).
        val prof = Proficiency.bonus(payload.level)
        val scores = payload.effectiveAbilityScores
        casters.forEach { entry ->
            val ability = Spellcasting.spellcastingAbility(entry) ?: return@forEach
            val mod = scores.modifier(ability)
            if (casters.size > 1) {
                Text(
                    entry.classId.slugToTitle().uppercase(),
                    fontFamily = Cinzel,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = palette.inkMute,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                )
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("Ability", ability.abbreviation, Modifier.weight(1f))
                StatChip("Save DC", (8 + prof + mod).toString(), Modifier.weight(1f))
                StatChip("Spell Atk", (prof + mod).signed(), Modifier.weight(1f))
            }
        }

        CodexButton("Browse all spells", Modifier.fillMaxWidth(), onClick = onBrowseSpells)

        SlotSection(payload, onApplyIntent)

        SectionHead("Cantrips", top = 22.dp, bottom = 10.dp)
        if (cantrips.isEmpty()) {
            DashedNotice("No cantrips known yet.")
            Spacer(Modifier.height(8.dp))
        } else {
            CantripPills(cantrips) { spellAction = SpellAction(it, cantrip = true) }
            Spacer(Modifier.height(10.dp))
        }
        CodexButton("Add cantrip", onClick = { addLevel = 0 })

        SectionHead(
            if (prepared) "Prepared · ${leveled.size}" else "Spells",
            top = 22.dp,
            bottom = 10.dp,
        )
        if (leveled.isEmpty()) {
            DashedNotice(if (prepared) "Nothing prepared — choose spells after a long rest." else "No spells known yet.")
            Spacer(Modifier.height(10.dp))
        } else {
            leveled.forEachIndexed { i, s ->
                if (i > 0) DashedRule()
                SpellRow(s) { spellAction = SpellAction(s, cantrip = false) }
            }
            Spacer(Modifier.height(10.dp))
        }
        CodexButton(if (prepared) "Prepare a spell" else "Learn a spell", onClick = { addLevel = 1 })
        Spacer(Modifier.height(8.dp))
    }

    addLevel?.let { lvl ->
        AddSpellDialog(
            cantrips = lvl == 0,
            alreadyHave = if (lvl == 0) payload.cantripsKnown.toSet() else castable,
            onPick = { picked ->
                if (lvl == 0) {
                    editPayload { it.copy(cantripsKnown = (it.cantripsKnown + picked.index).distinct()) }
                } else if (prepared) {
                    onApplyIntent(PrepareSpell(picked.index, picked.name, primaryClass))
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

    spellAction?.let { action ->
        val s = action.spell
        val hasSlot = action.cantrip || (payload.totalCurrentSlots[s.level] ?: 0) > 0
        AlertDialog(
            onDismissRequest = { spellAction = null },
            title = { Text(s.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${s.levelLabel} · ${s.schoolName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!action.cantrip && !hasSlot) {
                        Text("No level-${s.level} slots remaining.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = hasSlot, onClick = {
                    onApplyIntent(CastSpell(s.index, s.name, s.level, s.level, requiresConcentration = s.concentration, applyToSelf = true))
                    spellAction = null
                }) { Text("Cast") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        if (action.cantrip) {
                            editPayload { it.copy(cantripsKnown = it.cantripsKnown - s.index) }
                        } else if (prepared) {
                            onApplyIntent(UnprepareSpell(s.index, s.name, primaryClass))
                        } else {
                            editPayload { p ->
                                p.copy(spellsKnown = p.spellsKnown + (primaryClass to (p.spellsKnown[primaryClass].orEmpty() - s.index)))
                            }
                        }
                        spellAction = null
                    }) { Text(if (prepared && !action.cantrip) "Unprepare" else "Remove") }
                    TextButton(onClick = { spellAction = null }) { Text("Close") }
                }
            },
        )
    }
}

/** A spell tapped on the page, awaiting a cast/remove decision. */
private data class SpellAction(val spell: Spell, val cantrip: Boolean)

/** Slot tiles (tap to expend — Android extra kept from A10, pending #19). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotSection(payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit) {
    val maxSlots = payload.totalMaxSlots
    SectionHead("Spell Slots", top = 22.dp, bottom = 10.dp)
    if (maxSlots.isEmpty()) {
        DashedNotice("No spell slots at this level.")
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        maxSlots.keys.sorted().forEach { level ->
            val max = maxSlots[level] ?: 0
            val remaining = payload.totalCurrentSlots[level] ?: 0
            SlotTile(level, remaining, max, onClick = { if (remaining > 0) onApplyIntent(ExpendSpellSlot(level)) })
        }
    }
    Text(
        "Tap a tile to expend a slot; rest on the Combat tab to recover." +
            (if (payload.maxPactSlots > 0) " Pact slots merge at level ${payload.pactSlotLevel}." else ""),
        fontFamily = ImFell,
        fontStyle = FontStyle.Italic,
        fontSize = 11.sp,
        color = MaterialTheme.natPalette.inkMute,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** Cantrip capsule pills (tap for cast/remove). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CantripPills(cantrips: List<Spell>, onTap: (Spell) -> Unit) {
    val palette = MaterialTheme.natPalette
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cantrips.forEach { s ->
            val shape = RoundedCornerShape(50)
            Text(
                s.name,
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = palette.ink,
                modifier = Modifier
                    .clip(shape)
                    .border(1.dp, palette.accent.copy(alpha = 0.33f), shape)
                    .clickable { onTap(s) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** Prepared/known spell row: diamond · name · LVL n (tap for cast/remove). */
@Composable
private fun SpellRow(spell: Spell, onTap: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Diamond(size = 8.dp, fill = palette.accent)
        Text(
            spell.name,
            fontFamily = Cormorant,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = palette.ink,
            modifier = Modifier.padding(start = 10.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            "LVL ${spell.level}",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = palette.inkMute,
        )
    }
}

/** iOS non-caster empty state: drawn sigil, headline, subtitle, browse button. */
@Composable
private fun NoSpellsEmptyState(subtitle: String, onBrowseSpells: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val accent = palette.accent.copy(alpha = 0.6f)
        Canvas(Modifier.size(60.dp)) {
            val c = center
            // Dashed outer circle.
            drawCircle(
                accent,
                radius = 28.dp.toPx(),
                center = c,
                style = Stroke(
                    1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f),
                ),
            )
            // Centre diamond.
            val d = 7.dp.toPx()
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(c.x, c.y - d)
                lineTo(c.x + d, c.y)
                lineTo(c.x, c.y + d)
                lineTo(c.x - d, c.y)
                close()
            }
            drawPath(path, accent)
            // Six orbit dots.
            val r = 19.dp.toPx()
            for (i in 0 until 6) {
                val angle = Math.toRadians(60.0 * i)
                drawCircle(
                    accent,
                    radius = 1.5.dp.toPx(),
                    center = Offset(
                        c.x + (r * Math.cos(angle)).toFloat(),
                        c.y + (r * Math.sin(angle)).toFloat(),
                    ),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "No spells known.",
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp,
            color = palette.ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = palette.inkMute,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp),
        )
        Spacer(Modifier.height(18.dp))
        val shape = RoundedCornerShape(4.dp)
        Text(
            "BROWSE THE CATALOGUE",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.5.sp,
            color = palette.accent,
            modifier = Modifier
                .clip(shape)
                .border(1.dp, palette.accent, shape)
                .clickable(onClick = onBrowseSpells)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
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

private val spellIndex: Map<String, Spell> by lazy { DnD5eCatalog.spellLibrary.associateBy { it.index } }
private fun spell(id: String): Spell? = spellIndex[id]

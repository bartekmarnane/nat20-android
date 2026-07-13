package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import au.com.evonet.nat20.dnd5e.AbilityBonus
import au.com.evonet.nat20.dnd5e.AbilityScoreIncreases
import au.com.evonet.nat20.dnd5e.CustomRaceLibrary
import au.com.evonet.nat20.dnd5e.Race
import au.com.evonet.nat20.dnd5e.RaceTraitEntry
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The homebrew Custom Race form (parity #11, port of iOS `CustomRaceForm`): a
 * full-screen dialog on the parchment ground. Captures name, description, size
 * (Tiny–Large), speed/darkvision steppers, fixed ability increases (−2..+3),
 * comma-separated languages, and prose traits. Everything else defaults — a
 * homebrew is a regular [Race] whose id is `"custom:" + UUID`.
 */

/** A trait row being edited; [key] keeps Compose identity while name/body change. */
private data class TraitDraft(val key: Int, val name: String = "", val body: String = "")

private val SIZES = listOf("Tiny", "Small", "Medium", "Large")

@Composable
fun CustomRaceFormDialog(
    editing: Race?,
    onSave: (Race) -> Unit,
    onDelete: ((String) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val palette = MaterialTheme.natPalette

    var name by remember { mutableStateOf(editing?.name.orEmpty()) }
    var description by remember { mutableStateOf(editing?.description.orEmpty()) }
    var sizeIndex by remember {
        // Medium when new (or the stored size isn't one of the four options).
        mutableIntStateOf(SIZES.indexOfFirst { it.equals(editing?.size, ignoreCase = true) }.let { if (it >= 0) it else 2 })
    }
    var speed by remember { mutableIntStateOf(editing?.speed ?: 30) }
    var darkvision by remember { mutableIntStateOf(editing?.darkvision ?: 0) }
    var asi by remember { mutableStateOf(editing?.abilityBonuses().orEmpty()) }
    var languages by remember { mutableStateOf(editing?.languages?.joinToString(", ").orEmpty()) }
    var traits by remember {
        mutableStateOf(
            editing?.traits.orEmpty().mapIndexed { index, trait -> TraitDraft(index, trait.name, trait.body) },
        )
    }
    var nextTraitKey by remember { mutableIntStateOf(traits.size) }

    fun buildRace(): Race = Race(
        id = editing?.id ?: CustomRaceLibrary.newId(),
        name = name.trim(),
        description = description.trim(),
        abilityScoreIncreases = AbilityScoreIncreases(
            fixed = Ability.entries.mapNotNull { ability ->
                val amount = asi[ability] ?: 0
                if (amount == 0) null else AbilityBonus(ability.name.lowercase(), amount)
            },
        ),
        size = SIZES[sizeIndex.coerceIn(SIZES.indices)].lowercase(),
        speed = speed,
        darkvision = darkvision,
        languages = languages.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        traits = traits.filter { it.name.isNotBlank() }.map { RaceTraitEntry(it.name.trim(), it.body.trim()) },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(Modifier.fillMaxSize().background(palette.parchment).statusBarsPadding()) {
            // ── Header: CANCEL · two-line centred title · balancing spacer ──
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 4.dp),
            ) {
                Text(
                    "CANCEL",
                    fontFamily = Cinzel,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = palette.ink,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable(onClick = onDismiss),
                )
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (editing == null) "NEW RACE" else "EDIT RACE",
                        fontFamily = Cinzel,
                        fontSize = 11.sp,
                        letterSpacing = 3.sp,
                        color = palette.inkMute,
                    )
                    Text(
                        editing?.name ?: "Homebrew",
                        fontFamily = Cormorant,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        fontSize = 20.sp,
                        color = palette.accent,
                        maxLines = 1,
                    )
                }
            }

            // ── Scrollable sections ──────────────────────────────────────────
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column {
                    WizardFieldLabel("Name", required = true)
                    WizardTextField("Vampire Dwarf, Tabaxi-kin…", name, { name = it })
                }
                Column {
                    WizardFieldLabel("Description")
                    WizardTextField(
                        "A short blurb shown alongside the race in the editor and codex.",
                        description,
                        { description = it },
                        multiline = true,
                        lineLimit = 4,
                    )
                }
                Column {
                    WizardFieldLabel("Size")
                    WizardSegmented(SIZES, selectedIndex = sizeIndex) { sizeIndex = it }
                    Spacer(Modifier.padding(top = 10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            WizardFieldLabel("Speed", hint = "ft")
                            RaceStatStepper(
                                value = speed,
                                display = "$speed",
                                onDecrement = { speed = (speed - 5).coerceAtLeast(5) },
                                onIncrement = { speed = (speed + 5).coerceAtMost(60) },
                                canDecrement = speed > 5,
                                canIncrement = speed < 60,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            WizardFieldLabel("Darkvision", hint = "ft")
                            RaceStatStepper(
                                value = darkvision,
                                display = if (darkvision > 0) "$darkvision" else "—",
                                onDecrement = { darkvision = (darkvision - 30).coerceAtLeast(0) },
                                onIncrement = { darkvision = (darkvision + 30).coerceAtMost(120) },
                                canDecrement = darkvision > 0,
                                canIncrement = darkvision < 120,
                            )
                        }
                    }
                }
                Column {
                    WizardFieldLabel("Ability Score Increases", hint = "−2 to +3 each")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Ability.entries.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { ability ->
                                    AsiStepper(
                                        ability = ability,
                                        value = asi[ability] ?: 0,
                                        onChange = { asi = asi + (ability to it) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
                Column {
                    WizardFieldLabel("Languages", hint = "comma-separated")
                    WizardTextField("Common, Sylvan", languages, { languages = it })
                }
                Column {
                    WizardFieldLabel("Traits", hint = "prose only")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        traits.forEach { draft ->
                            TraitEditorCard(
                                draft = draft,
                                onChange = { updated -> traits = traits.map { if (it.key == updated.key) updated else it } },
                                onDelete = { traits = traits.filterNot { it.key == draft.key } },
                            )
                        }
                        AddTraitButton {
                            traits = traits + TraitDraft(nextTraitKey)
                            nextTraitKey++
                        }
                    }
                }
            }

            // ── Sticky footer (cream gradient + top hairline, EditorShell style) ──
            val hairline = palette.ink.copy(alpha = 0.13f)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.cream.copy(alpha = 0.5f),
                                palette.cream.copy(alpha = 0.98f),
                                palette.cream.copy(alpha = 0.98f),
                            ),
                        ),
                    )
                    .drawBehind { drawRect(color = hairline, size = Size(size.width, 1.dp.toPx())) }
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (editing != null && onDelete != null) {
                    WizardSecondaryButton("Delete") { onDelete(editing.id) }
                }
                Spacer(Modifier.weight(1f))
                WizardPrimaryButton("Save", enabled = name.isNotBlank()) { onSave(buildRace()) }
            }
        }
    }
}

// ── Steppers ─────────────────────────────────────────────────────────────────

/** Speed/darkvision stepper cell: [−  value  +] on a tile well. */
@Composable
private fun RaceStatStepper(
    value: Int,
    display: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean,
    canIncrement: Boolean,
) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tileStrong)
            .border(1.dp, palette.ink.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−", enabled = canDecrement, size = 26, onClick = onDecrement)
        Text(
            display,
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = if (value > 0) palette.accent else palette.inkMute,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        StepperButton("+", enabled = canIncrement, size = 26, onClick = onIncrement)
    }
}

/** One fixed-ASI stepper: abbreviation on top, [− value +], −2..+3, accent when non-zero. */
@Composable
private fun AsiStepper(ability: Ability, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.natPalette
    Column(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            ability.abbreviation,
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = palette.inkMute,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StepperButton("−", enabled = value > -2, size = 22) { onChange(value - 1) }
            Text(
                if (value > 0) "+$value" else "$value",
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = if (value == 0) palette.inkMute else palette.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 26.dp),
            )
            StepperButton("+", enabled = value < 3, size = 22) { onChange(value + 1) }
        }
    }
}

// ── Traits ───────────────────────────────────────────────────────────────────

/** A trait being edited: plain name field with a trailing delete, prose body below. */
@Composable
private fun TraitEditorCard(draft: TraitDraft, onChange: (TraitDraft) -> Unit, onDelete: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val nameStyle = TextStyle(
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = palette.ink,
            )
            BasicTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                textStyle = nameStyle,
                singleLine = true,
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (draft.name.isEmpty()) {
                            Text("Trait name", style = nameStyle.copy(color = palette.inkMute.copy(alpha = 0.8f)))
                        }
                        inner()
                    }
                },
            )
            Icon(
                Icons.Filled.Close,
                contentDescription = "Delete trait",
                tint = palette.inkMute,
                modifier = Modifier
                    .size(18.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .border(1.dp, palette.inkMute.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                    .clickable(onClick = onDelete)
                    .padding(3.dp),
            )
        }
        val bodyStyle = TextStyle(
            fontFamily = EbGaramond,
            fontSize = 13.sp,
            color = palette.inkSoft,
        )
        BasicTextField(
            value = draft.body,
            onValueChange = { onChange(draft.copy(body = it)) },
            textStyle = bodyStyle,
            minLines = 2,
            cursorBrush = SolidColor(palette.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (draft.body.isEmpty()) {
                        Text(
                            "What does it do? Prose only — no mechanics hooks.",
                            style = bodyStyle.copy(color = palette.inkMute.copy(alpha = 0.8f)),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/** "Add trait" dashed accent capsule (plus icon + small-caps label). */
@Composable
private fun AddTraitButton(onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val dashColor = palette.accent.copy(alpha = 0.5f)
    Row(
        Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(size.height / 2f),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(12.dp))
        Text(
            "ADD TRAIT",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.accent,
        )
    }
}

// ── Race-step affordances (used by the 2014 wizard) ──────────────────────────

/** "+ New race" dashed accent capsule under the wizard's race picker. */
@Composable
fun NewRaceButton(onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val dashColor = palette.accent.copy(alpha = 0.5f)
    Row(
        Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(size.height / 2f),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(12.dp))
        Text(
            "NEW RACE",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.5.sp,
            color = palette.accent,
        )
    }
}

/** "HOMEBREW" badge: cream small-caps on a translucent accent capsule. */
@Composable
fun HomebrewBadge() {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(palette.accent.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            "HOMEBREW",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.cream,
            maxLines = 1,
        )
    }
}

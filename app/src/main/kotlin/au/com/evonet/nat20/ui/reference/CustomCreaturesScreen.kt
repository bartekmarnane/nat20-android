package au.com.evonet.nat20.ui.reference

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.CustomCreature
import au.com.evonet.nat20.dnd5e.CustomCreatureLibrary
import au.com.evonet.nat20.ui.actions.ActionPickerShell
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The homebrew creature library (parity #44, port of iOS `CustomCreaturesView` +
 * `CustomCreatureForm`), reached from Settings → Reference. A parchment list of
 * saved creatures with an add tile; tapping one opens the editor sheet. Deleting
 * only removes the library entry.
 */
@Composable
fun CustomCreaturesScreen(onBack: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val creatures by CustomCreatureLibrary.creatures.collectAsState()
    var editing by remember { mutableStateOf<CustomCreature?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp, start = 22.dp, end = 22.dp)) {
            Box(
                Modifier.align(Alignment.CenterStart).size(38.dp).clip(CircleShape).background(palette.tileStrong).border(1.dp, palette.accent, CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = palette.accent, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HOMEBREW", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 3.sp, color = palette.inkMute)
                Text("Custom Creatures", fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = palette.ink)
            }
        }
        OrnamentalDivider(Modifier.padding(horizontal = 22.dp, vertical = 14.dp), opacity = 0.4f)

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .drawBehind {
                            drawRoundRect(
                                color = palette.accent.copy(alpha = 0.5f),
                                cornerRadius = CornerRadius(4.dp.toPx()),
                                style = Stroke(width = 1.4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                            )
                        }
                        .clickable { adding = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+ NEW CREATURE", fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.accent)
                }
            }
            if (creatures.isEmpty()) {
                item {
                    Text(
                        "No homebrew creatures yet. Forge one to keep it in your bestiary.",
                        fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkMute,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }
            items(creatures, key = { it.id }) { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.tile)
                        .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                        .clickable { editing = c }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = palette.accent)
                        Text(
                            "${c.size} ${c.type} · CR ${crText(c.challengeRating)}",
                            fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkSoft,
                        )
                    }
                    Text("AC ${c.armorClass} · ${c.hitPoints} HP", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 1.sp, color = palette.accent)
                }
            }
        }
    }

    if (adding) {
        CustomCreatureForm(existing = null, onCancel = { adding = false }, onSave = { CustomCreatureLibrary.add(it); adding = false }, onDelete = null)
    }
    editing?.let { c ->
        CustomCreatureForm(
            existing = c,
            onCancel = { editing = null },
            onSave = { CustomCreatureLibrary.update(it); editing = null },
            onDelete = { CustomCreatureLibrary.delete(c.id); editing = null },
        )
    }
}

@Composable
private fun CustomCreatureForm(existing: CustomCreature?, onCancel: () -> Unit, onSave: (CustomCreature) -> Unit, onDelete: (() -> Unit)?) {
    val palette = MaterialTheme.natPalette
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var size by remember { mutableStateOf(existing?.size ?: "Medium") }
    var type by remember { mutableStateOf(existing?.type ?: "beast") }
    var alignment by remember { mutableStateOf(existing?.alignment ?: "unaligned") }
    var ac by remember { mutableStateOf(existing?.armorClass ?: 12) }
    var hp by remember { mutableStateOf(existing?.hitPoints ?: 11) }
    var hitDice by remember { mutableStateOf(existing?.hitDice ?: "2d8+2") }
    var speed by remember { mutableStateOf(existing?.speed ?: 30) }
    var str by remember { mutableStateOf(existing?.strength ?: 10) }
    var dex by remember { mutableStateOf(existing?.dexterity ?: 10) }
    var con by remember { mutableStateOf(existing?.constitution ?: 10) }
    var intel by remember { mutableStateOf(existing?.intelligence ?: 10) }
    var wis by remember { mutableStateOf(existing?.wisdom ?: 10) }
    var cha by remember { mutableStateOf(existing?.charisma ?: 10) }
    var cr by remember { mutableStateOf(existing?.challengeRating ?: 0.25) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    ActionPickerShell(
        kicker = if (existing == null) "New Creature" else "Edit Creature",
        title = name.trim().ifEmpty { "Homebrew" },
        onCancel = onCancel,
        footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) {
                    Box(Modifier.weight(1f)) { PrimaryActionButton("Delete", isDanger = true) { onDelete() } }
                }
                Box(Modifier.weight(1f)) {
                    PrimaryActionButton("Save", isDisabled = name.isBlank()) {
                        onSave(
                            (existing ?: CustomCreature(id = CustomCreatureLibrary.newId(), name = name)).copy(
                                name = name.trim(), size = size, type = type.trim(), alignment = alignment.trim(),
                                armorClass = ac, hitPoints = hp, hitDice = hitDice.trim(), speed = speed,
                                strength = str, dexterity = dex, constitution = con, intelligence = intel, wisdom = wis, charisma = cha,
                                challengeRating = cr, notes = notes.trim(),
                            ),
                        )
                    }
                }
            }
        },
    ) {
        Section("Identity")
        Label("Name")
        WizardTextField("Owlbear, Quasit, Awakened Shrub…", name, { name = it })
        Spacer(Modifier.size(10.dp))
        Label("Size")
        SizePills(size) { size = it }
        Spacer(Modifier.size(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) { Label("Type"); WizardTextField("beast, fiend…", type, { type = it }) }
            Column(Modifier.weight(1f)) { Label("Alignment"); WizardTextField("unaligned…", alignment, { alignment = it }) }
        }

        Section("Defences")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stepper("Armor Class", ac, 5..30) { ac = it }
            Stepper("Speed (ft)", speed, 0..120, step = 5) { speed = it }
        }
        Spacer(Modifier.size(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stepper("Hit Points", hp, 1..1000) { hp = it }
            Column(Modifier.weight(1f)) { Label("Hit Dice"); WizardTextField("2d8+2", hitDice, { hitDice = it }) }
        }

        Section("Ability Scores")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stepper("STR", str, 1..30) { str = it }
            Stepper("DEX", dex, 1..30) { dex = it }
            Stepper("CON", con, 1..30) { con = it }
        }
        Spacer(Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stepper("INT", intel, 1..30) { intel = it }
            Stepper("WIS", wis, 1..30) { wis = it }
            Stepper("CHA", cha, 1..30) { cha = it }
        }

        Section("Challenge")
        CrPills(cr) { cr = it }

        Section("Notes")
        WizardTextField("Traits, actions, lore…", notes, { notes = it }, multiline = true, lineLimit = 4)
        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    val palette = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 3.sp, color = palette.accent)
        Box(Modifier.padding(start = 10.dp).weight(1f).size(1.dp).background(palette.accent.copy(alpha = 0.3f)))
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.5.sp, color = MaterialTheme.natPalette.inkMute, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Stepper(label: String, value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(Modifier.weight(1f)) {
        Label(label)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(palette.tileStrong).border(1.dp, palette.ink.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StepBtn("−") { onChange((value - step).coerceIn(range.first, range.last)) }
            Text("$value", fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = palette.accent)
            StepBtn("+") { onChange((value + step).coerceIn(range.first, range.last)) }
        }
    }
}

@Composable
private fun StepBtn(glyph: String, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(glyph, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = MaterialTheme.natPalette.ink)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SizePills(size: String, onPick: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Tiny", "Small", "Medium", "Large", "Huge", "Gargantuan").forEach { s ->
            ReferencePill(s, size == s) { onPick(s) }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CrPills(cr: Double, onPick: (Double) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0.0, 0.125, 0.25, 0.5, 1.0, 2.0, 3.0, 5.0, 8.0, 10.0, 15.0, 20.0).forEach { v ->
            ReferencePill("CR ${crText(v)}", cr == v) { onPick(v) }
        }
    }
}

private fun crText(cr: Double): String = when (cr) {
    0.125 -> "1/8"; 0.25 -> "1/4"; 0.5 -> "1/2"
    else -> if (cr % 1.0 == 0.0) cr.toInt().toString() else cr.toString()
}

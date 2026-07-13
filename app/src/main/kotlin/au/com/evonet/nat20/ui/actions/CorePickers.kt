package au.com.evonet.nat20.ui.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.RaceTraits
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e.initiativeBonus
import au.com.evonet.nat20.domain.NoteKind
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The "core" Slice-A action pickers (parity #19): amount entry (damage /
 * heal / temp HP), journal note, begin-encounter, confirm-action, and the
 * concentration-save follow-up. Each is a full-screen [ActionPickerShell]
 * body; the layer in `ActionsLayer.kt` maps commits onto intents.
 */

// ── Amount picker ──────────────────────────────────────────────────────────────

private val DAMAGE_TYPES = listOf(
    "Slashing", "Piercing", "Bludgeoning", "Fire", "Cold", "Lightning",
    "Acid", "Necrotic", "Radiant", "Force", "Poison", "Psychic", "Thunder",
)

/**
 * Numeric amount picker with optional damage-type chips and an optional
 * top note field (iOS `AmountPicker`). When [notePrompt] is set the commit
 * button is inlined at the end of the body (keyboard-friendly); otherwise
 * it pins to the footer. iOS's Heavy Armor Master "nonmagical weapon"
 * toggle is skipped — the Android `TakeDamage` intent doesn't model it yet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AmountPicker(
    kicker: String,
    title: String,
    readoutLabel: String = "Amount",
    unit: String = "HP",
    isDanger: Boolean = false,
    showDamageTypes: Boolean = false,
    notePrompt: String? = null,
    onCancel: () -> Unit,
    onCommit: (amount: Int, damageType: String?, note: String?) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var amountText by remember { mutableStateOf("0") }
    var damageType by remember { mutableStateOf(if (showDamageTypes) DAMAGE_TYPES.first() else "") }
    var isOther by remember { mutableStateOf(false) }
    var customType by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val amount = amountText.toIntOrNull() ?: 0
    val tone = if (isDanger) palette.danger else palette.accent
    val effectiveType = if (isOther) customType.trim() else damageType
    val commitDisabled = amount <= 0 || (showDamageTypes && isOther && effectiveType.isEmpty())
    val commitLabel = buildString {
        append("Confirm $amount $unit")
        if (showDamageTypes && effectiveType.isNotEmpty()) append(" · $effectiveType")
    }

    fun setAmount(value: Int) { amountText = value.coerceAtLeast(0).toString() }
    fun commit() {
        val type = if (showDamageTypes && effectiveType.isNotEmpty()) effectiveType else null
        onCommit(amount, type, note.trim().takeIf { it.isNotEmpty() })
    }

    ActionPickerShell(
        kicker = kicker,
        title = title,
        onCancel = onCancel,
        footer = if (notePrompt == null) {
            {
                PrimaryActionButton(
                    label = commitLabel,
                    isDanger = isDanger,
                    isDisabled = commitDisabled,
                    onClick = ::commit,
                )
            }
        } else {
            null
        },
    ) {
        if (notePrompt != null) {
            PickerSection(notePrompt, top = 6.dp)
            WizardTextField(notePrompt, note, { if (it.length <= 240) note = it })
        }

        // Big readout + quick steppers.
        PickerGap(if (notePrompt == null) 6.dp else 14.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(palette.tile)
                .border(1.4.dp, tone.copy(alpha = 0.33f), RoundedCornerShape(4.dp))
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    readoutLabel.uppercase(),
                    fontFamily = Cinzel,
                    fontSize = 11.sp,
                    letterSpacing = 2.5.sp,
                    color = palette.inkMute,
                )
                Text(
                    "$amount",
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 34.sp,
                    color = tone,
                    maxLines = 1,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StepCapsule("+1", tone) { setAmount(amount + 1) }
                StepCapsule("+5", tone) { setAmount(amount + 5) }
                StepCapsule("−1", tone) { setAmount(amount - 1) }
            }
        }

        if (showDamageTypes) {
            PickerSection("Damage Type")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                DAMAGE_TYPES.forEach { type ->
                    PickerChip(type, active = !isOther && damageType == type, tone = tone) {
                        damageType = type
                        isOther = false
                    }
                }
                PickerChip("Other", active = isOther, tone = tone) { isOther = true }
            }
            if (isOther) {
                PickerGap(6.dp)
                WizardTextField("Damage type", customType, { customType = it })
            }
        }

        PickerSection("Number Pad")
        NumberPad(
            onDigit = { d ->
                amountText = when {
                    amountText == "0" -> d
                    amountText.length >= 4 -> amountText
                    else -> amountText + d
                }
            },
            onClear = { amountText = "0" },
            onBackspace = { amountText = if (amountText.length <= 1) "0" else amountText.dropLast(1) },
        )

        if (notePrompt != null && !commitDisabled) {
            PickerGap(18.dp)
            PrimaryActionButton(label = commitLabel, isDanger = isDanger, onClick = ::commit)
        }
    }
}

@Composable
private fun StepCapsule(label: String, tone: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .size(width = 46.dp, height = 30.dp)
            .clip(CircleShape)
            .background(palette.tileStrong)
            .border(1.dp, tone.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = tone,
        )
    }
}

@Composable
private fun NumberPad(onDigit: (String) -> Unit, onClear: () -> Unit, onBackspace: () -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.chunked(3).forEach { rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowKeys.forEach { key ->
                    val isAlt = key == "Clear" || key == "⌫"
                    PadKey(key, isAlt, Modifier.weight(1f)) {
                        when (key) {
                            "Clear" -> onClear()
                            "⌫" -> onBackspace()
                            else -> onDigit(key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PadKey(label: String, isAlt: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (isAlt) androidx.compose.ui.graphics.Color.Transparent else palette.tileStrong)
            .border(1.dp, palette.ink.copy(alpha = 0.2f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = Cormorant,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            color = palette.ink,
        )
    }
}

// ── Note picker ────────────────────────────────────────────────────────────────

/**
 * Free-text journal entry picker (iOS `NotePicker`). [selectableKinds] empty
 * hides the kind chips — used by End encounter, whose kind is fixed combat.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotePicker(
    kicker: String = "Session",
    title: String = "Add to Journal",
    commitLabel: String = "Add to Journal",
    initialKind: NoteKind = NoteKind.QUEST,
    initialText: String = "",
    selectableKinds: List<NoteKind> = NoteKind.NARRATIVE,
    onCancel: () -> Unit,
    onCommit: (NoteKind, String) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var kind by remember { mutableStateOf(initialKind) }
    var text by remember { mutableStateOf(initialText) }
    val maxLength = 240
    val trimmed = text.trim()

    ActionPickerShell(
        kicker = kicker,
        title = title,
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(
                label = commitLabel,
                isDisabled = trimmed.isEmpty(),
                onClick = { onCommit(kind, trimmed) },
            )
        },
    ) {
        if (selectableKinds.isNotEmpty()) {
            PickerSection("Entry Kind", top = 6.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                selectableKinds.forEach { k ->
                    SmallCapsChip(k.label, active = kind == k) { kind = k }
                }
            }
        }

        PickerSection("What happened?", top = if (selectableKinds.isEmpty()) 6.dp else 16.dp)
        WizardTextField(
            "The party…",
            text,
            { if (it.length <= maxLength) text = it },
            multiline = true,
            lineLimit = 6,
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Spacer(Modifier.weight(1f))
            Text(
                "${text.length} / $maxLength",
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = palette.inkMute,
            )
        }
    }
}

// ── Begin encounter ────────────────────────────────────────────────────────────

/**
 * Opens an encounter: an animated initiative d20 (DEX/init bonus streamed as
 * a chip) plus a short context note (iOS `BeginEncounterPicker`). Commit
 * hands back the note and rolled total; the layer composes the combat
 * journal entry — matching iOS, no encounter intent exists.
 */
@Composable
internal fun BeginEncounterPicker(
    payload: DnD5ePayload,
    onCancel: () -> Unit,
    onCommit: (text: String, initiative: Int) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var text by remember { mutableStateOf("") }
    var rolledInitiative by remember { mutableStateOf<Int?>(null) }
    val maxLength = 240

    ActionPickerShell(
        kicker = "Session",
        title = "Begin encounter",
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(
                label = "Continue",
                isDisabled = rolledInitiative == null,
                onClick = { rolledInitiative?.let { onCommit(text.trim(), it) } },
            )
        },
    ) {
        PickerSection("Initiative", top = 6.dp)
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            RollResultView(
                baseSpec = RollSpec.d(1, 20),
                bonuses = payload.initiativeBonus.takeIf { it != 0 }
                    ?.let { listOf(RollBonus("INIT", it)) } ?: emptyList(),
                allowAdvantageToggle = false,
                luckyReroll = RaceTraits.hasHalflingLuck(payload.race),
                onSettled = { rolledInitiative = it.total },
            )
        }

        PickerSection("What's happening?")
        WizardTextField(
            "Goblins burst from the treeline…",
            text,
            { if (it.length <= maxLength) text = it },
            multiline = true,
            lineLimit = 4,
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Spacer(Modifier.weight(1f))
            Text(
                "${text.length} / $maxLength",
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = palette.inkMute,
            )
        }
    }
}

// ── Confirm action ─────────────────────────────────────────────────────────────

/**
 * Confirmation picker for one-shot actions (spend inspiration; recharge at
 * dawn once that intent exists): centred hero glyph + blurb + optional
 * "This will" bullets + a single footer commit (iOS `ConfirmActionPicker`).
 */
@Composable
internal fun ConfirmActionPicker(
    kicker: String,
    title: String,
    description: String,
    primaryLabel: String = "Confirm",
    secondaryEffects: List<String> = emptyList(),
    isDanger: Boolean = false,
    onCancel: () -> Unit,
    onCommit: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    ActionPickerShell(
        kicker = kicker,
        title = title,
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(label = primaryLabel, isDanger = isDanger, onClick = onCommit)
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PickerHeroGlyph()
            PickerGap(14.dp)
            Text(
                title,
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = palette.ink,
                textAlign = TextAlign.Center,
            )
            PickerGap(10.dp)
            Text(
                description,
                fontFamily = EbGaramond,
                fontSize = 15.sp,
                color = palette.inkSoft,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        if (secondaryEffects.isNotEmpty()) {
            PickerSection("This will")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                secondaryEffects.forEach { EffectBulletRow(it) }
            }
        }
    }
}

// ── Concentration save ─────────────────────────────────────────────────────────

/**
 * The concentration-save follow-up, chained automatically after Take damage
 * lands on a concentrating caster (iOS `ConcentrationSavePicker`). 5e RAW:
 * CON save vs DC = max(10, damage / 2). The in-app d20 pre-selects the
 * Held / Dropped chip from the total vs DC; the player can still flip it
 * (physical-dice escape hatch). Failure commits `EndConcentration`.
 */
@Composable
internal fun ConcentrationSavePicker(
    spellName: String,
    damageAmount: Int,
    dc: Int,
    conSaveBonus: Int,
    hasHalflingLuck: Boolean,
    onCancel: () -> Unit,
    onPassed: () -> Unit,
    onFailed: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var heldChoice by remember { mutableStateOf<Boolean?>(null) }
    var hasRolled by remember { mutableStateOf(false) }

    ActionPickerShell(
        kicker = "Concentration",
        title = "Concentration save",
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(
                label = when (heldChoice) {
                    null -> "Roll or pick an outcome"
                    true -> "Record save"
                    false -> "Record concentration dropped"
                },
                isDanger = heldChoice == false,
                isDisabled = heldChoice == null,
                onClick = { if (heldChoice == true) onPassed() else if (heldChoice == false) onFailed() },
            )
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PickerHeroGlyph()
            PickerGap(12.dp)
            Text(
                "Holding $spellName — took $damageAmount damage. Roll a Constitution save " +
                    "against the DC below; on failure the spell ends and any tracked effects drop.",
                fontFamily = EbGaramond,
                fontSize = 15.sp,
                color = palette.inkSoft,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        PickerSection("Roll DC")
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(palette.tile)
                .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SaveBadge("DC $dc", palette.accent)
            SaveBadge("CON ${if (conSaveBonus >= 0) "+" else ""}$conSaveBonus", palette.accentGold)
        }

        PickerSection("Roll the save")
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            RollResultView(
                baseSpec = RollSpec.d(1, 20),
                bonuses = if (conSaveBonus != 0) listOf(RollBonus("CON", conSaveBonus)) else emptyList(),
                allowAdvantageToggle = false,
                luckyReroll = hasHalflingLuck,
                onSettled = { result ->
                    hasRolled = true
                    heldChoice = result.total >= dc
                },
            )
        }
        if (hasRolled && heldChoice != null) {
            Text(
                if (heldChoice == true) "Roll meets the DC — concentration holds."
                else "Roll fell short — concentration drops.",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = if (heldChoice == true) palette.accent else palette.danger,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PickerSection("Did it hold?")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallCapsChip("Held", active = heldChoice == true, tone = palette.accent, modifier = Modifier.weight(1f)) {
                heldChoice = true
            }
            SmallCapsChip("Dropped", active = heldChoice == false, tone = palette.danger, modifier = Modifier.weight(1f)) {
                heldChoice = false
            }
        }
    }
}

@Composable
private fun SaveBadge(text: String, tone: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(tone)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text.uppercase(),
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = MaterialTheme.natPalette.cream,
        )
    }
}

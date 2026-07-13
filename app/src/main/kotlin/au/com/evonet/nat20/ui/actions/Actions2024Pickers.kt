package au.com.evonet.nat20.ui.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e2024.ItemKind2024
import au.com.evonet.nat20.dnd5e2024.Spell2024
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The 2024-specific action pickers (parity #31, port of iOS `Pickers2024`):
 * item entry and the cast chooser. Vitals/coin amounts reuse the shared
 * [AmountPicker], conditions reuse the shared [ConditionPicker], and the
 * concentration follow-up reuses the shared [ConcentrationSavePicker] — all
 * payload-agnostic. See [Actions2024Layer] for the intent wiring.
 */

// ── Item entry ─────────────────────────────────────────────────────────────────

/**
 * Name / kind / quantity for a new 2024 pack item (iOS `ItemEntrySheet2024`).
 * The kind chips drive [ItemKind2024] grouping on the Items tab.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ItemEntry2024Picker(
    onCancel: () -> Unit,
    onCommit: (name: String, quantity: Int, kind: ItemKind2024) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }
    var kind by remember { mutableStateOf(ItemKind2024.GEAR) }
    val trimmed = name.trim()

    ActionPickerShell(
        kicker = "Inventory",
        title = "Add item",
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(
                label = if (trimmed.isEmpty()) "Name the item" else "Add $trimmed",
                isDisabled = trimmed.isEmpty(),
                onClick = { onCommit(trimmed, quantity, kind) },
            )
        },
    ) {
        PickerSection("Item name", top = 6.dp)
        WizardTextField("Rope, potion, trophy…", name, { if (it.length <= 120) name = it })

        PickerSection("Kind")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            ItemKind2024.entries.forEach { k ->
                SmallCapsChip(k.displayName, active = kind == k) { kind = k }
            }
        }

        PickerSection("Quantity")
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(palette.tile)
                .border(1.dp, palette.ink.copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QtyStepButton("−", enabled = quantity > 1) { quantity = maxOf(1, quantity - 1) }
            Text(
                "$quantity",
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                color = palette.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(80.dp),
            )
            QtyStepButton("+", enabled = quantity < 999) { quantity = minOf(999, quantity + 1) }
        }
    }
}

@Composable
private fun QtyStepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .clip(CircleShape)
            .background(palette.tileStrong)
            .border(1.dp, palette.accent.copy(alpha = if (enabled) 0.33f else 0.15f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(symbol, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = palette.ink)
    }
}

// ── Cast chooser ───────────────────────────────────────────────────────────────

/**
 * Cantrips + prepared leveled spells the character can cast, slot-gated (iOS
 * `CastPicker2024`). All 2024 casters prepare, so leveled options come from the
 * prepared list; each leveled row is disabled unless a slot of its level
 * remains. Cast fires at the spell's own level (no upcast picker in the 2024
 * flow — matching iOS).
 */
@Composable
internal fun Cast2024Picker(
    cantrips: List<Spell2024>,
    leveled: List<Spell2024>,
    remainingSlots: Map<Int, Int>,
    onCancel: () -> Unit,
    onCast: (spell: Spell2024, slotLevel: Int) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    ActionPickerShell(
        kicker = "Combat",
        title = "Cast spell",
        onCancel = onCancel,
    ) {
        if (cantrips.isEmpty() && leveled.isEmpty()) {
            PickerGap(32.dp)
            Text(
                "Nothing to cast.\nLearn cantrips and prepare spells on the Spells tab.",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = palette.inkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            return@ActionPickerShell
        }

        if (cantrips.isNotEmpty()) {
            PickerSection("Cantrips", top = 6.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cantrips.sortedBy { it.name }.forEach { spell ->
                    SpellCastRow2024(spell.name, spell.school, enabled = true) { onCast(spell, 0) }
                }
            }
        }

        if (leveled.isNotEmpty()) {
            PickerSection("Prepared", top = if (cantrips.isEmpty()) 6.dp else 18.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                leveled.sortedWith(compareBy({ it.level }, { it.name })).forEach { spell ->
                    val slots = remainingSlots[spell.level] ?: 0
                    SpellCastRow2024(
                        spell.name,
                        "L${spell.level} · ${spell.school} · slots $slots",
                        enabled = slots > 0,
                    ) { onCast(spell, spell.level) }
                }
            }
        }
    }
}

@Composable
private fun SpellCastRow2024(title: String, detail: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val alpha = if (enabled) 1f else 0.4f
    PickerRow(onClick = if (enabled) onClick else null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = palette.ink.copy(alpha = alpha),
                )
                if (detail.isNotEmpty()) {
                    Text(
                        detail,
                        fontFamily = ImFell,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp,
                        color = palette.inkMute.copy(alpha = alpha),
                    )
                }
            }
            Text(
                (if (enabled) "Cast" else "No slot").uppercase(),
                fontFamily = Cinzel,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = if (enabled) palette.accent else palette.inkMute,
            )
        }
    }
}

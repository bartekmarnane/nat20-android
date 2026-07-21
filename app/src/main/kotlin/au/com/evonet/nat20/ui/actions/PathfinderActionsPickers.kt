@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package au.com.evonet.nat20.ui.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollResult
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.pf2e.Bulk
import au.com.evonet.nat20.pf2e.PFCoin
import au.com.evonet.nat20.pf2e.PFInventoryItem
import au.com.evonet.nat20.pf2e.PathfinderConditions
import au.com.evonet.nat20.pf2e.PathfinderPayload
import au.com.evonet.nat20.pf2e.PfArmors
import au.com.evonet.nat20.pf2e.PfFeatType
import au.com.evonet.nat20.pf2e.PfFeats
import au.com.evonet.nat20.pf2e.PfShields
import au.com.evonet.nat20.pf2e.PfSpell
import au.com.evonet.nat20.pf2e.PfSpells
import au.com.evonet.nat20.pf2e.PfWeapons
import au.com.evonet.nat20.pf2e.Runes
import au.com.evonet.nat20.pf2e.WeaponRunes
import au.com.evonet.nat20.pf2e.strike
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.slugToTitle

private fun Int.signedPf(): String = if (this >= 0) "+$this" else "$this"

// ── Small shared building blocks ────────────────────────────────────────────────

/** A tappable list row (label + optional trailing) on the parchment [PickerRow]. */
@Composable
private fun PfListRow(label: String, trailing: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    PickerRow(onClick = if (enabled) onClick else null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontFamily = Cormorant,
                fontSize = 15.sp,
                color = if (enabled) palette.ink else palette.inkMute,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                Text(trailing, fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 1.5.sp, color = palette.inkMute)
            }
        }
    }
}

/** A −/value/+ stepper row inside a picker. */
@Composable
private fun PfStepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontFamily = Cormorant, fontSize = 15.sp, color = palette.ink)
        SmallCapsChip("−", active = false) { if (value > range.first) onChange(value - 1) }
        Text(
            value.signedPf(),
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = palette.accent,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SmallCapsChip("+", active = false) { if (value < range.last) onChange(value + 1) }
    }
}

// ── Conditions ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PfApplyConditionPicker(active: Set<String>, onCancel: () -> Unit, onCommit: (id: String, value: Int?) -> Unit) {
    var selected by remember { mutableStateOf<PathfinderConditions.Entry?>(null) }
    var value by remember { mutableIntStateOf(1) }
    val choices = PathfinderConditions.all.filter { it.id !in active }
    ActionPickerShell(
        kicker = "Conditions",
        title = "Apply condition",
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(
                label = selected?.let { "Apply ${it.displayName}" + if (it.valued) " $value" else "" } ?: "Apply",
                isDisabled = selected == null,
                onClick = { selected?.let { onCommit(it.id, if (it.valued) value else null) } },
            )
        },
    ) {
        PickerSection("Standard", top = 6.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            choices.forEach { c ->
                SmallCapsChip(c.displayName, active = selected?.id == c.id) { selected = c; value = 1 }
            }
        }
        selected?.let { c ->
            PickerHelpText(c.summary, Modifier.padding(top = 12.dp))
            if (c.valued) {
                PickerSection("Value")
                PfStepper(c.displayName, value, 1..6) { value = it }
            }
        }
    }
}

@Composable
internal fun PfClearConditionPicker(payload: PathfinderPayload, onCancel: () -> Unit, onCommit: (String) -> Unit) {
    ActionPickerShell(kicker = "Conditions", title = "Clear condition", onCancel = onCancel) {
        PickerSection("Active", top = 6.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            payload.conditions.forEach { c ->
                val name = PathfinderConditions.displayName(c.id) + (c.value?.let { " $it" } ?: "")
                PfListRow(name) { onCommit(c.id) }
            }
        }
    }
}

// ── Strike (weapon → RollResultView d20 → PfStrike) ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PfStrikePicker(payload: PathfinderPayload, onCancel: () -> Unit, onStrike: (weaponId: String, attackNumber: Int, total: Int, target: String?) -> Unit) {
    val weapons = payload.weapons.mapNotNull { PfWeapons.by(it) }
    var weaponId by remember { mutableStateOf(weapons.firstOrNull()?.id) }
    var attackIndex by remember { mutableIntStateOf(0) } // 0=1st, 1=2nd, 2=3rd
    var target by remember { mutableStateOf("") }
    var rolled by remember { mutableStateOf<RollResult?>(null) }
    val weapon = weaponId?.let { PfWeapons.by(it) }
    val mods = weapon?.let { payload.strike(it).attackMods } ?: listOf(0, 0, 0)
    val mod = mods.getOrElse(attackIndex) { 0 }

    ActionPickerShell(kicker = "Combat", title = "Strike", onCancel = onCancel) {
        PickerSection("Weapon", top = 6.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            weapons.forEach { w ->
                SmallCapsChip(w.name, active = weaponId == w.id) { weaponId = w.id; attackIndex = 0 }
            }
        }
        PickerSection("Attack")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("1st", "2nd", "3rd").forEachIndexed { i, label ->
                SmallCapsChip("$label ${mods.getOrElse(i) { 0 }.signedPf()}", active = attackIndex == i, modifier = Modifier.weight(1f)) { attackIndex = i }
            }
        }
        PickerSection("Target (optional)")
        WizardTextField("Target", target, { target = it })
        PickerGap(16.dp)
        weapon?.let { w ->
            key(w.id, attackIndex) {
                RollResultView(
                    baseSpec = RollSpec.d(1, 20),
                    bonuses = listOf(RollBonus(w.name, mod)),
                    allowAdvantageToggle = false,
                    onSettled = { rolled = it },
                    onReset = { rolled = null },
                )
            }
            // Journalling waits for an explicit commit: the roll can settle more
            // than once (roll again, or correct a hand-entered face — A23), and
            // committing from onSettled would log a Strike every time.
            rolled?.let { result ->
                PickerGap(16.dp)
                PrimaryActionButton(
                    label = "Record strike (${result.total})",
                    onClick = {
                        onStrike(w.id, attackIndex + 1, result.total, target.trim().takeIf { it.isNotEmpty() })
                    },
                )
            }
        }
    }
}

// ── Cast a spell ─────────────────────────────────────────────────────────────────

@Composable
internal fun PfCastSpellPicker(payload: PathfinderPayload, onCancel: () -> Unit, onCast: (spell: PfSpell, spellRank: Int, slotRank: Int) -> Unit) {
    val cantrips = payload.cantrips.mapNotNull { PfSpells.by(it) }.sortedBy { it.name }
    ActionPickerShell(kicker = "Combat", title = "Cast spell", onCancel = onCancel) {
        if (cantrips.isNotEmpty()) {
            PickerSection("Cantrips", top = 6.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cantrips.forEach { s -> PfListRow(s.name, "AT-WILL") { onCast(s, 0, 0) } }
            }
        }
        payload.knownSpells.keys.sorted().forEach { rank ->
            val spells = payload.knownSpells[rank].orEmpty().mapNotNull { PfSpells.by(it) }.sortedBy { it.name }
            if (spells.isEmpty()) return@forEach
            val remaining = payload.currentSpellSlots[rank] ?: 0
            PickerSection("Rank $rank · $remaining ${if (remaining == 1) "slot" else "slots"}")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                spells.forEach { s ->
                    PfListRow(s.name, if (remaining > 0) "◇" else "SPENT", enabled = remaining > 0) { onCast(s, s.rank, rank) }
                }
            }
        }
    }
}

@Composable
internal fun PfFocusPicker(payload: PathfinderPayload, onCancel: () -> Unit, onCast: (name: String) -> Unit) {
    val spells = payload.focusSpells.mapNotNull { PfSpells.by(it) }
    ActionPickerShell(kicker = "Focus", title = "Cast focus spell", onCancel = onCancel) {
        PickerHelpText("Focus Points ${payload.focusPoints} / ${payload.maxFocusPoints}", Modifier.padding(top = 6.dp, bottom = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            spells.forEach { s -> PfListRow(s.name, s.actions) { onCast(s.name) } }
        }
    }
}

@Composable
internal fun PfLearnSpellPicker(payload: PathfinderPayload, onCancel: () -> Unit, onLearn: (id: String, rank: Int) -> Unit) {
    val tradition = payload.spellTradition
    val known = (payload.cantrips + payload.knownSpells.values.flatten()).toSet()
    val pool = PfSpells.all.filter { (tradition == null || tradition in it.traditions) && it.id !in known }
        .sortedWith(compareBy({ it.rank }, { it.name }))
    ActionPickerShell(kicker = "Manage", title = "Learn spell", onCancel = onCancel) {
        PickerSection("Add to repertoire", top = 6.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            pool.forEach { s -> PfListRow(s.name, s.rankLabel) { onLearn(s.id, s.rank) } }
        }
    }
}

// ── Equipment ────────────────────────────────────────────────────────────────────

@Composable
internal fun PfArmorPicker(currentId: String?, onCancel: () -> Unit, onPick: (String?) -> Unit) {
    ActionPickerShell(kicker = "Manage", title = "Equip armor", onCancel = onCancel) {
        PickerSection("Worn armor", top = 6.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PfListRow("Unarmored", if (currentId == null) "WORN" else null) { onPick(null) }
            PfArmors.all.forEach { a ->
                PfListRow("${a.name} · ${a.category.displayName} +${a.acBonus}", if (currentId == a.id) "WORN" else null) { onPick(a.id) }
            }
        }
    }
}

@Composable
internal fun PfShieldPicker(currentId: String?, onCancel: () -> Unit, onPick: (String?) -> Unit) {
    ActionPickerShell(kicker = "Manage", title = "Equip shield", onCancel = onCancel) {
        PickerSection("Held shield", top = 6.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PfListRow("None", if (currentId == null) "HELD" else null) { onPick(null) }
            PfShields.all.forEach { s ->
                PfListRow("${s.name} (+${s.raisedAcBonus} raised)", if (currentId == s.id) "HELD" else null) { onPick(s.id) }
            }
        }
    }
}

@Composable
internal fun PfAddWeaponPicker(onCancel: () -> Unit, onPick: (String) -> Unit) {
    ActionPickerShell(kicker = "Manage", title = "Add weapon", onCancel = onCancel) {
        PickerSection("Catalogue", top = 6.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PfWeapons.all.forEach { w ->
                PfListRow("${w.name} · ${w.category.displayName} ${w.damageDie}${w.damageType}") { onPick(w.id) }
            }
        }
    }
}

@Composable
internal fun PfRemoveWeaponPicker(payload: PathfinderPayload, onCancel: () -> Unit, onPick: (String) -> Unit) {
    ActionPickerShell(kicker = "Manage", title = "Remove weapon", onCancel = onCancel) {
        PickerSection("Carried", top = 6.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            payload.weapons.mapNotNull { PfWeapons.by(it) }.forEach { w -> PfListRow(w.name, "✕") { onPick(w.id) } }
        }
    }
}

@Composable
internal fun PfWeaponRunePicker(payload: PathfinderPayload, onCancel: () -> Unit, onApply: (weaponId: String, potency: Int, striking: Int) -> Unit) {
    val weapons = payload.weapons.mapNotNull { PfWeapons.by(it) }
    var weaponId by remember { mutableStateOf(weapons.firstOrNull()?.id) }
    val existing = weaponId?.let { payload.weaponRunes[it] } ?: WeaponRunes()
    var potency by remember(weaponId) { mutableIntStateOf(existing.potency) }
    var striking by remember(weaponId) { mutableIntStateOf(existing.striking) }
    ActionPickerShell(
        kicker = "Manage",
        title = "Etch weapon runes",
        onCancel = onCancel,
        footer = { PrimaryActionButton("Etch runes", isDisabled = weaponId == null, onClick = { weaponId?.let { onApply(it, potency, striking) } }) },
    ) {
        if (weapons.size > 1) {
            PickerSection("Weapon", top = 6.dp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                weapons.forEach { w -> SmallCapsChip(w.name, active = weaponId == w.id) { weaponId = w.id } }
            }
        }
        PickerSection("Fundamental runes")
        PfStepper("Potency", potency, 0..Runes.MAX_TIER) { potency = it }
        PfStepper("Striking", striking, 0..Runes.MAX_TIER) { striking = it }
    }
}

@Composable
internal fun PfArmorRunePicker(payload: PathfinderPayload, onCancel: () -> Unit, onApply: (potency: Int, resilient: Int) -> Unit) {
    var potency by remember { mutableIntStateOf(payload.armorRunes.potency) }
    var resilient by remember { mutableIntStateOf(payload.armorRunes.resilient) }
    ActionPickerShell(
        kicker = "Manage",
        title = "Etch armor runes",
        onCancel = onCancel,
        footer = { PrimaryActionButton("Etch runes", onClick = { onApply(potency, resilient) }) },
    ) {
        PickerSection("Fundamental runes", top = 6.dp)
        PfStepper("Potency", potency, 0..Runes.MAX_TIER) { potency = it }
        PfStepper("Resilient", resilient, 0..Runes.MAX_TIER) { resilient = it }
    }
}

// ── Inventory & coin ─────────────────────────────────────────────────────────────

@Composable
internal fun PfItemPicker(onCancel: () -> Unit, onAdd: (PFInventoryItem) -> Unit) {
    var name by remember { mutableStateOf("") }
    var light by remember { mutableStateOf(false) }
    ActionPickerShell(
        kicker = "Inventory",
        title = "Add item",
        onCancel = onCancel,
        footer = { PrimaryActionButton("Add ${name.trim().ifEmpty { "item" }}", isDisabled = name.isBlank(), onClick = { onAdd(PFInventoryItem(name.trim(), 1, if (light) Bulk.LIGHT else 0.0)) }) },
    ) {
        PickerSection("Name", top = 6.dp)
        WizardTextField("Item name", name, { name = it })
        PickerSection("Bulk")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallCapsChip("— negligible", active = !light) { light = false }
            SmallCapsChip("L light", active = light) { light = true }
        }
    }
}

@Composable
internal fun PfCoinPicker(payload: PathfinderPayload, onCancel: () -> Unit, onApply: (coin: PFCoin, delta: Int) -> Unit) {
    var coin by remember { mutableStateOf(PFCoin.GP) }
    var delta by remember { mutableIntStateOf(0) }
    val current = payload.coins[coin] ?: 0
    ActionPickerShell(
        kicker = "Inventory",
        title = "Adjust coin",
        onCancel = onCancel,
        footer = { PrimaryActionButton(if (delta >= 0) "Gain $delta ${coin.abbreviation}" else "Spend ${-delta} ${coin.abbreviation}", isDanger = delta < 0, isDisabled = delta == 0, onClick = { onApply(coin, delta) }) },
    ) {
        PickerSection("Coin", top = 6.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PFCoin.entries.reversed().forEach { c ->
                SmallCapsChip(c.abbreviation, active = coin == c, modifier = Modifier.weight(1f)) { coin = c; delta = 0 }
            }
        }
        PickerHelpText("Have $current ${coin.abbreviation}", Modifier.padding(top = 10.dp))
        PickerSection("Change")
        PfStepper(coin.abbreviation, delta, -current..9999) { delta = it }
    }
}

// ── Feats ────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PfTakeFeatPicker(payload: PathfinderPayload, onCancel: () -> Unit, onTake: (String) -> Unit) {
    var type by remember { mutableStateOf(PfFeatType.CLASS) }
    val pool = PfFeats.available(type, payload.ancestry, payload.className, payload.level).filter { it.id !in payload.feats }
    ActionPickerShell(kicker = "Manage", title = "Take feat", onCancel = onCancel) {
        PickerSection("Type", top = 6.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            PfFeatType.entries.forEach { t -> SmallCapsChip(t.displayName, active = type == t) { type = t } }
        }
        PickerSection("${type.displayName} feats")
        if (pool.isEmpty()) {
            PickerHelpText("No ${type.displayName.lowercase()} feats available at level ${payload.level}.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pool.forEach { f -> PfListRow("${f.name} · Lv ${f.level}") { onTake(f.id) } }
            }
        }
    }
}

@Composable
internal fun PfRemoveFeatPicker(payload: PathfinderPayload, onCancel: () -> Unit, onRemove: (String) -> Unit) {
    ActionPickerShell(kicker = "Manage", title = "Remove feat", onCancel = onCancel) {
        PickerSection("Taken", top = 6.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            payload.feats.mapNotNull { PfFeats.by(it) }.forEach { f -> PfListRow("${f.name} · ${f.type.displayName}", "✕") { onRemove(f.id) } }
        }
    }
}

@Composable
internal fun PfNotePicker(onCancel: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    ActionPickerShell(
        kicker = "Manage",
        title = "Add a note",
        onCancel = onCancel,
        footer = { PrimaryActionButton("Add note", isDisabled = text.isBlank(), onClick = { onAdd(text.trim()) }) },
    ) {
        PickerSection("Note", top = 6.dp)
        WizardTextField("Journal note", text, { text = it }, multiline = true)
    }
}

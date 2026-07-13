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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.ResolvedFeature
import au.com.evonet.nat20.dnd5e.ResolvedResourcePool
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.dnd5e.core.Condition
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.FeatureRecovery
import au.com.evonet.nat20.dnd5e.core.RestKind
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e.effectiveScore
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.slugToTitle
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The Slice-A world/rest/resource pickers (parity #19): coin, conditions,
 * rests (with per-effect retain toggles), hit dice, slot expend, point-pool
 * spend, and class-feature use. See `CorePickers.kt` for the vitals/session
 * pickers and `ActionsLayer.kt` for the intent wiring.
 */

// ── Coin picker ────────────────────────────────────────────────────────────────

/** Adjust the purse: denomination, gain/spend, amount, optional source (iOS `CoinPicker`). */
@Composable
internal fun CoinPicker(
    current: Map<Coin, Int>,
    onCancel: () -> Unit,
    onCommit: (Coin, Int, String?) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var coin by remember { mutableStateOf(Coin.GP) }
    var gaining by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }

    val amount = amountText.toIntOrNull() ?: 0
    val held = current[coin] ?: 0
    val canCommit = amount > 0 && (gaining || amount <= held)

    ActionPickerShell(
        kicker = "World",
        title = "Adjust coin",
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(
                label = if (!gaining && amount > held) {
                    "Only $held ${coin.abbreviation} held"
                } else {
                    "${if (gaining) "Gain" else "Spend"} $amount ${coin.abbreviation}"
                },
                isDanger = !gaining,
                isDisabled = !canCommit,
                onClick = {
                    onCommit(coin, if (gaining) amount else -amount, source.trim().takeIf { it.isNotEmpty() })
                },
            )
        },
    ) {
        // Current purse, all five denominations.
        PickerGap(6.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Coin.entries.forEach { c ->
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.tile)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "${current[c] ?: 0}",
                        fontFamily = Cormorant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = palette.ink,
                    )
                    Text(
                        c.abbreviation,
                        fontFamily = Cinzel,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = palette.inkMute,
                    )
                }
            }
        }

        PickerSection("Denomination")
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Coin.entries.forEach { c ->
                val active = coin == c
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (active) palette.accent else palette.tile)
                        .border(
                            1.dp,
                            if (active) palette.accent else palette.ink.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp),
                        )
                        .clickable { coin = c }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        c.abbreviation,
                        fontFamily = Cinzel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = if (active) palette.cream else palette.ink,
                    )
                    Text(
                        c.fullName,
                        fontFamily = ImFell,
                        fontStyle = FontStyle.Italic,
                        fontSize = 10.sp,
                        color = if (active) palette.cream else palette.inkSoft,
                        maxLines = 1,
                    )
                }
            }
        }

        PickerSection("Direction")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallCapsChip("Gain", active = gaining, tone = palette.accentGold) { gaining = true }
            SmallCapsChip("Spend", active = !gaining, tone = palette.danger) { gaining = false }
        }

        PickerSection("Amount")
        WizardTextField("0", amountText, { new -> amountText = new.filter(Char::isDigit).take(6) })

        PickerSection("Source (optional)")
        WizardTextField("Loot, inn stay, etc.", source, { source = it })
    }
}

// ── Condition picker ───────────────────────────────────────────────────────────

internal enum class ConditionPickerMode { APPLY, CLEAR }

/**
 * Apply or clear a condition (iOS `ConditionPicker`). Apply mode offers the
 * 14 standard 2014 conditions + Exhaustion + a custom field; clear mode
 * lists only the character's active conditions for tap-to-clear. The layer
 * maps "Exhaustion" onto the `AdjustExhaustion` track rather than the
 * condition list (Android models exhaustion as a 0–6 level, like the engine).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConditionPicker(
    mode: ConditionPickerMode,
    activeConditions: List<String> = emptyList(),
    onCancel: () -> Unit,
    onCommit: (String) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var customName by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }

    val standard = Condition.entries.map { it.displayName } + "Exhaustion"
    val commitTarget = selected ?: customName.trim().takeIf { it.isNotEmpty() }

    ActionPickerShell(
        kicker = "Conditions",
        title = if (mode == ConditionPickerMode.APPLY) "Apply condition" else "Clear condition",
        onCancel = onCancel,
        footer = if (mode == ConditionPickerMode.APPLY) {
            {
                PrimaryActionButton(
                    label = commitTarget?.let { "Apply $it" } ?: "Apply",
                    isDisabled = commitTarget == null,
                    onClick = { commitTarget?.let(onCommit) },
                )
            }
        } else {
            null
        },
    ) {
        when (mode) {
            ConditionPickerMode.APPLY -> {
                PickerSection("Standard", top = 6.dp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    standard.forEach { name ->
                        SmallCapsChip(name, active = selected == name) {
                            selected = if (selected == name) null else name
                            if (selected != null) customName = ""
                        }
                    }
                }

                PickerSection("Or custom")
                WizardTextField(
                    "Cursed, Marked, etc.",
                    customName,
                    { customName = it; if (it.isNotEmpty()) selected = null },
                )
            }

            ConditionPickerMode.CLEAR -> {
                if (activeConditions.isEmpty()) {
                    PickerGap(32.dp)
                    Text(
                        "No active conditions to clear.",
                        fontFamily = Cormorant,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                        color = palette.inkSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    PickerSection("Active · ${activeConditions.size}", top = 6.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeConditions.forEach { name ->
                            PickerRow(onClick = { onCommit(name) }) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        name,
                                        fontFamily = Cormorant,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = palette.ink,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text("✕", fontSize = 14.sp, color = palette.danger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Rest picker ────────────────────────────────────────────────────────────────

/**
 * Short / long rest commit (iOS `RestPicker`): hero glyph + blurb +
 * "This will" bullets + a per-effect "still on?" toggle list whose defaults
 * follow each effect's duration. Commit hands back the IDs of the effects
 * the player toggled OFF; the layer fires `CancelEffect` for each before
 * applying the rest intent.
 */
@Composable
internal fun RestPicker(
    kind: RestKind,
    description: String,
    secondaryEffects: List<String>,
    activeEffects: List<ActiveEffect>,
    onCancel: () -> Unit,
    onCommit: (cancelledIds: List<String>) -> Unit,
    onPrepareSpells: (() -> Unit)? = null,
) {
    val palette = MaterialTheme.natPalette
    val title = if (kind == RestKind.SHORT) "Short rest" else "Long rest"

    fun defaultRetained(effect: ActiveEffect): Boolean = when (val d = effect.duration) {
        is EffectDuration.UntilRest -> when (d.rest) {
            RestKind.SHORT -> false
            RestKind.LONG -> kind == RestKind.SHORT // a short rest keeps long-rest effects
        }
        else -> true
    }

    val retained = remember(activeEffects) {
        androidx.compose.runtime.mutableStateMapOf<String, Boolean>().apply {
            activeEffects.forEach { put(it.id, defaultRetained(it)) }
        }
    }

    ActionPickerShell(
        kicker = "Rest",
        title = title,
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(
                label = if (kind == RestKind.SHORT) "Take a short rest" else "Take a long rest",
                onClick = {
                    onCommit(activeEffects.filter { retained[it.id] == false }.map { it.id })
                },
            )
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

        if (onPrepareSpells != null) {
            PickerSection("Before you rest")
            PickerRow(borderTone = palette.accent.copy(alpha = 0.4f), onClick = onPrepareSpells) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Prepare spells",
                            fontFamily = Cormorant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = palette.ink,
                        )
                        PickerHelpText("Change your prepared list before committing the rest")
                    }
                    Text("›", fontSize = 18.sp, color = palette.inkMute)
                }
            }
        }

        if (activeEffects.isNotEmpty()) {
            PickerSection("Active effects")
            PickerHelpText(
                "Toggle off any effect that ends with the rest. Defaults follow each effect's duration.",
                Modifier.padding(bottom = 8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                activeEffects.forEach { effect ->
                    val isRetained = retained[effect.id] ?: defaultRetained(effect)
                    val tone = if (isRetained) palette.accent else palette.danger
                    PickerRow(
                        borderTone = tone.copy(alpha = 0.4f),
                        onClick = { retained[effect.id] = !isRetained },
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(18.dp).border(1.dp, palette.accent.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isRetained) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(palette.accent))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    effect.name,
                                    fontFamily = Cormorant,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = palette.ink,
                                )
                                Text(
                                    durationLabel(effect.duration).uppercase(),
                                    fontFamily = Cinzel,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.5.sp,
                                    color = palette.inkMute,
                                )
                            }
                            Text(
                                (if (isRetained) "Still on" else "Ends").uppercase(),
                                fontFamily = Cinzel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                                color = tone,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun durationLabel(duration: EffectDuration): String = when (duration) {
    EffectDuration.UntilCancelled -> "Until cancelled"
    EffectDuration.Concentration -> "Concentration"
    is EffectDuration.UntilRest ->
        if (duration.rest == RestKind.SHORT) "Until short rest" else "Until long rest"
    is EffectDuration.Rounds -> "${duration.count} rounds"
}

// ── Spend hit die ──────────────────────────────────────────────────────────────

/**
 * Rolls a hit die (iOS `SpendHitDiePicker`): pool banner, a class chip strip
 * when multiclass (RAW: the player picks which class's die to spend), and
 * the inline d20 primitive with the CON chip. Switching class resets the
 * roll (the die size changed). Commit hands back the clamped-≥1 total.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpendHitDiePicker(
    payload: DnD5ePayload,
    onCancel: () -> Unit,
    onCommit: (restored: Int) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    val classRows = payload.classes.map { entry ->
        Triple(entry.classId, DnD5eClasses.hitDie(entry.classId), entry.classId.slugToTitle())
    }
    var selectedClassId by remember { mutableStateOf(classRows.firstOrNull()?.first) }
    var rolledTotal by remember { mutableStateOf<Int?>(null) }

    val selected = classRows.firstOrNull { it.first == selectedClassId } ?: classRows.firstOrNull()
    val conMod = AbilityScores.modifier(payload.effectiveScore(Ability.CONSTITUTION))
    val remaining = payload.currentHitDice
    val canCommit = rolledTotal != null && remaining > 0

    ActionPickerShell(
        kicker = "Rest",
        title = "Spend hit die",
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(
                label = rolledTotal?.let { "Restore ${maxOf(1, it)} HP" } ?: "Roll first",
                isDisabled = !canCommit,
                onClick = { rolledTotal?.let { onCommit(maxOf(1, it)) } },
            )
        },
    ) {
        // Pool banner.
        Column(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "$remaining / ${payload.maxHitDice} remaining",
                fontFamily = Cormorant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = palette.accent,
            )
            Text(
                if (remaining == 0) "No hit dice left — long rest to recover"
                else "Long rest restores half (rounded down).",
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = palette.inkMute,
            )
        }

        if (classRows.size > 1) {
            PickerSection("Which die?")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                classRows.forEach { (classId, die, name) ->
                    PickerChip(
                        "$name d$die",
                        active = selected?.first == classId,
                    ) {
                        // Switching invalidates the in-progress roll — the die changed.
                        selectedClassId = classId
                        rolledTotal = null
                    }
                }
            }
        }

        PickerSection("Roll", top = if (classRows.size > 1) 12.dp else 6.dp)
        when {
            selected != null && remaining > 0 -> {
                // Remount the roll primitive when the die changes so a stale
                // result can't carry across class picks.
                key(selected.first) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        RollResultView(
                            baseSpec = RollSpec.d(1, selected.second),
                            bonuses = if (conMod != 0) listOf(RollBonus("CON", conMod)) else emptyList(),
                            allowAdvantageToggle = false,
                            onSettled = { rolledTotal = it.total },
                        )
                    }
                }
            }
            remaining == 0 -> {
                Text(
                    "Spent everything — take a long rest.",
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    color = palette.inkMute,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                )
            }
            else -> {
                Text(
                    "No class on the sheet — can't roll a hit die.",
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    color = palette.inkMute,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                )
            }
        }
    }
}

// ── Expend slot ────────────────────────────────────────────────────────────────

/** One expendable slot level; current/max already merge the warlock pact pool. */
internal data class SlotOption(val level: Int, val current: Int, val max: Int)

/**
 * Burn a spell slot without casting (Counterspell, sorcery-point conversion,
 * smite outside the attack flow — iOS `ExpendSlotPicker`). Defaults to the
 * lowest level with a slot remaining.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExpendSlotPicker(
    slots: List<SlotOption>,
    onCancel: () -> Unit,
    onCommit: (slotLevel: Int, source: String?) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var selectedLevel by remember {
        mutableIntStateOf((slots.firstOrNull { it.current > 0 } ?: slots.firstOrNull())?.level ?: 1)
    }
    var source by remember { mutableStateOf("") }
    val selected = slots.firstOrNull { it.level == selectedLevel }
    val canCommit = (selected?.current ?: 0) > 0

    ActionPickerShell(
        kicker = "Magic",
        title = "Expend a slot",
        onCancel = onCancel,
        footer = if (selected != null) {
            {
                PrimaryActionButton(
                    label = if (selected.current > 0) "Expend level ${selected.level} slot"
                    else "No level ${selected.level} slots",
                    isDisabled = !canCommit,
                    onClick = { onCommit(selected.level, source.trim().takeIf { it.isNotEmpty() }) },
                )
            }
        } else {
            null
        },
    ) {
        if (slots.isEmpty()) {
            PickerGap(32.dp)
            Text(
                "No spell slots to spend.\nRest to recover slots, then they'll show up here.",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = palette.inkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PickerSection("Slot level", top = 6.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                slots.forEach { slot ->
                    val active = slot.level == selectedLevel
                    val empty = slot.current == 0
                    Column(
                        Modifier
                            .widthIn(min = 52.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (active) palette.accent else palette.tile)
                            .border(
                                1.dp,
                                if (active) palette.accent else palette.ink.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp),
                            )
                            .clickable { selectedLevel = slot.level }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "L${slot.level}",
                            fontFamily = Cinzel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp,
                            color = if (active) palette.cream else if (empty) palette.inkMute else palette.ink,
                        )
                        Text(
                            "${slot.current}/${slot.max}",
                            fontFamily = ImFell,
                            fontStyle = FontStyle.Italic,
                            fontSize = 10.sp,
                            color = if (active) palette.cream else if (empty) palette.inkMute else palette.ink,
                        )
                    }
                }
            }

            selected?.let { slot ->
                PickerGap(8.dp)
                PickerRow {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Level ${slot.level}",
                            fontFamily = Cormorant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = palette.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${slot.current} / ${slot.max}",
                            fontFamily = Cormorant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = if (slot.current > 0) palette.accentGold else palette.inkMute,
                        )
                    }
                }
            }

            PickerSection("Reason (optional)")
            WizardTextField("Counterspell, → 2 sorcery points, Divine Smite…", source, { source = it })
        }
    }
}

// ── Spend resource ─────────────────────────────────────────────────────────────

/**
 * Spend points from a class point-pool — Ki, Sorcery Points, Lay on Hands
 * (iOS `SpendResourcePicker`). The tile only surfaces when a pool is online.
 */
@Composable
internal fun SpendResourcePicker(
    pools: List<ResolvedResourcePool>,
    onCancel: () -> Unit,
    onCommit: (poolID: String, amount: Int, note: String?) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var selectedPoolId by remember {
        mutableStateOf((pools.firstOrNull { it.current > 0 } ?: pools.firstOrNull())?.id ?: "")
    }
    var amount by remember { mutableIntStateOf(1) }
    var note by remember { mutableStateOf("") }

    val pool = pools.firstOrNull { it.id == selectedPoolId }
    val maxSpend = pool?.current ?: 0
    val canCommit = maxSpend > 0 && amount in 1..maxSpend

    ActionPickerShell(
        kicker = "Resources",
        title = "Spend resource",
        onCancel = onCancel,
        footer = if (pool != null) {
            {
                val unit = if (pool.id == "lay-on-hands") "HP" else if (amount == 1) "pt" else "pts"
                PrimaryActionButton(
                    label = if (maxSpend > 0) "Spend $amount $unit" else "${pool.displayName} empty",
                    isDisabled = !canCommit,
                    onClick = { onCommit(pool.id, amount, note.trim().takeIf { it.isNotEmpty() }) },
                )
            }
        } else {
            null
        },
    ) {
        if (pools.isEmpty()) {
            PickerGap(32.dp)
            Text(
                "No pools to spend.\nKi, Sorcery Points, and Lay on Hands show up here.",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = palette.inkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            if (pools.size > 1) {
                PickerSection("Pool", top = 6.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pools.forEach { p ->
                        val active = p.id == selectedPoolId
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (active) palette.accent else palette.tile)
                                .border(
                                    1.dp,
                                    if (active) palette.accent else palette.ink.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp),
                                )
                                .clickable {
                                    selectedPoolId = p.id
                                    amount = amount.coerceIn(1, maxOf(1, p.current))
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                p.abbreviation,
                                fontFamily = Cinzel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.5.sp,
                                color = if (active) palette.cream else palette.ink,
                            )
                            Text(
                                "${p.current}/${p.max}",
                                fontFamily = ImFell,
                                fontStyle = FontStyle.Italic,
                                fontSize = 10.sp,
                                color = if (active) palette.cream else palette.inkSoft,
                            )
                        }
                    }
                }
            }

            pool?.let { p ->
                PickerGap(if (pools.size > 1) 0.dp else 6.dp)
                PickerRow(Modifier.padding(top = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            p.displayName,
                            fontFamily = Cormorant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = palette.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${p.current} / ${p.max}",
                            fontFamily = Cormorant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = if (p.current > 0) palette.accentGold else palette.inkMute,
                        )
                    }
                }
            }

            PickerSection("Amount")
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
                AmountStepButton("−", enabled = maxSpend > 0) { amount = maxOf(1, amount - 1) }
                Text(
                    "$amount",
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 34.sp,
                    color = if (maxSpend > 0) palette.accent else palette.inkMute,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(80.dp),
                )
                AmountStepButton("+", enabled = maxSpend > 0) { amount = minOf(maxOf(1, maxSpend), amount + 1) }
            }

            PickerSection("Note (optional)")
            WizardTextField("Flurry of Blows, Quickened Spell, healed Kael…", note, { note = it })
        }
    }
}

@Composable
private fun AmountStepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(palette.tileStrong)
            .border(1.dp, palette.accent.copy(alpha = 0.33f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            fontFamily = Cormorant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            color = palette.ink,
        )
    }
}

// ── Use class feature ──────────────────────────────────────────────────────────

/**
 * Lists every class feature the character has online and triggers one in a
 * tap (iOS `UseClassFeaturePicker`). Uses/recovery badges ride each row;
 * exhausted counters disable their Use button. Android's resolved features
 * don't carry the granting class/level/description yet, so the rows are
 * flatter than iOS's grouped, expandable list.
 */
@Composable
internal fun UseClassFeaturePicker(
    features: List<ResolvedFeature>,
    onCancel: () -> Unit,
    onUse: (ResolvedFeature) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    ActionPickerShell(
        kicker = "Resources",
        title = "Use class feature",
        onCancel = onCancel,
    ) {
        if (features.isEmpty()) {
            PickerGap(40.dp)
            Text(
                "No class features yet.\nThey unlock as the character levels up.",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = palette.inkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PickerSection("Features", top = 6.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                features.forEach { feature ->
                    val exhausted = (feature.current ?: 1) <= 0
                    PickerRow {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    feature.displayName,
                                    fontFamily = Cormorant,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = palette.ink,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (feature.current != null && feature.max != null) {
                                        UsesBadge(feature.current!!, feature.max!!)
                                    }
                                    RecoveryBadge(feature.recovery)
                                }
                            }
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(if (exhausted) palette.tile else palette.accent)
                                    .border(
                                        1.dp,
                                        if (exhausted) palette.inkMute else palette.accent,
                                        CircleShape,
                                    )
                                    .clickable(enabled = !exhausted) { onUse(feature) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    "USE",
                                    fontFamily = Cinzel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 2.sp,
                                    color = if (exhausted) palette.inkMute else palette.cream,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsesBadge(remaining: Int, max: Int) {
    val palette = MaterialTheme.natPalette
    val tint = if (remaining <= 0) palette.danger else palette.accent
    Box(
        Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.1f))
            .border(1.dp, tint.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            "× $remaining/$max",
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = tint,
        )
    }
}

@Composable
private fun RecoveryBadge(recovery: FeatureRecovery) {
    val palette = MaterialTheme.natPalette
    val (label, tint) = when (recovery) {
        FeatureRecovery.SHORT_REST -> "Short rest" to palette.accent
        FeatureRecovery.LONG_REST -> "Long rest" to palette.accentDeep
        FeatureRecovery.AT_WILL -> "At will" to palette.accentGold
        FeatureRecovery.OTHER -> "Special" to palette.inkMute
    }
    Box(
        Modifier
            .clip(CircleShape)
            .border(1.dp, tint.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            label.uppercase(),
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = tint,
        )
    }
}

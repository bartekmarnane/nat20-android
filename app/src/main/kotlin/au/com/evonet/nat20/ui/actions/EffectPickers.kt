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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.SkillCatalog
import au.com.evonet.nat20.dnd5e.SpellEffectCatalog
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.EffectSource
import au.com.evonet.nat20.dnd5e.core.RestKind
import au.com.evonet.nat20.ui.codex.impactLabel
import au.com.evonet.nat20.ui.editor.StepperButton
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The slice-C EFFECT pickers (parity #19): the structured buff/debuff
 * [EffectPicker] (ability / save / skill × buff / penalty), the free-form
 * [CustomEffectPicker] (name + source + stacked modifiers + duration), and
 * [AllyCastPicker] (receive a party member's spell). All commit an
 * [ActiveEffect] the caller feeds to `ApplyEffect`. Ports of the iOS
 * `EffectPicker` / `CustomEffectPicker` / `AllyCastPicker`. Android has a full
 * [ActiveEffect] model, so — unlike iOS, which folds these into the journal
 * source string — the buff/debuff picker builds a real typed modifier.
 */

// ── Shared duration model ────────────────────────────────────────────────────

/** Duration chips shared by the buff/debuff + custom pickers (iOS `EffectPicker.Duration`). */
internal enum class EffectDurationChoice(val label: String, val includesUntilCancelled: Boolean = false) {
    UNTIL_CANCELLED("Until cancelled", true),
    TURN("1 round"),
    ONE_MINUTE("1 min"),
    TEN_MINUTES("10 min"),
    ONE_HOUR("1 hr"),
    SHORT_REST("Short rest"),
    LONG_REST("Long rest"),
    CONCENTRATION("Concentration");

    /** Maps the chip to a domain [EffectDuration]. Real-time decay isn't enforced (rests + concentration drive cancel). */
    fun toDomain(): EffectDuration = when (this) {
        UNTIL_CANCELLED -> EffectDuration.UntilCancelled
        TURN -> EffectDuration.Rounds(1)
        ONE_MINUTE -> EffectDuration.Rounds(10)
        TEN_MINUTES -> EffectDuration.Rounds(100)
        ONE_HOUR -> EffectDuration.Rounds(600)
        SHORT_REST -> EffectDuration.UntilRest(RestKind.SHORT)
        LONG_REST -> EffectDuration.UntilRest(RestKind.LONG)
        CONCENTRATION -> EffectDuration.Concentration
    }
}

// ── Buff / debuff picker ─────────────────────────────────────────────────────

/** Which stat family the buff/debuff lands on. */
internal enum class EffectTargetKind(val label: String, val metaWord: String) {
    ABILITY("Ability", "ability"),
    SAVE("Saving Throw", "save"),
    SKILL("Skill", "skill"),
}

internal enum class EffectSign { BUFF, DEBUFF }

/** Magnitude chip — dice collapse to their rounded-down average for the stored delta (iOS parity). */
internal enum class EffectMagnitude(private val base: Int, private val diceLabel: String?) {
    ONE(1, null),
    D4(2, "1d4"),
    D6(3, "1d6"),
    TWO(2, null);

    fun label(sign: EffectSign): String {
        val prefix = if (sign == EffectSign.BUFF) "+" else "−"
        return prefix + (diceLabel ?: base.toString())
    }

    fun delta(sign: EffectSign): Int = if (sign == EffectSign.BUFF) base else -base
}

/**
 * Buff/debuff a single ability, save, or skill by a fixed magnitude for a
 * chosen duration, with an optional source label. Commits a typed
 * [ActiveEffect] (AbilityDelta / SaveBonus / SkillBonus).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EffectPicker(
    kicker: String,
    title: String,
    targetKind: EffectTargetKind,
    sign: EffectSign,
    onCancel: () -> Unit,
    onCommit: (ActiveEffect) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    val tone = if (sign == EffectSign.DEBUFF) palette.danger else palette.accentGold
    val abilities = remember { Ability.entries.toList() }
    val skills = remember { SkillCatalog.all }

    var targetIndex by remember { mutableIntStateOf(0) }
    var magnitude by remember { mutableStateOf(EffectMagnitude.D4) }
    var duration by remember { mutableStateOf(EffectDurationChoice.ONE_MINUTE) }
    var source by remember { mutableStateOf("") }

    val targetName: String = when (targetKind) {
        EffectTargetKind.ABILITY, EffectTargetKind.SAVE -> abilities[targetIndex].abbreviation
        EffectTargetKind.SKILL -> skills[targetIndex].name
    }

    fun build(): ActiveEffect {
        val delta = magnitude.delta(sign)
        val modifier: EffectModifier = when (targetKind) {
            EffectTargetKind.ABILITY -> EffectModifier.AbilityDelta(abilities[targetIndex], delta)
            EffectTargetKind.SAVE -> EffectModifier.SaveBonus(abilities[targetIndex], delta)
            EffectTargetKind.SKILL -> EffectModifier.SkillBonus(skills[targetIndex].id, delta)
        }
        val base = "$targetName ${magnitude.label(sign)}"
        val src = source.trim()
        return ActiveEffect(
            id = ActiveEffect.newId(),
            name = if (src.isEmpty()) base else "$src — $base",
            source = EffectSource.Custom,
            modifiers = listOf(modifier),
            duration = duration.toDomain(),
            concentrationOwner = duration == EffectDurationChoice.CONCENTRATION,
        )
    }

    ActionPickerShell(
        kicker = kicker,
        title = title,
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(
                label = if (sign == EffectSign.BUFF) "Apply buff" else "Apply penalty",
                isDanger = sign == EffectSign.DEBUFF,
                onClick = { onCommit(build()) },
            )
        },
    ) {
        // Preview banner.
        EffectPreviewBanner(
            headline = "$targetName ${magnitude.label(sign)}",
            meta = listOfNotNull(targetKind.metaWord, duration.label, source.trim().takeIf { it.isNotEmpty() })
                .joinToString(" · "),
            tone = tone,
            modifier = Modifier.padding(top = 6.dp),
        )

        PickerSection(targetKind.label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            when (targetKind) {
                EffectTargetKind.SKILL -> skills.forEachIndexed { i, s ->
                    PickerChip(s.name, active = targetIndex == i) { targetIndex = i }
                }
                else -> abilities.forEachIndexed { i, a ->
                    SmallCapsChip(a.abbreviation, active = targetIndex == i) { targetIndex = i }
                }
            }
        }

        PickerSection("Magnitude")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            EffectMagnitude.entries.forEach { m ->
                PickerChip(m.label(sign), active = magnitude == m, tone = tone) { magnitude = m }
            }
        }

        PickerSection("Duration")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EffectDurationChoice.entries.filterNot { it.includesUntilCancelled }.forEach { d ->
                SmallCapsChip(d.label, active = duration == d) { duration = d }
            }
        }

        PickerSection("Source (optional)")
        WizardTextField("Bless, Maren's spell, etc.", source, { if (it.length <= 60) source = it })
        PickerGap(8.dp)
    }
}

// ── Custom effect picker ─────────────────────────────────────────────────────

/** The modifier kinds the custom picker can stage (mirrors iOS `CustomEffectPicker.ModifierKind`). */
internal enum class CustomModifierKind(val label: String) {
    ATTACK("Attack"),
    DAMAGE("Damage"),
    AC("AC"),
    ABILITY("Ability"),
    SAVE("Save"),
    SKILL("Skill"),
    RESIST("Resist"),
    ADVANTAGE("Advantage"),
    NOTE("Note");

    val needsMagnitude: Boolean get() = this !in setOf(RESIST, ADVANTAGE, NOTE)
    val needsAbility: Boolean get() = this == ABILITY || this == SAVE
    val needsSkill: Boolean get() = this == SKILL
    val needsText: Boolean get() = this == RESIST || this == ADVANTAGE || this == NOTE
    val textPlaceholder: String
        get() = when (this) {
            RESIST -> "fire, poison, …"
            ADVANTAGE -> "saves vs poison, STR checks while raging, …"
            NOTE -> "Narrative effect"
            else -> ""
        }
}

/**
 * Assembles an arbitrary [ActiveEffect] from scratch: free-text name + optional
 * source + one or more stacked typed modifiers + duration. Android's
 * [EffectModifier] hierarchy covers every kind the iOS picker exposes (attack,
 * damage, AC, ability, save, skill, resistance, advantage, note), so the
 * modifier-row editor ports at full depth — no gaps versus iOS. Picking a
 * Concentration duration makes the effect a concentration owner.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomEffectPicker(
    onCancel: () -> Unit,
    onCommit: (ActiveEffect) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    val abilities = remember { Ability.entries.toList() }
    val skills = remember { SkillCatalog.all }

    var name by remember { mutableStateOf("") }
    var sourceLabel by remember { mutableStateOf("") }
    var modifiers by remember { mutableStateOf(listOf<EffectModifier>()) }
    var duration by remember { mutableStateOf(EffectDurationChoice.UNTIL_CANCELLED) }

    var adding by remember { mutableStateOf(false) }
    var pendingKind by remember { mutableStateOf(CustomModifierKind.ATTACK) }
    var pendingAbility by remember { mutableIntStateOf(0) }
    // -1 ⇒ "all saves" (only when kind == SAVE).
    var pendingSaveAbility by remember { mutableIntStateOf(-1) }
    var pendingSkill by remember { mutableIntStateOf(0) }
    var pendingMagnitude by remember { mutableIntStateOf(1) }
    var pendingText by remember { mutableStateOf("") }

    fun resetPending() {
        adding = false
        pendingMagnitude = 1
        pendingText = ""
        pendingSaveAbility = -1
    }

    fun buildPending(): EffectModifier? {
        val text = pendingText.trim()
        return when (pendingKind) {
            CustomModifierKind.ATTACK -> EffectModifier.AttackBonus(pendingMagnitude)
            CustomModifierKind.DAMAGE -> EffectModifier.DamageBonus(pendingMagnitude)
            CustomModifierKind.AC -> EffectModifier.AcBonus(pendingMagnitude)
            CustomModifierKind.ABILITY -> EffectModifier.AbilityDelta(abilities[pendingAbility], pendingMagnitude)
            CustomModifierKind.SAVE -> EffectModifier.SaveBonus(pendingSaveAbility.takeIf { it >= 0 }?.let { abilities[it] }, pendingMagnitude)
            CustomModifierKind.SKILL -> EffectModifier.SkillBonus(skills[pendingSkill].id, pendingMagnitude)
            CustomModifierKind.RESIST -> text.takeIf { it.isNotEmpty() }?.let { EffectModifier.DamageResistance(it) }
            CustomModifierKind.ADVANTAGE -> text.takeIf { it.isNotEmpty() }?.let { EffectModifier.AdvantageOn(it) }
            CustomModifierKind.NOTE -> text.takeIf { it.isNotEmpty() }?.let { EffectModifier.FreeText(it) }
        }
    }

    val canAddPending = !pendingKind.needsText || pendingText.trim().isNotEmpty()
    val canCommit = name.trim().isNotEmpty() && modifiers.isNotEmpty()

    fun commit() {
        if (!canCommit) return
        val src = sourceLabel.trim()
        val display = if (src.isEmpty()) name.trim() else "$src — ${name.trim()}"
        onCommit(
            ActiveEffect(
                id = ActiveEffect.newId(),
                name = display,
                source = EffectSource.Custom,
                modifiers = modifiers,
                duration = duration.toDomain(),
                concentrationOwner = duration == EffectDurationChoice.CONCENTRATION,
            ),
        )
    }

    ActionPickerShell(
        kicker = "Effects",
        title = "Custom effect",
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(label = "Apply effect", isDisabled = !canCommit, onClick = ::commit)
        },
    ) {
        EffectPreviewBanner(
            headline = name.trim().ifEmpty { "Unnamed effect" },
            meta = if (modifiers.isEmpty()) "Add a modifier to see its impact" else modifiers.joinToString(" · ") { it.impactLabel() },
            tone = palette.accent,
            modifier = Modifier.padding(top = 6.dp),
        )

        PickerSection("Name")
        WizardTextField("Goggles of Night, sneak stance, …", name, { if (it.length <= 60) name = it })

        PickerSection("Source (optional)")
        WizardTextField("DM ruling, ally's spell, …", sourceLabel, { if (it.length <= 60) sourceLabel = it })

        PickerSection("Modifiers")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            modifiers.forEachIndexed { index, modifier ->
                PickerRow {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            modifier.impactLabel(),
                            fontFamily = Cormorant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = palette.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "✕",
                            fontSize = 13.sp,
                            color = palette.danger.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { modifiers = modifiers.filterIndexed { i, _ -> i != index } }
                                .padding(4.dp),
                        )
                    }
                }
            }

            if (adding) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.tile.copy(alpha = 0.5f))
                        .border(1.dp, palette.accent.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SubLabel("Kind")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        CustomModifierKind.entries.forEach { k ->
                            PickerChip(k.label, active = pendingKind == k) {
                                pendingKind = k
                                if (k == CustomModifierKind.SAVE) pendingSaveAbility = -1
                            }
                        }
                    }

                    if (pendingKind.needsAbility) {
                        SubLabel("Ability")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (pendingKind == CustomModifierKind.SAVE) {
                                SmallCapsChip("All", active = pendingSaveAbility == -1) { pendingSaveAbility = -1 }
                            }
                            abilities.forEachIndexed { i, a ->
                                val active = if (pendingKind == CustomModifierKind.SAVE) pendingSaveAbility == i else pendingAbility == i
                                SmallCapsChip(a.abbreviation, active = active) {
                                    if (pendingKind == CustomModifierKind.SAVE) pendingSaveAbility = i else pendingAbility = i
                                }
                            }
                        }
                    }

                    if (pendingKind.needsSkill) {
                        SubLabel("Skill")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            skills.forEachIndexed { i, s -> PickerChip(s.name, active = pendingSkill == i) { pendingSkill = i } }
                        }
                    }

                    if (pendingKind.needsMagnitude) {
                        SubLabel("Magnitude")
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StepperButton("−", enabled = pendingMagnitude > -5) { if (pendingMagnitude > -5) pendingMagnitude-- }
                            Text(
                                if (pendingMagnitude >= 0) "+$pendingMagnitude" else "$pendingMagnitude",
                                fontFamily = Cinzel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = when {
                                    pendingMagnitude > 0 -> palette.accent
                                    pendingMagnitude < 0 -> palette.danger
                                    else -> palette.ink
                                },
                            )
                            StepperButton("+", enabled = pendingMagnitude < 5) { if (pendingMagnitude < 5) pendingMagnitude++ }
                        }
                    }

                    if (pendingKind.needsText) {
                        SubLabel("Detail")
                        WizardTextField(pendingKind.textPlaceholder, pendingText, { if (it.length <= 80) pendingText = it })
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallCapsChip("Cancel", active = false, tone = palette.inkMute, modifier = Modifier.weight(1f)) { resetPending() }
                        SmallCapsChip(
                            "Add",
                            active = canAddPending,
                            tone = palette.accent,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (canAddPending) buildPending()?.let { modifiers = modifiers + it; resetPending() }
                        }
                    }
                }
            } else {
                SmallCapsChip("Add modifier", active = false, modifier = Modifier.fillMaxWidth()) { adding = true }
            }
        }

        PickerSection("Duration")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EffectDurationChoice.entries.forEach { d ->
                SmallCapsChip(d.label, active = duration == d) { duration = d }
            }
        }
        PickerGap(8.dp)
    }
}

// ── Ally's spell picker ──────────────────────────────────────────────────────

/**
 * Receive a spell another party member cast on this character (iOS
 * `AllyCastPicker`). Android has no party roster (parity #37), so the caster is
 * a free-text field rather than a roster of tap-to-fill chips, and the spell
 * list is the effect catalogue's target-picked buffs. Variant selection for
 * choice spells (Enhance Ability / Enlarge–Reduce) is omitted — Android's effect
 * templates carry no option shapes (documented gap). The ally holds any
 * concentration, so the applied effect is never a concentration owner here.
 */
@Composable
internal fun AllyCastPicker(
    onCancel: () -> Unit,
    onCommit: (ActiveEffect) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    val targetable = remember { SpellEffectCatalog.targetable }
    var caster by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }

    val previewEffect = selected?.let { SpellEffectCatalog.allyCast(it, caster) }
    val canCommit = selected != null && caster.trim().isNotEmpty()

    ActionPickerShell(
        kicker = "Effects",
        title = "Ally's spell",
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(
                label = "Apply effect",
                isDisabled = !canCommit,
                onClick = { previewEffect?.let(onCommit) },
            )
        },
    ) {
        previewEffect?.let { e ->
            EffectPreviewBanner(
                headline = e.name,
                meta = e.modifiers.joinToString(" · ") { it.impactLabel() },
                tone = palette.accent,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        PickerSection("Caster")
        WizardTextField("Party member's name", caster, { if (it.length <= 40) caster = it })

        PickerSection("Spell")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            targetable.forEach { (id, template) ->
                val active = selected == id
                PickerRow(borderTone = if (active) palette.accent else null, onClick = { selected = id }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                template.name,
                                fontFamily = Cormorant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = palette.ink,
                            )
                            val summary = template.modifiers.joinToString(" · ") { it.impactLabel() }
                            if (summary.isNotBlank()) {
                                Text(summary, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkSoft, maxLines = 2)
                            }
                        }
                        if (active) Text("✓", fontSize = 13.sp, color = palette.accent)
                    }
                }
            }
        }
        PickerGap(8.dp)
    }
}

// ── Shared atoms ─────────────────────────────────────────────────────────────

/** Tinted preview banner: a disc glyph over a headline + small-caps meta line. */
@Composable
private fun EffectPreviewBanner(headline: String, meta: String, tone: Color, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tone.copy(alpha = 0.067f))
            .border(1.dp, tone.copy(alpha = 0.4f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(palette.tileStrong).border(1.dp, tone.copy(alpha = 0.53f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("◆", fontSize = 11.sp, color = tone)
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(headline, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = palette.ink, maxLines = 1)
            if (meta.isNotBlank()) {
                Text(meta.uppercase(), fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.sp, color = palette.inkMute, maxLines = 1)
            }
        }
    }
}

/** Sub-section label inside the add-modifier card. */
@Composable
private fun SubLabel(text: String) {
    Text(
        text.uppercase(),
        fontFamily = Cinzel,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        color = MaterialTheme.natPalette.inkMute,
    )
}

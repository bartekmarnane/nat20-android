package au.com.evonet.nat20.ui.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import au.com.evonet.nat20.dnd5e.CastSpell
import au.com.evonet.nat20.dnd5e.DnD5eCatalog
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.EffectScope
import au.com.evonet.nat20.dnd5e.RaceTraits
import au.com.evonet.nat20.dnd5e.Spell
import au.com.evonet.nat20.dnd5e.SpellEffectCatalog
import au.com.evonet.nat20.dnd5e.SpellResolutionKind
import au.com.evonet.nat20.dnd5e.Spellcasting
import au.com.evonet.nat20.dnd5e.castableSpellIDs
import au.com.evonet.nat20.dnd5e.core.AttackOutcome
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e.core.SaveOutcome
import au.com.evonet.nat20.dnd5e.effectAttackBonus
import au.com.evonet.nat20.dnd5e.effectiveAbilityScores
import au.com.evonet.nat20.dnd5e.spellcastingClasses
import au.com.evonet.nat20.dnd5e.totalCurrentSlots
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The slice-B CAST flow (parity #19): a castable-spell chooser (the Act
 * sheet's "Cast a spell" tile) feeding the shared [CastTargetPicker] — target,
 * slot, attack-or-save branch, outcome, defeated — which both the Actions
 * layer and the Spells tab commit through. Port of the iOS spell-library →
 * `CastTargetPicker` flow, branched by the SRD resolution fields.
 */

// ── Spell chooser ──────────────────────────────────────────────────────────────

/** Lists everything the character can currently cast; picking hands off to the target picker. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CastSpellChooserPicker(
    payload: DnD5ePayload,
    onCancel: () -> Unit,
    onPick: (Spell) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    val library = remember { DnD5eCatalog.spellLibrary.associateBy { it.index } }
    val castable = remember(payload) { payload.castableSpellIDs.mapNotNull { library[it] } }
    val cantrips = castable.filter { it.level == 0 }.sortedBy { it.name }
    val leveled = castable.filter { it.level >= 1 }.sortedWith(compareBy({ it.level }, { it.name }))

    ActionPickerShell(kicker = "Magic", title = "Cast a spell", onCancel = onCancel) {
        if (cantrips.isEmpty() && leveled.isEmpty()) {
            PickerGap(32.dp)
            Text(
                "Nothing to cast yet.\nLearn or prepare spells on the Spells tab first.",
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                cantrips.forEach { s -> PickerChip(s.name, active = false) { onPick(s) } }
            }
        }
        if (leveled.isNotEmpty()) {
            PickerSection("Spells", top = if (cantrips.isEmpty()) 6.dp else 16.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                leveled.forEach { s ->
                    val hasSlot = (s.level..9).any { (payload.totalCurrentSlots[it] ?: 0) > 0 } || s.ritual
                    PickerRow(onClick = { onPick(s) }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                s.name,
                                fontFamily = Cormorant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = if (hasSlot) palette.ink else palette.inkMute,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "LVL ${s.level}" + (if (!hasSlot) " · NO SLOT" else ""),
                                fontFamily = Cinzel,
                                fontSize = 11.sp,
                                letterSpacing = 1.5.sp,
                                color = palette.inkMute,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Cast target picker ─────────────────────────────────────────────────────────

/**
 * The shared cast-resolution picker: slot (leveled spells, ritual toggle) →
 * target → attack-or-save branch per [Spell.resolutionKind] → damage (crit-
 * doubled, save-for-half rolls full) → defeated → a fully-resolved [CastSpell].
 * iOS picks the slot in the library; Android folds the slot chips in here so
 * the Spells-tab rows and the Act tile share one flow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CastTargetPicker(
    payload: DnD5ePayload,
    spell: Spell,
    onCancel: () -> Unit,
    onCommit: (CastSpell) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    val casterLevel = payload.level
    val prof = Proficiency.bonus(casterLevel)
    val castingEntry = payload.spellcastingClasses.firstOrNull()
    val castingAbility = castingEntry?.let { Spellcasting.spellcastingAbility(it) }
    val castingMod = castingAbility?.let { payload.effectiveAbilityScores.modifier(it) } ?: 0
    val spellSaveDC = 8 + prof + castingMod
    val effectAttack = payload.effectAttackBonus
    val attackBonuses = buildList {
        if (castingAbility != null) add(RollBonus(castingAbility.abbreviation, castingMod))
        add(RollBonus("Proficiency", prof))
        if (effectAttack != 0) add(RollBonus("Effects", effectAttack))
    }

    val template = SpellEffectCatalog.template(spell.index)
    val availableSlotLevels = (maxOf(1, spell.level)..9).filter { (payload.totalCurrentSlots[it] ?: 0) > 0 }

    var slotLevel by remember { mutableStateOf(if (spell.level == 0) 0 else availableSlotLevels.firstOrNull()) }
    var asRitual by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf("") }
    var applyToSelf by remember { mutableStateOf(false) }
    var attackOutcome by remember { mutableStateOf<AttackOutcome?>(null) }
    var attackD20 by remember { mutableStateOf<Int?>(null) }
    var attackTotal by remember { mutableStateOf<Int?>(null) }
    var saveOutcome by remember { mutableStateOf<SaveOutcome?>(null) }
    var damageTotal by remember { mutableStateOf<Int?>(null) }
    var manualDamageText by remember { mutableStateOf("") }
    var killed by remember { mutableStateOf(false) }

    val kind = spell.resolutionKind
    val isSaveForHalf = kind == SpellResolutionKind.SAVE && spell.saveHalves
    val effectiveSlot = if (asRitual) spell.level else (slotLevel ?: spell.level)
    val damageNotation = spell.damageDice(effectiveSlot, casterLevel)
    val damageSpec = damageNotation?.let { RollSpec.parse(it) }
    val crit = attackOutcome == AttackOutcome.CRITICAL
    val slotReady = spell.level == 0 || asRitual || (slotLevel != null && (payload.totalCurrentSlots[slotLevel!!] ?: 0) > 0)

    val damageStepActive = spell.dealsDamage && when (kind) {
        SpellResolutionKind.ATTACK -> attackOutcome == AttackOutcome.HIT || crit
        SpellResolutionKind.SAVE -> isSaveForHalf || saveOutcome == SaveOutcome.FAILED
        SpellResolutionKind.DAMAGE_ONLY -> true
        SpellResolutionKind.UTILITY -> false
    }
    val manualDamage = manualDamageText.toIntOrNull()
    val damageSettled = if (damageSpec != null) damageTotal != null else manualDamage != null
    val rolledDamage = if (damageSpec != null) damageTotal else manualDamage
    val killedStepActive = damageStepActive && damageSettled && !isSaveForHalf

    fun resetResolution() {
        attackOutcome = null
        attackD20 = null
        attackTotal = null
        saveOutcome = null
        damageTotal = null
        killed = false
    }

    val resolutionReady = when (kind) {
        SpellResolutionKind.ATTACK -> when (attackOutcome) {
            null -> false
            AttackOutcome.MISS -> true
            else -> !spell.dealsDamage || damageSettled
        }
        SpellResolutionKind.SAVE ->
            if (isSaveForHalf) {
                !spell.dealsDamage || damageSettled
            } else {
                when (saveOutcome) {
                    null -> false
                    SaveOutcome.PASSED -> true
                    SaveOutcome.FAILED -> !spell.dealsDamage || damageSettled
                }
            }
        SpellResolutionKind.DAMAGE_ONLY -> !spell.dealsDamage || damageSettled
        SpellResolutionKind.UTILITY -> true
    }
    val canCommit = slotReady && resolutionReady

    val commitLabel = when {
        !slotReady -> "No slot to cast with"
        kind == SpellResolutionKind.ATTACK -> when (attackOutcome) {
            null -> "Pick an outcome"
            AttackOutcome.MISS -> "Record miss"
            AttackOutcome.HIT -> if (killed && killedStepActive) "Record kill" else "Record hit"
            AttackOutcome.CRITICAL -> if (killed && killedStepActive) "Record critical kill" else "Record critical hit"
        }
        kind == SpellResolutionKind.SAVE && isSaveForHalf -> if (damageSettled) "Record cast" else "Roll damage"
        kind == SpellResolutionKind.SAVE -> when (saveOutcome) {
            null -> "Pick a save result"
            SaveOutcome.PASSED -> "Record save"
            SaveOutcome.FAILED -> if (killed && killedStepActive) "Record kill" else "Record save failed"
        }
        else -> if (killed && killedStepActive) "Record kill" else "Cast"
    }

    fun commit() {
        val damageOut = if (damageStepActive && damageSettled) rolledDamage else null
        onCommit(
            CastSpell(
                spellID = spell.index,
                spellName = spell.name,
                spellLevel = spell.level,
                slotLevel = effectiveSlot,
                asRitual = asRitual,
                target = target.trim().takeIf { it.isNotEmpty() },
                requiresConcentration = spell.concentration,
                applyToSelf = applyToSelf && template?.scope == EffectScope.TARGET_PICKED,
                attackD20 = attackD20.takeIf { kind == SpellResolutionKind.ATTACK },
                attackTotal = attackTotal.takeIf { kind == SpellResolutionKind.ATTACK },
                attackOutcome = attackOutcome.takeIf { kind == SpellResolutionKind.ATTACK },
                saveDC = spellSaveDC.takeIf { kind == SpellResolutionKind.SAVE },
                saveAbility = spell.dc?.dcType?.name?.takeIf { kind == SpellResolutionKind.SAVE && it.isNotEmpty() },
                saveOutcome = saveOutcome.takeIf { kind == SpellResolutionKind.SAVE && !isSaveForHalf },
                damageHalvedBySave = false, // save-for-half rolls full; each target halves externally
                damage = damageOut,
                damageType = spell.damage?.damageType?.name?.lowercase()?.takeIf { it.isNotEmpty() && damageOut != null },
                targetKilled = killed && killedStepActive,
            ),
        )
    }

    ActionPickerShell(
        kicker = "Magic",
        title = "Cast ${spell.name}",
        onCancel = onCancel,
        footer = {
            PrimaryActionButton(
                label = commitLabel,
                isDanger = attackOutcome == AttackOutcome.MISS || (saveOutcome == SaveOutcome.PASSED && !isSaveForHalf),
                isDisabled = !canCommit,
                onClick = ::commit,
            )
        },
    ) {
        PickerGap(6.dp)
        Text(
            "${spell.levelLabel} · ${spell.schoolName}" +
                (if (spell.concentration) " · Concentration" else "") +
                (if (spell.ritual) " · Ritual" else ""),
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp,
            color = palette.inkMute,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // ── Slot (leveled spells) ──
        if (spell.level >= 1) {
            PickerSection("Slot")
            if (availableSlotLevels.isEmpty() && !spell.ritual) {
                Text(
                    "No level-${spell.level}+ slots remaining — rest to recover.",
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    color = palette.danger,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    availableSlotLevels.forEach { lvl ->
                        val left = payload.totalCurrentSlots[lvl] ?: 0
                        PickerChip("L$lvl · $left left", active = !asRitual && slotLevel == lvl) {
                            slotLevel = lvl
                            asRitual = false
                            damageTotal = null // upcast changes the dice
                        }
                    }
                    if (spell.ritual) {
                        PickerChip("Ritual (no slot)", active = asRitual) {
                            asRitual = !asRitual
                            damageTotal = null
                        }
                    }
                }
            }
        }

        PickerSection("Target")
        WizardTextField("The ogre, the party, yourself…", target, { if (it.length <= 120) target = it })

        if (template?.scope == EffectScope.TARGET_PICKED) {
            PickerGap(10.dp)
            SmallCapsChip("Apply effect to self", active = applyToSelf) { applyToSelf = !applyToSelf }
            PickerHelpText("Tick when you're the target — the ${template.name} effect lands on your sheet.", Modifier.padding(top = 4.dp))
        }

        // ── Resolution branch ──
        when (kind) {
            SpellResolutionKind.ATTACK -> {
                PickerSection("Spell attack roll")
                key(effectiveSlot) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        RollResultView(
                            baseSpec = RollSpec.d(1, 20),
                            bonuses = attackBonuses,
                            luckyReroll = RaceTraits.hasHalflingLuck(payload.race),
                            onSettled = { result ->
                                attackTotal = result.total
                                attackD20 = result.naturalD20
                                attackOutcome = when {
                                    result.isNatural20 -> AttackOutcome.CRITICAL
                                    result.isNatural1 -> AttackOutcome.MISS
                                    else -> null
                                }
                                damageTotal = null
                            },
                            onReset = ::resetResolution,
                        )
                    }
                }
                if (attackTotal != null) {
                    PickerSection("Outcome")
                    OutcomeChipRow(
                        selection = attackOutcome,
                        options = listOf(
                            OutcomeOption<AttackOutcome?>(AttackOutcome.MISS, "Miss", palette.danger),
                            OutcomeOption<AttackOutcome?>(AttackOutcome.HIT, "Hit", palette.accent),
                            OutcomeOption<AttackOutcome?>(AttackOutcome.CRITICAL, "Critical", palette.accentGold),
                        ),
                        onSelect = { attackOutcome = it; damageTotal = null },
                    )
                }
            }

            SpellResolutionKind.SAVE -> {
                PickerSection("Save")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DcBadge("DC $spellSaveDC", palette.accent)
                    spell.dc?.dcType?.name?.takeIf { it.isNotEmpty() }?.let { DcBadge("$it save", palette.accentGold) }
                }
                if (isSaveForHalf) {
                    PickerHelpText(
                        "Each target rolls its own save — half damage on a success. Roll full damage below and halve per target.",
                        Modifier.padding(top = 8.dp),
                    )
                } else {
                    PickerGap(10.dp)
                    OutcomeChipRow(
                        selection = saveOutcome,
                        options = listOf(
                            OutcomeOption<SaveOutcome?>(SaveOutcome.FAILED, "Failed", palette.accentGold),
                            OutcomeOption<SaveOutcome?>(SaveOutcome.PASSED, "Saved", palette.danger),
                        ),
                        onSelect = { saveOutcome = it; damageTotal = null },
                    )
                }
            }

            SpellResolutionKind.DAMAGE_ONLY -> Unit

            SpellResolutionKind.UTILITY -> {
                PickerSection("Resolution")
                PickerHelpText("No roll needed — name the target and record the cast.")
            }
        }

        // ── Damage ──
        if (damageStepActive) {
            PickerSection(
                when {
                    crit -> "Damage · critical doubles the dice"
                    isSaveForHalf -> "Damage · full (halve on a save)"
                    else -> "Damage"
                },
            )
            if (damageSpec != null) {
                key(effectiveSlot, attackOutcome, saveOutcome) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        RollResultView(
                            baseSpec = if (crit) damageSpec.critDoubled() else damageSpec,
                            allowAdvantageToggle = false,
                            onSettled = { damageTotal = maxOf(0, it.total) },
                            onReset = { damageTotal = null },
                        )
                    }
                }
            } else {
                PickerHelpText(
                    "This spell's dice (${damageNotation ?: "special"}) need a manual roll — enter the total.",
                    Modifier.padding(bottom = 6.dp),
                )
                WizardTextField(
                    "Rolled damage",
                    manualDamageText,
                    { new -> manualDamageText = new.filter(Char::isDigit).take(3) },
                )
            }
        }

        if (killedStepActive) {
            PickerSection("Was the target defeated?")
            DefeatedToggle(killed) { killed = it }
        }

        PickerGap(8.dp)
    }
}

@Composable
private fun DcBadge(text: String, tone: androidx.compose.ui.graphics.Color) {
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

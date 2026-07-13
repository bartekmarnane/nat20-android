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
import au.com.evonet.nat20.dnd5e.AttackMath
import au.com.evonet.nat20.dnd5e.DamageRiders
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.ItemKind
import au.com.evonet.nat20.dnd5e.MakeAttack
import au.com.evonet.nat20.dnd5e.RaceTraits
import au.com.evonet.nat20.dnd5e.WeaponProperties
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.AttackOutcome
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e.equippedWeapons
import au.com.evonet.nat20.ui.codex.impactLabel
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The slice-B COMBAT pickers (parity #19): the full attack flow (replacing the
 * old `AttackSheet` dialog), the concentration review picker, and the active-
 * effects triage list. Ports of the iOS `AttackPicker` / `ConcentrationPicker`
 * / `EffectsListPicker`, carrying the rider sections Android's engine models
 * (Sneak Attack, Divine Smite, Savage Attacks).
 */

// ── Attack picker ──────────────────────────────────────────────────────────────

/**
 * Weapon attack in the picker-shell idiom: target → weapon chips (equipped +
 * "Other…" free-text) → inline d20 attack roll (nat 20/1 auto-suggests the
 * outcome) → outcome chips → weapon damage (crit-doubled) + rider sections →
 * defeated toggle → [MakeAttack]. Divine Smite drains its slot atomically with
 * the log; ranged weapons spend a piece of ammunition on hit or miss.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AttackPicker(
    payload: DnD5ePayload,
    onCancel: () -> Unit,
    onCommit: (MakeAttack) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    val weapons = payload.equippedWeapons

    var target by remember { mutableStateOf("") }
    var useCustom by remember { mutableStateOf(weapons.isEmpty()) }
    var customWeapon by remember { mutableStateOf("") }
    var customDamageText by remember { mutableStateOf("") }
    var weaponName by remember { mutableStateOf(weapons.firstOrNull()?.name) }

    val weaponItem = if (useCustom) null else weapons.firstOrNull { it.name == weaponName }
    val attack = remember(weaponName, useCustom) { weaponItem?.let { AttackMath.forWeapon(it, payload) } }
    val weapon = weaponItem?.weapon
    val weaponKey = if (useCustom) "custom" else weaponName ?: "none"

    // Roll + outcome state, all torn down when the weapon pick changes.
    var attackTotal by remember(weaponKey) { mutableStateOf<Int?>(null) }
    var attackD20 by remember(weaponKey) { mutableStateOf<Int?>(null) }
    var outcome by remember(weaponKey) { mutableStateOf<AttackOutcome?>(null) }
    var weaponDamage by remember(weaponKey) { mutableStateOf<Int?>(null) }
    var killed by remember(weaponKey) { mutableStateOf(false) }

    // Riders (A15/A17 + Savage Attacks): reset when the weapon or outcome changes.
    val rogueLevel = DamageRiders.classLevel(payload, "rogue")
    val paladinLevel = DamageRiders.classLevel(payload, "paladin")
    val sneakSpec = if (weapon != null && DamageRiders.sneakAttackEligible(weapon)) DamageRiders.sneakAttackSpec(rogueLevel) else null
    val availableSlots = (1..9).filter { (payload.currentSpellSlots[it] ?: 0) > 0 }
    val ammo = if (weapon?.kind == WeaponProperties.Kind.RANGED) {
        payload.inventory.firstOrNull { it.kind == ItemKind.AMMUNITION && it.quantity > 0 }
    } else {
        null
    }
    var sneakOn by remember(weaponKey, outcome) { mutableStateOf(false) }
    var sneakDamage by remember(weaponKey, outcome) { mutableStateOf<Int?>(null) }
    var smiteSlot by remember(weaponKey, outcome) { mutableStateOf<Int?>(null) }
    var smiteVsUndead by remember(weaponKey, outcome) { mutableStateOf(false) }
    var smiteDamage by remember(weaponKey, outcome) { mutableStateOf<Int?>(null) }
    var savageDamage by remember(weaponKey, outcome) { mutableStateOf<Int?>(null) }

    val crit = outcome == AttackOutcome.CRITICAL
    val landed = outcome == AttackOutcome.HIT || crit
    val customDamage = customDamageText.toIntOrNull()
    val savageEligible = crit && weapon != null && weapon.kind == WeaponProperties.Kind.MELEE &&
        RaceTraits.hasSavageAttacks(payload.race)
    val damageSettled = if (useCustom) customDamage != null else weaponDamage != null
    val ridersReady = (!sneakOn || sneakDamage != null) && (smiteSlot == null || smiteDamage != null)
    val committedName = if (useCustom) customWeapon.trim() else attack?.name
    val canCommit = !committedName.isNullOrEmpty() && attackTotal != null && outcome != null &&
        (outcome == AttackOutcome.MISS || (damageSettled && ridersReady))

    ActionPickerShell(kicker = "Roll", title = "Attack roll", onCancel = onCancel) {
        PickerSection("Target", top = 6.dp)
        WizardTextField("Goblin captain, the wight, …", target, { if (it.length <= 120) target = it })

        PickerSection(if (weapons.isEmpty()) "Weapon" else "Weapon (equipped)")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            weapons.forEach { item ->
                PickerChip(item.name, active = !useCustom && weaponName == item.name) {
                    useCustom = false
                    weaponName = item.name
                }
            }
            PickerChip("Other…", active = useCustom) { useCustom = true }
        }
        if (useCustom) {
            PickerGap(6.dp)
            WizardTextField("Improvised club, borrowed dagger…", customWeapon, { if (it.length <= 60) customWeapon = it })
        }
        if (ammo != null) {
            PickerHelpText("Ammunition: ${ammo.name} (${ammo.quantity}) — one is spent on hit or miss.", Modifier.padding(top = 6.dp))
        } else if (weapon?.kind == WeaponProperties.Kind.RANGED) {
            Text(
                "No ammunition — add some on the Items tab.",
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = palette.danger,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        PickerSection("Attack roll")
        if (attack == null && !useCustom) {
            Text(
                "No weapon equipped — ready one on the Items tab or pick Other.",
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 12.sp,
                color = palette.inkMute,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            )
        } else {
            key(weaponKey) {
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    RollResultView(
                        baseSpec = RollSpec.d(1, 20),
                        bonuses = attack?.attackBonuses ?: emptyList(),
                        luckyReroll = RaceTraits.hasHalflingLuck(payload.race),
                        onSettled = { result ->
                            attackTotal = result.total
                            attackD20 = result.naturalD20
                            outcome = when {
                                result.isNatural20 -> AttackOutcome.CRITICAL
                                result.isNatural1 -> AttackOutcome.MISS
                                else -> null // the player confirms hit/miss against the AC
                            }
                            weaponDamage = null
                        },
                        onReset = {
                            attackTotal = null
                            attackD20 = null
                            outcome = null
                            weaponDamage = null
                        },
                    )
                }
            }
        }

        if (attackTotal != null) {
            PickerSection("Outcome")
            OutcomeChipRow(
                selection = outcome,
                options = listOf(
                    OutcomeOption<AttackOutcome?>(AttackOutcome.MISS, "Miss", palette.danger),
                    OutcomeOption<AttackOutcome?>(AttackOutcome.HIT, "Hit", palette.accent),
                    OutcomeOption<AttackOutcome?>(AttackOutcome.CRITICAL, "Critical", palette.accentGold),
                ),
                onSelect = { outcome = it; weaponDamage = null },
            )
        }

        if (landed) {
            PickerSection(if (crit) "Damage · critical doubles the dice" else "Damage")
            if (useCustom) {
                WizardTextField(
                    "Rolled damage",
                    customDamageText,
                    { new -> customDamageText = new.filter(Char::isDigit).take(3) },
                )
            } else if (attack != null) {
                key(weaponKey, outcome) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        RollResultView(
                            baseSpec = if (crit) attack.damageSpec.critDoubled() else attack.damageSpec,
                            bonuses = attack.damageBonuses,
                            allowAdvantageToggle = false,
                            onSettled = { weaponDamage = maxOf(0, it.total) },
                            onReset = { weaponDamage = null },
                        )
                    }
                }
            }

            // ── Sneak Attack (Rogue, finesse/ranged weapon) ──
            if (sneakSpec != null) {
                PickerSection("Sneak Attack?")
                PickerHelpText(
                    "Once per turn, with advantage or an ally adjacent to the target.",
                    Modifier.padding(bottom = 6.dp),
                )
                SmallCapsChip("Sneak Attack ${sneakSpec.count}d6", active = sneakOn) {
                    sneakOn = !sneakOn
                    sneakDamage = null
                }
                if (sneakOn) {
                    key(weaponKey, outcome, "sneak") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            RollResultView(
                                baseSpec = if (crit) sneakSpec.critDoubled() else sneakSpec,
                                allowAdvantageToggle = false,
                                onSettled = { sneakDamage = maxOf(0, it.total) },
                                onReset = { sneakDamage = null },
                            )
                        }
                    }
                }
            }

            // ── Divine Smite (Paladin, expends a slot with the log) ──
            if (paladinLevel > 0 && availableSlots.isNotEmpty()) {
                PickerSection("Divine Smite?")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    SmallCapsChip("No smite", active = smiteSlot == null) {
                        smiteSlot = null
                        smiteDamage = null
                    }
                    availableSlots.forEach { lvl ->
                        val dice = DamageRiders.divineSmiteDice(lvl, smiteVsUndead)
                        PickerChip("L$lvl · ${dice}d8", active = smiteSlot == lvl) {
                            smiteSlot = if (smiteSlot == lvl) null else lvl
                            smiteDamage = null
                        }
                    }
                }
                smiteSlot?.let { lvl ->
                    PickerGap(6.dp)
                    SmallCapsChip("vs Undead / Fiend (+1d8)", active = smiteVsUndead) {
                        smiteVsUndead = !smiteVsUndead
                        smiteDamage = null
                    }
                    val smiteSpec = DamageRiders.divineSmiteSpec(lvl, smiteVsUndead)!!
                    key(weaponKey, outcome, lvl, smiteVsUndead) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            RollResultView(
                                baseSpec = if (crit) smiteSpec.critDoubled() else smiteSpec,
                                allowAdvantageToggle = false,
                                onSettled = { smiteDamage = maxOf(0, it.total) },
                                onReset = { smiteDamage = null },
                            )
                        }
                    }
                }
            }

            // ── Savage Attacks (Half-Orc, melee critical: one extra weapon die) ──
            if (savageEligible && weapon != null) {
                PickerSection("Savage Attacks")
                PickerHelpText("Melee critical — roll one extra weapon damage die.", Modifier.padding(bottom = 6.dp))
                val savageSpec = RollSpec.parse(weapon.damageDice)?.copy(modifier = 0)?.let { RollSpec.d(1, it.faces) }
                if (savageSpec != null) {
                    key(weaponKey, "savage") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            RollResultView(
                                baseSpec = savageSpec,
                                allowAdvantageToggle = false,
                                onSettled = { savageDamage = maxOf(0, it.total) },
                                onReset = { savageDamage = null },
                            )
                        }
                    }
                }
            }

            if (damageSettled) {
                PickerSection("Was the target defeated?")
                DefeatedToggle(killed) { killed = it }
            }
        }

        if (canCommit) {
            PickerGap(18.dp)
            val totalDamage = if (outcome == AttackOutcome.MISS) {
                null
            } else {
                listOfNotNull(
                    if (useCustom) customDamage else weaponDamage,
                    sneakDamage.takeIf { sneakOn },
                    smiteDamage.takeIf { smiteSlot != null },
                    savageDamage.takeIf { savageEligible },
                ).sum()
            }
            val riders = buildList {
                if (sneakOn && sneakSpec != null) add("Sneak Attack ${sneakSpec.count}d6")
                smiteSlot?.let { add("Divine Smite (${it.ordinalSlot()})") }
                if (savageEligible && savageDamage != null) add("Savage Attacks")
            }
            PrimaryActionButton(
                label = when {
                    outcome == AttackOutcome.MISS -> "Record miss"
                    crit -> if (killed) "Record critical kill" else "Record critical hit"
                    else -> if (killed) "Record kill" else "Record hit"
                },
                isDanger = outcome == AttackOutcome.MISS,
                onClick = {
                    onCommit(
                        MakeAttack(
                            weaponName = committedName!!,
                            attackTotal = attackTotal!!,
                            outcome = outcome!!,
                            damage = totalDamage,
                            damageType = attack?.damageType,
                            target = target.trim().ifEmpty { null },
                            riders = if (outcome == AttackOutcome.MISS) emptyList() else riders,
                            expendSlotLevel = smiteSlot.takeIf { outcome != AttackOutcome.MISS },
                            ammoItemId = ammo?.id, // a loosed arrow is spent on hit or miss
                            targetKilled = killed && landed,
                        ),
                    )
                },
            )
        }
    }
}

/** "1st" / "2nd" / "3rd" / "Nth" for a spell-slot level. */
internal fun Int.ordinalSlot(): String = when (this) {
    1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${this}th"
}

// ── Concentration picker ───────────────────────────────────────────────────────

/**
 * Reviews + adjusts concentration (iOS `ConcentrationPicker`). Casting handles
 * most starts automatically — this covers ending on demand, marking a non-spell
 * source, or corrections. Starting a new focus implicitly drops the old (the
 * layer commits an `ApplyEffect` with a concentration-owner effect).
 */
@Composable
internal fun ConcentrationPicker(
    current: String?,
    onCancel: () -> Unit,
    onStart: (String) -> Unit,
    onEnd: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var targetText by remember { mutableStateOf("") }
    val trimmed = targetText.trim()

    ActionPickerShell(
        kicker = "Effects",
        title = "Concentration",
        onCancel = onCancel,
        footer = {
            if (current != null) {
                PrimaryActionButton(label = "End concentration", isDanger = true, onClick = onEnd)
            } else {
                PrimaryActionButton(
                    label = "Start concentrating",
                    isDisabled = trimmed.isEmpty(),
                    onClick = { onStart(trimmed) },
                )
            }
        },
    ) {
        if (current != null) {
            PickerGap(6.dp)
            PickerRow(borderTone = palette.accent.copy(alpha = 0.4f)) {
                Column {
                    Text(
                        "CURRENTLY CONCENTRATING",
                        fontFamily = Cinzel,
                        fontSize = 11.sp,
                        letterSpacing = 2.5.sp,
                        color = palette.inkMute,
                    )
                    Text(
                        current,
                        fontFamily = Cormorant,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = palette.ink,
                    )
                }
            }

            PickerSection("Or switch to something else")
            PickerHelpText(
                "Casting a new concentration spell switches automatically — this is for non-spell sources.",
                Modifier.padding(bottom = 8.dp),
            )
            WizardTextField("Bless, Spike Growth, etc.", targetText, { if (it.length <= 60) targetText = it })
            PickerGap(10.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        1.2.dp,
                        (if (trimmed.isEmpty()) palette.inkMute else palette.accent).copy(alpha = 0.4f),
                        RoundedCornerShape(4.dp),
                    )
                    .clickable(enabled = trimmed.isNotEmpty()) { onStart(trimmed) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "SWITCH CONCENTRATION",
                    fontFamily = Cinzel,
                    fontSize = 11.sp,
                    letterSpacing = 2.5.sp,
                    color = if (trimmed.isEmpty()) palette.inkMute else palette.accent,
                )
            }
        } else {
            PickerGap(10.dp)
            Text(
                "Casting a concentration spell starts concentration automatically. Use this when a " +
                    "non-spell source — an item, an ally's spell on you, or a correction — is what's anchoring it.",
                fontFamily = EbGaramond,
                fontSize = 15.sp,
                color = palette.inkSoft,
                lineHeight = 21.sp,
            )
            PickerSection("Concentrating on")
            WizardTextField("Bless, Spike Growth, etc.", targetText, { if (it.length <= 60) targetText = it })
        }
    }
}

// ── Effects list picker ────────────────────────────────────────────────────────

/**
 * Full-screen triage list of every active effect: name + impact + duration
 * per row with a direct End affordance (iOS `EffectsListPicker`).
 */
@Composable
internal fun EffectsListPicker(
    effects: List<ActiveEffect>,
    onCancel: () -> Unit,
    onCancelEffect: (String) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    ActionPickerShell(kicker = "Effects", title = "Active effects", onCancel = onCancel) {
        if (effects.isEmpty()) {
            PickerGap(40.dp)
            Text(
                "No active effects.\nSpells, items, and class features that apply temporary modifiers show up here.",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = palette.inkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PickerGap(14.dp)
            PickerHelpText(
                "End an effect with its button. Ending a concentration effect here drops concentration too.",
                Modifier.padding(bottom = 8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                effects.forEach { effect ->
                    PickerRow {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        effect.name,
                                        fontFamily = Cormorant,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = palette.ink,
                                    )
                                    if (effect.concentrationOwner) {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .border(1.dp, palette.accent, CircleShape),
                                        )
                                    }
                                }
                                val impact = effect.modifiers.joinToString(" · ") { it.impactLabel() }
                                if (impact.isNotBlank()) {
                                    Text(
                                        impact,
                                        fontFamily = Cormorant,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 13.sp,
                                        color = palette.inkSoft,
                                        maxLines = 2,
                                    )
                                }
                                Text(
                                    effectDurationLabel(effect).uppercase(),
                                    fontFamily = Cinzel,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.5.sp,
                                    color = palette.inkMute,
                                )
                            }
                            Spacer(Modifier.size(8.dp))
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .border(1.dp, palette.danger, CircleShape)
                                    .clickable { onCancelEffect(effect.id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    "END",
                                    fontFamily = Cinzel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 2.sp,
                                    color = palette.danger,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun effectDurationLabel(effect: ActiveEffect): String = when (val d = effect.duration) {
    au.com.evonet.nat20.dnd5e.core.EffectDuration.UntilCancelled -> "Until cancelled"
    au.com.evonet.nat20.dnd5e.core.EffectDuration.Concentration -> "Concentration"
    is au.com.evonet.nat20.dnd5e.core.EffectDuration.UntilRest ->
        if (d.rest == au.com.evonet.nat20.dnd5e.core.RestKind.SHORT) "Until short rest" else "Until long rest"
    is au.com.evonet.nat20.dnd5e.core.EffectDuration.Rounds -> "${d.count} rounds"
}

package au.com.evonet.nat20.ui.codex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.AttackMath
import au.com.evonet.nat20.dnd5e.DamageRiders
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.ItemKind
import au.com.evonet.nat20.dnd5e.MakeAttack
import au.com.evonet.nat20.dnd5e.WeaponProperties
import au.com.evonet.nat20.dnd5e.equippedWeapons
import au.com.evonet.nat20.dnd5e.core.AttackOutcome
import au.com.evonet.nat20.dnd5e.core.RollResult
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.roll.RollResultView

/**
 * The attack flow (A15): pick an equipped weapon → roll the attack d20 (ability
 * + proficiency, advantage toggle) → confirm the outcome (auto-suggested from a
 * natural 20 / 1) → roll damage (weapon dice, doubled on a crit) → log a
 * [MakeAttack]. Uses the A16 `RollResultView` for both rolls.
 */
@Composable
internal fun AttackSheet(payload: DnD5ePayload, onApplyIntent: (CharacterIntent) -> Unit, onDismiss: () -> Unit) {
    val weapons = payload.equippedWeapons
    var weaponName by remember { mutableStateOf(weapons.firstOrNull()?.name) }
    val attack = remember(weaponName) { weapons.firstOrNull { it.name == weaponName }?.let { AttackMath.forWeapon(it, payload) } }

    var attackTotal by remember(weaponName) { mutableStateOf<Int?>(null) }
    var outcome by remember(weaponName) { mutableStateOf<AttackOutcome?>(null) }
    var weaponDamage by remember(weaponName) { mutableStateOf<Int?>(null) }
    var target by remember { mutableStateOf("") }

    // Class damage riders (A15/A17): Sneak Attack (Rogue) and Divine Smite (Paladin).
    val rogueLevel = DamageRiders.classLevel(payload, "rogue")
    val paladinLevel = DamageRiders.classLevel(payload, "paladin")
    val weapon = weapons.firstOrNull { it.name == weaponName }?.weapon
    val sneakSpec = remember(weaponName) {
        if (weapon != null && DamageRiders.sneakAttackEligible(weapon)) DamageRiders.sneakAttackSpec(rogueLevel) else null
    }
    val availableSlots = (1..9).filter { (payload.currentSpellSlots[it] ?: 0) > 0 }
    // Ranged weapons spend a piece of ammunition per attack (first matching stack).
    val ammo = if (weapon?.kind == WeaponProperties.Kind.RANGED) payload.inventory.firstOrNull { it.kind == ItemKind.AMMUNITION && it.quantity > 0 } else null
    var sneakOn by remember(weaponName, outcome) { mutableStateOf(false) }
    var sneakDamage by remember(weaponName, outcome) { mutableStateOf<Int?>(null) }
    var smiteSlot by remember(weaponName, outcome) { mutableStateOf<Int?>(null) }
    var smiteVsUndead by remember(weaponName, outcome) { mutableStateOf(false) }
    var smiteDamage by remember(weaponName, outcome) { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attack") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (attack == null) {
                    Text(
                        "No weapon equipped. Equip one on the Items tab first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    if (weapons.size > 1) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            weapons.forEach { w ->
                                FilterChip(selected = w.name == weaponName, onClick = { weaponName = w.name }, label = { Text(w.name) })
                            }
                        }
                    }

                    if (ammo != null) {
                        Text("Ammunition: ${ammo.name} (${ammo.quantity})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (weapon?.kind == WeaponProperties.Kind.RANGED) {
                        Text("No ammunition — add some on the Items tab.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }

                    // Step 1 — attack roll.
                    SectionLabel("Attack roll")
                    RollResultView(
                        baseSpec = RollSpec.d(1, 20),
                        bonuses = attack.attackBonuses,
                        luckyReroll = payload.race.contains("halfling", ignoreCase = true),
                        onSettled = { result ->
                            attackTotal = result.total
                            outcome = when {
                                result.isNatural20 -> AttackOutcome.CRITICAL
                                result.isNatural1 -> AttackOutcome.MISS
                                else -> null // player confirms hit/miss against the AC
                            }
                            weaponDamage = null
                        },
                    )

                    // Step 2 — outcome.
                    if (attackTotal != null) {
                        SectionLabel("Outcome")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutcomeChip("Miss", outcome == AttackOutcome.MISS) { outcome = AttackOutcome.MISS; weaponDamage = null }
                            OutcomeChip("Hit", outcome == AttackOutcome.HIT) { outcome = AttackOutcome.HIT; weaponDamage = null }
                            OutcomeChip("Critical", outcome == AttackOutcome.CRITICAL) { outcome = AttackOutcome.CRITICAL; weaponDamage = null }
                        }
                    }

                    // Step 3 — damage (hit/crit only): weapon dice + any class riders, each doubled on a crit.
                    if (outcome == AttackOutcome.HIT || outcome == AttackOutcome.CRITICAL) {
                        val crit = outcome == AttackOutcome.CRITICAL
                        SectionLabel("Weapon damage")
                        RollResultView(
                            baseSpec = if (crit) attack.damageSpec.critDoubled() else attack.damageSpec,
                            bonuses = attack.damageBonuses,
                            allowAdvantageToggle = false,
                            onSettled = { result: RollResult -> weaponDamage = maxOf(0, result.total) },
                        )

                        if (sneakSpec != null) {
                            FilterChip(
                                selected = sneakOn,
                                onClick = { sneakOn = !sneakOn; sneakDamage = null },
                                label = { Text("Sneak Attack ${sneakSpec.count}d6") },
                            )
                            if (sneakOn) {
                                RollResultView(
                                    baseSpec = if (crit) sneakSpec.critDoubled() else sneakSpec,
                                    allowAdvantageToggle = false,
                                    onSettled = { result: RollResult -> sneakDamage = maxOf(0, result.total) },
                                )
                            }
                        }

                        if (paladinLevel > 0 && availableSlots.isNotEmpty()) {
                            SectionLabel("Divine Smite")
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                availableSlots.forEach { lvl ->
                                    FilterChip(
                                        selected = smiteSlot == lvl,
                                        onClick = { smiteSlot = if (smiteSlot == lvl) null else lvl; smiteDamage = null },
                                        label = { Text("L$lvl slot") },
                                    )
                                }
                            }
                            smiteSlot?.let { lvl ->
                                FilterChip(
                                    selected = smiteVsUndead,
                                    onClick = { smiteVsUndead = !smiteVsUndead; smiteDamage = null },
                                    label = { Text("vs Undead / Fiend") },
                                )
                                val smiteSpec = DamageRiders.divineSmiteSpec(lvl, smiteVsUndead)!!
                                RollResultView(
                                    baseSpec = if (crit) smiteSpec.critDoubled() else smiteSpec,
                                    allowAdvantageToggle = false,
                                    onSettled = { result: RollResult -> smiteDamage = maxOf(0, result.total) },
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Target (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            val ridersReady = (!sneakOn || sneakDamage != null) && (smiteSlot == null || smiteDamage != null)
            val ready = attack != null && attackTotal != null && outcome != null &&
                (outcome == AttackOutcome.MISS || (weaponDamage != null && ridersReady))
            val totalDamage = if (outcome == AttackOutcome.MISS) null else listOfNotNull(
                weaponDamage,
                sneakDamage.takeIf { sneakOn },
                smiteDamage.takeIf { smiteSlot != null },
            ).sum()
            val riders = buildList {
                if (sneakOn && sneakSpec != null) add("Sneak Attack ${sneakSpec.count}d6")
                smiteSlot?.let { add("Divine Smite (${it.ordinalSlot()})") }
            }
            TextButton(
                enabled = ready,
                onClick = {
                    onApplyIntent(
                        MakeAttack(
                            weaponName = attack!!.name,
                            attackTotal = attackTotal!!,
                            outcome = outcome!!,
                            damage = totalDamage,
                            damageType = attack.damageType,
                            target = target.ifBlank { null },
                            riders = if (outcome == AttackOutcome.MISS) emptyList() else riders,
                            expendSlotLevel = smiteSlot.takeIf { outcome != AttackOutcome.MISS },
                            ammoItemId = ammo?.id, // a loosed arrow is spent on hit or miss
                        ),
                    )
                    onDismiss()
                },
            ) {
                Text(
                    when (outcome) {
                        AttackOutcome.MISS -> "Record miss"
                        AttackOutcome.CRITICAL -> "Record critical hit"
                        else -> "Record hit"
                    },
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
}

/** "1st" / "2nd" / "3rd" / "Nth" for a spell-slot level. */
private fun Int.ordinalSlot(): String = when (this) {
    1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${this}th"
}

@Composable
private fun OutcomeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.clickable { onClick() },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

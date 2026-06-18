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
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.MakeAttack
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
    var damage by remember(weaponName) { mutableStateOf<Int?>(null) }
    var target by remember { mutableStateOf("") }

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

                    // Step 1 — attack roll.
                    SectionLabel("Attack roll")
                    RollResultView(
                        baseSpec = RollSpec.d(1, 20),
                        bonuses = attack.attackBonuses,
                        onSettled = { result ->
                            attackTotal = result.total
                            outcome = when {
                                result.isNatural20 -> AttackOutcome.CRITICAL
                                result.isNatural1 -> AttackOutcome.MISS
                                else -> null // player confirms hit/miss against the AC
                            }
                            damage = null
                        },
                    )

                    // Step 2 — outcome.
                    if (attackTotal != null) {
                        SectionLabel("Outcome")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutcomeChip("Hit", outcome == AttackOutcome.HIT) { outcome = AttackOutcome.HIT; damage = null }
                            OutcomeChip("Miss", outcome == AttackOutcome.MISS) { outcome = AttackOutcome.MISS; damage = null }
                            OutcomeChip("Crit", outcome == AttackOutcome.CRITICAL) { outcome = AttackOutcome.CRITICAL; damage = null }
                        }
                    }

                    // Step 3 — damage (hit/crit only).
                    if (outcome == AttackOutcome.HIT || outcome == AttackOutcome.CRITICAL) {
                        SectionLabel("Damage")
                        val spec = if (outcome == AttackOutcome.CRITICAL) attack.damageSpec.critDoubled() else attack.damageSpec
                        RollResultView(
                            baseSpec = spec,
                            bonuses = attack.damageBonus?.let { listOf(it) } ?: emptyList(),
                            allowAdvantageToggle = false,
                            onSettled = { result: RollResult -> damage = maxOf(0, result.total) },
                        )
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
            val ready = attack != null && attackTotal != null && outcome != null &&
                (outcome == AttackOutcome.MISS || damage != null)
            TextButton(
                enabled = ready,
                onClick = {
                    onApplyIntent(
                        MakeAttack(
                            weaponName = attack!!.name,
                            attackTotal = attackTotal!!,
                            outcome = outcome!!,
                            damage = damage,
                            damageType = attack.damageType,
                            target = target.ifBlank { null },
                        ),
                    )
                    onDismiss()
                },
            ) { Text("Log attack") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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

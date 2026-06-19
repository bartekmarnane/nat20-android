package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.core.AttackOutcome
import au.com.evonet.nat20.dnd5e.core.RollResult
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e2024.AttackMath2024
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.dnd5e2024.MakeAttack2024
import au.com.evonet.nat20.dnd5e2024.weapons
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.roll.RollResultView

/**
 * The 2024 attack flow (A21): pick a weapon from the pack → roll the attack d20
 * (ability + proficiency + effect chips + the Great Weapon Master rider) →
 * confirm the outcome (auto-suggested from a natural 20 / 1) → roll damage
 * (doubled on a crit) → log a [MakeAttack2024]. The weapon's **mastery** is
 * surfaced as a reminder. Mirrors the 2014 `AttackSheet`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AttackSheet2024(payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit, onDismiss: () -> Unit) {
    val weapons = payload.weapons
    var weaponId by remember { mutableStateOf(weapons.firstOrNull()?.id) }
    val weapon = remember(weaponId) { weapons.firstOrNull { it.id == weaponId } }
    val attack = remember(weaponId) { weapon?.let { AttackMath2024.forWeapon(it, payload) } }

    var attackTotal by remember(weaponId) { mutableStateOf<Int?>(null) }
    var outcome by remember(weaponId) { mutableStateOf<AttackOutcome?>(null) }
    var damage by remember(weaponId) { mutableStateOf<Int?>(null) }
    var target by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attack") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (attack == null) {
                    Text("No weapon in your pack. Add one on the Items tab first.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    if (weapons.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            weapons.forEach { w -> FilterChip(selected = w.id == weaponId, onClick = { weaponId = w.id }, label = { Text(w.name) }) }
                        }
                    }
                    Text("Mastery: ${attack.mastery.displayName} — ${attack.mastery.summary}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

                    SectionLabel2024("Attack roll")
                    RollResultView(
                        baseSpec = RollSpec.d(1, 20),
                        bonuses = attack.attackBonuses,
                        onSettled = { result ->
                            attackTotal = result.total
                            outcome = when {
                                result.isNatural20 -> AttackOutcome.CRITICAL
                                result.isNatural1 -> AttackOutcome.MISS
                                else -> null
                            }
                            damage = null
                        },
                    )

                    if (attackTotal != null) {
                        SectionLabel2024("Outcome")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutcomeChip2024("Hit", outcome == AttackOutcome.HIT) { outcome = AttackOutcome.HIT; damage = null }
                            OutcomeChip2024("Miss", outcome == AttackOutcome.MISS) { outcome = AttackOutcome.MISS; damage = null }
                            OutcomeChip2024("Crit", outcome == AttackOutcome.CRITICAL) { outcome = AttackOutcome.CRITICAL; damage = null }
                        }
                    }

                    if (outcome == AttackOutcome.HIT || outcome == AttackOutcome.CRITICAL) {
                        SectionLabel2024("Damage")
                        val spec = if (outcome == AttackOutcome.CRITICAL) attack.damageSpec.critDoubled() else attack.damageSpec
                        RollResultView(
                            baseSpec = spec,
                            bonuses = attack.damageBonuses,
                            allowAdvantageToggle = false,
                            onSettled = { result: RollResult -> damage = maxOf(0, result.total) },
                        )
                    }

                    OutlinedTextField(target, { target = it }, label = { Text("Target (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            val ready = attack != null && attackTotal != null && outcome != null && (outcome == AttackOutcome.MISS || damage != null)
            TextButton(enabled = ready, onClick = {
                onApplyIntent(MakeAttack2024(attack!!.name, attackTotal!!, outcome!!, damage, attack.damageType, attack.mastery.name, target.ifBlank { null }))
                onDismiss()
            }) { Text("Log attack") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SectionLabel2024(text: String) =
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

@Composable
private fun OutcomeChip2024(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
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

package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.core.AttackOutcome
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.dnd5e2024.AttackMath2024
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.dnd5e2024.MakeAttack2024
import au.com.evonet.nat20.dnd5e2024.weapons
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.actions.ActionPickerShell
import au.com.evonet.nat20.ui.actions.OutcomeChipRow
import au.com.evonet.nat20.ui.actions.OutcomeOption
import au.com.evonet.nat20.ui.actions.PickerChip
import au.com.evonet.nat20.ui.actions.PickerGap
import au.com.evonet.nat20.ui.actions.PickerHelpText
import au.com.evonet.nat20.ui.actions.PickerSection
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.roll.RollResultView
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.PrimaryActionButton
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The 2024 attack flow (A21 / parity #33) on the shared [ActionPickerShell]
 * parchment chrome: target → weapon chips (each carrying its **mastery** label)
 * → inline d20 attack roll (a natural 20 / 1 auto-suggests the outcome) → outcome
 * chips → weapon damage (crit-doubles the dice) → [MakeAttack2024]. Mirrors the
 * 2014 [au.com.evonet.nat20.ui.actions.AttackPicker].
 *
 * 2024-specific: the chosen weapon's **mastery** property is surfaced as a
 * reminder (the 2024 combat lever that replaces the 2014 riders). Android's flow
 * is **richer than iOS 2024's** flat weapon list — iOS resolves the whole attack
 * in the engine, while Android keeps the staged roll flow (matching its 2014
 * picker). There are deliberately NO 2014 damage riders (Sneak Attack / Divine
 * Smite / Savage Attacks) here — 2024 uses masteries instead.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AttackSheet2024(payload: DnD5e2024Payload, onApplyIntent: (CharacterIntent) -> Unit, onDismiss: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val weapons = payload.weapons

    var target by remember { mutableStateOf("") }
    var weaponId by remember { mutableStateOf(weapons.firstOrNull()?.id) }
    val weapon = remember(weaponId) { weapons.firstOrNull { it.id == weaponId } }
    val attack = remember(weaponId) { weapon?.let { AttackMath2024.forWeapon(it, payload) } }
    val weaponKey = weaponId ?: "none"

    var attackTotal by remember(weaponKey) { mutableStateOf<Int?>(null) }
    var outcome by remember(weaponKey) { mutableStateOf<AttackOutcome?>(null) }
    var damage by remember(weaponKey) { mutableStateOf<Int?>(null) }

    val crit = outcome == AttackOutcome.CRITICAL
    val landed = outcome == AttackOutcome.HIT || crit
    val canCommit = attack != null && attackTotal != null && outcome != null &&
        (outcome == AttackOutcome.MISS || damage != null)

    ActionPickerShell(kicker = "Combat", title = "Attack", onCancel = onDismiss) {
        if (attack == null) {
            PickerGap(18.dp)
            Text(
                "No weapon in your pack.\nAdd one on the Items tab first.",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = palette.inkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            return@ActionPickerShell
        }

        PickerSection("Target", top = 6.dp)
        WizardTextField("Goblin captain, the wight, …", target, { if (it.length <= 120) target = it })

        PickerSection("Weapon")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            weapons.forEach { w ->
                PickerChip(w.name, active = w.id == weaponId) { weaponId = w.id }
            }
        }
        PickerHelpText(
            "Mastery: ${attack.mastery.displayName} — ${attack.mastery.summary}",
            Modifier.padding(top = 6.dp),
        )

        PickerSection("Attack roll")
        key(weaponKey) {
            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                RollResultView(
                    baseSpec = RollSpec.d(1, 20),
                    bonuses = attack.attackBonuses,
                    onSettled = { result ->
                        attackTotal = result.total
                        outcome = when {
                            result.isNatural20 -> AttackOutcome.CRITICAL
                            result.isNatural1 -> AttackOutcome.MISS
                            else -> null // the player confirms hit/miss against the AC
                        }
                        damage = null
                    },
                    onReset = {
                        attackTotal = null
                        outcome = null
                        damage = null
                    },
                )
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
                onSelect = { outcome = it; damage = null },
            )
        }

        if (landed) {
            PickerSection(if (crit) "Damage · critical doubles the dice" else "Damage")
            key(weaponKey, outcome) {
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    RollResultView(
                        baseSpec = if (crit) attack.damageSpec.critDoubled() else attack.damageSpec,
                        bonuses = attack.damageBonuses,
                        allowAdvantageToggle = false,
                        onSettled = { damage = maxOf(0, it.total) },
                        onReset = { damage = null },
                    )
                }
            }
        }

        if (canCommit) {
            PickerGap(18.dp)
            PrimaryActionButton(
                label = when {
                    outcome == AttackOutcome.MISS -> "Record miss"
                    crit -> "Record critical hit"
                    else -> "Record hit"
                },
                isDanger = outcome == AttackOutcome.MISS,
                onClick = {
                    onApplyIntent(
                        MakeAttack2024(
                            attack.name,
                            attackTotal!!,
                            outcome!!,
                            if (outcome == AttackOutcome.MISS) null else damage,
                            attack.damageType,
                            attack.mastery.name,
                            target.trim().ifEmpty { null },
                        ),
                    )
                    onDismiss()
                },
            )
        }
    }
}

package au.com.evonet.nat20.ui.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.dnd5e2024.AcquireItem2024
import au.com.evonet.nat20.dnd5e2024.AdjustCoin2024
import au.com.evonet.nat20.dnd5e2024.ApplyCondition2024
import au.com.evonet.nat20.dnd5e2024.CastSpell2024
import au.com.evonet.nat20.dnd5e2024.ChangeExhaustion2024
import au.com.evonet.nat20.dnd5e2024.ClearCondition2024
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Catalog
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.dnd5e2024.EndConcentration2024
import au.com.evonet.nat20.dnd5e2024.GainTempHp2024
import au.com.evonet.nat20.dnd5e2024.Heal2024
import au.com.evonet.nat20.dnd5e2024.InventoryItem2024
import au.com.evonet.nat20.dnd5e2024.LongRest2024
import au.com.evonet.nat20.dnd5e2024.SetInspiration2024
import au.com.evonet.nat20.dnd5e2024.TakeDamage2024
import au.com.evonet.nat20.dnd5e2024.temporarySaveBonus
import au.com.evonet.nat20.dnd5e2024.weapons
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.ui.sheet.AttackSheet2024
import au.com.evonet.nat20.ui.sheet.LevelUp2024Wizard

/** The pickers this 2024 layer can mount full-screen (parity #31). */
private enum class Picker2024 {
    TAKE_DAMAGE, HEAL, TEMP_HP,
    ADD_ITEM, ADD_GOLD, SPEND_GOLD,
    APPLY_CONDITION, CLEAR_CONDITION,
    CAST,
    ATTACK, LEVEL_UP,
}

/** A concentration save owed after damage landed on a concentrating caster. */
private data class PendingConcentrationSave2024(
    val spellName: String,
    val damageAmount: Int,
    val dc: Int,
    val conSaveBonus: Int,
)

/**
 * The 2024 actions layer (parity #31): hosts the grouped [Actions2024SheetView]
 * plus the follow-up pickers, firing the `*2024` intents through the same
 * [onApplyIntent] path the codex tabs use (campaign-logged upstream). It is the
 * simpler sibling of the 2014 [DnD5eActionsLayer] — five groups, no checks /
 * summons / effects / resource-pool tiles (see [Actions2024SheetView]).
 *
 * Attack and Level Up are hosted here directly (reusing the existing internal
 * `AttackSheet2024` / `LevelUp2024Wizard`), so the shell only needs to toggle
 * [showSheet]. Vitals/coin amounts reuse the shared [AmountPicker]; conditions
 * reuse [ConditionPicker]; the concentration follow-up reuses
 * [ConcentrationSavePicker].
 */
@Composable
internal fun DnD5e2024ActionsLayer(
    payload: DnD5e2024Payload,
    showSheet: Boolean,
    onDismissSheet: () -> Unit,
    onApplyIntent: (CharacterIntent) -> Unit,
) {
    var picker by remember { mutableStateOf<Picker2024?>(null) }
    var pendingConSave by remember { mutableStateOf<PendingConcentrationSave2024?>(null) }

    val castableCantrips = payload.cantripsKnown.mapNotNull { DnD5e2024Catalog.spell(it) }
    val castableLeveled = payload.preparedSpells.values.flatten().mapNotNull { DnD5e2024Catalog.spell(it) }
    val canAttack = payload.weapons.isNotEmpty()
    val canCast = castableCantrips.isNotEmpty() || castableLeveled.isNotEmpty()

    if (showSheet) {
        Actions2024SheetView(
            payload = payload,
            canAttack = canAttack,
            canCast = canCast,
            canLevelUp = payload.level < DnD5e2024Payload.MAX_LEVEL,
            hasActiveConditions = payload.activeConditions.isNotEmpty(),
            onDismiss = onDismissSheet,
            onSelect = { id ->
                onDismissSheet()
                when (id) {
                    Action2024Ids.ATTACK -> picker = Picker2024.ATTACK
                    Action2024Ids.CAST -> picker = Picker2024.CAST
                    Action2024Ids.DAMAGE -> picker = Picker2024.TAKE_DAMAGE
                    Action2024Ids.HEAL -> picker = Picker2024.HEAL
                    Action2024Ids.TEMP_HP -> picker = Picker2024.TEMP_HP
                    Action2024Ids.ADD_ITEM -> picker = Picker2024.ADD_ITEM
                    Action2024Ids.ADD_GOLD -> picker = Picker2024.ADD_GOLD
                    Action2024Ids.SPEND_GOLD -> picker = Picker2024.SPEND_GOLD
                    Action2024Ids.LEVEL_UP -> picker = Picker2024.LEVEL_UP
                    Action2024Ids.LONG_REST -> onApplyIntent(LongRest2024())
                    Action2024Ids.INSPIRATION -> onApplyIntent(SetInspiration2024(!payload.hasInspiration))
                    Action2024Ids.EXHAUSTION_UP -> onApplyIntent(ChangeExhaustion2024(1))
                    Action2024Ids.EXHAUSTION_DOWN -> onApplyIntent(ChangeExhaustion2024(-1))
                    Action2024Ids.APPLY_CONDITION -> picker = Picker2024.APPLY_CONDITION
                    Action2024Ids.CLEAR_CONDITION -> picker = Picker2024.CLEAR_CONDITION
                }
            },
        )
    }

    when (picker) {
        Picker2024.TAKE_DAMAGE -> AmountPicker(
            kicker = "Vitals",
            title = "Take damage",
            readoutLabel = "How much damage?",
            isDanger = true,
            onCancel = { picker = null },
            // iOS 2024 keeps the damage entry plain (no damage-type / resistance
            // step); temp HP still soaks before a concentration save is owed.
            onCommit = { amount, _, _ ->
                val concentratingOn = payload.concentratingOn
                val hpDamage = amount - minOf(payload.temporaryHp, amount)
                onApplyIntent(TakeDamage2024(amount))
                picker = null
                if (concentratingOn != null && hpDamage > 0) {
                    pendingConSave = PendingConcentrationSave2024(
                        spellName = concentratingOn,
                        damageAmount = hpDamage,
                        dc = maxOf(10, hpDamage / 2),
                        conSaveBonus = conSaveBonus2024(payload),
                    )
                }
            },
        )

        Picker2024.HEAL -> AmountPicker(
            kicker = "Vitals",
            title = "Heal",
            readoutLabel = "How much healing?",
            onCancel = { picker = null },
            onCommit = { amount, _, _ -> onApplyIntent(Heal2024(amount)); picker = null },
        )

        Picker2024.TEMP_HP -> AmountPicker(
            kicker = "Vitals",
            title = "Gain temp HP",
            onCancel = { picker = null },
            onCommit = { amount, _, _ -> onApplyIntent(GainTempHp2024(amount)); picker = null },
        )

        Picker2024.ADD_GOLD -> AmountPicker(
            kicker = "Inventory",
            title = "Add gold",
            readoutLabel = "How much gold?",
            unit = "gp",
            onCancel = { picker = null },
            onCommit = { amount, _, _ -> onApplyIntent(AdjustCoin2024(Coin.GP, amount)); picker = null },
        )

        Picker2024.SPEND_GOLD -> AmountPicker(
            kicker = "Inventory",
            title = "Spend gold",
            readoutLabel = "How much gold?",
            unit = "gp",
            isDanger = true,
            onCancel = { picker = null },
            onCommit = { amount, _, _ -> onApplyIntent(AdjustCoin2024(Coin.GP, -amount)); picker = null },
        )

        Picker2024.ADD_ITEM -> ItemEntry2024Picker(
            onCancel = { picker = null },
            onCommit = { name, quantity, kind ->
                onApplyIntent(
                    AcquireItem2024(InventoryItem2024(InventoryItem2024.newId(), name, quantity = quantity, kind = kind)),
                )
                picker = null
            },
        )

        Picker2024.APPLY_CONDITION -> ConditionPicker(
            mode = ConditionPickerMode.APPLY,
            onCancel = { picker = null },
            onCommit = { name ->
                // 2024 models exhaustion as a cumulative track, not a condition.
                if (name.equals("Exhaustion", ignoreCase = true)) {
                    onApplyIntent(ChangeExhaustion2024(1))
                } else {
                    onApplyIntent(ApplyCondition2024(name))
                }
                picker = null
            },
        )

        Picker2024.CLEAR_CONDITION -> {
            val exhaustionRow = payload.exhaustionLevel
                .takeIf { it > 0 }
                ?.let { "Exhaustion · level $it" }
            ConditionPicker(
                mode = ConditionPickerMode.CLEAR,
                activeConditions = payload.activeConditions + listOfNotNull(exhaustionRow),
                onCancel = { picker = null },
                onCommit = { name ->
                    if (name == exhaustionRow) {
                        onApplyIntent(ChangeExhaustion2024(-1))
                    } else {
                        onApplyIntent(ClearCondition2024(name))
                    }
                    picker = null
                },
            )
        }

        Picker2024.CAST -> Cast2024Picker(
            cantrips = castableCantrips,
            leveled = castableLeveled,
            remainingSlots = payload.currentSpellSlots,
            onCancel = { picker = null },
            onCast = { spell, slotLevel ->
                onApplyIntent(
                    CastSpell2024(
                        spellID = spell.id,
                        spellName = spell.name,
                        spellLevel = slotLevel,
                        slotLevel = slotLevel,
                        requiresConcentration = spell.concentration,
                        applyToSelf = true,
                    ),
                )
                picker = null
            },
        )

        Picker2024.ATTACK -> AttackSheet2024(
            payload = payload,
            onApplyIntent = { onApplyIntent(it); picker = null },
            onDismiss = { picker = null },
        )

        Picker2024.LEVEL_UP -> LevelUp2024Wizard(
            payload = payload,
            onApplyIntent = { onApplyIntent(it); picker = null },
            onDismiss = { picker = null },
        )

        null -> Unit
    }

    pendingConSave?.let { pending ->
        ConcentrationSavePicker(
            spellName = pending.spellName,
            damageAmount = pending.damageAmount,
            dc = pending.dc,
            conSaveBonus = pending.conSaveBonus,
            // 2024 heroic-species luck isn't modelled on the roll primitive yet.
            hasHalflingLuck = false,
            onCancel = { pendingConSave = null },
            onPassed = { pendingConSave = null },
            onFailed = {
                onApplyIntent(EndConcentration2024())
                pendingConSave = null
            },
        )
    }
}

/** Constitution saving-throw bonus for the concentration save (mirrors the Stats tab math). */
private fun conSaveBonus2024(payload: DnD5e2024Payload): Int {
    val conMod = AbilityScores.modifier(payload.effectiveScore(Ability.CONSTITUTION))
    val proficient = payload.classes.firstOrNull()?.classId
        ?.let { id -> DnD5e2024Catalog.classes.firstOrNull { it.id == id }?.savingThrows }
        .orEmpty()
        .contains(Ability.CONSTITUTION)
    val prof = if (proficient) Proficiency.bonus(payload.level) else 0
    return conMod + prof + payload.temporarySaveBonus(Ability.CONSTITUTION)
}

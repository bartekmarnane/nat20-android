package au.com.evonet.nat20.ui.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.pf2e.PFCoin
import au.com.evonet.nat20.pf2e.PathfinderPayload
import au.com.evonet.nat20.pf2e.PfAddInventoryItem
import au.com.evonet.nat20.pf2e.PfAddNote
import au.com.evonet.nat20.pf2e.PfAddWeapon
import au.com.evonet.nat20.pf2e.PfAdjustCoin
import au.com.evonet.nat20.pf2e.PfAdjustHeroPoints
import au.com.evonet.nat20.pf2e.PfApplyCondition
import au.com.evonet.nat20.pf2e.PfCastFocusSpell
import au.com.evonet.nat20.pf2e.PfCastSpell
import au.com.evonet.nat20.pf2e.PfClearCondition
import au.com.evonet.nat20.pf2e.PfDailyPreparations
import au.com.evonet.nat20.pf2e.PfEquipArmor
import au.com.evonet.nat20.pf2e.PfEquipShield
import au.com.evonet.nat20.pf2e.PfGainTempHp
import au.com.evonet.nat20.pf2e.PfHeal
import au.com.evonet.nat20.pf2e.PfLearnSpell
import au.com.evonet.nat20.pf2e.PfRaiseShield
import au.com.evonet.nat20.pf2e.PfRefocus
import au.com.evonet.nat20.pf2e.PfRemoveFeat
import au.com.evonet.nat20.pf2e.PfRemoveWeapon
import au.com.evonet.nat20.pf2e.PfSetArmorRunes
import au.com.evonet.nat20.pf2e.PfSetDying
import au.com.evonet.nat20.pf2e.PfSetWeaponRunes
import au.com.evonet.nat20.pf2e.PfSetWounded
import au.com.evonet.nat20.pf2e.PfStrike
import au.com.evonet.nat20.pf2e.PfTakeDamage
import au.com.evonet.nat20.pf2e.PfTakeFeat

/** The follow-up pickers this PF2e layer can mount full-screen (parity #35). */
private enum class PfPicker {
    TAKE_DAMAGE, HEAL, TEMP_HP,
    CAST_FOCUS, CAST, STRIKE,
    APPLY_CONDITION, CLEAR_CONDITION,
    ADD_ITEM, ADJUST_COIN,
    EQUIP_ARMOR, EQUIP_SHIELD, ADD_WEAPON, REMOVE_WEAPON,
    ETCH_WEAPON_RUNES, ETCH_ARMOR_RUNES, LEARN_SPELL,
    TAKE_FEAT, REMOVE_FEAT, ADD_NOTE, LEVEL_UP,
}

/**
 * The Pathfinder 2e actions layer (parity #35): hosts the grouped
 * [PathfinderActionsSheet] plus the follow-up pickers, firing the `Pf*` intents
 * through the same [onApplyIntent] path the sheet used to fire inline (campaign-
 * logged upstream). Every mutation the old interactive tabs offered has a home
 * here; simple counter tiles (dying/wounded/hero/refocus/daily-prep/raise-shield)
 * fire directly, the rest open a picker. Level Up opens the parchment
 * [PfLevelUpWizard] (parity #36).
 */
@Composable
internal fun PathfinderActionsLayer(
    payload: PathfinderPayload,
    showSheet: Boolean,
    onDismissSheet: () -> Unit,
    onApplyIntent: (CharacterIntent) -> Unit,
) {
    var picker by remember { mutableStateOf<PfPicker?>(null) }

    if (showSheet) {
        PathfinderActionsSheet(
            payload = payload,
            canLevelUp = payload.level < PathfinderPayload.MAX_LEVEL,
            onDismiss = onDismissSheet,
            onSelect = { id ->
                onDismissSheet()
                when (id) {
                    PfActionIds.DAMAGE -> picker = PfPicker.TAKE_DAMAGE
                    PfActionIds.HEAL -> picker = PfPicker.HEAL
                    PfActionIds.TEMP_HP -> picker = PfPicker.TEMP_HP
                    PfActionIds.DYING_UP -> onApplyIntent(PfSetDying(payload.dying + 1))
                    PfActionIds.DYING_DOWN -> onApplyIntent(PfSetDying(payload.dying - 1))
                    PfActionIds.WOUNDED_UP -> onApplyIntent(PfSetWounded(payload.wounded + 1))
                    PfActionIds.WOUNDED_DOWN -> onApplyIntent(PfSetWounded(payload.wounded - 1))
                    PfActionIds.HERO_SPEND -> onApplyIntent(PfAdjustHeroPoints(-1))
                    PfActionIds.HERO_GAIN -> onApplyIntent(PfAdjustHeroPoints(1))
                    PfActionIds.CAST_FOCUS -> picker = PfPicker.CAST_FOCUS
                    PfActionIds.REFOCUS -> onApplyIntent(PfRefocus())
                    PfActionIds.DAILY_PREP -> onApplyIntent(PfDailyPreparations())
                    PfActionIds.STRIKE -> picker = PfPicker.STRIKE
                    PfActionIds.CAST -> picker = PfPicker.CAST
                    PfActionIds.APPLY_CONDITION -> picker = PfPicker.APPLY_CONDITION
                    PfActionIds.CLEAR_CONDITION -> picker = PfPicker.CLEAR_CONDITION
                    PfActionIds.ADD_ITEM -> picker = PfPicker.ADD_ITEM
                    PfActionIds.ADJUST_COIN -> picker = PfPicker.ADJUST_COIN
                    PfActionIds.EQUIP_ARMOR -> picker = PfPicker.EQUIP_ARMOR
                    PfActionIds.EQUIP_SHIELD -> picker = PfPicker.EQUIP_SHIELD
                    PfActionIds.RAISE_SHIELD -> onApplyIntent(PfRaiseShield(!payload.shieldRaised))
                    PfActionIds.ADD_WEAPON -> picker = PfPicker.ADD_WEAPON
                    PfActionIds.REMOVE_WEAPON -> picker = PfPicker.REMOVE_WEAPON
                    PfActionIds.ETCH_WEAPON_RUNES -> picker = PfPicker.ETCH_WEAPON_RUNES
                    PfActionIds.ETCH_ARMOR_RUNES -> picker = PfPicker.ETCH_ARMOR_RUNES
                    PfActionIds.LEARN_SPELL -> picker = PfPicker.LEARN_SPELL
                    PfActionIds.TAKE_FEAT -> picker = PfPicker.TAKE_FEAT
                    PfActionIds.REMOVE_FEAT -> picker = PfPicker.REMOVE_FEAT
                    PfActionIds.ADD_NOTE -> picker = PfPicker.ADD_NOTE
                    PfActionIds.LEVEL_UP -> picker = PfPicker.LEVEL_UP
                }
            },
        )
    }

    when (picker) {
        PfPicker.TAKE_DAMAGE -> AmountPicker(
            kicker = "Vitals", title = "Take damage", readoutLabel = "How much damage?", isDanger = true,
            onCancel = { picker = null },
            onCommit = { amount, _, _ -> onApplyIntent(PfTakeDamage(amount)); picker = null },
        )
        PfPicker.HEAL -> AmountPicker(
            kicker = "Vitals", title = "Heal", readoutLabel = "How much healing?",
            onCancel = { picker = null },
            onCommit = { amount, _, _ -> onApplyIntent(PfHeal(amount)); picker = null },
        )
        PfPicker.TEMP_HP -> AmountPicker(
            kicker = "Vitals", title = "Gain temp HP",
            onCancel = { picker = null },
            onCommit = { amount, _, _ -> onApplyIntent(PfGainTempHp(amount)); picker = null },
        )
        PfPicker.CAST_FOCUS -> PfFocusPicker(payload, onCancel = { picker = null }, onCast = { name -> onApplyIntent(PfCastFocusSpell(name)); picker = null })
        PfPicker.CAST -> PfCastSpellPicker(payload, onCancel = { picker = null }, onCast = { s, spellRank, slotRank -> onApplyIntent(PfCastSpell(s.id, s.name, spellRank, slotRank)); picker = null })
        PfPicker.STRIKE -> PfStrikePicker(payload, onCancel = { picker = null }, onStrike = { weaponId, attackNumber, total, target -> onApplyIntent(PfStrike(weaponId, attackNumber, total, target)); picker = null })
        PfPicker.APPLY_CONDITION -> PfApplyConditionPicker(payload.conditions.map { it.id }.toSet(), onCancel = { picker = null }, onCommit = { id, value -> onApplyIntent(PfApplyCondition(id, value)); picker = null })
        PfPicker.CLEAR_CONDITION -> PfClearConditionPicker(payload, onCancel = { picker = null }, onCommit = { id -> onApplyIntent(PfClearCondition(id)); picker = null })
        PfPicker.ADD_ITEM -> PfItemPicker(onCancel = { picker = null }, onAdd = { item -> onApplyIntent(PfAddInventoryItem(item)); picker = null })
        PfPicker.ADJUST_COIN -> PfCoinPicker(payload, onCancel = { picker = null }, onApply = { coin: PFCoin, delta -> if (delta != 0) onApplyIntent(PfAdjustCoin(coin, delta)); picker = null })
        PfPicker.EQUIP_ARMOR -> PfArmorPicker(payload.armor, onCancel = { picker = null }, onPick = { id -> onApplyIntent(PfEquipArmor(id)); picker = null })
        PfPicker.EQUIP_SHIELD -> PfShieldPicker(payload.shield, onCancel = { picker = null }, onPick = { id -> onApplyIntent(PfEquipShield(id)); picker = null })
        PfPicker.ADD_WEAPON -> PfAddWeaponPicker(onCancel = { picker = null }, onPick = { id -> onApplyIntent(PfAddWeapon(id)); picker = null })
        PfPicker.REMOVE_WEAPON -> PfRemoveWeaponPicker(payload, onCancel = { picker = null }, onPick = { id -> onApplyIntent(PfRemoveWeapon(id)); picker = null })
        PfPicker.ETCH_WEAPON_RUNES -> PfWeaponRunePicker(payload, onCancel = { picker = null }, onApply = { id, pot, str -> onApplyIntent(PfSetWeaponRunes(id, pot, str)); picker = null })
        PfPicker.ETCH_ARMOR_RUNES -> PfArmorRunePicker(payload, onCancel = { picker = null }, onApply = { pot, res -> onApplyIntent(PfSetArmorRunes(pot, res)); picker = null })
        PfPicker.LEARN_SPELL -> PfLearnSpellPicker(payload, onCancel = { picker = null }, onLearn = { id, rank -> onApplyIntent(PfLearnSpell(id, rank)); picker = null })
        PfPicker.TAKE_FEAT -> PfTakeFeatPicker(payload, onCancel = { picker = null }, onTake = { id -> onApplyIntent(PfTakeFeat(id)); picker = null })
        PfPicker.REMOVE_FEAT -> PfRemoveFeatPicker(payload, onCancel = { picker = null }, onRemove = { id -> onApplyIntent(PfRemoveFeat(id)); picker = null })
        PfPicker.ADD_NOTE -> PfNotePicker(onCancel = { picker = null }, onAdd = { text -> onApplyIntent(PfAddNote(text)); picker = null })
        PfPicker.LEVEL_UP -> PfLevelUpWizard(payload, onApplyIntent = onApplyIntent, onDismiss = { picker = null })
        null -> Unit
    }
}

package au.com.evonet.nat20.ui.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import au.com.evonet.nat20.dnd5e.AddNote
import au.com.evonet.nat20.dnd5e.AdjustCoin
import au.com.evonet.nat20.dnd5e.AdjustExhaustion
import au.com.evonet.nat20.dnd5e.ApplyCondition
import au.com.evonet.nat20.dnd5e.ApplyEffect
import au.com.evonet.nat20.dnd5e.CancelEffect
import au.com.evonet.nat20.dnd5e.ClearCondition
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.EndConcentration
import au.com.evonet.nat20.dnd5e.ExpendSpellSlot
import au.com.evonet.nat20.dnd5e.GainTempHp
import au.com.evonet.nat20.dnd5e.Heal
import au.com.evonet.nat20.dnd5e.LongRest
import au.com.evonet.nat20.dnd5e.MarkDeathSave
import au.com.evonet.nat20.dnd5e.RaceTraits
import au.com.evonet.nat20.dnd5e.RollDeathSave
import au.com.evonet.nat20.dnd5e.SetInspiration
import au.com.evonet.nat20.dnd5e.ShortRest
import au.com.evonet.nat20.dnd5e.Spell
import au.com.evonet.nat20.dnd5e.SpendHitDie
import au.com.evonet.nat20.dnd5e.SpendResource
import au.com.evonet.nat20.dnd5e.TakeDamage
import au.com.evonet.nat20.dnd5e.UseClassFeature
import au.com.evonet.nat20.dnd5e.availableClassFeatures
import au.com.evonet.nat20.dnd5e.availableResourcePools
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.CastingProgression
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectSource
import au.com.evonet.nat20.dnd5e.core.FeatureRecovery
import au.com.evonet.nat20.dnd5e.core.RestKind
import au.com.evonet.nat20.dnd5e.effectiveDamageResistances
import au.com.evonet.nat20.dnd5e.maxPactSlots
import au.com.evonet.nat20.dnd5e.savingThrowBonus
import au.com.evonet.nat20.dnd5e.totalCurrentSlots
import au.com.evonet.nat20.dnd5e.totalMaxSlots
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.NoteKind

/**
 * Where the codex shell should send an action tile whose mechanic already
 * has an Android surface (parity #19: route to the EXISTING flow rather than
 * duplicating it — the remaining routes shrink as picker slices land).
 */
internal enum class ActionRoute {
    /** Level up → the existing `LevelUpWizard`. */
    LEVEL_UP,
    /** Long-rest spell prep → Spells tab prepare flow. */
    SPELLS_TAB,
    /** Acquire / use / drop item → editable Items tab. */
    ITEMS_TAB,
    /** Summons (familiar, conjures, steed, companions) → Lore tab Minions. */
    LORE_TAB,
}

/** The Slice-A/B pickers this layer can mount full-screen. */
private enum class PickerKind {
    TAKE_DAMAGE, HEAL, TEMP_HP,
    ADD_NOTE, END_ENCOUNTER, BEGIN_ENCOUNTER,
    INSPIRATION,
    APPLY_CONDITION, CLEAR_CONDITION,
    ADJUST_COIN,
    SHORT_REST, LONG_REST, SPEND_HIT_DIE,
    EXPEND_SLOT, SPEND_RESOURCE, USE_FEATURE,
    // Slice B — rolls, combat, cast, effects.
    SKILL_CHECK, ABILITY_CHECK, SAVING_THROW, DEATH_SAVE,
    ATTACK, CONCENTRATION, MANAGE_EFFECTS, CAST_CHOOSE,
}

/** A concentration save owed after damage landed on a concentrating caster. */
private data class PendingConcentrationSave(
    val spellName: String,
    val damageAmount: Int,
    val dc: Int,
    val conSaveBonus: Int,
)

/**
 * The 2014 actions layer (parity #19 slice A): hosts the grouped Actions
 * sheet plus the full-screen follow-up pickers, firing intents through the
 * same [onApplyIntent] path the codex tabs use (campaign-logged upstream).
 * Tiles whose mechanic lives elsewhere emit an [ActionRoute] for the shell.
 */
@Composable
internal fun DnD5eActionsLayer(
    payload: DnD5ePayload,
    showSheet: Boolean,
    onDismissSheet: () -> Unit,
    onApplyIntent: (CharacterIntent) -> Unit,
    onRoute: (ActionRoute) -> Unit,
) {
    var picker by remember { mutableStateOf<PickerKind?>(null) }
    var pendingConSave by remember { mutableStateOf<PendingConcentrationSave?>(null) }
    // The spell mid-cast (chooser → target picker hand-off, slice B).
    var castingSpell by remember { mutableStateOf<Spell?>(null) }

    if (showSheet) {
        ActionsSheetView(
            payload = payload,
            onDismiss = onDismissSheet,
            onSelect = { id ->
                onDismissSheet()
                when (id) {
                    // Slice-A pickers.
                    "damage" -> picker = PickerKind.TAKE_DAMAGE
                    "heal" -> picker = PickerKind.HEAL
                    "temp" -> picker = PickerKind.TEMP_HP
                    "note" -> picker = PickerKind.ADD_NOTE
                    "encounter" -> picker = PickerKind.END_ENCOUNTER
                    "begin-encounter" -> picker = PickerKind.BEGIN_ENCOUNTER
                    "insp" -> picker = PickerKind.INSPIRATION
                    "apply" -> picker = PickerKind.APPLY_CONDITION
                    "clear" -> picker = PickerKind.CLEAR_CONDITION
                    "coin" -> picker = PickerKind.ADJUST_COIN
                    "short-rest" -> picker = PickerKind.SHORT_REST
                    "long-rest" -> picker = PickerKind.LONG_REST
                    "hit-die" -> picker = PickerKind.SPEND_HIT_DIE
                    "slot" -> picker = PickerKind.EXPEND_SLOT
                    "spend-resource" -> picker = PickerKind.SPEND_RESOURCE
                    "feature" -> picker = PickerKind.USE_FEATURE

                    // Slice-B pickers.
                    "skill" -> picker = PickerKind.SKILL_CHECK
                    "check" -> picker = PickerKind.ABILITY_CHECK
                    "save" -> picker = PickerKind.SAVING_THROW
                    "death" -> picker = PickerKind.DEATH_SAVE
                    "attack" -> picker = PickerKind.ATTACK
                    "concentrate" -> picker = PickerKind.CONCENTRATION
                    "manage-effects" -> picker = PickerKind.MANAGE_EFFECTS
                    "cast" -> picker = PickerKind.CAST_CHOOSE

                    // Mechanics with existing surfaces — route, don't duplicate.
                    "level" -> onRoute(ActionRoute.LEVEL_UP)
                    "add-item", "use-item", "drop" -> onRoute(ActionRoute.ITEMS_TAB)
                    "summon-familiar", "conjure-animals", "tashas-summon",
                    "animate-dead", "find-steed", "bind-companion",
                    -> onRoute(ActionRoute.LORE_TAB)
                }
            },
        )
    }

    when (picker) {
        PickerKind.TAKE_DAMAGE -> AmountPicker(
            kicker = "Vitals",
            title = "Take damage",
            readoutLabel = "How much damage?",
            isDanger = true,
            showDamageTypes = true,
            notePrompt = "What happened?",
            onCancel = { picker = null },
            onCommit = { amount, type, note ->
                // Predict the HP actually lost the same way the intent will
                // (resistance halves, then temp HP soaks) so the RAW
                // concentration save can chain — the store's apply path
                // doesn't hand the event back to the UI.
                val wasConcentratingOn = payload.concentratingOn
                val typeKey = type?.trim()?.lowercase()
                val resisted = !typeKey.isNullOrEmpty() && typeKey in payload.effectiveDamageResistances
                val effective = if (resisted) amount / 2 else amount
                val hpDamage = effective - minOf(payload.temporaryHp, effective)
                onApplyIntent(TakeDamage(amount, type, note))
                picker = null
                if (wasConcentratingOn != null && hpDamage > 0) {
                    pendingConSave = PendingConcentrationSave(
                        spellName = wasConcentratingOn,
                        damageAmount = hpDamage,
                        dc = maxOf(10, hpDamage / 2),
                        conSaveBonus = payload.savingThrowBonus(Ability.CONSTITUTION),
                    )
                }
            },
        )

        PickerKind.HEAL -> AmountPicker(
            kicker = "Vitals",
            title = "Heal",
            readoutLabel = "How much healing?",
            notePrompt = "What healed you?",
            onCancel = { picker = null },
            onCommit = { amount, _, note ->
                onApplyIntent(Heal(amount, source = note))
                picker = null
            },
        )

        PickerKind.TEMP_HP -> AmountPicker(
            kicker = "Vitals",
            title = "Gain temp HP",
            onCancel = { picker = null },
            onCommit = { amount, _, _ ->
                onApplyIntent(GainTempHp(amount))
                picker = null
            },
        )

        PickerKind.ADD_NOTE -> NotePicker(
            onCancel = { picker = null },
            onCommit = { kind, text ->
                onApplyIntent(AddNote(text, kind))
                picker = null
            },
        )

        PickerKind.END_ENCOUNTER -> NotePicker(
            title = "End encounter",
            commitLabel = "Record",
            initialKind = NoteKind.COMBAT,
            initialText = "Encounter ended.",
            selectableKinds = emptyList(),
            onCancel = { picker = null },
            onCommit = { kind, text ->
                onApplyIntent(AddNote(text, kind))
                picker = null
            },
        )

        PickerKind.BEGIN_ENCOUNTER -> BeginEncounterPicker(
            payload = payload,
            onCancel = { picker = null },
            onCommit = { text, initiative ->
                // Matches iOS: the encounter opener is a combat journal note
                // with the rolled initiative folded in — no encounter intent.
                val roll = "Rolled $initiative for initiative."
                val composed = when {
                    text.isEmpty() -> roll
                    text.last() in ".!?" -> "$text $roll"
                    else -> "$text. $roll"
                }
                onApplyIntent(AddNote(composed, NoteKind.COMBAT))
                picker = null
            },
        )

        PickerKind.INSPIRATION -> {
            val has = payload.hasInspiration
            ConfirmActionPicker(
                kicker = "Resources",
                title = if (has) "Spend inspiration" else "Gain inspiration",
                description = if (has) {
                    "Cash in your inspiration for advantage on a roll, or grant it to a fellow adventurer."
                } else {
                    "Mark inspiration earned. It sits ready to spend on a roll of your choice."
                },
                primaryLabel = if (has) "Spend" else "Gain inspiration",
                onCancel = { picker = null },
                onCommit = {
                    onApplyIntent(SetInspiration(!has))
                    picker = null
                },
            )
        }

        PickerKind.APPLY_CONDITION -> ConditionPicker(
            mode = ConditionPickerMode.APPLY,
            onCancel = { picker = null },
            onCommit = { name ->
                // Exhaustion is a 0–6 track on Android, not a boolean condition.
                if (name.equals("Exhaustion", ignoreCase = true)) {
                    onApplyIntent(AdjustExhaustion(+1))
                } else {
                    onApplyIntent(ApplyCondition(name))
                }
                picker = null
            },
        )

        PickerKind.CLEAR_CONDITION -> {
            val exhaustionRow = payload.exhaustionLevel
                .takeIf { it > 0 }
                ?.let { "Exhaustion · level $it" }
            ConditionPicker(
                mode = ConditionPickerMode.CLEAR,
                activeConditions = payload.activeConditions + listOfNotNull(exhaustionRow),
                onCancel = { picker = null },
                onCommit = { name ->
                    if (name == exhaustionRow) {
                        onApplyIntent(AdjustExhaustion(-1))
                    } else {
                        onApplyIntent(ClearCondition(name))
                    }
                    picker = null
                },
            )
        }

        PickerKind.ADJUST_COIN -> CoinPicker(
            current = payload.coins,
            onCancel = { picker = null },
            onCommit = { coin, delta, source ->
                onApplyIntent(AdjustCoin(coin, delta, source))
                picker = null
            },
        )

        PickerKind.SHORT_REST -> RestPicker(
            kind = RestKind.SHORT,
            description = "Spend an hour binding wounds and catching breath. " +
                "You may spend hit dice individually after the rest.",
            secondaryEffects = shortRestBullets(payload),
            activeEffects = payload.activeEffects,
            onCancel = { picker = null },
            onCommit = { cancelledIds ->
                cancelledIds.forEach { onApplyIntent(CancelEffect(it)) }
                onApplyIntent(ShortRest())
                picker = null
            },
        )

        PickerKind.LONG_REST -> RestPicker(
            kind = RestKind.LONG,
            description = "Eight hours of sleep restores hit points, half hit dice, " +
                "and most expended resources.",
            secondaryEffects = longRestBullets(payload),
            activeEffects = payload.activeEffects,
            onCancel = { picker = null },
            onCommit = { cancelledIds ->
                cancelledIds.forEach { onApplyIntent(CancelEffect(it)) }
                onApplyIntent(LongRest())
                picker = null
            },
            onPrepareSpells = if (payload.classes.any { CastingProgression.usesPreparation(it.classId) }) {
                {
                    picker = null
                    onRoute(ActionRoute.SPELLS_TAB)
                }
            } else {
                null
            },
        )

        PickerKind.SPEND_HIT_DIE -> SpendHitDiePicker(
            payload = payload,
            onCancel = { picker = null },
            onCommit = { restored ->
                onApplyIntent(SpendHitDie(restored))
                picker = null
            },
        )

        PickerKind.EXPEND_SLOT -> ExpendSlotPicker(
            slots = payload.totalMaxSlots.keys.sorted().map { level ->
                SlotOption(
                    level = level,
                    current = payload.totalCurrentSlots[level] ?: 0,
                    max = payload.totalMaxSlots[level] ?: 0,
                )
            },
            onCancel = { picker = null },
            onCommit = { level, source ->
                onApplyIntent(ExpendSpellSlot(level, source))
                picker = null
            },
        )

        PickerKind.SPEND_RESOURCE -> SpendResourcePicker(
            pools = payload.availableResourcePools(),
            onCancel = { picker = null },
            onCommit = { poolId, amount, note ->
                onApplyIntent(SpendResource(poolId, amount, note))
                picker = null
            },
        )

        PickerKind.USE_FEATURE -> UseClassFeaturePicker(
            features = payload.availableClassFeatures(),
            onCancel = { picker = null },
            onUse = { feature ->
                onApplyIntent(
                    UseClassFeature(
                        featureID = feature.id,
                        featureName = feature.displayName,
                        recovery = feature.recovery,
                        usesRemaining = feature.current,
                    ),
                )
                picker = null
            },
        )

        // ── Slice B — rolls ──

        PickerKind.SKILL_CHECK -> SkillCheckPicker(
            payload = payload,
            onCancel = { picker = null },
            onCommit = { onApplyIntent(it); picker = null },
        )

        PickerKind.ABILITY_CHECK -> AbilityCheckPicker(
            payload = payload,
            onCancel = { picker = null },
            onCommit = { onApplyIntent(it); picker = null },
        )

        PickerKind.SAVING_THROW -> SavingThrowPicker(
            payload = payload,
            onCancel = { picker = null },
            onCommit = { onApplyIntent(it); picker = null },
        )

        PickerKind.DEATH_SAVE -> DeathSavePicker(
            deathSaves = payload.deathSaves,
            currentHp = payload.currentHp,
            hasHalflingLuck = RaceTraits.hasHalflingLuck(payload.race),
            onCancel = { picker = null },
            onRoll = { d20 ->
                onApplyIntent(RollDeathSave(d20))
                picker = null
            },
            onMark = { outcome ->
                onApplyIntent(MarkDeathSave(outcome))
                picker = null
            },
        )

        // ── Slice B — combat / magic / effects ──

        PickerKind.ATTACK -> AttackPicker(
            payload = payload,
            onCancel = { picker = null },
            onCommit = { onApplyIntent(it); picker = null },
        )

        PickerKind.CONCENTRATION -> ConcentrationPicker(
            current = payload.concentratingOn,
            onCancel = { picker = null },
            onStart = { name ->
                // Non-spell concentration source: an ad-hoc owner effect —
                // ApplyEffect swaps out any prior concentration (5e: one at a time).
                onApplyIntent(
                    ApplyEffect(
                        ActiveEffect(
                            id = ActiveEffect.newId(),
                            name = name,
                            source = EffectSource.Custom,
                            modifiers = emptyList(),
                            duration = EffectDuration.Concentration,
                            concentrationOwner = true,
                        ),
                    ),
                )
                picker = null
            },
            onEnd = {
                onApplyIntent(EndConcentration())
                picker = null
            },
        )

        PickerKind.MANAGE_EFFECTS -> EffectsListPicker(
            effects = payload.activeEffects,
            onCancel = { picker = null },
            // Stays open for triage — the row list live-updates as effects end.
            onCancelEffect = { onApplyIntent(CancelEffect(it)) },
        )

        PickerKind.CAST_CHOOSE -> CastSpellChooserPicker(
            payload = payload,
            onCancel = { picker = null },
            onPick = { spell ->
                picker = null
                castingSpell = spell
            },
        )

        null -> Unit
    }

    castingSpell?.let { spell ->
        CastTargetPicker(
            payload = payload,
            spell = spell,
            onCancel = { castingSpell = null },
            onCommit = { intent ->
                onApplyIntent(intent)
                castingSpell = null
            },
        )
    }

    pendingConSave?.let { pending ->
        ConcentrationSavePicker(
            spellName = pending.spellName,
            damageAmount = pending.damageAmount,
            dc = pending.dc,
            conSaveBonus = pending.conSaveBonus,
            hasHalflingLuck = RaceTraits.hasHalflingLuck(payload.race),
            onCancel = { pendingConSave = null },
            onPassed = { pendingConSave = null },
            onFailed = {
                onApplyIntent(EndConcentration())
                pendingConSave = null
            },
        )
    }
}

// ── Rest bullet copy ───────────────────────────────────────────────────────────

private fun shortRestBullets(payload: DnD5ePayload): List<String> = buildList {
    if (payload.maxPactSlots > 0) add("Restore pact magic slots")
    val features = payload.availableClassFeatures().filter { it.recovery == FeatureRecovery.SHORT_REST }
    if (features.isNotEmpty()) add("Refresh ${features.joinToString(", ") { it.displayName }}")
    val pools = payload.availableResourcePools().filter { it.recovery == FeatureRecovery.SHORT_REST }
    if (pools.isNotEmpty()) add("Refresh ${pools.joinToString(", ") { it.displayName }}")
    add("End effects that last until a short rest")
}

private fun longRestBullets(payload: DnD5ePayload): List<String> = buildList {
    add("Restore all hit points")
    if (payload.temporaryHp > 0) add("Clear temporary HP")
    if (payload.totalMaxSlots.isNotEmpty()) add("Restore all spell slots")
    if (payload.hitDiceSpent > 0) add("Regain ${maxOf(1, payload.hitDiceSpent / 2)} hit dice")
    if (payload.availableClassFeatures().isNotEmpty() || payload.availableResourcePools().isNotEmpty()) {
        add("Refresh class features and pools")
    }
    if (!payload.deathSaves.isCleared) add("Clear death saves")
    if (payload.exhaustionLevel > 0) add("Shed one level of exhaustion")
    if (payload.concentratingOn != null || payload.activeEffects.isNotEmpty()) {
        add("End concentration and timed effects")
    }
}

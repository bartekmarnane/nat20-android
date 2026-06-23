package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.AttackOutcome
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.RestKind
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.dnd5e.core.ClassResourceCatalog
import au.com.evonet.nat20.dnd5e.core.DeathSaveOutcome
import au.com.evonet.nat20.dnd5e.core.DeathSaves
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.dnd5e.core.Exhaustion
import au.com.evonet.nat20.dnd5e.core.FeatureRecovery
import au.com.evonet.nat20.dnd5e.core.FeatureUseEntry
import au.com.evonet.nat20.dnd5e.core.HpChoice
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterIntentError
import au.com.evonet.nat20.domain.IntentResult
import au.com.evonet.nat20.domain.NoteKind
import au.com.evonet.nat20.domain.Ruleset
import au.com.evonet.nat20.domain.Summon
import java.util.UUID

/**
 * The first handful of 5e intents (A3 slice): the HP/vitals trio plus
 * LevelUp. Each validates before producing its result; on a thrown error the
 * caller's character is untouched (Kotlin value semantics — we never mutate
 * the input, we return a derived copy).
 *
 * These are deliberately simpler than the shipped iOS versions: TakeDamage
 * omits resistance/immunity/feat interactions and LevelUp omits subclass /
 * ASI / spell-slot bookkeeping, since those payload fields aren't in the A3
 * slice yet. The shapes match so the logic can grow in place.
 */

/** Pulls the 5e payload off a character, or throws if it's the wrong type. */
private fun Character.dnd5ePayload(): DnD5ePayload =
    payload as? DnD5ePayload
        ?: throw CharacterIntentError.Invalid("Character is not a D&D 5e character")

/** Applies damage: temp HP absorbs first, then current HP, floored at 0. */
data class TakeDamage(
    val amount: Int,
    val damageType: String? = null,
    val note: String? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Damage must be positive")
        val payload = character.dnd5ePayload()

        // 5e: resistance halves the typed damage (rounded down) before temp HP soaks it.
        val typeKey = damageType?.trim()?.lowercase()
        val resisted = typeKey != null && typeKey.isNotEmpty() && typeKey in payload.effectiveDamageResistances
        val effectiveAmount = if (resisted) amount / 2 else amount

        val tempAbsorbed = minOf(payload.temporaryHp, effectiveAmount)
        val hpDamage = effectiveAmount - tempAbsorbed
        val newTempHp = payload.temporaryHp - tempAbsorbed
        var newHp = maxOf(0, payload.currentHp - hpDamage)

        // Relentless Endurance (Half-Orc): the first time per long rest the character is reduced to 0
        // but not killed outright (overkill ≥ HP max), drop to 1 HP instead.
        var relentless = false
        val overkill = hpDamage >= payload.currentHp + payload.effectiveMaxHp
        if (newHp == 0 && payload.currentHp > 0 && !payload.relentlessEnduranceUsed && !overkill && RaceTraits.hasRelentlessEndurance(payload.race)) {
            newHp = 1
            relentless = true
        }

        // Concentration: damage prompts a CON save (DC = max of 10 and half the damage taken).
        // The player resolves it manually (rolls the save, ends concentration on a fail).
        val concentrationDc = if (payload.concentratingOn != null && hpDamage > 0) maxOf(10, hpDamage / 2) else null

        val updated = payload.copy(currentHp = newHp, temporaryHp = newTempHp, relentlessEnduranceUsed = payload.relentlessEnduranceUsed || relentless)
        val event = DamageTakenEvent(
            amount = amount,
            damageType = damageType,
            note = if (relentless) (note?.let { "$it · " } ?: "") + "Relentless Endurance kept them at 1 HP" else note,
            previousHp = payload.currentHp,
            newHp = newHp,
            tempAbsorbed = tempAbsorbed,
            maxHp = payload.effectiveMaxHp,
            resistanceApplied = resisted,
            concentrationCheckDC = concentrationDc,
        )
        return IntentResult(character.copy(payload = updated), event)
    }
}

/** Restores HP, clamped to max. Healing off 0 HP ends the dying state (clears death saves, RAW). */
data class Heal(
    val amount: Int,
    val source: String? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Heal amount must be positive")
        val payload = character.dnd5ePayload()

        val newHp = minOf(payload.effectiveMaxHp, payload.currentHp + amount)
        val revived = payload.currentHp == 0 && newHp > 0 && !payload.deathSaves.isCleared
        val updated = payload.copy(
            currentHp = newHp,
            deathSaves = if (revived) DeathSaves.cleared else payload.deathSaves,
        )
        val event = HealedEvent(
            amount = amount,
            source = source,
            previousHp = payload.currentHp,
            newHp = newHp,
            maxHp = payload.effectiveMaxHp,
            revived = revived,
        )
        return IntentResult(character.copy(payload = updated), event)
    }
}

/**
 * Records one death-save outcome (success / failure / clear). Three successes
 * stabilizes; three failures is death. Rejected once already stable (success),
 * already dead (failure), or already cleared (clear). The nat-20 = revive and
 * nat-1 = two-failures rules are guided in the UI (no die roller yet — A16).
 */
data class MarkDeathSave(
    val outcome: DeathSaveOutcome,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        val previous = payload.deathSaves
        val next = when (outcome) {
            DeathSaveOutcome.SUCCESS -> {
                if (previous.successes >= 3) throw CharacterIntentError.Invalid("Already stable (3 successes)")
                previous.copy(successes = previous.successes + 1)
            }
            DeathSaveOutcome.FAILURE -> {
                if (previous.failures >= 3) throw CharacterIntentError.Invalid("Already dead (3 failures)")
                previous.copy(failures = previous.failures + 1)
            }
            DeathSaveOutcome.CLEAR -> {
                if (previous.isCleared) throw CharacterIntentError.Invalid("Death saves already cleared")
                DeathSaves.cleared
            }
        }
        val updated = payload.copy(deathSaves = next)
        return IntentResult(character.copy(payload = updated), DeathSaveMarkedEvent(outcome, previous, next))
    }
}

/** Grants temporary HP; 5e non-stacking — keep the higher value. */
data class GainTempHp(
    val amount: Int,
    val source: String? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Temp HP must be positive")
        val payload = character.dnd5ePayload()

        val newValue = maxOf(payload.temporaryHp, amount)
        val updated = payload.copy(temporaryHp = newValue)
        val event = TempHpGainedEvent(
            amount = amount,
            source = source,
            previous = payload.temporaryHp,
            newValue = newValue,
        )
        return IntentResult(character.copy(payload = updated), event)
    }
}

/** Records a free-text journal note. Doesn't change character state. */
data class AddNote(
    val text: String,
    val kind: NoteKind? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw CharacterIntentError.Invalid("Note must not be empty")
        return IntentResult(character, NoteEvent(text = trimmed, kind = kind))
    }
}

// ── Combat: attacks + initiative (A15) ────────────────────────────────────────

/**
 * Logs a weapon attack: the attack roll's total, the resolved [outcome], and
 * (on a hit/crit) the rolled damage. Records to the journal without mutating the
 * attacker — there's no target entity to damage yet (that's party/encounter
 * scope). The roll itself happens in the UI via the A16 die primitive.
 */
data class MakeAttack(
    val weaponName: String,
    val attackTotal: Int,
    val outcome: AttackOutcome,
    val damage: Int? = null,
    val damageType: String? = null,
    val target: String? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (weaponName.isBlank()) throw CharacterIntentError.Invalid("Attack needs a weapon")
        val event = AttackEvent(
            weaponName = weaponName,
            attackTotal = attackTotal,
            outcome = outcome,
            damage = damage?.takeIf { outcome != AttackOutcome.MISS },
            damageType = damageType,
            target = target?.trim()?.takeIf { it.isNotEmpty() },
        )
        return IntentResult(character, event)
    }
}

/** Sets (or, with null, clears) this encounter's initiative; the d20 is rolled in the UI. */
data class SetInitiative(
    val value: Int?,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        return IntentResult(
            character.copy(payload = payload.copy(initiative = value)),
            InitiativeEvent(value),
        )
    }
}

/** Applies a condition (standard or house-rule) by name; idempotent (case-insensitive). */
data class ApplyCondition(
    val name: String,
    val source: String? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw CharacterIntentError.Invalid("Condition name cannot be empty")
        val payload = character.dnd5ePayload()
        val alreadyApplied = payload.activeConditions.any { it.equals(trimmed, ignoreCase = true) }
        val updated = if (alreadyApplied) payload else payload.copy(activeConditions = payload.activeConditions + trimmed)
        return IntentResult(
            character.copy(payload = updated),
            ConditionAppliedEvent(trimmed, source, wasNew = !alreadyApplied),
        )
    }
}

/** Clears a condition by name (case-insensitive); a no-op if absent. */
data class ClearCondition(
    val name: String,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        val remaining = payload.activeConditions.filterNot { it.equals(name.trim(), ignoreCase = true) }
        val wasPresent = remaining.size != payload.activeConditions.size
        return IntentResult(
            character.copy(payload = payload.copy(activeConditions = remaining)),
            ConditionClearedEvent(name.trim(), wasPresent),
        )
    }
}

/** Raises or lowers the exhaustion track by [delta], clamped to 0–6 (level 6 = death). */
data class AdjustExhaustion(
    val delta: Int,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (delta == 0) throw CharacterIntentError.Invalid("Exhaustion change can't be zero")
        val payload = character.dnd5ePayload()
        val previous = payload.exhaustionLevel
        val next = Exhaustion.clamp(previous + delta)
        if (next == previous) throw CharacterIntentError.Invalid("Exhaustion already at ${if (delta > 0) "maximum" else "zero"}")
        return IntentResult(
            character.copy(payload = payload.copy(exhaustionLevel = next)),
            ExhaustionChangedEvent(previous, next),
        )
    }
}

/** Grants or spends Inspiration. No-op (still emits an event) if already in the target state. */
data class SetInspiration(
    val has: Boolean,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        val updated = payload.copy(hasInspiration = has)
        return IntentResult(character.copy(payload = updated), InspirationChangedEvent(has))
    }
}

// ── Inventory & coins (A10) ───────────────────────────────────────────────────

/**
 * Adds an item to the inventory. Non-equippable items (gear, potions, ammo,
 * tools, treasure) sharing a [InventoryItem.catalogueID] **stack** — their
 * quantities merge onto the existing line. Weapons, armor, and shields always
 * append a distinct line so each can be equipped independently.
 */
data class AcquireItem(
    val item: InventoryItem,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (item.quantity <= 0) throw CharacterIntentError.Invalid("Quantity must be positive")
        val payload = character.dnd5ePayload()

        val stackTarget = if (!item.kind.isEquippable && item.catalogueID != null) {
            payload.inventory.indexOfFirst {
                !it.kind.isEquippable && it.catalogueID == item.catalogueID
            }
        } else {
            -1
        }

        val updatedInventory = payload.inventory.toMutableList()
        if (stackTarget >= 0) {
            val existing = updatedInventory[stackTarget]
            updatedInventory[stackTarget] = existing.copy(quantity = existing.quantity + item.quantity)
        } else {
            updatedInventory.add(item)
        }

        val updated = payload.copy(inventory = updatedInventory)
        val event = ItemAcquiredEvent(itemName = item.name, quantity = item.quantity)
        return IntentResult(character.copy(payload = updated), event)
    }
}

/** Drops some or all of an item stack; removes the line when the stack empties. */
data class DropItem(
    val itemID: String,
    val quantity: Int = 1,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (quantity <= 0) throw CharacterIntentError.Invalid("Quantity must be positive")
        val payload = character.dnd5ePayload()

        val index = payload.inventory.indexOfFirst { it.id == itemID }
        if (index < 0) throw CharacterIntentError.Invalid("No item with id $itemID")
        val existing = payload.inventory[index]
        val dropped = minOf(quantity, existing.quantity)

        val updatedInventory = payload.inventory.toMutableList()
        if (dropped >= existing.quantity) {
            updatedInventory.removeAt(index)
        } else {
            updatedInventory[index] = existing.copy(quantity = existing.quantity - dropped)
        }

        val updated = payload.copy(inventory = updatedInventory)
        val event = ItemDroppedEvent(itemName = existing.name, quantity = dropped)
        return IntentResult(character.copy(payload = updated), event)
    }
}

/**
 * Consumes one of a usable item (potion, scroll, charged wondrous). Decrements
 * the stack, removing the line when it empties. [healingRolled] (rolled in the
 * picker for healing potions) restores HP, clamped to max.
 */
data class UseItem(
    val itemID: String,
    val healingRolled: Int? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()

        val index = payload.inventory.indexOfFirst { it.id == itemID }
        if (index < 0) throw CharacterIntentError.Invalid("No item with id $itemID")
        val existing = payload.inventory[index]

        val updatedInventory = payload.inventory.toMutableList()
        if (existing.quantity <= 1) {
            updatedInventory.removeAt(index)
        } else {
            updatedInventory[index] = existing.copy(quantity = existing.quantity - 1)
        }

        val newHp = if (healingRolled != null && healingRolled > 0) {
            minOf(payload.effectiveMaxHp, payload.currentHp + healingRolled)
        } else {
            payload.currentHp
        }

        // Catalogue auto-apply (A17): potions/oils land their effect on the user.
        val itemEffect = ItemEffectCatalog.effectFor(existing.catalogueID)
        val newEffects = if (itemEffect != null) payload.activeEffects + itemEffect else payload.activeEffects

        val updated = payload.copy(inventory = updatedInventory, currentHp = newHp, activeEffects = newEffects)
        val event = ItemUsedEvent(
            itemName = existing.name,
            healingRolled = healingRolled?.takeIf { it > 0 },
            previousHp = if (newHp != payload.currentHp) payload.currentHp else null,
            newHp = if (newHp != payload.currentHp) newHp else null,
        )
        return IntentResult(character.copy(payload = updated), event)
    }
}

/**
 * Adjusts a coin pouch by [delta] (positive = gain, negative = spend). Spending
 * more than the character holds is rejected; the pouch never goes negative.
 */
data class AdjustCoin(
    val coin: Coin,
    val delta: Int,
    val source: String? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (delta == 0) throw CharacterIntentError.Invalid("Coin adjustment must be non-zero")
        val payload = character.dnd5ePayload()

        val current = payload.coins[coin] ?: 0
        val newValue = current + delta
        if (newValue < 0) {
            throw CharacterIntentError.Invalid("Not enough ${coin.abbreviation}: have $current, spending ${-delta}")
        }

        val updatedCoins = payload.coins.toMutableMap()
        if (newValue == 0) updatedCoins.remove(coin) else updatedCoins[coin] = newValue

        val updated = payload.copy(coins = updatedCoins)
        val event = CoinAdjustedEvent(coin = coin, delta = delta, source = source)
        return IntentResult(character.copy(payload = updated), event)
    }
}

// ── Spellcasting (A10) ────────────────────────────────────────────────────────

/**
 * Casts a spell at [slotLevel] (0 for a cantrip). Consumes a slot unless it's a
 * cantrip, a ritual, or cast from a scroll. When casting at the warlock pact
 * level, the short-rest pact pool drains first (it's the cheaper resource); a
 * pure warlock has no regular slots there, so always lands on the pact branch.
 *
 * Concentration tracking and on-cast effects are deferred to active effects
 * (A17); attack/save resolution to the combat audit (A15).
 */
data class CastSpell(
    val spellID: String,
    val spellName: String,
    val spellLevel: Int,
    val slotLevel: Int,
    val asRitual: Boolean = false,
    val fromScroll: Boolean = false,
    val target: String? = null,
    /** Concentration spell — sets [DnD5ePayload.concentratingOn] and drops any prior concentration (A17). */
    val requiresConcentration: Boolean = false,
    /** For a target-picked buff (Bless, Mage Armor), whether the caster applies the effect to themselves. */
    val applyToSelf: Boolean = false,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (slotLevel < spellLevel) {
            throw CharacterIntentError.Invalid("Slot level cannot be lower than the spell level")
        }
        val payload = character.dnd5ePayload()

        val consumeSlot = slotLevel > 0 && !asRitual && !fromScroll
        var fromPact = false
        var updated = payload
        if (consumeSlot) {
            val canUsePact = slotLevel == payload.pactSlotLevel && payload.currentPactSlots > 0
            if (canUsePact) {
                fromPact = true
                updated = payload.copy(currentPactSlots = payload.currentPactSlots - 1)
            } else {
                val remaining = payload.currentSpellSlots[slotLevel] ?: 0
                if (remaining <= 0) {
                    throw CharacterIntentError.Invalid("No level $slotLevel spell slots remaining")
                }
                updated = payload.copy(currentSpellSlots = payload.currentSpellSlots.withSlot(slotLevel, remaining - 1))
            }
        }

        // Concentration: a new concentration spell drops every prior concentration-owning effect.
        val startsConcentration = requiresConcentration && !asRitual
        if (startsConcentration) {
            updated = updated.copy(
                concentratingOn = spellName,
                activeEffects = updated.activeEffects.filterNot { it.concentrationOwner },
            )
        }
        // Catalogue auto-apply: always-self / caster-rider land unconditionally;
        // target-picked buffs only when the caster opted to apply to themselves.
        SpellEffectCatalog.template(spellID)?.let { tmpl ->
            val applies = when (tmpl.scope) {
                EffectScope.ALWAYS_SELF, EffectScope.CASTER_RIDER -> true
                EffectScope.TARGET_PICKED -> applyToSelf
            }
            if (applies) updated = updated.copy(activeEffects = updated.activeEffects + tmpl.resolve(spellID))
        }

        val trimmedTarget = target?.trim()?.takeIf { it.isNotEmpty() }
        val event = CastSpellEvent(
            spellID = spellID,
            spellName = spellName,
            spellLevel = spellLevel,
            slotLevel = slotLevel,
            wasUpcast = slotLevel > spellLevel && spellLevel > 0,
            wasRitual = asRitual,
            fromScroll = fromScroll,
            fromPact = fromPact,
            target = trimmedTarget,
        )
        return IntentResult(character.copy(payload = updated), event)
    }
}

/** Adds a spell to a prepared caster's daily list (no-op if already prepared). */
data class PrepareSpell(
    val spellID: String,
    val spellName: String,
    val classID: String,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        val bucket = payload.preparedSpells[classID].orEmpty()
        val wasNew = spellID !in bucket
        val updated = if (wasNew) {
            payload.copy(preparedSpells = payload.preparedSpells + (classID to (bucket + spellID)))
        } else {
            payload
        }
        return IntentResult(
            character.copy(payload = updated),
            SpellPreparedEvent(spellID, spellName, classID, wasNew),
        )
    }
}

/** Removes a spell from a prepared caster's daily list. */
data class UnprepareSpell(
    val spellID: String,
    val spellName: String,
    val classID: String,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        val bucket = payload.preparedSpells[classID].orEmpty()
        val newBucket = bucket.filterNot { it == spellID }
        val wasRemoved = newBucket.size != bucket.size
        val newMap = if (newBucket.isEmpty()) {
            payload.preparedSpells - classID
        } else {
            payload.preparedSpells + (classID to newBucket)
        }
        return IntentResult(
            character.copy(payload = payload.copy(preparedSpells = newMap)),
            SpellUnpreparedEvent(spellID, spellName, classID, wasRemoved),
        )
    }
}

/** Spends a spell slot without casting a catalogued spell (Counterspell, font of magic, etc.). Pact-first. */
data class ExpendSpellSlot(
    val slotLevel: Int,
    val source: String? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (slotLevel !in 1..9) throw CharacterIntentError.Invalid("Spell slot level must be 1–9")
        val payload = character.dnd5ePayload()

        val fromPact = slotLevel == payload.pactSlotLevel && payload.currentPactSlots > 0
        val updated: DnD5ePayload
        val remaining: Int
        if (fromPact) {
            remaining = payload.currentPactSlots - 1
            updated = payload.copy(currentPactSlots = remaining)
        } else {
            val available = payload.currentSpellSlots[slotLevel] ?: 0
            if (available <= 0) throw CharacterIntentError.Invalid("No level $slotLevel spell slots remaining")
            remaining = available - 1
            updated = payload.copy(currentSpellSlots = payload.currentSpellSlots.withSlot(slotLevel, remaining))
        }
        val trimmed = source?.trim()?.takeIf { it.isNotEmpty() }
        return IntentResult(
            character.copy(payload = updated),
            SpellSlotExpendedEvent(slotLevel, fromPact, remaining, trimmed),
        )
    }
}

// ── Class resources (A10) ─────────────────────────────────────────────────────

/** Spends [amount] from a point pool (Ki, Sorcery Points, Lay on Hands). Rejected if it would overdraw. */
data class SpendResource(
    val poolID: String,
    val amount: Int,
    val note: String? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Spend amount must be positive")
        val payload = character.dnd5ePayload()
        val pool = ClassResourceCatalog.pool(poolID)
            ?: throw CharacterIntentError.Invalid("Unknown resource pool $poolID")
        val max = payload.maxResource(poolID)
            ?: throw CharacterIntentError.Invalid("No ${pool.displayName} pool on this character")
        val current = payload.currentResource(poolID) ?: max
        if (amount > current) {
            throw CharacterIntentError.Invalid("Not enough ${pool.displayName}: $current left, tried to spend $amount")
        }
        val remaining = current - amount
        val updated = payload.copy(resourcePools = payload.resourcePools + (poolID to remaining))
        val event = ResourceSpentEvent(poolID, pool.displayName, amount, remaining, max, note?.trim()?.takeIf { it.isNotEmpty() })
        return IntentResult(character.copy(payload = updated), event)
    }
}

/**
 * Uses a use-counter class feature (Rage, Action Surge, …). When [usesRemaining]
 * is non-null the counter decrements by one (rejected at 0); a null count is an
 * unlimited feature (Rage at L20) that fires without tracking. The attached
 * `ActiveEffect` (rage damage, etc.) lands with A17.
 */
data class UseClassFeature(
    val featureID: String,
    val featureName: String,
    val recovery: FeatureRecovery,
    val usesRemaining: Int? = null,
    val note: String? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        var remainingAfter: Int? = null
        var updated = payload
        if (usesRemaining != null) {
            if (usesRemaining <= 0) throw CharacterIntentError.Invalid("No $featureName uses remaining")
            val next = usesRemaining - 1
            remainingAfter = next
            updated = payload.copy(
                classFeatureUses = payload.classFeatureUses + (featureID to FeatureUseEntry(next, recovery)),
            )
        }
        // Catalogue auto-apply (A17): the feature's effect lands on the user. Rage's damage
        // bonus is swapped to the level-correct tier; concentration features replace any prior.
        ClassFeatureEffectCatalog.template(featureID)?.let { tmpl ->
            var effect = tmpl.resolve(featureID)
            if (featureID == "rage") {
                val barbLevel = updated.classes.firstOrNull { it.classId.lowercase() == "barbarian" }?.level ?: 1
                effect = ClassFeatureEffectCatalog.withRageDamage(effect, barbLevel)
            }
            if (effect.concentrationOwner) {
                updated = updated.copy(
                    concentratingOn = effect.name,
                    activeEffects = updated.activeEffects.filterNot { it.concentrationOwner },
                )
            }
            updated = updated.copy(activeEffects = updated.activeEffects + effect)
        }
        val event = ClassFeatureUsedEvent(featureID, featureName, recovery, remainingAfter, note?.trim()?.takeIf { it.isNotEmpty() })
        return IntentResult(character.copy(payload = updated), event)
    }
}

// ── Active effects (A17) ──────────────────────────────────────────────────────

/** Ends concentration, dropping every concentration-owning effect (5e: one at a time). */
class EndConcentration : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        val target = payload.concentratingOn
        val updated = payload.copy(
            concentratingOn = null,
            activeEffects = payload.activeEffects.filterNot { it.concentrationOwner },
        )
        return IntentResult(character.copy(payload = updated), ConcentrationEndedEvent(target))
    }

    override fun equals(other: Any?): Boolean = other is EndConcentration
    override fun hashCode(): Int = javaClass.hashCode()
}

/**
 * Resolves a concentration check from a rolled [d20] against [dc] (the damage
 * that triggered it sets DC = max(10, damage / 2)). Total = d20 + the CON
 * saving-throw bonus, computed here so the math stays in the pure domain. A
 * failed total **auto-ends concentration** (drops every concentration-owning
 * effect — the A17 "auto-break on damage"). Rejected if not concentrating.
 */
data class RollConcentrationSave(
    val d20: Int,
    val dc: Int,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (d20 !in 1..20) throw CharacterIntentError.Invalid("A d20 roll is 1–20")
        val payload = character.dnd5ePayload()
        val focus = payload.concentratingOn
            ?: throw CharacterIntentError.Invalid("Not concentrating on anything")
        val bonus = payload.savingThrowBonus(Ability.CONSTITUTION)
        val maintained = (d20 + bonus) >= dc
        val updated = if (maintained) payload else payload.copy(
            concentratingOn = null,
            activeEffects = payload.activeEffects.filterNot { it.concentrationOwner },
        )
        return IntentResult(
            character.copy(payload = updated),
            ConcentrationSaveRolledEvent(d20 = d20, bonus = bonus, dc = dc, focus = focus, maintained = maintained),
        )
    }
}

/** Cancels a single active effect by id; also drops concentration if no owner effects remain. */
data class CancelEffect(
    val effectId: String,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        val effect = payload.activeEffects.firstOrNull { it.id == effectId }
            ?: throw CharacterIntentError.Invalid("No active effect $effectId")
        val remaining = payload.activeEffects.filterNot { it.id == effectId }
        val stillConcentrating = remaining.any { it.concentrationOwner }
        val updated = payload.copy(
            activeEffects = remaining,
            concentratingOn = if (effect.concentrationOwner && !stillConcentrating) null else payload.concentratingOn,
        )
        return IntentResult(character.copy(payload = updated), EffectCancelledEvent(effect.name))
    }
}

/** Applies an ad-hoc effect (a DM buff/debuff); concentration owners replace any prior concentration. */
data class ApplyEffect(
    val effect: ActiveEffect,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        var updated = payload
        if (effect.concentrationOwner) {
            updated = updated.copy(
                concentratingOn = effect.name,
                activeEffects = updated.activeEffects.filterNot { it.concentrationOwner },
            )
        }
        updated = updated.copy(activeEffects = updated.activeEffects + effect)
        return IntentResult(character.copy(payload = updated), EffectAppliedEvent(effect.name))
    }
}

/**
 * Spends one hit die to heal during a short rest. [healingRolled] is the final
 * HP to restore (die roll + CON mod, computed in the picker — kept out of the
 * pure domain), clamped to max HP. Rejected if no hit dice remain.
 */
data class SpendHitDie(
    val healingRolled: Int,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (healingRolled < 0) throw CharacterIntentError.Invalid("Healing cannot be negative")
        val payload = character.dnd5ePayload()
        if (payload.currentHitDice <= 0) throw CharacterIntentError.Invalid("No hit dice remaining")

        val newHp = minOf(payload.effectiveMaxHp, payload.currentHp + healingRolled)
        val updated = payload.copy(hitDiceSpent = payload.hitDiceSpent + 1)
        val event = HitDieSpentEvent(
            healingRolled = healingRolled,
            previousHp = payload.currentHp,
            newHp = newHp,
            remaining = updated.currentHitDice,
        )
        return IntentResult(character.copy(payload = updated.copy(currentHp = newHp)), event)
    }
}

/**
 * Resolves a death save from a rolled d20, applying the RAW outcome atomically
 * (so the nat-1 "two failures" can't race two separate marks): 20 = revive at
 * 1 HP + clear; 1 = two failures; 10+ = one success; else one failure.
 */
data class RollDeathSave(
    val d20: Int,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (d20 !in 1..20) throw CharacterIntentError.Invalid("A d20 roll is 1–20")
        val payload = character.dnd5ePayload()
        val previous = payload.deathSaves

        var revivedHp: Int? = null
        val next: DeathSaves = when {
            d20 == 20 -> {
                revivedHp = if (payload.currentHp == 0) 1 else payload.currentHp
                DeathSaves.cleared
            }
            d20 == 1 -> DeathSaves.clamped(previous.successes, previous.failures + 2)
            d20 >= 10 -> previous.copy(successes = minOf(3, previous.successes + 1))
            else -> previous.copy(failures = minOf(3, previous.failures + 1))
        }

        val updated = payload.copy(
            deathSaves = next,
            currentHp = revivedHp ?: payload.currentHp,
        )
        return IntentResult(
            character.copy(payload = updated),
            DeathSaveRolledEvent(d20 = d20, previous = previous, newState = next, revivedAt = revivedHp),
        )
    }
}

/**
 * A short rest: restores warlock pact slots, short-rest class-feature counters
 * (Action Surge, Second Wind, Channel Divinity, Bardic Inspiration ≥ L5), and
 * short-rest point pools (Ki). Long-rest resources stay spent.
 */
class ShortRest : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        val maxPact = payload.maxPactSlots
        val restored = maxOf(0, maxPact - payload.currentPactSlots)
        // Drop short-rest feature counters (each carries the recovery it was used under).
        val keptFeatures = payload.classFeatureUses.filterValues { it.recovery != FeatureRecovery.SHORT_REST }
        // Reset short-rest point pools (absent key = full).
        val shortRestPoolIds = ClassResourceCatalog.allPools.filter { it.recovery == FeatureRecovery.SHORT_REST }.map { it.id }
        val keptPools = payload.resourcePools - shortRestPoolIds.toSet()
        // Drop effects that last "until a short rest".
        val keptEffects = payload.activeEffects.filterNot { it.duration == EffectDuration.UntilRest(RestKind.SHORT) }
        val updated = payload.copy(
            currentPactSlots = maxPact,
            classFeatureUses = keptFeatures,
            resourcePools = keptPools,
            activeEffects = keptEffects,
        )
        return IntentResult(character.copy(payload = updated), ShortRestEvent(restored))
    }

    override fun equals(other: Any?): Boolean = other is ShortRest
    override fun hashCode(): Int = javaClass.hashCode()
}

/** A long rest: back to full HP, temp HP cleared, all spell slots (regular + pact) restored. */
class LongRest : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        val hpRestored = payload.effectiveMaxHp - payload.currentHp
        val tempCleared = payload.temporaryHp
        val maxSlots = payload.maxSpellSlots
        val maxPact = payload.maxPactSlots
        val regularRestored = maxSlots.values.sum() - payload.currentSpellSlots.values.sum()
        val pactRestored = maxOf(0, maxPact - payload.currentPactSlots)
        // 5e: regain up to half your total hit dice (min 1) on a long rest.
        val hitDiceRegained = if (payload.hitDiceSpent == 0) 0 else maxOf(1, payload.hitDiceSpent / 2)
        val deathSavesCleared = !payload.deathSaves.isCleared
        val exhaustionAfter = Exhaustion.clamp(payload.exhaustionLevel - 1)
        val updated = payload.copy(
            currentHp = payload.effectiveMaxHp,
            temporaryHp = 0,
            currentSpellSlots = maxSlots,
            currentPactSlots = maxPact,
            hitDiceSpent = maxOf(0, payload.hitDiceSpent - hitDiceRegained),
            // Every class resource (pools + feature counters) refreshes on a long rest.
            resourcePools = emptyMap(),
            classFeatureUses = emptyMap(),
            deathSaves = DeathSaves.cleared,
            exhaustionLevel = exhaustionAfter,
            relentlessEnduranceUsed = false,
            // End concentration and clear timed/concentration effects (Until-Cancelled buffs persist).
            concentratingOn = null,
            activeEffects = payload.activeEffects.filterNot {
                it.concentrationOwner || it.duration is EffectDuration.Concentration ||
                    it.duration is EffectDuration.UntilRest || it.duration is EffectDuration.Rounds
            },
        )
        val event = LongRestEvent(
            hpRestored = maxOf(0, hpRestored),
            tempCleared = tempCleared,
            slotsRestored = maxOf(0, regularRestored + pactRestored),
            hitDiceRegained = hitDiceRegained,
            deathSavesCleared = deathSavesCleared,
            exhaustionRecovered = exhaustionAfter < payload.exhaustionLevel,
        )
        return IntentResult(character.copy(payload = updated), event)
    }

    override fun equals(other: Any?): Boolean = other is LongRest
    override fun hashCode(): Int = javaClass.hashCode()
}

/** Sets a level's remaining slot count, dropping the key when it reaches zero. */
private fun Map<Int, Int>.withSlot(level: Int, remaining: Int): Map<Int, Int> =
    if (remaining <= 0) this - level else this + (level to remaining)

/**
 * Advances a class by one level, adding HP. [isNewClass] inserts a fresh
 * class line for a multiclass; otherwise the named class must already exist.
 */
data class LevelUp(
    val classId: String,
    val isNewClass: Boolean = false,
    val hpChoice: HpChoice = HpChoice.Average,
    /** Subclass id chosen this level-up (only valid at the class's subclass level). */
    val subclass: String? = null,
    /** Ability Score Improvement applied this level-up — at most +2 total, capped at 20. (A half-feat's +1 rides here.) */
    val abilityIncreases: Map<Ability, Int> = emptyMap(),
    /** A feat taken in place of the ASI (A11); recorded on [DnD5ePayload.chosenFeats]. */
    val feat: String? = null,
    /** Display name for the journal (catalogue-resolved by the caller). */
    val className: String = "",
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val payload = character.dnd5ePayload()
        if (payload.level >= DnD5ePayload.MAX_LEVEL) {
            throw CharacterIntentError.Invalid("Already at maximum level")
        }
        if (classId.isEmpty()) {
            throw CharacterIntentError.Invalid("LevelUp requires a classId")
        }

        val existingIndex = payload.classes.indexOfFirst { it.classId == classId }
        if (isNewClass && existingIndex >= 0) {
            throw CharacterIntentError.Invalid(
                "Cannot add $classId — character already has levels in it",
            )
        }
        if (!isNewClass && existingIndex < 0) {
            throw CharacterIntentError.Invalid(
                "No existing levels in $classId — pass isNewClass = true to multiclass into it",
            )
        }

        val hitDie = DnD5eClasses.hitDie(classId)
        if (hpChoice is HpChoice.Rolled && hpChoice.value !in 1..hitDie) {
            throw CharacterIntentError.Invalid(
                "Rolled HP ${hpChoice.value} outside 1..$hitDie for a d$hitDie",
            )
        }
        if (abilityIncreases.values.sum() > 2 || abilityIncreases.values.any { it < 0 }) {
            throw CharacterIntentError.Invalid("An Ability Score Improvement grants at most +2 total")
        }
        if (feat != null && Feats.feat(feat) == null) {
            throw CharacterIntentError.Invalid("Unknown feat $feat")
        }

        val previousLevel = payload.level
        val updatedClasses = payload.classes.toMutableList()
        if (existingIndex >= 0) {
            val entry = updatedClasses[existingIndex]
            updatedClasses[existingIndex] = entry.copy(
                level = entry.level + 1,
                subclass = subclass ?: entry.subclass,
            )
        } else {
            updatedClasses.add(ClassEntry(classId = classId, level = 1, subclass = subclass))
        }
        val classLevelAfter = updatedClasses.first { it.classId == classId }.level

        // Apply the ASI, capping each score at 20.
        var newScores = payload.abilityScores
        for ((ability, increase) in abilityIncreases) {
            newScores = newScores.with(ability, minOf(20, newScores.score(ability) + increase))
        }

        val conMod = AbilityScores.modifier(newScores.constitution)
        val hpGained = LevelUpMath.levelUpHp(hpChoice, hitDie, conMod)

        // Grant any newly-unlocked spell slots while preserving spent ones.
        val leveled = payload.copy(classes = updatedClasses, abilityScores = newScores)
        val oldMaxSlots = payload.maxSpellSlots
        val newCurrentSlots = leveled.maxSpellSlots.mapValues { (level, max) ->
            val gained = maxOf(0, max - (oldMaxSlots[level] ?: 0))
            (payload.currentSpellSlots[level] ?: 0) + gained
        }
        val newPactSlots = payload.currentPactSlots + maxOf(0, leveled.maxPactSlots - payload.maxPactSlots)

        // Stored maxHp is the *base*; a per-level rider (an already-held or newly-taken
        // Tough) tops a full character up via [bonusMaxHpPerLevel]. A feat taken *this*
        // level boosts only the max (through effectiveMaxHp), not the rolled current — RAW.
        val newFeats = if (feat != null && feat !in payload.chosenFeats) payload.chosenFeats + feat else payload.chosenFeats
        val updated = leveled.copy(
            maxHp = payload.maxHp + hpGained,
            currentHp = payload.currentHp + hpGained + payload.bonusMaxHpPerLevel,
            currentSpellSlots = newCurrentSlots,
            currentPactSlots = newPactSlots,
            chosenFeats = newFeats,
        )
        val event = LeveledUpEvent(
            classId = classId,
            className = className,
            isNewClass = isNewClass,
            previousLevel = previousLevel,
            newLevel = updated.level,
            classLevelAfter = classLevelAfter,
            hpChoice = hpChoice,
            hpGained = hpGained,
            subclass = subclass,
            abilityIncreases = abilityIncreases,
            feat = feat,
        )
        return IntentResult(character.copy(payload = updated), event)
    }
}

// ── Familiars + summoned creatures (A18) ────────────────────────────────────────

/**
 * Spawns a [Summon] (familiar, mount, companion, or summon group). The summon is
 * built in the UI from [CreatureCatalog] (fresh ids + spawn timestamp keep the
 * intent pure); this just attaches it to the cross-ruleset `Character.summons`.
 */
data class SummonCreature(val summon: Summon) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (summon.creatures.isEmpty()) throw CharacterIntentError.Invalid("A summon needs at least one creature")
        val updated = character.copy(summons = character.summons + summon)
        return IntentResult(updated, CreatureSummonedEvent(summon.label, summon.creatures.size))
    }
}

/** Dismisses a summon group, detaching its creatures. */
data class DismissSummon(val summonId: UUID) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val summon = character.summons.firstOrNull { it.id == summonId }
            ?: throw CharacterIntentError.Invalid("No summon with id $summonId")
        val updated = character.copy(summons = character.summons.filterNot { it.id == summonId })
        return IntentResult(updated, SummonDismissedEvent(summon.label))
    }
}

/** Sets a single creature's current HP (statblock-card damage/heal), clamped to 0..maxHp. */
data class SetCreatureHp(val summonId: UUID, val creatureId: UUID, val newHp: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val summon = character.summons.firstOrNull { it.id == summonId }
            ?: throw CharacterIntentError.Invalid("No summon with id $summonId")
        val creature = summon.creatures.firstOrNull { it.id == creatureId }
            ?: throw CharacterIntentError.Invalid("No creature with id $creatureId")
        val clamped = newHp.coerceIn(0, creature.maxHp)
        val updatedSummons = character.summons.map { s ->
            if (s.id != summonId) s
            else s.copy(creatures = s.creatures.map { c -> if (c.id == creatureId) c.copy(currentHp = clamped) else c })
        }
        return IntentResult(character.copy(summons = updatedSummons), CreatureHpEvent(creature.name, creature.currentHp, clamped, creature.maxHp))
    }
}

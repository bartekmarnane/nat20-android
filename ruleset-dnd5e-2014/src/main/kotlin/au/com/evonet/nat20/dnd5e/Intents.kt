package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.dnd5e.core.ClassResourceCatalog
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
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

        val tempAbsorbed = minOf(payload.temporaryHp, amount)
        val hpDamage = amount - tempAbsorbed
        val newTempHp = payload.temporaryHp - tempAbsorbed
        val newHp = maxOf(0, payload.currentHp - hpDamage)

        val updated = payload.copy(currentHp = newHp, temporaryHp = newTempHp)
        val event = DamageTakenEvent(
            amount = amount,
            damageType = damageType,
            note = note,
            previousHp = payload.currentHp,
            newHp = newHp,
            tempAbsorbed = tempAbsorbed,
            maxHp = payload.maxHp,
        )
        return IntentResult(character.copy(payload = updated), event)
    }
}

/** Restores HP, clamped to max. */
data class Heal(
    val amount: Int,
    val source: String? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Heal amount must be positive")
        val payload = character.dnd5ePayload()

        val newHp = minOf(payload.maxHp, payload.currentHp + amount)
        val updated = payload.copy(currentHp = newHp)
        val event = HealedEvent(
            amount = amount,
            source = source,
            previousHp = payload.currentHp,
            newHp = newHp,
            maxHp = payload.maxHp,
        )
        return IntentResult(character.copy(payload = updated), event)
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
            minOf(payload.maxHp, payload.currentHp + healingRolled)
        } else {
            payload.currentHp
        }

        val updated = payload.copy(inventory = updatedInventory, currentHp = newHp)
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
        val event = ClassFeatureUsedEvent(featureID, featureName, recovery, remainingAfter, note?.trim()?.takeIf { it.isNotEmpty() })
        return IntentResult(character.copy(payload = updated), event)
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

        val newHp = minOf(payload.maxHp, payload.currentHp + healingRolled)
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
        val updated = payload.copy(
            currentPactSlots = maxPact,
            classFeatureUses = keptFeatures,
            resourcePools = keptPools,
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
        val hpRestored = payload.maxHp - payload.currentHp
        val tempCleared = payload.temporaryHp
        val maxSlots = payload.maxSpellSlots
        val maxPact = payload.maxPactSlots
        val regularRestored = maxSlots.values.sum() - payload.currentSpellSlots.values.sum()
        val pactRestored = maxOf(0, maxPact - payload.currentPactSlots)
        // 5e: regain up to half your total hit dice (min 1) on a long rest.
        val hitDiceRegained = if (payload.hitDiceSpent == 0) 0 else maxOf(1, payload.hitDiceSpent / 2)
        val updated = payload.copy(
            currentHp = payload.maxHp,
            temporaryHp = 0,
            currentSpellSlots = maxSlots,
            currentPactSlots = maxPact,
            hitDiceSpent = maxOf(0, payload.hitDiceSpent - hitDiceRegained),
            // Every class resource (pools + feature counters) refreshes on a long rest.
            resourcePools = emptyMap(),
            classFeatureUses = emptyMap(),
        )
        val event = LongRestEvent(
            hpRestored = maxOf(0, hpRestored),
            tempCleared = tempCleared,
            slotsRestored = maxOf(0, regularRestored + pactRestored),
            hitDiceRegained = hitDiceRegained,
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

        val previousLevel = payload.level
        val updatedClasses = payload.classes.toMutableList()
        if (existingIndex >= 0) {
            val entry = updatedClasses[existingIndex]
            updatedClasses[existingIndex] = entry.copy(level = entry.level + 1)
        } else {
            updatedClasses.add(ClassEntry(classId = classId, level = 1))
        }
        val classLevelAfter = updatedClasses.first { it.classId == classId }.level

        val conMod = AbilityScores.modifier(payload.abilityScores.constitution)
        val hpGained = LevelUpMath.levelUpHp(hpChoice, hitDie, conMod)

        val updated = payload.copy(
            classes = updatedClasses,
            maxHp = payload.maxHp + hpGained,
            currentHp = payload.currentHp + hpGained,
        )
        val event = LeveledUpEvent(
            classId = classId,
            className = "", // catalogue display name lands with content steps
            isNewClass = isNewClass,
            previousLevel = previousLevel,
            newLevel = updated.level,
            classLevelAfter = classLevelAfter,
            hpChoice = hpChoice,
            hpGained = hpGained,
        )
        return IntentResult(character.copy(payload = updated), event)
    }
}

package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.dnd5e.core.HpChoice
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterIntentError
import au.com.evonet.nat20.domain.IntentResult
import au.com.evonet.nat20.domain.NoteKind
import au.com.evonet.nat20.domain.Ruleset

/**
 * The starter D&D 5e (2024) intents (A18). These mirror the 2014 shapes but
 * operate on [DnD5e2024Payload] and use the 2024 exhaustion model. They reuse
 * the shared `-core` math (`AbilityScores`, `LevelUpMath`, `DnD5eClasses`).
 */

private fun Character.payload2024(): DnD5e2024Payload =
    payload as? DnD5e2024Payload ?: throw CharacterIntentError.Invalid("Character is not a D&D 5e (2024) character")

data class TakeDamage2024(val amount: Int, val damageType: String? = null) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Damage must be positive")
        val p = character.payload2024()
        val typeKey = damageType?.trim()?.lowercase()
        val resisted = typeKey != null && typeKey.isNotEmpty() && typeKey in p.effectiveDamageResistances
        val effective = if (resisted) amount / 2 else amount
        val tempAbsorbed = minOf(p.temporaryHp, effective)
        val hpDamage = effective - tempAbsorbed
        var newHp = maxOf(0, p.currentHp - hpDamage)
        // Relentless Endurance (Orc): when reduced to 0 (but not killed outright) the
        // first time per long rest, drop to 1 HP instead. Massive overkill still drops you.
        var relentless = false
        val overkill = hpDamage >= p.currentHp + p.effectiveMaxHp // damage ≥ current + max ⇒ instant death (RAW)
        if (newHp == 0 && p.currentHp > 0 && !p.relentlessEnduranceUsed && !overkill && SpeciesTraits2024.hasRelentlessEndurance(p.species)) {
            newHp = 1
            relentless = true
        }
        val concentrationDc = if (p.concentratingOn != null && hpDamage > 0) maxOf(10, hpDamage / 2) else null
        val updated = p.copy(currentHp = newHp, temporaryHp = p.temporaryHp - tempAbsorbed, relentlessEnduranceUsed = p.relentlessEnduranceUsed || relentless)
        return IntentResult(character.copy(payload = updated), DamageTaken2024Event(amount, damageType, p.currentHp, newHp, tempAbsorbed, resisted, concentrationDc, relentless))
    }
}

data class Heal2024(val amount: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Heal amount must be positive")
        val p = character.payload2024()
        val newHp = minOf(p.effectiveMaxHp, p.currentHp + amount)
        val revived = p.currentHp == 0 && newHp > 0
        val updated = p.copy(currentHp = newHp, deathSaves = if (revived) au.com.evonet.nat20.dnd5e.core.DeathSaves.cleared else p.deathSaves)
        return IntentResult(character.copy(payload = updated), Healed2024Event(amount, p.currentHp, newHp))
    }
}

data class GainTempHp2024(val amount: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Temp HP must be positive")
        val p = character.payload2024()
        val newValue = maxOf(p.temporaryHp, amount) // non-stacking
        return IntentResult(character.copy(payload = p.copy(temporaryHp = newValue)), TempHpGained2024Event(amount, newValue))
    }
}

data class AddNote2024(val text: String, val kind: NoteKind? = null) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw CharacterIntentError.Invalid("Note must not be empty")
        return IntentResult(character, Note2024Event(trimmed, kind))
    }
}

data class ChangeExhaustion2024(val delta: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (delta == 0) throw CharacterIntentError.Invalid("Exhaustion change can't be zero")
        val p = character.payload2024()
        val next = Exhaustion2024.clamp(p.exhaustionLevel + delta)
        if (next == p.exhaustionLevel) throw CharacterIntentError.Invalid("Exhaustion already at ${if (delta > 0) "maximum" else "zero"}")
        return IntentResult(character.copy(payload = p.copy(exhaustionLevel = next)), ExhaustionChanged2024Event(p.exhaustionLevel, next))
    }
}

data class SetInspiration2024(val has: Boolean) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        return IntentResult(character.copy(payload = p.copy(hasInspiration = has)), InspirationChanged2024Event(has))
    }
}

data class ApplyCondition2024(val name: String) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw CharacterIntentError.Invalid("Condition name cannot be empty")
        val p = character.payload2024()
        val already = p.activeConditions.any { it.equals(trimmed, ignoreCase = true) }
        val updated = if (already) p else p.copy(activeConditions = p.activeConditions + trimmed)
        return IntentResult(character.copy(payload = updated), ConditionChanged2024Event(trimmed, applied = true))
    }
}

data class ClearCondition2024(val name: String) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        val remaining = p.activeConditions.filterNot { it.equals(name.trim(), ignoreCase = true) }
        return IntentResult(character.copy(payload = p.copy(activeConditions = remaining)), ConditionChanged2024Event(name.trim(), applied = false))
    }
}

/** A long rest: full HP, temp cleared, all spell slots back, death saves cleared, one level of exhaustion shed, half hit dice back. */
class LongRest2024 : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        val maxSlots = p.maxSpellSlots
        val slotsRestored = maxSlots.values.sum() - p.currentSpellSlots.values.sum()
        val exhaustionAfter = Exhaustion2024.clamp(p.exhaustionLevel - 1)
        val hitDiceRegained = if (p.hitDiceSpent == 0) 0 else maxOf(1, p.hitDiceSpent / 2)
        val updated = p.copy(
            currentHp = p.effectiveMaxHp, temporaryHp = 0,
            currentSpellSlots = maxSlots,
            deathSaves = au.com.evonet.nat20.dnd5e.core.DeathSaves.cleared,
            exhaustionLevel = exhaustionAfter,
            relentlessEnduranceUsed = false,
            hitDiceSpent = maxOf(0, p.hitDiceSpent - hitDiceRegained),
            // End concentration and clear timed effects; Until-Cancelled buffs persist.
            concentratingOn = null,
            activeEffects = p.activeEffects.filterNot {
                it.concentrationOwner || it.duration is au.com.evonet.nat20.dnd5e.core.EffectDuration.Concentration ||
                    it.duration is au.com.evonet.nat20.dnd5e.core.EffectDuration.UntilRest || it.duration is au.com.evonet.nat20.dnd5e.core.EffectDuration.Rounds
            },
        )
        return IntentResult(
            character.copy(payload = updated),
            LongRested2024Event(maxOf(0, p.effectiveMaxHp - p.currentHp), maxOf(0, slotsRestored), exhaustionAfter < p.exhaustionLevel),
        )
    }
    override fun equals(other: Any?): Boolean = other is LongRest2024
    override fun hashCode(): Int = javaClass.hashCode()
}

/** Spends one hit die to heal (rolled amount passed in, clamped to max). */
data class SpendHitDie2024(val healingRolled: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (healingRolled < 0) throw CharacterIntentError.Invalid("Healing cannot be negative")
        val p = character.payload2024()
        if (p.currentHitDice <= 0) throw CharacterIntentError.Invalid("No hit dice remaining")
        val newHp = minOf(p.effectiveMaxHp, p.currentHp + healingRolled)
        val updated = p.copy(hitDiceSpent = p.hitDiceSpent + 1, currentHp = newHp)
        return IntentResult(character.copy(payload = updated), HitDieSpent2024Event(healingRolled, p.currentHp, newHp, updated.currentHitDice))
    }
}

/** Casts a spell at [slotLevel] (0 = cantrip), consuming a slot unless it's a cantrip; applies catalogue effects + concentration (A21/A17). */
data class CastSpell2024(
    val spellID: String,
    val spellName: String,
    val spellLevel: Int,
    val slotLevel: Int,
    val target: String? = null,
    val requiresConcentration: Boolean = false,
    val applyToSelf: Boolean = false,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (slotLevel < spellLevel) throw CharacterIntentError.Invalid("Slot level cannot be lower than the spell level")
        val p = character.payload2024()
        var updated = p
        if (slotLevel > 0) {
            val remaining = p.currentSpellSlots[slotLevel] ?: 0
            if (remaining <= 0) throw CharacterIntentError.Invalid("No level $slotLevel spell slots remaining")
            updated = p.copy(currentSpellSlots = p.currentSpellSlots.withSlot(slotLevel, remaining - 1))
        }
        if (requiresConcentration) {
            updated = updated.copy(concentratingOn = spellName, activeEffects = updated.activeEffects.filterNot { it.concentrationOwner })
        }
        SpellEffectCatalog2024.template(spellID)?.let { tmpl ->
            val applies = when (tmpl.scope) {
                EffectScope2024.ALWAYS_SELF, EffectScope2024.CASTER_RIDER -> true
                EffectScope2024.TARGET_PICKED -> applyToSelf
            }
            if (applies) updated = updated.copy(activeEffects = updated.activeEffects + tmpl.resolve(spellID))
        }
        val event = CastSpell2024Event(spellID, spellName, slotLevel, slotLevel > spellLevel && spellLevel > 0, target?.trim()?.takeIf { it.isNotEmpty() })
        return IntentResult(character.copy(payload = updated), event)
    }
}

/** Ends concentration, dropping every concentration-owning effect. */
class EndConcentration2024 : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        val target = p.concentratingOn
        val updated = p.copy(concentratingOn = null, activeEffects = p.activeEffects.filterNot { it.concentrationOwner })
        return IntentResult(character.copy(payload = updated), ConcentrationEnded2024Event(target))
    }
    override fun equals(other: Any?): Boolean = other is EndConcentration2024
    override fun hashCode(): Int = javaClass.hashCode()
}

/** Cancels a single active effect by id; clears concentration if no owner effects remain. */
data class CancelEffect2024(val effectId: String) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        val effect = p.activeEffects.firstOrNull { it.id == effectId } ?: throw CharacterIntentError.Invalid("No active effect $effectId")
        val remaining = p.activeEffects.filterNot { it.id == effectId }
        val stillConcentrating = remaining.any { it.concentrationOwner }
        val updated = p.copy(activeEffects = remaining, concentratingOn = if (effect.concentrationOwner && !stillConcentrating) null else p.concentratingOn)
        return IntentResult(character.copy(payload = updated), EffectCancelled2024Event(effect.name))
    }
}

/** Applies an ad-hoc effect (a DM buff/debuff). */
data class ApplyEffect2024(val effect: au.com.evonet.nat20.dnd5e.core.ActiveEffect) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        var updated = p
        if (effect.concentrationOwner) updated = updated.copy(concentratingOn = effect.name, activeEffects = updated.activeEffects.filterNot { it.concentrationOwner })
        updated = updated.copy(activeEffects = updated.activeEffects + effect)
        return IntentResult(character.copy(payload = updated), EffectApplied2024Event(effect.name))
    }
}

/** Spends a spell slot without casting a catalogued spell. */
data class ExpendSpellSlot2024(val slotLevel: Int, val source: String? = null) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (slotLevel !in 1..9) throw CharacterIntentError.Invalid("Spell slot level must be 1–9")
        val p = character.payload2024()
        val remaining = p.currentSpellSlots[slotLevel] ?: 0
        if (remaining <= 0) throw CharacterIntentError.Invalid("No level $slotLevel spell slots remaining")
        val updated = p.copy(currentSpellSlots = p.currentSpellSlots.withSlot(slotLevel, remaining - 1))
        return IntentResult(character.copy(payload = updated), SlotExpended2024Event(slotLevel, remaining - 1, source?.trim()?.takeIf { it.isNotEmpty() }))
    }
}

data class PrepareSpell2024(val spellID: String, val spellName: String, val classID: String) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        val bucket = p.preparedSpells[classID].orEmpty()
        val updated = if (spellID in bucket) p else p.copy(preparedSpells = p.preparedSpells + (classID to (bucket + spellID)))
        return IntentResult(character.copy(payload = updated), SpellPrep2024Event(spellName, prepared = true))
    }
}

data class UnprepareSpell2024(val spellID: String, val spellName: String, val classID: String) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        val bucket = p.preparedSpells[classID].orEmpty().filterNot { it == spellID }
        val newMap = if (bucket.isEmpty()) p.preparedSpells - classID else p.preparedSpells + (classID to bucket)
        return IntentResult(character.copy(payload = p.copy(preparedSpells = newMap)), SpellPrep2024Event(spellName, prepared = false))
    }
}

/** Resolves a death save from a rolled d20 atomically (nat-20 revive, nat-1 two failures, 10+ success). */
data class RollDeathSave2024(val d20: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (d20 !in 1..20) throw CharacterIntentError.Invalid("A d20 roll is 1–20")
        val p = character.payload2024()
        val prev = p.deathSaves
        var revivedHp: Int? = null
        val next = when {
            d20 == 20 -> { revivedHp = if (p.currentHp == 0) 1 else p.currentHp; au.com.evonet.nat20.dnd5e.core.DeathSaves.cleared }
            d20 == 1 -> au.com.evonet.nat20.dnd5e.core.DeathSaves.clamped(prev.successes, prev.failures + 2)
            d20 >= 10 -> prev.copy(successes = minOf(3, prev.successes + 1))
            else -> prev.copy(failures = minOf(3, prev.failures + 1))
        }
        val updated = p.copy(deathSaves = next, currentHp = revivedHp ?: p.currentHp)
        return IntentResult(character.copy(payload = updated), DeathSaveRolled2024Event(d20, prev, next, revivedHp))
    }
}

data class SetInitiative2024(val value: Int?) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        return IntentResult(character.copy(payload = p.copy(initiative = value)), Initiative2024Event(value))
    }
}

/** Logs a resolved weapon attack (rolled in the UI). Mutates nothing — purely a journal entry. */
data class MakeAttack2024(
    val weaponName: String,
    val attackTotal: Int,
    val outcome: au.com.evonet.nat20.dnd5e.core.AttackOutcome,
    val damage: Int? = null,
    val damageType: String? = null,
    val mastery: String? = null,
    val target: String? = null,
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (weaponName.isBlank()) throw CharacterIntentError.Invalid("Attack needs a weapon")
        character.payload2024() // type guard
        val event = Attack2024Event(
            weaponName, attackTotal, outcome,
            damage?.takeIf { outcome != au.com.evonet.nat20.dnd5e.core.AttackOutcome.MISS },
            damageType, mastery, target?.trim()?.takeIf { it.isNotEmpty() },
        )
        return IntentResult(character, event)
    }
}

// ── Inventory + equipment (A21) ─────────────────────────────────────────────────

/** Adds a line item to the pack; stackable kinds (gear/consumable/treasure) merge by catalogue id. */
data class AcquireItem2024(val item: InventoryItem2024) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (item.quantity <= 0) throw CharacterIntentError.Invalid("Quantity must be positive")
        val p = character.payload2024()
        val stackable = item.kind != ItemKind2024.WEAPON && item.kind != ItemKind2024.ARMOR && item.catalogueID != null
        val index = if (stackable) p.inventory.indexOfFirst { it.catalogueID == item.catalogueID && it.kind == item.kind } else -1
        val updated = p.inventory.toMutableList()
        if (index >= 0) updated[index] = updated[index].copy(quantity = updated[index].quantity + item.quantity) else updated.add(item)
        return IntentResult(character.copy(payload = p.copy(inventory = updated)), ItemAcquired2024Event(item.name, item.quantity))
    }
}

/** Drops some or all of a pack item; removes the line when the stack empties. */
data class DropItem2024(val itemID: String, val quantity: Int = 1) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (quantity <= 0) throw CharacterIntentError.Invalid("Quantity must be positive")
        val p = character.payload2024()
        val index = p.inventory.indexOfFirst { it.id == itemID }
        if (index < 0) throw CharacterIntentError.Invalid("No item with id $itemID")
        val existing = p.inventory[index]
        val dropped = minOf(quantity, existing.quantity)
        val updated = p.inventory.toMutableList()
        if (dropped >= existing.quantity) updated.removeAt(index) else updated[index] = existing.copy(quantity = existing.quantity - dropped)
        return IntentResult(character.copy(payload = p.copy(inventory = updated)), ItemDropped2024Event(existing.name, dropped))
    }
}

/** Dons or doffs armor (drives AC); pass null to go unarmored. The id must resolve in [Armors2024]. */
data class EquipArmor2024(val armorId: String?) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        val resolved = armorId?.let { Armors2024.armor(it) ?: throw CharacterIntentError.Invalid("Unknown armor $it") }
        return IntentResult(character.copy(payload = p.copy(equippedArmor = resolved?.id)), ArmorEquipped2024Event(resolved?.name))
    }
}

/** Straps on or stows a shield (+2 AC). */
data class SetShield2024(val equipped: Boolean) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        return IntentResult(character.copy(payload = p.copy(hasShield = equipped)), ShieldChanged2024Event(equipped))
    }
}

/** Adjusts a coin pouch by [delta] (positive = gain, negative = spend); never goes negative. */
data class AdjustCoin2024(val coin: Coin, val delta: Int, val source: String? = null) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (delta == 0) throw CharacterIntentError.Invalid("Coin adjustment must be non-zero")
        val p = character.payload2024()
        val current = p.coins[coin] ?: 0
        val newValue = current + delta
        if (newValue < 0) throw CharacterIntentError.Invalid("Not enough ${coin.abbreviation}: have $current, spending ${-delta}")
        val coins = p.coins.toMutableMap()
        if (newValue == 0) coins.remove(coin) else coins[coin] = newValue
        return IntentResult(character.copy(payload = p.copy(coins = coins)), CoinAdjusted2024Event(coin, delta, source?.trim()?.takeIf { it.isNotEmpty() }))
    }
}

/** Sets the character's weapon-mastery picks (de-duped, case-insensitive, capped by class progression). */
data class SetWeaponMasteries2024(val masteries: List<String>) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        val cap = p.classes.maxOfOrNull { WeaponMasteryProgression2024.slots(it.classId, it.level) } ?: 0
        val cleaned = masteries.mapNotNull { WeaponMastery2024.from(it)?.name?.lowercase() }.distinct().take(cap)
        return IntentResult(character.copy(payload = p.copy(weaponMasteries = cleaned)), WeaponMasteries2024Event(cleaned))
    }
}

/** Sets a level's remaining slot count, dropping the key at zero. */
private fun Map<Int, Int>.withSlot(level: Int, remaining: Int): Map<Int, Int> =
    if (remaining <= 0) this - level else this + (level to remaining)

/** A copy with all spell slots reset to full — used at creation and on a long rest. */
fun DnD5e2024Payload.withFullSpellSlots(): DnD5e2024Payload = copy(currentSpellSlots = maxSpellSlots)

data class LevelUp2024(
    val classId: String,
    val isNewClass: Boolean = false,
    val hpChoice: HpChoice = HpChoice.Average,
    val subclass: String? = null,
    /** Ability Score Improvement applied this level (≤ +2 total, capped at 20). Also carries a half-feat's +1. */
    val abilityIncreases: Map<au.com.evonet.nat20.dnd5e.core.Ability, Int> = emptyMap(),
    /** A General/Epic feat taken in place of (or alongside, for half-feats) the ASI at an advancement level. */
    val feat: String? = null,
    val className: String = "",
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.payload2024()
        if (p.level >= DnD5e2024Payload.MAX_LEVEL) throw CharacterIntentError.Invalid("Already at maximum level")
        if (classId.isEmpty()) throw CharacterIntentError.Invalid("LevelUp requires a classId")

        val index = p.classes.indexOfFirst { it.classId == classId }
        if (isNewClass && index >= 0) throw CharacterIntentError.Invalid("Already has levels in $classId")
        if (!isNewClass && index < 0) throw CharacterIntentError.Invalid("No existing levels in $classId — pass isNewClass = true")

        val hitDie = DnD5eClasses.hitDie(classId)
        if (hpChoice is HpChoice.Rolled && hpChoice.value !in 1..hitDie) {
            throw CharacterIntentError.Invalid("Rolled HP ${hpChoice.value} outside 1..$hitDie")
        }
        if (abilityIncreases.values.sum() > 2 || abilityIncreases.values.any { it < 0 }) {
            throw CharacterIntentError.Invalid("An Ability Score Improvement grants at most +2 total")
        }
        if (feat != null && Feats2024.feat(feat) == null) {
            throw CharacterIntentError.Invalid("Unknown feat $feat")
        }

        val classes = p.classes.toMutableList()
        if (index >= 0) {
            classes[index] = classes[index].copy(level = classes[index].level + 1, subclass = subclass ?: classes[index].subclass)
        } else {
            classes.add(ClassEntry2024(classId, 1, subclass))
        }
        val classLevelAfter = classes.first { it.classId == classId }.level

        var newScores = p.abilityScores
        for ((ability, inc) in abilityIncreases) newScores = newScores.with(ability, minOf(20, newScores.score(ability) + inc))

        val conMod = AbilityScores.modifier(newScores.constitution)
        val hpGained = LevelUpMath.levelUpHp(hpChoice, hitDie, conMod)

        // Grant newly-unlocked spell slots, preserving spent ones.
        val leveled = p.copy(classes = classes, abilityScores = newScores)
        val oldMax = p.maxSpellSlots
        val newCurrentSlots = leveled.maxSpellSlots.mapValues { (lvl, max) ->
            (p.currentSpellSlots[lvl] ?: 0) + maxOf(0, max - (oldMax[lvl] ?: 0))
        }

        val newFeats = if (feat != null && feat !in p.chosenFeats) p.chosenFeats + feat else p.chosenFeats
        // Stored maxHp is the *base*; the new level's share of any existing per-level rider
        // (Dwarven Toughness, an already-held Tough) tops a full character up too. A feat
        // taken *this* level boosts only the max (via effectiveMaxHp), not current — RAW.
        val updated = leveled.copy(
            maxHp = p.maxHp + hpGained,
            currentHp = p.currentHp + hpGained + p.bonusMaxHpPerLevel,
            currentSpellSlots = newCurrentSlots,
            chosenFeats = newFeats,
        )
        return IntentResult(
            character.copy(payload = updated),
            LeveledUp2024Event(classId, className, updated.level, classLevelAfter, hpGained, subclass, abilityIncreases, feat),
        )
    }
}

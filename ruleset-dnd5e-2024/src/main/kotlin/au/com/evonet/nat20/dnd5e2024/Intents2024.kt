package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.dnd5e.core.AbilityScores
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
        val tempAbsorbed = minOf(p.temporaryHp, amount)
        val newHp = maxOf(0, p.currentHp - (amount - tempAbsorbed))
        val updated = p.copy(currentHp = newHp, temporaryHp = p.temporaryHp - tempAbsorbed)
        return IntentResult(character.copy(payload = updated), DamageTaken2024Event(amount, damageType, p.currentHp, newHp, tempAbsorbed))
    }
}

data class Heal2024(val amount: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Heal amount must be positive")
        val p = character.payload2024()
        val newHp = minOf(p.maxHp, p.currentHp + amount)
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
            currentHp = p.maxHp, temporaryHp = 0,
            currentSpellSlots = maxSlots,
            deathSaves = au.com.evonet.nat20.dnd5e.core.DeathSaves.cleared,
            exhaustionLevel = exhaustionAfter,
            hitDiceSpent = maxOf(0, p.hitDiceSpent - hitDiceRegained),
        )
        return IntentResult(
            character.copy(payload = updated),
            LongRested2024Event(maxOf(0, p.maxHp - p.currentHp), maxOf(0, slotsRestored), exhaustionAfter < p.exhaustionLevel),
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
        val newHp = minOf(p.maxHp, p.currentHp + healingRolled)
        val updated = p.copy(hitDiceSpent = p.hitDiceSpent + 1, currentHp = newHp)
        return IntentResult(character.copy(payload = updated), HitDieSpent2024Event(healingRolled, p.currentHp, newHp, updated.currentHitDice))
    }
}

/** Casts a spell at [slotLevel] (0 = cantrip), consuming a slot unless it's a cantrip. */
data class CastSpell2024(val spellID: String, val spellName: String, val spellLevel: Int, val slotLevel: Int, val target: String? = null) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (slotLevel < spellLevel) throw CharacterIntentError.Invalid("Slot level cannot be lower than the spell level")
        val p = character.payload2024()
        var updated = p
        if (slotLevel > 0) {
            val remaining = p.currentSpellSlots[slotLevel] ?: 0
            if (remaining <= 0) throw CharacterIntentError.Invalid("No level $slotLevel spell slots remaining")
            updated = p.copy(currentSpellSlots = p.currentSpellSlots.withSlot(slotLevel, remaining - 1))
        }
        val event = CastSpell2024Event(spellID, spellName, slotLevel, slotLevel > spellLevel && spellLevel > 0, target?.trim()?.takeIf { it.isNotEmpty() })
        return IntentResult(character.copy(payload = updated), event)
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

        val classes = p.classes.toMutableList()
        if (index >= 0) {
            classes[index] = classes[index].copy(level = classes[index].level + 1, subclass = subclass ?: classes[index].subclass)
        } else {
            classes.add(ClassEntry2024(classId, 1, subclass))
        }
        val classLevelAfter = classes.first { it.classId == classId }.level
        val conMod = AbilityScores.modifier(p.abilityScores.constitution)
        val hpGained = LevelUpMath.levelUpHp(hpChoice, hitDie, conMod)
        val updated = p.copy(classes = classes, maxHp = p.maxHp + hpGained, currentHp = p.currentHp + hpGained)
        return IntentResult(
            character.copy(payload = updated),
            LeveledUp2024Event(classId, className, updated.level, classLevelAfter, hpGained),
        )
    }
}

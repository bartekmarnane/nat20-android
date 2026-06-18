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

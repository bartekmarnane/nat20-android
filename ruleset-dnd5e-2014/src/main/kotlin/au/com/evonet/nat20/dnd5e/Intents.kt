package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.DnD5eClasses
import au.com.evonet.nat20.dnd5e.core.HpChoice
import au.com.evonet.nat20.dnd5e.core.LevelUpMath
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterIntentError
import au.com.evonet.nat20.domain.IntentResult
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

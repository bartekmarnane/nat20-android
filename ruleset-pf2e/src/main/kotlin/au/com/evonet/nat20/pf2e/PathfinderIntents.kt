package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterIntentError
import au.com.evonet.nat20.domain.IntentResult
import au.com.evonet.nat20.domain.NoteKind
import au.com.evonet.nat20.domain.Ruleset
import au.com.evonet.nat20.pf2e.core.ValuedCondition

/** The Pathfinder 2e foundation-slice intents (A22) — vitals + the dying track + Hero Points + conditions. */

private fun Character.pf(): PathfinderPayload =
    payload as? PathfinderPayload ?: throw CharacterIntentError.Invalid("Character is not a Pathfinder 2e character")

data class PfAddNote(val text: String, val kind: NoteKind? = null) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw CharacterIntentError.Invalid("Note must not be empty")
        return IntentResult(character, PfNoteEvent(trimmed, kind))
    }
}

/**
 * Applies damage: temp HP absorbs first, then HP floored at 0. PF2e dying rules:
 * dropping to 0 HP knocks you out at **Dying = 1 + your Wounded value**; taking
 * damage while already at 0 increases Dying by 1 (capped at death).
 */
data class PfTakeDamage(val amount: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Damage must be positive")
        val p = character.pf()
        val tempAbsorbed = minOf(p.temporaryHp, amount)
        val hpDamage = amount - tempAbsorbed
        val newHp = maxOf(0, p.currentHp - hpDamage)
        var dying = p.dying
        if (newHp == 0) {
            dying = if (p.currentHp > 0) maxOf(p.dying, p.wounded + 1) else minOf(PathfinderPayload.DYING_MAX, p.dying + 1)
        }
        val updated = p.copy(currentHp = newHp, temporaryHp = p.temporaryHp - tempAbsorbed, dying = dying)
        return IntentResult(character.copy(payload = updated), PfDamageTakenEvent(amount, p.currentHp, newHp, tempAbsorbed, dying.takeIf { it > 0 }))
    }
}

/** Restores HP, clamped to max. Recovering from 0 HP ends Dying and increases Wounded by 1 (RAW). */
data class PfHeal(val amount: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Heal amount must be positive")
        val p = character.pf()
        val newHp = minOf(p.maxHp, p.currentHp + amount)
        val recovered = p.currentHp == 0 && newHp > 0 && p.dying > 0
        val updated = p.copy(
            currentHp = newHp,
            dying = if (recovered) 0 else p.dying,
            wounded = if (recovered) p.wounded + 1 else p.wounded,
        )
        return IntentResult(character.copy(payload = updated), PfHealedEvent(amount, p.currentHp, newHp, recovered))
    }
}

data class PfGainTempHp(val amount: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (amount <= 0) throw CharacterIntentError.Invalid("Temp HP must be positive")
        val p = character.pf()
        val newValue = maxOf(p.temporaryHp, amount) // non-stacking
        return IntentResult(character.copy(payload = p.copy(temporaryHp = newValue)), PfTempHpGainedEvent(amount, newValue))
    }
}

/** Adjusts the Dying value directly (recovery checks resolved in the UI). Clamped 0..max; 0 also clears nothing else. */
data class PfSetDying(val value: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.pf()
        val next = value.coerceIn(0, PathfinderPayload.DYING_MAX)
        if (next == p.dying) throw CharacterIntentError.Invalid("Dying already $next")
        // Losing the dying condition raises Wounded by 1 (RAW).
        val wounded = if (p.dying > 0 && next == 0) p.wounded + 1 else p.wounded
        return IntentResult(character.copy(payload = p.copy(dying = next, wounded = wounded)), PfDyingChangedEvent(p.dying, next))
    }
}

data class PfSetWounded(val value: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.pf()
        val next = maxOf(0, value)
        return IntentResult(character.copy(payload = p.copy(wounded = next)), PfWoundedChangedEvent(p.wounded, next))
    }
}

/** Gains or spends a Hero Point (capped at [PathfinderPayload.HERO_POINT_MAX]). */
data class PfAdjustHeroPoints(val delta: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (delta == 0) throw CharacterIntentError.Invalid("Hero Point change can't be zero")
        val p = character.pf()
        val next = (p.heroPoints + delta).coerceIn(0, PathfinderPayload.HERO_POINT_MAX)
        if (next == p.heroPoints) throw CharacterIntentError.Invalid("Hero Points already at ${if (delta > 0) "maximum" else "zero"}")
        return IntentResult(character.copy(payload = p.copy(heroPoints = next)), PfHeroPointsChangedEvent(p.heroPoints, next))
    }
}

/** Applies (or updates the value of) a condition. */
data class PfApplyCondition(val id: String, val value: Int? = null) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val slug = id.trim().lowercase()
        if (slug.isEmpty()) throw CharacterIntentError.Invalid("Condition name cannot be empty")
        val p = character.pf()
        val others = p.conditions.filterNot { it.id.equals(slug, ignoreCase = true) }
        val updated = p.copy(conditions = others + ValuedCondition(slug, value))
        return IntentResult(character.copy(payload = updated), PfConditionChangedEvent(PathfinderConditions.displayName(slug), value, applied = true))
    }
}

data class PfClearCondition(val id: String) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val slug = id.trim().lowercase()
        val p = character.pf()
        val updated = p.copy(conditions = p.conditions.filterNot { it.id.equals(slug, ignoreCase = true) })
        return IntentResult(character.copy(payload = updated), PfConditionChangedEvent(PathfinderConditions.displayName(slug), null, applied = false))
    }
}

package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntent
import au.com.evonet.nat20.domain.CharacterIntentError
import au.com.evonet.nat20.domain.IntentResult
import au.com.evonet.nat20.domain.NoteKind
import au.com.evonet.nat20.domain.Ruleset
import au.com.evonet.nat20.pf2e.core.AdvancementSchedule
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
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

// ── Equipment + Strikes (A22 slice 3) ───────────────────────────────────────────

/** Dons or doffs armor (drives AC); null = unarmored. The id must resolve in [PfArmors]. */
data class PfEquipArmor(val armorId: String?) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.pf()
        val resolved = armorId?.let { PfArmors.by(it) ?: throw CharacterIntentError.Invalid("Unknown armor $it") }
        return IntentResult(character.copy(payload = p.copy(armor = resolved?.id)), PfNoteEvent(resolved?.let { "Donned ${it.name}" } ?: "Removed armor"))
    }
}

/** Holds or stows a shield (also lowers it). */
data class PfEquipShield(val shieldId: String?) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.pf()
        val resolved = shieldId?.let { PfShields.by(it) ?: throw CharacterIntentError.Invalid("Unknown shield $it") }
        return IntentResult(character.copy(payload = p.copy(shield = resolved?.id, shieldRaised = false)), PfNoteEvent(resolved?.let { "Drew the ${it.name}" } ?: "Stowed the shield"))
    }
}

/** Raises or lowers the held shield (the Raise a Shield action), adding its circumstance bonus to AC. */
data class PfRaiseShield(val raised: Boolean) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.pf()
        if (p.shield == null) throw CharacterIntentError.Invalid("No shield held")
        return IntentResult(character.copy(payload = p.copy(shieldRaised = raised)), PfNoteEvent(if (raised) "Raised a Shield" else "Lowered the shield"))
    }
}

/** Adds a weapon to the carried set (drives the Strikes). */
data class PfAddWeapon(val weaponId: String) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val weapon = PfWeapons.by(weaponId) ?: throw CharacterIntentError.Invalid("Unknown weapon $weaponId")
        val p = character.pf()
        val updated = if (weaponId in p.weapons) p else p.copy(weapons = p.weapons + weaponId)
        return IntentResult(character.copy(payload = updated), PfNoteEvent("Acquired ${weapon.name}"))
    }
}

data class PfRemoveWeapon(val weaponId: String) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.pf()
        val weapon = PfWeapons.by(weaponId)
        return IntentResult(character.copy(payload = p.copy(weapons = p.weapons - weaponId)), PfNoteEvent("Dropped ${weapon?.name ?: weaponId}"))
    }
}

/** Logs a resolved Strike (the player rolls; this journals the attack number + total against an optional DC/AC). */
data class PfStrike(val weaponId: String, val attackNumber: Int, val total: Int, val target: String? = null) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val weapon = PfWeapons.by(weaponId) ?: throw CharacterIntentError.Invalid("Unknown weapon $weaponId")
        character.pf()
        return IntentResult(character, PfStrikeEvent(weapon.name, attackNumber, total, target?.trim()?.takeIf { it.isNotEmpty() }))
    }
}

// ── Spellcasting (A22 slice 4) ──────────────────────────────────────────────────

/** Casts a spell at [slotRank] (0 = cantrip, no slot). Heightened when slotRank > spellRank. */
data class PfCastSpell(val spellId: String, val spellName: String, val spellRank: Int, val slotRank: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        if (slotRank < spellRank) throw CharacterIntentError.Invalid("A slot can't be lower than the spell's rank")
        val p = character.pf()
        var updated = p
        if (slotRank > 0) {
            val remaining = p.currentSpellSlots[slotRank] ?: 0
            if (remaining <= 0) throw CharacterIntentError.Invalid("No rank $slotRank spell slots remaining")
            updated = p.copy(currentSpellSlots = p.currentSpellSlots.withSlot(slotRank, remaining - 1))
        }
        return IntentResult(character.copy(payload = updated), PfSpellCastEvent(spellName, slotRank, heightened = slotRank > spellRank && spellRank > 0))
    }
}

/** Casts a focus spell, spending one Focus Point. */
data class PfCastFocusSpell(val spellName: String) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.pf()
        if (p.focusPoints <= 0) throw CharacterIntentError.Invalid("No Focus Points remaining")
        return IntentResult(character.copy(payload = p.copy(focusPoints = p.focusPoints - 1)), PfSpellCastEvent(spellName, 0, heightened = false, focus = true))
    }
}

/** Refocus — regain one Focus Point (capped at the pool maximum). */
class PfRefocus : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.pf()
        if (p.focusPoints >= p.maxFocusPoints) throw CharacterIntentError.Invalid("Focus pool already full")
        return IntentResult(character.copy(payload = p.copy(focusPoints = minOf(p.maxFocusPoints, p.focusPoints + 1))), PfNoteEvent("Refocused — regained a Focus Point"))
    }
    override fun equals(other: Any?): Boolean = other is PfRefocus
    override fun hashCode(): Int = javaClass.hashCode()
}

/** Daily Preparations / a full night's rest — restore all spell slots and the focus pool to maximum. */
class PfDailyPreparations : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.pf()
        val max = p.maxSpellSlots
        val slotsRestored = max.values.sum() - p.currentSpellSlots.values.sum()
        val updated = p.copy(currentSpellSlots = max, focusPoints = p.maxFocusPoints)
        return IntentResult(character.copy(payload = updated), PfDailyPrepEvent(maxOf(0, slotsRestored)))
    }
    override fun equals(other: Any?): Boolean = other is PfDailyPreparations
    override fun hashCode(): Int = javaClass.hashCode()
}

/** Adds a spell to the repertoire/prepared list (rank ≥ 1) or the cantrip list (rank 0). */
data class PfLearnSpell(val spellId: String, val rank: Int) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val spell = PfSpells.by(spellId) ?: throw CharacterIntentError.Invalid("Unknown spell $spellId")
        val p = character.pf()
        val updated = if (rank == 0) {
            if (spellId in p.cantrips) p else p.copy(cantrips = p.cantrips + spellId)
        } else {
            val bucket = p.knownSpells[rank].orEmpty()
            if (spellId in bucket) p else p.copy(knownSpells = p.knownSpells + (rank to (bucket + spellId)))
        }
        return IntentResult(character.copy(payload = updated), PfNoteEvent("Learned ${spell.name}"))
    }
}

/** Sets a rank's remaining slots, dropping the key at zero. */
private fun Map<Int, Int>.withSlot(rank: Int, remaining: Int): Map<Int, Int> =
    if (remaining <= 0) this - rank else this + (rank to remaining)

/** A copy with all spell slots reset to full — used at creation + on Daily Preparations. */
fun PathfinderPayload.withFullSpellSlots(): PathfinderPayload = copy(currentSpellSlots = maxSpellSlots)

// ── Level-up / advancement (A22 slice 5) ────────────────────────────────────────

/**
 * Advances the character one level. PF2e HP is fixed (no roll): `class HP/level +
 * CON modifier`. Bumping the level alone improves every proficiency-scaled stat
 * (bonus = level + rank). At skill-increase levels the player may raise one
 * skill one rank (capped by [AdvancementSchedule.maxSkillRank]); at 5/10/15/20
 * they allocate four ability boosts (+2 / +1-at-18). The class-specific
 * proficiency-rank jumps (weapon mastery, save expertise) are a later slice.
 */
data class PfLevelUp(
    val skillIncrease: PfSkill? = null,
    val abilityBoosts: List<PfAbility> = emptyList(),
) : CharacterIntent {
    override fun applyTo(character: Character, ruleset: Ruleset): IntentResult {
        val p = character.pf()
        if (p.level >= PathfinderPayload.MAX_LEVEL) throw CharacterIntentError.Invalid("Already at maximum level")
        val newLevel = p.level + 1

        // Skill increase, gated to a legal level + the rank ceiling for that level.
        var skills = p.skills
        if (skillIncrease != null) {
            if (!AdvancementSchedule.grantsSkillIncrease(newLevel)) throw CharacterIntentError.Invalid("Level $newLevel doesn't grant a skill increase")
            val current = p.skills[skillIncrease] ?: Proficiency.UNTRAINED
            val raised = current.next ?: throw CharacterIntentError.Invalid("${skillIncrease.displayName} is already Legendary")
            if (raised.rank > AdvancementSchedule.maxSkillRank(newLevel).rank) throw CharacterIntentError.Invalid("Can't raise above ${AdvancementSchedule.maxSkillRank(newLevel).displayName} at level $newLevel")
            skills = p.skills + (skillIncrease to raised)
        }

        // Ability boosts at 5/10/15/20.
        var scores = p.abilityScores
        if (abilityBoosts.isNotEmpty()) {
            if (!AdvancementSchedule.grantsAbilityBoosts(newLevel)) throw CharacterIntentError.Invalid("Level $newLevel doesn't grant ability boosts")
            if (abilityBoosts.size != abilityBoosts.toSet().size) throw CharacterIntentError.Invalid("Can't boost the same ability twice")
            for (a in abilityBoosts) {
                val cur = scores.score(a)
                scores = scores.with(a, cur + if (cur >= 18) 1 else 2)
            }
        }

        val conMod = PfAbilityScores.modifier(scores.constitution)
        val hpGained = (PathfinderCatalog.pfClass(p.className)?.hpPerLevel ?: 8) + conMod

        val leveled = p.copy(
            level = newLevel,
            abilityScores = scores,
            skills = skills,
            maxHp = p.maxHp + maxOf(1, hpGained),
            currentHp = p.currentHp + maxOf(1, hpGained),
        )
        // Newly-unlocked spell slots top up (caster ranks scale with level), preserving spent ones.
        val updated = if (leveled.isCaster) {
            val oldMax = p.maxSpellSlots
            val newSlots = leveled.maxSpellSlots.mapValues { (rank, max) ->
                (p.currentSpellSlots[rank] ?: 0) + maxOf(0, max - (oldMax[rank] ?: 0))
            }
            leveled.copy(currentSpellSlots = newSlots)
        } else {
            leveled
        }
        return IntentResult(
            character.copy(payload = updated),
            PfLeveledUpEvent(newLevel, maxOf(1, hpGained), skillIncrease?.displayName, abilityBoosts.map { it.abbreviation }),
        )
    }
}

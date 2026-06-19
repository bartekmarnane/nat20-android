package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save

/**
 * Derived Pathfinder 2e statistics (A22). Every check in PF2e is
 * `ability modifier + proficiency bonus (level + rank) + others`. The
 * conditions' status penalties (Frightened N) fold in as a blanket penalty to
 * the relevant rolls. Mirrors the iOS `PathfinderStats`.
 */

/** Ability modifier for the given ability. */
fun PathfinderPayload.modifier(ability: PfAbility): Int = PfAbilityScores.modifier(abilityScores.score(ability))

/** The blanket status penalty from Frightened (the canonical "−N to everything" condition). */
val PathfinderPayload.frightenedPenalty: Int
    get() = conditions.firstOrNull { it.id.equals("frightened", ignoreCase = true) }?.value ?: 0

/** Perception = WIS modifier + proficiency(level) − Frightened. */
val PathfinderPayload.perceptionBonus: Int
    get() = modifier(PfAbility.WISDOM) + perception.bonus(level) - frightenedPenalty

/** A saving throw bonus = governing-ability modifier + that save's proficiency(level) − Frightened. */
fun PathfinderPayload.saveBonus(save: Save): Int =
    modifier(save.ability) + (saves[save] ?: Proficiency.UNTRAINED).bonus(level) - frightenedPenalty

/** A skill bonus = governing-ability modifier + that skill's proficiency(level) − Frightened. */
fun PathfinderPayload.skillBonus(skill: PfSkill): Int =
    modifier(skill.ability) + (skills[skill] ?: Proficiency.UNTRAINED).bonus(level) - frightenedPenalty

/** A Lore skill bonus (always Intelligence) = INT modifier + its proficiency(level) − Frightened. */
fun PathfinderPayload.loreBonus(subtype: String): Int =
    modifier(PfSkill.LORE_ABILITY) + (loreSkills[subtype] ?: Proficiency.UNTRAINED).bonus(level) - frightenedPenalty

/** Class DC = 10 + key-ability modifier + class-DC proficiency(level). */
val PathfinderPayload.classDcValue: Int
    get() = 10 + (keyAbility?.let { modifier(it) } ?: 0) + classDC.bonus(level)

/** Unarmored Armor Class = 10 + DEX modifier + unarmored proficiency(level) − Frightened. */
val PathfinderPayload.armorClass: Int
    get() = 10 + modifier(PfAbility.DEXTERITY) + unarmoredProficiency.bonus(level) - frightenedPenalty

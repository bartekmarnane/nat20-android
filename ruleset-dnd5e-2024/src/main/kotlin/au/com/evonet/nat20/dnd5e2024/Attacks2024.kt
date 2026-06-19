package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollSpec

/**
 * Pure 2024 attack math (A21): derives the d20 attack bonuses and damage spec
 * for a [Weapon2024] wielded by a 2024 character. Mirrors the 2014 `AttackMath`,
 * over the 2024 weapon catalogue, and folds the edition's **feat damage riders**:
 * Great Weapon Master adds the Proficiency Bonus to a Heavy weapon's damage. The
 * weapon's **mastery** property rides along for display. Effect attack/damage
 * chips (Bless, Rage, Hunter's Mark) fold in from active effects.
 */
object AttackMath2024 {
    data class WeaponAttack2024(
        val name: String,
        val abilityLabel: String,
        val attackBonuses: List<RollBonus>,
        val damageSpec: RollSpec,
        val damageBonuses: List<RollBonus>,
        val damageType: String,
        val mastery: WeaponMastery2024,
    )

    /** Weapons that use DEX (ranged). */
    private val RANGED = setOf("shortbow", "longbow")
    /** Finesse weapons (use the better of STR/DEX). */
    private val FINESSE = setOf("dagger", "shortsword", "rapier")
    /** Melee Heavy weapons — the Great Weapon Master damage rider applies to these. */
    private val HEAVY_MELEE = setOf("greatsword", "greataxe", "maul", "glaive", "halberd")

    fun forWeapon(weapon: Weapon2024, payload: DnD5e2024Payload): WeaponAttack2024 {
        val scores = payload.effectiveAbilityScores
        val str = AbilityScores.modifier(scores.strength)
        val dex = AbilityScores.modifier(scores.dexterity)
        val (label, mod) = when {
            weapon.id in RANGED -> "DEX" to dex
            weapon.id in FINESSE -> if (dex >= str) "DEX" to dex else "STR" to str
            else -> "STR" to str
        }
        val prof = Proficiency.bonus(payload.level)
        val effectAttack = payload.effectAttackBonus
        val effectDamage = payload.effectDamageBonus
        // 2024 Great Weapon Master: on a hit with a Heavy weapon, add your Proficiency Bonus to the damage.
        val gwm = "great-weapon-master" in payload.allFeats && weapon.id in HEAVY_MELEE

        val attackBonuses = buildList {
            add(RollBonus(label, mod))
            add(RollBonus("Proficiency", prof))
            if (effectAttack != 0) add(RollBonus("Effects", effectAttack))
        }
        val damageBonuses = buildList {
            if (mod != 0) add(RollBonus(label, mod))
            if (gwm) add(RollBonus("Great Weapon Master", prof))
            if (effectDamage != 0) add(RollBonus("Effects", effectDamage))
        }
        // "1d8 slashing" → spec + type.
        val parts = weapon.damage.split(" ", limit = 2)
        return WeaponAttack2024(
            name = weapon.name,
            abilityLabel = label,
            attackBonuses = attackBonuses,
            damageSpec = RollSpec.parse(parts.getOrNull(0) ?: "") ?: RollSpec.d(1, 4),
            damageBonuses = damageBonuses,
            damageType = parts.getOrNull(1)?.trim().orEmpty(),
            mastery = weapon.mastery,
        )
    }
}

/** The character's weapons (every catalogue-linked weapon in the pack), for the attack picker. */
val DnD5e2024Payload.weapons: List<Weapon2024>
    get() = inventory.filter { it.kind == ItemKind2024.WEAPON }.mapNotNull { it.catalogueID?.let(Weapons2024::weapon) }

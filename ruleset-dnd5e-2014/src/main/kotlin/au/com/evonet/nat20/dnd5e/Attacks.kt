package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.Proficiency
import au.com.evonet.nat20.dnd5e.core.RollBonus
import au.com.evonet.nat20.dnd5e.core.RollSpec

/**
 * Pure attack math (A15): derives the d20 attack bonuses and damage spec for a
 * weapon against a character. Mirrors the iOS `RollBonuses.attack` / damage
 * helpers, trimmed to the base rules — ability mod (STR, or DEX for ranged /
 * the better of the two for finesse) + proficiency on the attack, ability mod on
 * the damage. Class/feat riders (Rage, Sneak Attack, Divine Smite) wait on A17.
 * Proficiency is assumed (characters are proficient with the weapons they wield).
 */
object AttackMath {
    data class WeaponAttack(
        val name: String,
        val abilityLabel: String,
        val attackBonuses: List<RollBonus>,
        val damageSpec: RollSpec,
        val damageBonuses: List<RollBonus>,
        val damageType: String,
    )

    fun forWeapon(item: InventoryItem, payload: DnD5ePayload): WeaponAttack? {
        val weapon = item.weapon ?: return null
        val str = payload.abilityScores.modifier(Ability.STRENGTH)
        val dex = payload.abilityScores.modifier(Ability.DEXTERITY)
        val ranged = weapon.kind == WeaponProperties.Kind.RANGED
        val finesse = weapon.properties.any { it.contains("finesse", ignoreCase = true) }
        val (label, mod) = when {
            ranged -> "DEX" to dex
            finesse -> if (dex >= str) "DEX" to dex else "STR" to str
            else -> "STR" to str
        }
        val prof = Proficiency.bonus(payload.level)
        // Active-effect riders (Bless +2 attack, Rage/Hex/Hunter's Mark +N damage) fold in as chips.
        val effectAttack = payload.effectAttackBonus
        val effectDamage = payload.effectDamageBonus
        val attackBonuses = buildList {
            add(RollBonus(label, mod))
            add(RollBonus("Proficiency", prof))
            if (effectAttack != 0) add(RollBonus("Effects", effectAttack))
        }
        val damageBonuses = buildList {
            if (mod != 0) add(RollBonus(label, mod))
            if (effectDamage != 0) add(RollBonus("Effects", effectDamage))
        }
        return WeaponAttack(
            name = item.name,
            abilityLabel = label,
            attackBonuses = attackBonuses,
            damageSpec = RollSpec.parse(weapon.damageDice) ?: RollSpec.d(1, 4),
            damageBonuses = damageBonuses,
            damageType = weapon.damageType,
        )
    }
}

/** The character's equipped weapons (for the attack picker). */
val DnD5ePayload.equippedWeapons: List<InventoryItem>
    get() = inventory.filter { it.equipped && it.kind == ItemKind.WEAPON }

/** Initiative bonus = DEX modifier, plus the Alert feat's flat +5 (A11). */
val DnD5ePayload.initiativeBonus: Int
    get() = abilityScores.modifier(Ability.DEXTERITY) + (if ("alert" in chosenFeats) 5 else 0)

package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save
import au.com.evonet.nat20.pf2e.core.SpellcastingProgression

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

/** A saving throw bonus = ability modifier + proficiency(level) + resilient-rune item bonus − Frightened. */
fun PathfinderPayload.saveBonus(save: Save): Int =
    modifier(save.ability) + (saves[save] ?: Proficiency.UNTRAINED).bonus(level) + armorRunes.resilient - frightenedPenalty

/** A skill bonus = governing-ability modifier + that skill's proficiency(level) − Frightened. */
fun PathfinderPayload.skillBonus(skill: PfSkill): Int =
    modifier(skill.ability) + (skills[skill] ?: Proficiency.UNTRAINED).bonus(level) - frightenedPenalty

/** A Lore skill bonus (always Intelligence) = INT modifier + its proficiency(level) − Frightened. */
fun PathfinderPayload.loreBonus(subtype: String): Int =
    modifier(PfSkill.LORE_ABILITY) + (loreSkills[subtype] ?: Proficiency.UNTRAINED).bonus(level) - frightenedPenalty

/** Class DC = 10 + key-ability modifier + class-DC proficiency(level). */
val PathfinderPayload.classDcValue: Int
    get() = 10 + (keyAbility?.let { modifier(it) } ?: 0) + classDC.bonus(level)

/**
 * Armor Class. Worn armor: `10 + min(DEX, dexCap) + armor-category proficiency
 * (level) + armor item bonus`. Unarmored: `10 + DEX + unarmored proficiency`.
 * Frightened folds a blanket status penalty either way.
 */
val PathfinderPayload.armorClass: Int
    get() {
        val dexMod = modifier(PfAbility.DEXTERITY)
        val worn = armor?.let { PfArmors.by(it) }
        val base = if (worn != null) {
            val prof = armorProficiencies[worn.category] ?: Proficiency.UNTRAINED
            10 + minOf(dexMod, worn.dexCap) + prof.bonus(level) + worn.acBonus + armorRunes.potency
        } else {
            10 + dexMod + unarmoredProficiency.bonus(level)
        }
        return base - frightenedPenalty
    }

/** AC while the held shield is Raised (its circumstance bonus on top of [armorClass]); null if no shield. */
val PathfinderPayload.armorClassRaised: Int?
    get() = shield?.let { PfShields.by(it) }?.let { armorClass + it.raisedAcBonus }

/** A weapon's Strike — the attack modifiers for the 1st/2nd/3rd attack (MAP) + the damage line. */
data class Strike(val weapon: PfWeapon, val attackMods: List<Int>, val damage: String, val damageType: String)

/** The character's Strikes, derived from their carried [weapons]. */
val PathfinderPayload.strikes: List<Strike>
    get() = weapons.mapNotNull { PfWeapons.by(it) }.map { strike(it) }

/** Build a [Strike] for one weapon: attack = ability mod + weapon-category proficiency(level); MAP −5/−10 (−4/−8 agile). */
fun PathfinderPayload.strike(weapon: PfWeapon): Strike {
    val str = modifier(PfAbility.STRENGTH)
    val dex = modifier(PfAbility.DEXTERITY)
    val attackAbility = when {
        weapon.ranged -> dex
        weapon.finesse -> maxOf(str, dex)
        else -> str
    }
    val prof = (weaponProficiencies[weapon.category] ?: Proficiency.UNTRAINED).bonus(level)
    val runes = weaponRunes[weapon.id] ?: WeaponRunes()
    val first = attackAbility + prof + runes.potency - frightenedPenalty // potency rune = item bonus to attack
    val map = if (weapon.agile) 4 else 5
    val attackMods = listOf(first, first - map, first - map * 2)
    // Damage adds STR for melee strikes (including finesse); ranged adds nothing (no propulsive/thrown modelled yet).
    val dmgBonus = if (weapon.ranged) 0 else str
    // Striking rune multiplies the weapon's damage dice (total dice = 1 + striking).
    val dice = heightenDamageDice(weapon.damageDie, 1 + runes.striking)
    val damage = dice + if (dmgBonus != 0) (if (dmgBonus > 0) "+$dmgBonus" else "$dmgBonus") else ""
    return Strike(weapon, attackMods, damage, weapon.damageType)
}

// ── Spellcasting (A22 slice 4) ──────────────────────────────────────────────────

/** True if the character has a spellcasting tradition. */
val PathfinderPayload.isCaster: Boolean get() = spellTradition != null

/** The full-caster spell-slot table at this level (rank → count); empty for a non-caster. */
val PathfinderPayload.maxSpellSlots: Map<Int, Int>
    get() = if (isCaster) SpellcastingProgression.fullCasterSlots(level) else emptyMap()

/** Spell attack modifier = casting-ability modifier + spell proficiency(level) − Frightened. */
val PathfinderPayload.spellAttack: Int
    get() = (castingAbility?.let { modifier(it) } ?: 0) + spellProficiency.bonus(level) - frightenedPenalty

/** Spell DC = 10 + the spell attack modifier (the Frightened penalty already folded in). */
val PathfinderPayload.spellDc: Int get() = 10 + spellAttack

// ── Runes + Bulk (A22 slice 9) ──────────────────────────────────────────────────

/** Multiply a "NdX" damage string's die count (a striking rune of tier T → 1+T dice). */
internal fun heightenDamageDice(damageDie: String, totalDice: Int): String {
    val parts = damageDie.lowercase().split("d")
    val count = parts.getOrNull(0)?.toIntOrNull() ?: return damageDie
    val face = parts.getOrNull(1)?.toIntOrNull() ?: return damageDie
    return "${count * totalDice}d$face"
}

/** Total Bulk carried: inventory items + coins (1,000 coins = 1 Bulk). Worn armor counts as 1 less than carried (not modelled here). */
val PathfinderPayload.totalBulk: Double
    get() = inventory.sumOf { it.bulk * it.quantity } + coins.values.sum() * Bulk.PER_COIN

/** Carrying-capacity threshold at which the character becomes encumbered: 5 + STR modifier. */
val PathfinderPayload.encumberedThreshold: Int
    get() = 5 + modifier(PfAbility.STRENGTH)

/** True once the effective (floored) Bulk exceeds the encumbered threshold. */
val PathfinderPayload.isEncumbered: Boolean
    get() = Bulk.effective(totalBulk) > encumberedThreshold

package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.RollSpec

/**
 * Class damage riders that attach to a weapon hit (A15/A17): the Rogue's **Sneak
 * Attack** and the Paladin's **Divine Smite**. Pure math — the attack sheet rolls
 * the rider dice (doubled on a crit, like the weapon dice) and folds the total
 * into the logged damage; Divine Smite also expends the spent slot. Port of the
 * iOS `SneakAttack` / `DivineSmite` damage helpers.
 */
object DamageRiders {
    /** Levels in a given class (0 if none), used to gate + scale the riders. */
    fun classLevel(payload: DnD5ePayload, classId: String): Int =
        payload.classes.firstOrNull { it.classId.equals(classId, ignoreCase = true) }?.level ?: 0

    // ── Sneak Attack ──────────────────────────────────────────────────────────

    /** Sneak Attack scales 1d6 per two Rogue levels, rounded up: 1d6 at L1–2, 2d6 at L3–4, … 10d6 at L19–20. */
    fun sneakAttackDice(rogueLevel: Int): Int = if (rogueLevel <= 0) 0 else (rogueLevel + 1) / 2

    /** Sneak Attack needs a finesse or ranged weapon (RAW: and advantage / an adjacent ally — left to the player). */
    fun sneakAttackEligible(weapon: WeaponProperties): Boolean =
        weapon.kind == WeaponProperties.Kind.RANGED || weapon.properties.any { it.contains("finesse", ignoreCase = true) }

    /** The Sneak Attack damage spec for a Rogue of [rogueLevel] (null if not eligible/no levels). */
    fun sneakAttackSpec(rogueLevel: Int): RollSpec? = sneakAttackDice(rogueLevel).takeIf { it > 0 }?.let { RollSpec.d(it, 6) }

    // ── Divine Smite ──────────────────────────────────────────────────────────

    /**
     * Divine Smite deals 2d8 radiant for a 1st-level slot, +1d8 per slot level above
     * 1st (capped at 5d8 from a 4th-level slot), and +1d8 against an Undead or Fiend
     * (which can exceed the cap, RAW → up to 6d8).
     */
    fun divineSmiteDice(slotLevel: Int, vsUndeadOrFiend: Boolean = false): Int =
        minOf(5, slotLevel + 1) + (if (vsUndeadOrFiend) 1 else 0)

    /** The Divine Smite damage spec for a [slotLevel] slot (null if the level is out of 1..9). */
    fun divineSmiteSpec(slotLevel: Int, vsUndeadOrFiend: Boolean = false): RollSpec? =
        if (slotLevel in 1..9) RollSpec.d(divineSmiteDice(slotLevel, vsUndeadOrFiend), 8) else null
}

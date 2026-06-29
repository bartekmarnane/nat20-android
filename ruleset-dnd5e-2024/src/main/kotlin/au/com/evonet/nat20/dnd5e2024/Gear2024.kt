package au.com.evonet.nat20.dnd5e2024

/**
 * D&D 5e (2024) adventuring gear — a representative SRD spread including the
 * Shield (the one piece with an AC bonus). Display model; costs/weights are the
 * standard PHB/SRD values. Port of the iOS `Gears2024`.
 */
data class GearItem2024(
    val id: String,
    val name: String,
    /** "5 gp", "1 cp". */
    val cost: String,
    /** Pounds; null = negligible. */
    val weight: Double? = null,
    /** Flat AC bonus while wielded — the Shield (+2); null for everything else. */
    val acBonus: Int? = null,
    val description: String = "",
)

object Gears2024 {
    val all: List<GearItem2024> = listOf(
        GearItem2024("shield", "Shield", "10 gp", 6.0, 2, "A wooden or metal shield. Wielding it grants a +2 bonus to AC; you can benefit from only one shield at a time."),
        GearItem2024("backpack", "Backpack", "2 gp", 5.0, null, "Holds up to 30 pounds of gear within 1 cubic foot."),
        GearItem2024("bedroll", "Bedroll", "1 gp", 7.0, null, "Padding and a blanket for sleeping outdoors."),
        GearItem2024("rope-hempen", "Rope, Hempen (50 ft)", "1 gp", 10.0, null, "Has 2 Hit Points and can be burst with a DC 17 Strength check."),
        GearItem2024("torch", "Torch", "1 cp", 1.0, null, "Burns for 1 hour, casting Bright Light in a 20-foot radius and Dim Light for an extra 20 feet."),
        GearItem2024("rations", "Rations (1 day)", "5 sp", 2.0, null, "Dry foodstuffs suitable for extended travel."),
        GearItem2024("waterskin", "Waterskin", "2 sp", 5.0, null, "Holds 4 pints of liquid (weight shown when full)."),
        GearItem2024("tinderbox", "Tinderbox", "5 sp", 1.0, null, "Flint, fire steel, and tinder used to kindle a flame."),
        GearItem2024("lantern-hooded", "Lantern, Hooded", "5 gp", 2.0, null, "Casts Bright Light in a 30-foot radius and Dim Light for an extra 30 feet; can be lowered to Dim Light only."),
        GearItem2024("oil-flask", "Oil (flask)", "1 sp", 1.0, null, "Fuels a lantern for 6 hours, or can be thrown as an improvised weapon."),
        GearItem2024("crowbar", "Crowbar", "2 gp", 5.0, null, "Grants Advantage on Strength checks where leverage applies."),
        GearItem2024("grappling-hook", "Grappling Hook", "2 gp", 4.0, null, "A hook for securing a rope to an edge or outcropping."),
        GearItem2024("healers-kit", "Healer's Kit", "5 gp", 3.0, null, "Ten uses. Lets you stabilize a creature at 0 HP without an ability check."),
        GearItem2024("thieves-tools", "Thieves' Tools", "25 gp", 1.0, null, "Picks and probes for disarming traps and opening locks (Dexterity)."),
        GearItem2024("caltrops", "Caltrops (bag of 20)", "1 gp", 2.0, null, "Scatter to cover a 5-foot square; a creature entering risks a punctured foot and reduced Speed."),
        GearItem2024("climbers-kit", "Climber's Kit", "25 gp", 12.0, null, "Pitons, boot tips, gloves, and a harness for anchoring yourself while climbing."),
        GearItem2024("antitoxin", "Antitoxin (vial)", "50 gp", null, null, "Drink it to gain Advantage on saves against poison for 1 hour."),
        GearItem2024("holy-water", "Holy Water (flask)", "25 gp", 1.0, null, "Throw it (improvised weapon) to deal Radiant damage to a Fiend or Undead."),
        GearItem2024("component-pouch", "Component Pouch", "25 gp", 2.0, null, "Holds the material components for spells that lack a listed cost."),
        GearItem2024("spellbook", "Spellbook", "50 gp", 3.0, null, "A leather-bound tome of 100 blank vellum pages for a wizard's spells."),
    )

    fun gear(id: String): GearItem2024? = all.firstOrNull { it.id == id }
}

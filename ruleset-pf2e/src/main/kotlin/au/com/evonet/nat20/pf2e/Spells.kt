package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.pf2e.core.SpellTradition

/**
 * A Pathfinder 2e spell + a representative seed catalogue (A22). Rank 0 = cantrip
 * (auto-heightened to half the caster's level, rounded up). A spell belongs to
 * one or more traditions; the casting class's tradition gates which it can use.
 * Full ORC coverage + a move to bundled JSON is a follow-up. ORC content.
 */
data class PfSpell(
    val id: String,
    val name: String,
    val rank: Int,
    val traditions: List<SpellTradition>,
    /** Action cost: "1", "2", "3", "R" (reaction), or "1 min". */
    val actions: String,
    val traits: List<String> = emptyList(),
    val summary: String = "",
    /** Focus spells are cast from the focus pool, not slots. */
    val focus: Boolean = false,
) {
    val isCantrip: Boolean get() = rank == 0
    val rankLabel: String get() = if (rank == 0) "Cantrip" else "Rank $rank"
}

object PfSpells {
    val all: List<PfSpell> = listOf(
        // Cantrips (rank 0)
        PfSpell("daze", "Daze", 0, listOf(SpellTradition.ARCANE, SpellTradition.DIVINE, SpellTradition.OCCULT), "2", listOf("Cantrip", "Mental"), "Deal mental damage and possibly stun a creature."),
        PfSpell("electric-arc", "Electric Arc", 0, listOf(SpellTradition.ARCANE, SpellTradition.PRIMAL), "2", listOf("Cantrip", "Electricity"), "An arc of lightning leaps between one or two creatures."),
        PfSpell("shield", "Shield", 0, listOf(SpellTradition.ARCANE, SpellTradition.DIVINE, SpellTradition.OCCULT), "1", listOf("Cantrip"), "Raise a magical shield for +1 AC; can block damage and shatter."),
        PfSpell("telekinetic-projectile", "Telekinetic Projectile", 0, listOf(SpellTradition.ARCANE, SpellTradition.OCCULT), "2", listOf("Cantrip"), "Hurl an object for bludgeoning/piercing/slashing damage."),
        PfSpell("light", "Light", 0, listOf(SpellTradition.ARCANE, SpellTradition.DIVINE, SpellTradition.OCCULT, SpellTradition.PRIMAL), "2", listOf("Cantrip", "Light"), "An object sheds bright light in a 20-foot radius."),
        PfSpell("prestidigitation", "Prestidigitation", 0, listOf(SpellTradition.ARCANE, SpellTradition.DIVINE, SpellTradition.OCCULT, SpellTradition.PRIMAL), "2", listOf("Cantrip"), "A minor magical trick: cook, lift, make, or tidy."),
        // Rank 1
        PfSpell("magic-missile", "Magic Missile", 1, listOf(SpellTradition.ARCANE, SpellTradition.OCCULT), "1 to 3", listOf("Force"), "Unerring darts of force, one per action spent."),
        PfSpell("heal", "Heal", 1, listOf(SpellTradition.DIVINE, SpellTradition.PRIMAL), "1 to 3", listOf("Healing", "Vitality"), "Restore Hit Points by touch, at range, or in a burst."),
        PfSpell("fear", "Fear", 1, listOf(SpellTradition.ARCANE, SpellTradition.DIVINE, SpellTradition.OCCULT, SpellTradition.PRIMAL), "2", listOf("Emotion", "Fear", "Mental"), "Frighten a creature (Will save)."),
        PfSpell("grease", "Grease", 1, listOf(SpellTradition.ARCANE, SpellTradition.PRIMAL), "2", emptyList(), "Make a surface or object slippery."),
        PfSpell("bless", "Bless", 1, listOf(SpellTradition.DIVINE, SpellTradition.OCCULT), "2", listOf("Aura"), "Allies in the aura gain a +1 status bonus to attacks."),
        // Rank 2
        PfSpell("invisibility", "Invisibility", 2, listOf(SpellTradition.ARCANE, SpellTradition.OCCULT), "2", listOf("Illusion"), "A touched creature becomes invisible for 10 minutes."),
        PfSpell("mirror-image", "Mirror Image", 2, listOf(SpellTradition.ARCANE, SpellTradition.OCCULT), "2", listOf("Illusion"), "Illusory duplicates misdirect attacks against you."),
        PfSpell("see-the-unseen", "See the Unseen", 2, listOf(SpellTradition.ARCANE, SpellTradition.OCCULT), "2", listOf("Divination"), "See invisible creatures as concealed."),
        // Rank 3
        PfSpell("fireball", "Fireball", 3, listOf(SpellTradition.ARCANE, SpellTradition.PRIMAL), "2", listOf("Fire"), "A blast of fire deals 6d6 in a 20-foot burst (Reflex)."),
        PfSpell("haste", "Haste", 3, listOf(SpellTradition.ARCANE, SpellTradition.OCCULT, SpellTradition.PRIMAL), "2", emptyList(), "A target gains an extra action to Strike or Stride."),
    )

    val byTradition: Map<SpellTradition, List<PfSpell>> = SpellTradition.entries.associateWith { t -> all.filter { t in it.traditions } }

    fun by(id: String): PfSpell? = all.firstOrNull { it.id == id }
    fun forTradition(tradition: SpellTradition): List<PfSpell> = byTradition[tradition].orEmpty()
}

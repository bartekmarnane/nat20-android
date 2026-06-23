package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import kotlinx.serialization.Serializable

/**
 * The D&D 5e (2014) **feat** system (A11). Unlike the 2024 edition's tiered
 * feats, 2014 feats are a single optional pool taken **in place of an Ability
 * Score Improvement**. Some are "half-feats" that also grant a +1 to one of a
 * small set of abilities (the player allocates it on pick). Port of the iOS
 * `Feat` + `FeatCatalog` (the 29 PHB feats iOS ships).
 *
 * The deep parameterised mechanics (Magic Initiate's spell picks, Skill Expert's
 * expertise, Eldritch Adept's invocation, Metamagic Adept's metamagic, the
 * Touched feats' granted spell) are surfaced as takeable feats here; wiring their
 * *parameters* into play is the deep-catalogue follow-up. The clean always-on
 * riders (Tough's HP, Alert's initiative) fold into derived stats now.
 */
@Serializable
data class FeatPrerequisite(
    /** Meeting **any** listed threshold passes (matches the iOS `passes(for:)`). Empty ⇒ no ability gate. */
    val minimumAbilityScores: Map<Ability, Int> = emptyMap(),
    val requiresSpellcasting: Boolean = false,
) {
    fun passes(scores: AbilityScores, isSpellcaster: Boolean): Boolean {
        if (requiresSpellcasting && !isSpellcaster) return false
        if (minimumAbilityScores.isNotEmpty() && minimumAbilityScores.none { (a, t) -> scores.score(a) >= t }) return false
        return true
    }
}

@Serializable
data class Feat(
    val id: String,
    val name: String,
    val description: String,
    val prerequisite: FeatPrerequisite? = null,
    /**
     * For a half-feat: the abilities the +1 may be allocated to (empty ⇒ a full
     * feat with no ability bump). A feat that lets you choose *any* ability lists
     * all six.
     */
    val halfFeatAbilities: List<Ability> = emptyList(),
) {
    /** True when this feat grants a +1 ability increase the player allocates on pick. */
    val grantsAbilityIncrease: Boolean get() = halfFeatAbilities.isNotEmpty()

    /** Whether a character with [scores] (and optional spellcasting) may take this feat. */
    fun isAvailable(scores: AbilityScores, isSpellcaster: Boolean = false): Boolean =
        prerequisite?.passes(scores, isSpellcaster) ?: true
}

/** The bundled 2014 feat catalogue — the 29 feats the iOS app ships (PHB + the Tasha's pickups iOS carries). */
object Feats {
    private val ALL_ABILITIES = Ability.entries
    private val MENTAL = listOf(Ability.INTELLIGENCE, Ability.WISDOM, Ability.CHARISMA)

    val all: List<Feat> = listOf(
        feat("great-weapon-master", "Great Weapon Master", "On a crit or a kill, make a bonus-action melee attack; you can take −5 to hit for +10 damage with a heavy weapon."),
        feat("sharpshooter", "Sharpshooter", "Ignore ranged long-range Disadvantage and cover; you can take −5 to hit for +10 damage with a ranged weapon."),
        feat("lucky", "Lucky", "You have 3 luck points; spend one to roll an extra d20 on an attack, check, or save, or to make an attacker reroll."),
        feat("alert", "Alert", "You gain +5 to Initiative, can't be surprised while conscious, and don't grant attackers Advantage for being unseen."),
        feat("tough", "Tough", "Your hit point maximum increases by 2 for every character level."),
        feat("war-caster", "War Caster", "Advantage on concentration saves, cast with hands full, and cast a spell as an Opportunity Attack.", pre = spellcaster()),
        feat("polearm-master", "Polearm Master", "Bonus-action butt-end strike and Opportunity Attacks when a creature enters your reach with a polearm."),
        feat("crossbow-expert", "Crossbow Expert", "Ignore Loading, no Disadvantage in melee, and a bonus-action hand-crossbow shot."),
        feat("sentinel", "Sentinel", "Stop a foe's movement on an Opportunity Attack hit and punish attacks made against your allies."),
        feat("mobile", "Mobile", "Your Speed increases by 10 feet, you Dash through difficult terrain, and you avoid Opportunity Attacks from foes you hit."),
        feat("resilient", "Resilient", "Gain proficiency in one saving throw and a +1 to that ability.", half = ALL_ABILITIES),
        feat("skill-expert", "Skill Expert", "Gain one skill proficiency, Expertise in one skill, and a +1 ability increase.", half = ALL_ABILITIES),
        feat("telekinetic", "Telekinetic", "Learn Mage Hand, shove a creature 5 feet as a bonus action, and gain +1 to a mental ability.", half = MENTAL),
        feat("telepathic", "Telepathic", "Speak telepathically, cast Detect Thoughts once per day, and gain +1 to a mental ability.", half = MENTAL),
        feat("magic-initiate", "Magic Initiate", "Learn two cantrips and one 1st-level spell from a chosen class's spell list."),
        feat("fey-touched", "Fey Touched", "Learn Misty Step and one 1st-level divination/enchantment spell, and gain +1 to a mental ability.", half = MENTAL),
        feat("shadow-touched", "Shadow Touched", "Learn Invisibility and one 1st-level illusion/necromancy spell, and gain +1 to a mental ability.", half = MENTAL),
        feat("athlete", "Athlete", "Stand from prone cheaply, climb at full speed, and gain +1 to Strength or Dexterity.", half = listOf(Ability.STRENGTH, Ability.DEXTERITY)),
        feat("gunner", "Gunner", "Proficiency with firearms, ignore Loading, no Disadvantage in melee, and +1 Dexterity.", half = listOf(Ability.DEXTERITY)),
        feat("eldritch-adept", "Eldritch Adept", "Learn one Eldritch Invocation of your choice.", pre = spellcaster()),
        feat("metamagic-adept", "Metamagic Adept", "Learn two Metamagic options and gain 2 sorcery points to fuel them.", pre = spellcaster()),
        feat("inspiring-leader", "Inspiring Leader", "Spend 10 minutes to grant up to six allies temporary hit points equal to your level + CHA modifier.", pre = ability(Ability.CHARISMA to 13)),
        feat("observant", "Observant", "Read lips, gain +5 to passive Perception and Investigation, and +1 to Intelligence or Wisdom.", half = listOf(Ability.INTELLIGENCE, Ability.WISDOM)),
        feat("tavern-brawler", "Tavern Brawler", "Proficiency with improvised weapons, a d4 unarmed strike, a bonus-action grapple, and +1 Strength or Constitution.", half = listOf(Ability.STRENGTH, Ability.CONSTITUTION)),
        feat("dungeon-delver", "Dungeon Delver", "Advantage to detect secret doors and on saves vs traps, resistance to trap damage, and faster searching."),
        feat("dual-wielder", "Dual Wielder", "+1 AC while wielding two weapons, two-weapon fighting with non-light weapons, and draw two weapons at once."),
        feat("defensive-duelist", "Defensive Duelist", "When wielding a finesse weapon, use your reaction to add your Proficiency Bonus to AC against one melee attack.", pre = ability(Ability.DEXTERITY to 13)),
        feat("heavily-armored", "Heavily Armored", "Proficiency with Heavy armor and +1 Strength. (Requires Medium-armor proficiency.)", half = listOf(Ability.STRENGTH)),
        feat("heavy-armor-master", "Heavy Armor Master", "While in Heavy armor, reduce nonmagical bludgeoning/piercing/slashing damage by 3, and gain +1 Strength. (Requires Heavy-armor proficiency.)", half = listOf(Ability.STRENGTH)),
    )

    private val byId = all.associateBy { it.id }
    fun feat(id: String): Feat? = byId[id]

    /** The feats a character may take given their scores + whether they cast spells. */
    fun available(scores: AbilityScores, isSpellcaster: Boolean): List<Feat> = all.filter { it.isAvailable(scores, isSpellcaster) }

    private fun feat(id: String, name: String, description: String, pre: FeatPrerequisite? = null, half: List<Ability> = emptyList()) =
        Feat(id, name, description, pre, half)
    private fun spellcaster() = FeatPrerequisite(requiresSpellcasting = true)
    private fun ability(vararg pairs: Pair<Ability, Int>) = FeatPrerequisite(minimumAbilityScores = pairs.toMap())
}

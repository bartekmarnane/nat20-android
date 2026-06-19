package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import kotlinx.serialization.Serializable

/**
 * The D&D 5e (2024) **tiered feat** system (A21). Feats are no longer one flat
 * optional pool — every feat carries a [FeatCategory2024] with its own access
 * rule: Origin feats come from backgrounds at level 1; General feats replace an
 * ASI from level 4; Fighting Style feats are granted by martial features; Epic
 * Boons unlock at level 19. Many General feats are "half-feats" that also grant
 * a +1 ability bump. Port of the iOS `Feat2024` + `Feats2024`.
 */
@Serializable
enum class FeatCategory2024(val displayName: String, val minimumLevel: Int) {
    ORIGIN("Origin", 1),
    GENERAL("General", 4),
    FIGHTING_STYLE("Fighting Style", 1),
    EPIC_BOON("Epic Boon", 19),
}

/** Eligibility gate for a feat: meeting *any* ability threshold passes; plus optional level/spellcasting gates. */
@Serializable
data class FeatPrerequisite2024(
    val minimumAbilityScores: Map<Ability, Int> = emptyMap(),
    val minimumLevel: Int? = null,
    val requiresSpellcasting: Boolean = false,
)

/** A 2024 feat. Unlike the 2014 feat, every one is tiered and many include a built-in +1 ability increase. */
@Serializable
data class Feat2024(
    val id: String,
    val name: String,
    val description: String,
    val category: FeatCategory2024,
    val prerequisite: FeatPrerequisite2024? = null,
    /** True when the feat includes a +1 ability-score increase the player allocates on pick (half-feat). */
    val grantsAbilityIncrease: Boolean = false,
) {
    /** Whether a character of [level] with [scores] (and optional spellcasting) may take this feat. */
    fun isAvailable(level: Int, scores: AbilityScores, isSpellcaster: Boolean = false): Boolean {
        val floor = prerequisite?.minimumLevel ?: category.minimumLevel
        if (level < floor) return false
        val p = prerequisite ?: return true
        if (p.minimumAbilityScores.isNotEmpty() && p.minimumAbilityScores.none { (a, t) -> scores.score(a) >= t }) return false
        if (p.requiresSpellcasting && !isSpellcaster) return false
        return true
    }
}

/** The bundled 2024 feat catalogue (PHB-representative; full coverage is a follow-up). */
object Feats2024 {
    val all: List<Feat2024> = listOf(
        // ── Origin feats (level 1, via background or Human's Versatile trait) ──
        origin("magic-initiate-cleric", "Magic Initiate (Cleric)", "Learn two cleric cantrips and one 1st-level cleric spell."),
        origin("magic-initiate-wizard", "Magic Initiate (Wizard)", "Learn two wizard cantrips and one 1st-level wizard spell."),
        origin("magic-initiate-druid", "Magic Initiate (Druid)", "Learn two druid cantrips and one 1st-level druid spell."),
        origin("savage-attacker", "Savage Attacker", "Once per turn, reroll a melee weapon's damage dice and use either total."),
        origin("alert", "Alert", "Add your Proficiency Bonus to Initiative, and swap Initiative with a willing ally."),
        origin("tough", "Tough", "Your HP maximum increases by twice your character level."),
        origin("crafter", "Crafter", "Tool proficiencies, cheaper purchases, and faster crafting of gear."),
        origin("healer", "Healer", "Use a Healer's Kit to restore hit points as a Utilize action."),
        origin("lucky", "Lucky", "Spend Luck Points to gain Advantage on a d20 Test or impose Disadvantage on an attacker."),
        origin("musician", "Musician", "Proficiency with three musical instruments; grant Heroic Inspiration after a rest."),
        origin("skilled", "Skilled", "Gain proficiency in any three skills or tools of your choice."),
        origin("tavern-brawler", "Tavern Brawler", "Reroll unarmed/improvised damage, push on a hit, and unarmed strikes deal more."),
        // ── General feats (level 4+, replace an ASI) ──
        general("ability-score-improvement", "Ability Score Improvement", "Increase one ability score by 2, or two ability scores by 1 each (max 20)."),
        general("great-weapon-master", "Great Weapon Master", "Heavy weapon damage bonus and a bonus-action attack on a crit or kill.", min(Ability.STRENGTH to 13), halfFeat = true),
        general("sharpshooter", "Sharpshooter", "Ignore ranged Disadvantage at long range and cover; trade accuracy for damage.", min(Ability.DEXTERITY to 13), halfFeat = true),
        general("grappler", "Grappler", "On an unarmed hit you can damage and grapple; gain Advantage attacking a creature you grapple.", min(Ability.STRENGTH to 13, Ability.DEXTERITY to 13), halfFeat = true),
        general("war-caster", "War Caster", "Advantage on concentration saves, cast with hands full, and cast as an Opportunity Attack.", requiresSpellcasting = true, halfFeat = true),
        general("resilient", "Resilient", "Gain proficiency in one saving throw and a +1 to that ability.", halfFeat = true),
        general("skill-expert", "Skill Expert", "Gain a skill proficiency, Expertise in one skill, and a +1 ability increase.", halfFeat = true),
        general("mobile", "Mobile", "Your Speed increases by 10 feet and difficult terrain doesn't slow your Dash."),
        general("sentinel", "Sentinel", "Stop a foe's movement on an Opportunity Attack hit and punish attacks on your allies."),
        general("polearm-master", "Polearm Master", "Bonus-action butt-end strike and Opportunity Attacks when foes enter your reach.", min(Ability.STRENGTH to 13, Ability.DEXTERITY to 13), halfFeat = true),
        general("crossbow-expert", "Crossbow Expert", "Ignore Loading, no Disadvantage in melee, and a bonus-action hand-crossbow shot.", min(Ability.DEXTERITY to 13), halfFeat = true),
        general("dual-wielder", "Dual Wielder", "Draw two weapons at once and wield heavier one-handed weapons in each hand.", min(Ability.STRENGTH to 13, Ability.DEXTERITY to 13), halfFeat = true),
        general("shield-master", "Shield Master", "Shove with your shield as a bonus action and add it to Dexterity saves.", min(Ability.STRENGTH to 13), halfFeat = true),
        general("heavy-armor-master", "Heavy Armor Master", "Reduce bludgeoning, piercing, and slashing damage while in Heavy armor.", min(Ability.STRENGTH to 13), halfFeat = true),
        general("inspiring-leader", "Inspiring Leader", "Spend 10 minutes to grant allies temporary hit points.", halfFeat = true),
        general("mage-slayer", "Mage Slayer", "Punish nearby casters and gain advantage on saves against their spells.", halfFeat = true),
        general("elemental-adept", "Elemental Adept", "Your spells ignore resistance to a chosen damage type and treat 1s as 2s.", requiresSpellcasting = true, halfFeat = true),
        general("lucky-general", "Lucky", "Bank Luck Points to gain Advantage or impose Disadvantage at higher tiers."),
        // ── Fighting Style feats (granted by a class's Fighting Style feature) ──
        fightingStyle("archery", "Archery", "You gain a +2 bonus to attack rolls you make with ranged weapons."),
        fightingStyle("defense", "Defense", "While wearing Light, Medium, or Heavy armor, you gain a +1 bonus to Armor Class."),
        fightingStyle("dueling", "Dueling", "When wielding a melee weapon in one hand and no other weapon, +2 damage."),
        fightingStyle("great-weapon-fighting", "Great Weapon Fighting", "Treat any 1 or 2 on a damage die as a 3 for a two-handed melee weapon."),
        fightingStyle("two-weapon-fighting", "Two-Weapon Fighting", "Add your ability modifier to the damage of your off-hand attack with a Light weapon."),
        fightingStyle("protection", "Protection", "Impose Disadvantage on an attack against a creature within 5 feet of you."),
        fightingStyle("blind-fighting", "Blind Fighting", "You have Blindsight with a range of 10 feet."),
        fightingStyle("thrown-weapon-fighting", "Thrown Weapon Fighting", "Draw a thrown weapon as part of the attack and add +2 to its damage."),
        // ── Epic Boons (level 19+, in place of an ASI) ──
        epicBoon("boon-of-combat-prowess", "Boon of Combat Prowess", "When you miss with an attack roll, you can hit instead. Once per turn."),
        epicBoon("boon-of-dimensional-travel", "Boon of Dimensional Travel", "After you take the Attack or Magic action, teleport up to 30 feet."),
        epicBoon("boon-of-fate", "Boon of Fate", "When a creature within 60 feet succeeds or fails a d20 Test, add or subtract 2d4. Once per turn."),
        epicBoon("boon-of-irresistible-offense", "Boon of Irresistible Offense", "Your weapon damage ignores Resistance; extra damage on a natural 20."),
        epicBoon("boon-of-truesight", "Boon of Truesight", "You have Truesight with a range of 60 feet."),
    )

    private val byId = all.associateBy { it.id }
    fun feat(id: String): Feat2024? = byId[id]
    fun inCategory(category: FeatCategory2024): List<Feat2024> = all.filter { it.category == category }

    private fun min(vararg pairs: Pair<Ability, Int>) = pairs.toMap()
    private fun origin(id: String, name: String, desc: String) = Feat2024(id, name, desc, FeatCategory2024.ORIGIN)
    private fun general(id: String, name: String, desc: String, abilityMins: Map<Ability, Int> = emptyMap(), requiresSpellcasting: Boolean = false, halfFeat: Boolean = false) =
        Feat2024(id, name, desc, FeatCategory2024.GENERAL, (abilityMins.takeIf { it.isNotEmpty() } != null || requiresSpellcasting).let { if (it) FeatPrerequisite2024(abilityMins, requiresSpellcasting = requiresSpellcasting) else null }, halfFeat)
    private fun fightingStyle(id: String, name: String, desc: String) = Feat2024(id, name, desc, FeatCategory2024.FIGHTING_STYLE)
    private fun epicBoon(id: String, name: String, desc: String) = Feat2024(id, name, desc, FeatCategory2024.EPIC_BOON)
}

/**
 * A player's choice at an ASI level — either bump ability scores (+2/one or
 * +1/two) or take a General feat in its place. Captured so above-level-1
 * creation and level-ups produce mechanically complete characters.
 */
@Serializable
sealed interface AdvancementChoice2024 {
    @Serializable
    data class AbilityScoreImprovement(val bumps: Map<Ability, Int>) : AdvancementChoice2024
    @Serializable
    data class Feat(val featId: String) : AdvancementChoice2024

    /** Whether this is a legal choice at [level] given the pre-advancement [scores]. */
    fun isValid(level: Int, scores: AbilityScores): Boolean = when (this) {
        is AbilityScoreImprovement ->
            bumps.values.sum() == 2 && bumps.values.all { it in 1..2 } && bumps.all { (a, d) -> scores.score(a) + d <= 20 }
        is Feat -> Feats2024.feat(featId)?.let { it.category == FeatCategory2024.GENERAL && it.isAvailable(level, scores) } ?: false
    }
}

package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.pf2e.core.AbilityBuild
import au.com.evonet.nat20.pf2e.core.AdvancementSchedule
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.SpellcastingProgression

/**
 * Assembles a [PathfinderPayload] from creation-wizard choices (A22): resolves
 * the ability scores through the PF2e boost/flaw build, computes HP
 * (`ancestry HP + class HP/level + CON mod`), folds the class + background +
 * subclass proficiencies, writes the day-one spell/feat/equipment picks, seeds
 * the GM Core starting wealth, and — for above-level-1 creation — replays
 * levels 2..[Choices.targetLevel] through the same [PfLevelMath] the Level Up
 * flow uses. Pure logic so it's unit-testable independent of the UI.
 */
object PathfinderBuilder {
    data class Choices(
        val name: String,
        val ancestryId: String,
        val heritage: String?,
        val backgroundId: String,
        val classId: String,
        val keyAbility: PfAbility,
        /** Free ability boosts the ancestry grants (distinct). */
        val ancestryFreeBoosts: List<PfAbility> = emptyList(),
        /** The chosen background boost (one of its two options) + the background's free boost. */
        val backgroundBoost: PfAbility? = null,
        val backgroundFreeBoost: PfAbility? = null,
        /** The four final free boosts (distinct). */
        val freeBoosts: List<PfAbility> = emptyList(),
        /** Class skill proficiencies the player picked. */
        val chosenSkills: List<PfSkill> = emptyList(),
        /** The level the character is created at (1..20); levels 2+ replay [PfLevelMath]. */
        val targetLevel: Int = 1,
        /** The class's defining subclass pick ([PfSubclass] id); required when the class has one. */
        val subclass: String? = null,
        /** Day-one spell picks (caster classes) — cantrips + rank-1 repertoire, by [PfSpells] id. */
        val cantrips: List<String> = emptyList(),
        val rank1Spells: List<String> = emptyList(),
        /** The level-1 ancestry + class feat picks, by [PfFeats] id. */
        val ancestryFeat: String? = null,
        val classFeat: String? = null,
        /** Advancement choices for levels 2..targetLevel: boosts at 5/10/15/20, skill increases at odd levels ≥3. */
        val advancementBoosts: Map<Int, List<PfAbility>> = emptyMap(),
        val skillIncreases: Map<Int, PfSkill> = emptyMap(),
        /** Feats picked against the slots earned along the way, keyed by (level, type). */
        val advancementFeats: Map<FeatSlotKey, String> = emptyMap(),
        /** Starting equipment picks (catalogue ids); non-proficient armor/weapons are dropped. */
        val armor: String? = null,
        val shield: String? = null,
        val weapons: List<String> = emptyList(),
    )

    /** The ability build implied by the choices (kept so the result is re-editable + testable). */
    fun abilityBuild(choices: Choices): AbilityBuild {
        val ancestry = PathfinderCatalog.ancestry(choices.ancestryId)
        val background = PathfinderCatalog.background(choices.backgroundId)
        val steps = mutableListOf<List<PfAbility>>()
        // 1. Ancestry: fixed boosts + free boosts.
        (ancestry?.boosts.orEmpty() + choices.ancestryFreeBoosts).takeIf { it.isNotEmpty() }?.let { steps += it }
        // 2. Background: chosen boost + free boost.
        listOfNotNull(choices.backgroundBoost ?: background?.boostOptions?.firstOrNull(), choices.backgroundFreeBoost)
            .takeIf { it.isNotEmpty() }?.let { steps += it }
        // 3. Class: the key ability.
        steps += listOf(choices.keyAbility)
        // 4. Four free boosts.
        choices.freeBoosts.takeIf { it.isNotEmpty() }?.let { steps += it }
        return AbilityBuild(flaws = listOfNotNull(ancestry?.flaw), boostSteps = steps)
    }

    fun scores(choices: Choices): PfAbilityScores = abilityBuild(choices).resolved()

    /** Number of trained skills the class grants (class count + INT modifier), before the background skill. */
    fun skillSlots(choices: Choices): Int {
        val cls = PathfinderCatalog.pfClass(choices.classId) ?: return 0
        val intMod = PfAbilityScores.modifier(scores(choices).intelligence)
        return cls.trainedSkills + maxOf(0, intMod)
    }

    fun build(choices: Choices): PathfinderPayload {
        val ancestry = PathfinderCatalog.ancestry(choices.ancestryId)
        val cls = PathfinderCatalog.pfClass(choices.classId)
        val background = PathfinderCatalog.background(choices.backgroundId)
        val subclass = cls?.subclasses?.firstOrNull { it.id == choices.subclass }
        val scores = scores(choices)
        val conMod = PfAbilityScores.modifier(scores.constitution)
        val maxHp = (ancestry?.hp ?: 8) + (cls?.hpPerLevel ?: 8) + conMod
        val targetLevel = choices.targetLevel.coerceIn(PathfinderPayload.MIN_LEVEL, PathfinderPayload.MAX_LEVEL)

        val skills = buildMap {
            background?.let { put(it.trainedSkill, Proficiency.TRAINED) }
            subclass?.grantedSkill?.let { put(it, Proficiency.TRAINED) }
            choices.chosenSkills.forEach { put(it, Proficiency.TRAINED) }
        }

        // Spell picks: valid catalogue spells of the right rank + tradition, capped at the day-one quotas.
        val tradition = cls?.tradition
        val cantrips = if (tradition == null) emptyList() else choices.cantrips
            .mapNotNull(PfSpells::by).filter { it.rank == 0 && tradition in it.traditions }
            .map { it.id }.distinct().take(SpellcastingProgression.CANTRIPS_PER_DAY)
        val rank1Cap = SpellcastingProgression.fullCasterSlots(targetLevel)[1] ?: 0
        val rank1 = if (tradition == null) emptyList() else choices.rank1Spells
            .mapNotNull(PfSpells::by).filter { it.rank == 1 && tradition in it.traditions }
            .map { it.id }.distinct().take(rank1Cap)

        // Feats in earn order — ancestry, class, then the advancement slots by level.
        val feats = buildList {
            listOfNotNull(choices.ancestryFeat, choices.classFeat).forEach { id -> PfFeats.by(id)?.let { add(it.id) } }
            choices.advancementFeats.entries
                .sortedWith(compareBy({ it.key.level }, { it.key.type.ordinal }))
                .forEach { (_, id) -> PfFeats.by(id)?.let { add(it.id) } }
        }.distinct()

        // Equipment: silently drop armor/weapons the class isn't proficient with (mirrors iOS).
        fun armorProficient(a: PfArmor) = (cls?.armorProf?.get(a.category) ?: Proficiency.UNTRAINED) != Proficiency.UNTRAINED
        fun weaponProficient(w: PfWeapon) = (cls?.weaponProf?.get(w.category) ?: Proficiency.UNTRAINED) != Proficiency.UNTRAINED
        val armor = choices.armor?.let(PfArmors::by)?.takeIf(::armorProficient)?.id
        val shield = choices.shield?.let(PfShields::by)?.id
        val weapons = choices.weapons.mapNotNull(PfWeapons::by).filter(::weaponProficient).map { it.id }.distinct()

        var payload = PathfinderPayload(
            ancestry = choices.ancestryId,
            heritage = choices.heritage,
            background = choices.backgroundId,
            className = choices.classId,
            subclass = subclass?.id,
            level = 1,
            abilityScores = scores,
            keyAbility = choices.keyAbility,
            maxHp = maxHp, currentHp = maxHp,
            perception = cls?.perception ?: Proficiency.TRAINED,
            unarmoredProficiency = cls?.unarmored ?: Proficiency.TRAINED,
            classDC = cls?.classDC ?: Proficiency.TRAINED,
            saves = cls?.saves.orEmpty(),
            skills = skills,
            loreSkills = background?.let { mapOf(it.lore to Proficiency.TRAINED) }.orEmpty(),
            speed = ancestry?.speed ?: 25,
            armorProficiencies = cls?.armorProf.orEmpty(),
            weaponProficiencies = cls?.weaponProf.orEmpty(),
            armor = armor, shield = shield, weapons = weapons,
            feats = feats,
            coins = mapOf(PFCoin.GP to Wealth.startingWealthGP(targetLevel)),
            // Casters: tradition + the casting ability (= the class key ability for the seed classes),
            // trained spell proficiency, full level-1 slots + cantrips.
            spellTradition = tradition,
            castingAbility = if (tradition != null) choices.keyAbility else null,
            spellProficiency = if (tradition != null) Proficiency.TRAINED else Proficiency.UNTRAINED,
            cantrips = cantrips,
            knownSpells = if (rank1.isEmpty()) emptyMap() else mapOf(1 to rank1),
        ).let { if (it.isCaster) it.withFullSpellSlots() else it }

        // Levels 2..target replay the exact Level Up maths; illegal picks are dropped, not thrown.
        for (level in 2..targetLevel) {
            val boosts = if (AdvancementSchedule.grantsAbilityBoosts(level)) {
                choices.advancementBoosts[level].orEmpty().distinct().take(AdvancementSchedule.BOOSTS_PER_ABILITY_BOOST_LEVEL)
            } else {
                emptyList()
            }
            val skillIncrease = choices.skillIncreases[level]?.takeIf { skill ->
                AdvancementSchedule.grantsSkillIncrease(level) &&
                    (payload.skills[skill] ?: Proficiency.UNTRAINED).next
                        ?.let { it.rank <= AdvancementSchedule.maxSkillRank(level).rank } == true
            }
            payload = PfLevelMath.advance(payload, skillIncrease, boosts).first
        }
        return payload
    }
}

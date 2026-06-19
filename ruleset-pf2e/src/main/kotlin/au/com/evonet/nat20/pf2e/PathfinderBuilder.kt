package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.pf2e.core.AbilityBuild
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency

/**
 * Assembles a level-1 [PathfinderPayload] from creation-wizard choices (A22):
 * resolves the ability scores through the PF2e boost/flaw build, computes HP
 * (`ancestry HP + class HP/level + CON mod`), and folds the class + background
 * proficiencies. Pure logic so it's unit-testable independent of the UI.
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
        val scores = scores(choices)
        val conMod = PfAbilityScores.modifier(scores.constitution)
        val maxHp = (ancestry?.hp ?: 8) + (cls?.hpPerLevel ?: 8) + conMod

        val skills = buildMap {
            background?.let { put(it.trainedSkill, Proficiency.TRAINED) }
            choices.chosenSkills.forEach { put(it, Proficiency.TRAINED) }
        }
        return PathfinderPayload(
            ancestry = choices.ancestryId,
            heritage = choices.heritage,
            background = choices.backgroundId,
            className = choices.classId,
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
        )
    }
}

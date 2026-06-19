package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.domain.CharacterPayload
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save
import au.com.evonet.nat20.pf2e.core.ValuedCondition
import kotlinx.serialization.Serializable

/**
 * D&D-free at last: a Pathfinder 2e (Remaster) character. The first non-D&D
 * ruleset (`rulesetID "pf2e-remaster"`). The structural departures from the 5e
 * payloads are front and centre:
 *
 * - **Per-statistic proficiency ranks.** Perception, every save, every skill,
 *   the class DC, and (later) armor/weapons each carry their own [Proficiency]
 *   on the five-rank ladder, rather than one shared proficiency bonus.
 * - **Hero Points & the dying track.** PF2e's metacurrency and its dying/wounded
 *   counters are first-class character state.
 * - **Valued conditions.** [ValuedCondition]s carry live numbers (Frightened 2).
 *
 * This is the **foundation slice** (A22): identity, abilities, HP + the dying
 * track, Hero Points, perception/saves/skills/class-DC proficiencies, and
 * conditions. Spells, equipment + runes, feats, and the action economy are
 * follow-up slices (mirrors the iOS `PathfinderPayload`, trimmed). Derived stats
 * live in [PathfinderStats].
 */
@Serializable
data class PathfinderPayload(
    val ancestry: String = "",
    val heritage: String? = null,
    val background: String? = null,
    val className: String = "",
    val level: Int = 1,
    val abilityScores: PfAbilityScores = PfAbilityScores(),
    /** Drives the class DC + (later) spell attack/DC; null until a class is chosen. */
    val keyAbility: PfAbility? = null,
    val maxHp: Int = MINIMUM_HP,
    val currentHp: Int = maxHp,
    val temporaryHp: Int = 0,
    /** Dying value (0 = not dying). At dying [DYING_MAX] the character dies. */
    val dying: Int = 0,
    /** Wounded value — raises the dying value you return to when knocked out. */
    val wounded: Int = 0,
    val heroPoints: Int = 1,
    val perception: Proficiency = Proficiency.UNTRAINED,
    /** Unarmored defense proficiency — drives unarmored AC for this slice. */
    val unarmoredProficiency: Proficiency = Proficiency.UNTRAINED,
    val saves: Map<Save, Proficiency> = emptyMap(),
    val skills: Map<PfSkill, Proficiency> = emptyMap(),
    /** Lore skills keyed by subtype string ("Warfare Lore"), all Intelligence-based. */
    val loreSkills: Map<String, Proficiency> = emptyMap(),
    val classDC: Proficiency = Proficiency.UNTRAINED,
    val conditions: List<ValuedCondition> = emptyList(),
    val speed: Int = 25,
    val alignmentOrEdicts: String? = null,
) : CharacterPayload {

    val isDying: Boolean get() = dying in 1 until DYING_MAX
    val isDead: Boolean get() = dying >= DYING_MAX

    companion object {
        const val MIN_LEVEL: Int = 1
        const val MAX_LEVEL: Int = 20
        const val MINIMUM_HP: Int = 1
        /** Dying 4 = death (PF2e). */
        const val DYING_MAX: Int = 4
        /** A character starts each day with 1 Hero Point; the table cap is 3. */
        const val HERO_POINT_MAX: Int = 3
    }
}

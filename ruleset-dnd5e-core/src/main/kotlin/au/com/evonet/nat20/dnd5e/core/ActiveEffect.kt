package au.com.evonet.nat20.dnd5e.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * The unified in-play modifier model (A17): spells, items, and class features
 * fan out into typed [ActiveEffect]s with a source, a duration, and a list of
 * [EffectModifier]s. Derived stats (AC, ability scores, saves, skills,
 * resistances, attack/damage) fold these in live; rests + concentration are the
 * authoritative auto-cancel triggers. Port of the iOS `DnD5eCore/ActiveEffect`.
 *
 * All types are `@Serializable` sealed hierarchies (closed polymorphism), so
 * they round-trip through the payload codec without extra registration.
 */
@Serializable
data class ActiveEffect(
    val id: String,
    /** Display name for chips + journal entries ("Shield", "Rage"). */
    val name: String,
    /** Where this effect came from — drives auto-cancel + links back to the source. */
    val source: EffectSource,
    /** Typed modifier list (multiple per effect are normal — Bless buffs attacks AND saves). */
    val modifiers: List<EffectModifier>,
    val duration: EffectDuration,
    /** True ⇒ ends automatically when concentration drops (one concentration spell at a time). */
    val concentrationOwner: Boolean = false,
) {
    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

/** Where an [ActiveEffect] originated. */
@Serializable
sealed interface EffectSource {
    @Serializable @SerialName("spell") data class Spell(val id: String) : EffectSource
    @Serializable @SerialName("item") data class Item(val id: String) : EffectSource
    @Serializable @SerialName("feature") data class Feature(val id: String) : EffectSource
    @Serializable @SerialName("custom") data object Custom : EffectSource
    @Serializable @SerialName("externalCaster") data class ExternalCaster(val name: String, val spellID: String? = null) : EffectSource
}

/** How long an effect lasts (real-time decay isn't enforced; rests + concentration drive cancel). */
@Serializable
sealed interface EffectDuration {
    @Serializable @SerialName("untilCancelled") data object UntilCancelled : EffectDuration
    @Serializable @SerialName("concentration") data object Concentration : EffectDuration
    @Serializable @SerialName("untilRest") data class UntilRest(val rest: RestKind) : EffectDuration
    @Serializable @SerialName("rounds") data class Rounds(val count: Int) : EffectDuration
}

@Serializable
enum class RestKind { SHORT, LONG }

/** A single typed modifier an effect applies. Dice bonuses store the static average (Bless +1d4 → +2). */
@Serializable
sealed interface EffectModifier {
    @Serializable @SerialName("acBonus") data class AcBonus(val value: Int) : EffectModifier
    @Serializable @SerialName("acOverride") data class AcOverride(val formula: ACOverrideFormula) : EffectModifier
    @Serializable @SerialName("abilityDelta") data class AbilityDelta(val ability: Ability, val value: Int) : EffectModifier
    @Serializable @SerialName("abilitySet") data class AbilitySet(val ability: Ability, val value: Int) : EffectModifier
    /** Save bonus; null [ability] = all saves. */
    @Serializable @SerialName("saveBonus") data class SaveBonus(val ability: Ability? = null, val value: Int) : EffectModifier
    @Serializable @SerialName("attackBonus") data class AttackBonus(val value: Int) : EffectModifier
    @Serializable @SerialName("damageBonus") data class DamageBonus(val value: Int) : EffectModifier
    @Serializable @SerialName("skillBonus") data class SkillBonus(val skillId: String, val value: Int) : EffectModifier
    @Serializable @SerialName("damageResistance") data class DamageResistance(val type: String) : EffectModifier
    @Serializable @SerialName("advantageOn") data class AdvantageOn(val descriptor: String) : EffectModifier
    @Serializable @SerialName("condition") data class Condition(val name: String) : EffectModifier
    @Serializable @SerialName("freeText") data class FreeText(val text: String) : EffectModifier
}

/** How an `.acOverride` computes the unarmored base AC. */
@Serializable
sealed interface ACOverrideFormula {
    /** `base + DEX` — Mage Armor (base 13). */
    @Serializable @SerialName("baseDex") data class BaseDex(val base: Int) : ACOverrideFormula
    /** `base + DEX + ability` — Barbarian (10+DEX+CON), Monk (10+DEX+WIS). */
    @Serializable @SerialName("baseDexAbility")
    data class BaseDexAbility(val base: Int, val ability: Ability, val requires: ACOverrideRequirement = ACOverrideRequirement.NONE) : ACOverrideFormula

    val requirement: ACOverrideRequirement
        get() = when (this) {
            is BaseDex -> ACOverrideRequirement.NONE
            is BaseDexAbility -> requires
        }
}

@Serializable
enum class ACOverrideRequirement { NONE, NO_SHIELD }

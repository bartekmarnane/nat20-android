package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.ACOverrideFormula
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.ClassResourceCatalog
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.EffectSource
import au.com.evonet.nat20.dnd5e.core.RestKind

/**
 * The effect catalogues (A17): spell / class-feature / item ids → an
 * [ActiveEffect] template the producing intent applies. Port of the iOS
 * `SpellEffectCatalog` / `ClassFeatureEffectCatalog` / `ItemEffectCatalog`.
 * Dice bonuses store the static average (Bless +1d4 → +2). Coverage spans the
 * common buffs/debuffs; the long tail is added entry-by-entry.
 */

/** Who a spell effect lands on when cast. */
enum class EffectScope {
    /** Always applies to the caster (Shield, Mage Armor on self). */
    ALWAYS_SELF,
    /** A caster-anchored rider that always applies (Hex, Hunter's Mark). */
    CASTER_RIDER,
    /** A buff cast on others; only self-applies when the caster opts in (Bless, Enhance Ability). */
    TARGET_PICKED,
}

object SpellEffectCatalog {
    data class Template(
        val name: String,
        val modifiers: List<EffectModifier>,
        val duration: EffectDuration,
        val concentrationOwner: Boolean,
        val scope: EffectScope,
    ) {
        fun resolve(spellId: String): ActiveEffect = ActiveEffect(
            id = ActiveEffect.newId(),
            name = name,
            source = EffectSource.Spell(spellId),
            modifiers = modifiers,
            duration = duration,
            concentrationOwner = concentrationOwner,
        )
    }

    private val templates: Map<String, Template> = mapOf(
        "bless" to Template("Bless", listOf(EffectModifier.AttackBonus(2), EffectModifier.SaveBonus(null, 2)), EffectDuration.Concentration, true, EffectScope.TARGET_PICKED),
        "bane" to Template("Bane", listOf(EffectModifier.AttackBonus(-2), EffectModifier.SaveBonus(null, -2)), EffectDuration.Concentration, true, EffectScope.TARGET_PICKED),
        "shield" to Template("Shield", listOf(EffectModifier.AcBonus(5)), EffectDuration.Rounds(1), false, EffectScope.ALWAYS_SELF),
        "shield-of-faith" to Template("Shield of Faith", listOf(EffectModifier.AcBonus(2)), EffectDuration.Concentration, true, EffectScope.TARGET_PICKED),
        "mage-armor" to Template("Mage Armor", listOf(EffectModifier.AcOverride(ACOverrideFormula.BaseDex(13))), EffectDuration.UntilRest(RestKind.LONG), false, EffectScope.TARGET_PICKED),
        "barkskin" to Template("Barkskin", listOf(EffectModifier.FreeText("AC can't be less than 16")), EffectDuration.Concentration, true, EffectScope.TARGET_PICKED),
        "hex" to Template("Hex", listOf(EffectModifier.DamageBonus(3), EffectModifier.FreeText("Disadvantage on the chosen ability")), EffectDuration.Concentration, true, EffectScope.CASTER_RIDER),
        "hunters-mark" to Template("Hunter's Mark", listOf(EffectModifier.DamageBonus(3)), EffectDuration.Concentration, true, EffectScope.CASTER_RIDER),
        "divine-favor" to Template("Divine Favor", listOf(EffectModifier.DamageBonus(2)), EffectDuration.Concentration, true, EffectScope.CASTER_RIDER),
        "haste" to Template("Haste", listOf(EffectModifier.AcBonus(2), EffectModifier.AdvantageOn("DEX saves"), EffectModifier.FreeText("Double speed, extra action")), EffectDuration.Concentration, true, EffectScope.TARGET_PICKED),
        "stoneskin" to Template("Stoneskin", listOf(EffectModifier.DamageResistance("bludgeoning"), EffectModifier.DamageResistance("piercing"), EffectModifier.DamageResistance("slashing"), EffectModifier.FreeText("Nonmagical only")), EffectDuration.Concentration, true, EffectScope.TARGET_PICKED),
        "protection-from-energy" to Template("Protection from Energy", listOf(EffectModifier.FreeText("Resistance to the chosen damage type")), EffectDuration.Concentration, true, EffectScope.TARGET_PICKED),
        "resistance" to Template("Resistance", listOf(EffectModifier.SaveBonus(null, 1)), EffectDuration.Concentration, true, EffectScope.TARGET_PICKED),
        "guidance" to Template("Guidance", listOf(EffectModifier.SkillBonus("__any__", 2)), EffectDuration.Concentration, true, EffectScope.TARGET_PICKED),
        "enhance-ability" to Template("Enhance Ability", listOf(EffectModifier.AdvantageOn("checks with the chosen ability")), EffectDuration.Concentration, true, EffectScope.TARGET_PICKED),
        "enlarge" to Template("Enlarge", listOf(EffectModifier.DamageBonus(2), EffectModifier.AdvantageOn("STR checks and saves")), EffectDuration.Concentration, true, EffectScope.TARGET_PICKED),
    )

    fun template(spellId: String): Template? = templates[spellId.lowercase()]
}

object ClassFeatureEffectCatalog {
    data class Template(
        val name: String,
        val modifiers: List<EffectModifier>,
        val duration: EffectDuration,
        val concentrationOwner: Boolean,
    ) {
        fun resolve(featureId: String): ActiveEffect = ActiveEffect(
            id = ActiveEffect.newId(),
            name = name,
            source = EffectSource.Feature(featureId),
            modifiers = modifiers,
            duration = duration,
            concentrationOwner = concentrationOwner,
        )
    }

    private val templates: Map<String, Template> = mapOf(
        "rage" to Template(
            "Rage",
            listOf(
                EffectModifier.DamageBonus(2), // overridden to the level-correct tier in UseClassFeature
                EffectModifier.DamageResistance("bludgeoning"),
                EffectModifier.DamageResistance("piercing"),
                EffectModifier.DamageResistance("slashing"),
                EffectModifier.AdvantageOn("STR checks and saves"),
            ),
            EffectDuration.UntilCancelled,
            false,
        ),
        "hunters-mark" to Template("Hunter's Mark", listOf(EffectModifier.DamageBonus(3)), EffectDuration.Concentration, true),
        "aura-of-protection" to Template("Aura of Protection", listOf(EffectModifier.SaveBonus(null, 3), EffectModifier.FreeText("Allies within 10 ft too")), EffectDuration.UntilCancelled, false),
    )

    fun template(featureId: String): Template? = templates[featureId.lowercase()]

    /** Rage's damage bonus scales by barbarian level; swap the static +2 for the live tier. */
    fun withRageDamage(effect: ActiveEffect, barbarianLevel: Int): ActiveEffect {
        val tier = ClassResourceCatalog.rageDamageBonus(barbarianLevel)
        return effect.copy(modifiers = effect.modifiers.map { if (it is EffectModifier.DamageBonus) EffectModifier.DamageBonus(tier) else it })
    }
}

object ItemEffectCatalog {
    private val templates: Map<String, ActiveEffect> = mapOf(
        "potion-of-giant-strength" to ActiveEffect(
            id = "", name = "Giant Strength", source = EffectSource.Item("potion-of-giant-strength"),
            modifiers = listOf(EffectModifier.AbilitySet(Ability.STRENGTH, 21)), duration = EffectDuration.UntilRest(RestKind.LONG),
        ),
        "potion-of-heroism" to ActiveEffect(
            id = "", name = "Heroism", source = EffectSource.Item("potion-of-heroism"),
            modifiers = listOf(EffectModifier.SaveBonus(null, 0), EffectModifier.FreeText("10 temp HP, immune to fear")), duration = EffectDuration.UntilRest(RestKind.LONG),
        ),
    )

    fun effectFor(catalogueId: String?): ActiveEffect? =
        catalogueId?.let { templates[it] }?.copy(id = ActiveEffect.newId())
}

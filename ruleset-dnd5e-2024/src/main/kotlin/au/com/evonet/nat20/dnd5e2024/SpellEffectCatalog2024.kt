package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.dnd5e.core.ACOverrideFormula
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.EffectSource
import au.com.evonet.nat20.dnd5e.core.RestKind

/** Who a 2024 spell effect lands on when cast (mirrors the 2014 scope rules). */
enum class EffectScope2024 { ALWAYS_SELF, CASTER_RIDER, TARGET_PICKED }

/** SRD 5.2 spell → [ActiveEffect] templates the cast intent applies (A21). Reuses the `-core` effect model. */
object SpellEffectCatalog2024 {
    data class Template(
        val name: String,
        val modifiers: List<EffectModifier>,
        val duration: EffectDuration,
        val concentrationOwner: Boolean,
        val scope: EffectScope2024,
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
        "bless" to Template("Bless", listOf(EffectModifier.AttackBonus(2), EffectModifier.SaveBonus(null, 2)), EffectDuration.Concentration, true, EffectScope2024.TARGET_PICKED),
        "bane" to Template("Bane", listOf(EffectModifier.AttackBonus(-2), EffectModifier.SaveBonus(null, -2)), EffectDuration.Concentration, true, EffectScope2024.TARGET_PICKED),
        "shield" to Template("Shield", listOf(EffectModifier.AcBonus(5)), EffectDuration.Rounds(1), false, EffectScope2024.ALWAYS_SELF),
        "shield-of-faith" to Template("Shield of Faith", listOf(EffectModifier.AcBonus(2)), EffectDuration.Concentration, true, EffectScope2024.TARGET_PICKED),
        "mage-armor" to Template("Mage Armor", listOf(EffectModifier.AcOverride(ACOverrideFormula.BaseDex(13))), EffectDuration.UntilRest(RestKind.LONG), false, EffectScope2024.TARGET_PICKED),
        "hex" to Template("Hex", listOf(EffectModifier.DamageBonus(3)), EffectDuration.Concentration, true, EffectScope2024.CASTER_RIDER),
        "hunters-mark" to Template("Hunter's Mark", listOf(EffectModifier.DamageBonus(3)), EffectDuration.Concentration, true, EffectScope2024.CASTER_RIDER),
        "divine-favor" to Template("Divine Favor", listOf(EffectModifier.DamageBonus(2)), EffectDuration.Concentration, true, EffectScope2024.CASTER_RIDER),
        "haste" to Template("Haste", listOf(EffectModifier.AcBonus(2), EffectModifier.AdvantageOn("DEX saves"), EffectModifier.FreeText("Double speed, extra action")), EffectDuration.Concentration, true, EffectScope2024.TARGET_PICKED),
        "stoneskin" to Template("Stoneskin", listOf(EffectModifier.DamageResistance("bludgeoning"), EffectModifier.DamageResistance("piercing"), EffectModifier.DamageResistance("slashing")), EffectDuration.Concentration, true, EffectScope2024.TARGET_PICKED),
        "guidance" to Template("Guidance", listOf(EffectModifier.SkillBonus("__any__", 2)), EffectDuration.Concentration, true, EffectScope2024.TARGET_PICKED),
    )

    fun template(spellId: String): Template? = templates[spellId.lowercase()]
}

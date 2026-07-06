package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.ACOverrideFormula
import au.com.evonet.nat20.dnd5e.core.ACOverrideRequirement
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.EffectSource

/**
 * Folds [DnD5ePayload.activeEffects] (plus the passive class effects like
 * Barbarian / Monk Unarmored Defense) into the character's live stats — ability
 * scores, saves, skills, resistances, attack/damage, and AC. Port of the iOS
 * `DnD5ePayload` effect accessors + `PassiveClassEffectCatalog`.
 */

/** Every effect that contributes to derived stats: the live ones plus passive class + race + fighting-style effects. */
val DnD5ePayload.allEffects: List<ActiveEffect>
    get() = activeEffects + passiveClassEffects() + RaceTraits.passiveEffects(race) + passiveStyleEffects()

/** The Defense fighting style's always-on +1 AC while wearing armor (folds through the AC calculator). */
fun DnD5ePayload.passiveStyleEffects(): List<ActiveEffect> = buildList {
    val armored = inventory.any { it.equipped && it.kind == ItemKind.ARMOR }
    if ("defense" in fightingStyles && armored) {
        add(
            ActiveEffect(
                id = "passive:style:defense",
                name = "Defense",
                source = EffectSource.Feature("fighting-style:defense"),
                modifiers = listOf(EffectModifier.AcBonus(1)),
                duration = au.com.evonet.nat20.dnd5e.core.EffectDuration.UntilCancelled,
            ),
        )
    }
}

/** Skill proficiencies the character has, including those auto-granted by a race trait (Elf Keen Senses). */
val DnD5ePayload.effectiveSkillProficiencies: List<String>
    get() = (selectedSkills + RaceTraits.grantedSkills(race)).distinct()

/**
 * Extra max HP per character level from always-on feats: Tough (+2/level). Stored
 * [DnD5ePayload.maxHp] is the *base*; this folds in via [effectiveMaxHp] so a
 * single rule drives every HP clamp + display (mirrors the 2024 edition).
 */
val DnD5ePayload.bonusMaxHpPerLevel: Int
    get() = if ("tough" in chosenFeats) 2 else 0

/** The character's real max HP — stored base plus the per-level feat riders. */
val DnD5ePayload.effectiveMaxHp: Int
    get() = maxHp + bonusMaxHpPerLevel * level

/**
 * Effective walking speed in feet, resolved from the catalogue race (dwarves 25,
 * wood elf 35, most others 30). Falls back to 30 when the race is unset or unknown.
 */
val DnD5ePayload.effectiveSpeed: Int
    get() = DnD5eCatalog.race(race)?.speed ?: 30

/**
 * Synthesises the always-on class effects that ride the same modifier pipeline:
 * Barbarian Unarmored Defense (10 + DEX + CON) and Monk Unarmored Defense
 * (10 + DEX + WIS, no shield). Only relevant while unarmored — the AC calculator
 * gates the override on having no armor equipped.
 */
fun DnD5ePayload.passiveClassEffects(): List<ActiveEffect> = buildList {
    for (entry in classes) {
        when (entry.classId.lowercase()) {
            "barbarian" -> add(
                unarmoredDefense("barbarian", "Unarmored Defense", ACOverrideFormula.BaseDexAbility(10, Ability.CONSTITUTION, ACOverrideRequirement.NONE)),
            )
            "monk" -> add(
                unarmoredDefense("monk", "Unarmored Defense", ACOverrideFormula.BaseDexAbility(10, Ability.WISDOM, ACOverrideRequirement.NO_SHIELD)),
            )
        }
    }
}

private fun unarmoredDefense(classId: String, name: String, formula: ACOverrideFormula): ActiveEffect = ActiveEffect(
    id = "passive:$classId:unarmored-defense",
    name = name,
    source = EffectSource.Feature("$classId-unarmored-defense"),
    modifiers = listOf(EffectModifier.AcOverride(formula)),
    duration = au.com.evonet.nat20.dnd5e.core.EffectDuration.UntilCancelled,
)

/** Effective ability score = base + summed deltas, then floored by the highest `.abilitySet` (Giant Strength). */
fun DnD5ePayload.effectiveScore(ability: Ability): Int {
    val base = abilityScores.score(ability)
    var delta = 0
    var setValue: Int? = null
    for (effect in activeEffects) {
        for (modifier in effect.modifiers) {
            when (modifier) {
                is EffectModifier.AbilityDelta -> if (modifier.ability == ability) delta += modifier.value
                is EffectModifier.AbilitySet -> if (modifier.ability == ability) setValue = maxOf(setValue ?: Int.MIN_VALUE, modifier.value)
                else -> {}
            }
        }
    }
    val withDelta = base + delta
    return if (setValue != null) maxOf(setValue, withDelta) else withDelta
}

/** Effective scores across all six abilities (for the medallions / modifiers). */
val DnD5ePayload.effectiveAbilityScores: AbilityScores
    get() = Ability.entries.fold(abilityScores) { scores, ability -> scores.with(ability, effectiveScore(ability)) }

/**
 * A complete saving-throw modifier for [ability]: effective ability mod +
 * proficiency (only the primary/first class grants save proficiencies in 5e) +
 * any effect bonuses. Used by the Stats tab and the concentration check.
 */
fun DnD5ePayload.savingThrowBonus(ability: Ability): Int {
    val proficient = ability in (
        DnD5eCatalog.characterClass(classes.firstOrNull()?.classId ?: "")?.savingThrowAbilities().orEmpty()
        )
    val prof = if (proficient) au.com.evonet.nat20.dnd5e.core.Proficiency.bonus(level) else 0
    return effectiveAbilityScores.modifier(ability) + prof + temporarySaveBonus(ability)
}

/** Net save bonus from effects for [ability] — both ability-scoped and all-saves (null) modifiers contribute. */
fun DnD5ePayload.temporarySaveBonus(ability: Ability): Int =
    activeEffects.sumOf { effect ->
        effect.modifiers.sumOf { modifier ->
            if (modifier is EffectModifier.SaveBonus && (modifier.ability == null || modifier.ability == ability)) modifier.value else 0
        }
    }

/** Net per-skill bonus from effects (Guidance, Enhance Ability). */
fun DnD5ePayload.temporarySkillBonus(skillId: String): Int =
    activeEffects.sumOf { effect ->
        effect.modifiers.sumOf { modifier ->
            if (modifier is EffectModifier.SkillBonus && modifier.skillId == skillId) modifier.value else 0
        }
    }

/** Flat attack-roll bonus from effects (Bless +2, Rage doesn't add to attack — only damage). */
val DnD5ePayload.effectAttackBonus: Int
    get() = activeEffects.sumOf { e -> e.modifiers.sumOf { if (it is EffectModifier.AttackBonus) it.value else 0 } }

/** Flat damage bonus from effects (Rage, Hex, Hunter's Mark, Divine Favor). */
val DnD5ePayload.effectDamageBonus: Int
    get() = activeEffects.sumOf { e -> e.modifiers.sumOf { if (it is EffectModifier.DamageBonus) it.value else 0 } }

/** Damage types the character resists, lower-cased — from active effects **and** innate race traits (A19). */
val DnD5ePayload.effectiveDamageResistances: Set<String>
    get() = allEffects.flatMap { e -> e.modifiers.mapNotNull { (it as? EffectModifier.DamageResistance)?.type?.trim()?.lowercase()?.takeIf { t -> t.isNotEmpty() } } }.toSet()

/** Free-text advantage descriptors from effects, for display (Rage: "STR checks and saves"). */
val DnD5ePayload.advantageDescriptors: List<String>
    get() = activeEffects.flatMap { e -> e.modifiers.mapNotNull { (it as? EffectModifier.AdvantageOn)?.descriptor } }

/** Conditions an effect imposes (Greater Invisibility → Invisible), for the condition fold. */
val DnD5ePayload.effectImposedConditions: List<String>
    get() = activeEffects.flatMap { e -> e.modifiers.mapNotNull { (it as? EffectModifier.Condition)?.name } }

/**
 * Every condition affecting the character: the manually-tracked [activeConditions]
 * plus any imposed by an active effect (A17 condition hook). Effect-sourced ones
 * clear automatically when the effect ends, so they aren't stored in
 * [activeConditions]. De-duplicated case-insensitively, manual entries first.
 */
val DnD5ePayload.effectiveConditions: List<String>
    get() = (activeConditions + effectImposedConditions)
        .distinctBy { it.lowercase() }

/** Free-text notes from effects, surfaced on the sheet for rules the engine doesn't model mechanically. */
val DnD5ePayload.effectFreeText: List<String>
    get() = activeEffects.flatMap { e -> e.modifiers.mapNotNull { (it as? EffectModifier.FreeText)?.text } }

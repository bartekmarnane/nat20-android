package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.dnd5e.core.DeathSaves
import au.com.evonet.nat20.dnd5e.core.Exhaustion
import au.com.evonet.nat20.dnd5e.core.FeatureUseEntry
import au.com.evonet.nat20.domain.CharacterPayload
import kotlinx.serialization.Serializable

/**
 * D&D 5e character data. This is the **minimal A3 slice** — race, classes,
 * the six abilities, and HP. The shipped iOS payload carries far more
 * (spells, inventory, effects, conditions, coins, rest state, …); those
 * fields arrive with the steps that need them (A7+/A10). Greenfield, so no
 * legacy-migration decoders — only the current schema.
 *
 * [level] and [characterClass] are derived from [classes] (no stored
 * duplication), matching the iOS computed properties.
 */
@Serializable
data class DnD5ePayload(
    val race: String = "",
    val classes: List<ClassEntry> = emptyList(),
    val abilityScores: AbilityScores = AbilityScores(),
    val maxHp: Int = MINIMUM_MAX_HP,
    /** Defaults to full health (= [maxHp]) for a fresh character. */
    val currentHp: Int = maxHp,
    /**
     * Temporary HP — a separate pool that absorbs damage before [currentHp]
     * and doesn't stack (a higher value replaces, otherwise discarded).
     */
    val temporaryHp: Int = 0,
    /** Background id (A8); resolved through [DnD5eCatalog.background] at display. */
    val background: String = "",
    /** Skill ids the character is proficient in (background-granted + class picks). */
    val selectedSkills: List<String> = emptyList(),
    /** Alignment label, e.g. "Lawful Good"; null until chosen. */
    val alignment: String? = null,
    /** Carried items (weapons, armor, gear, potions); equipped ones drive AC (A10). */
    val inventory: List<InventoryItem> = emptyList(),
    /** Purse keyed by denomination; a missing key means zero of that coin. */
    val coins: Map<Coin, Int> = emptyMap(),
    /** Cantrip ids known (flat across all classes — cantrips never consume slots). */
    val cantripsKnown: List<String> = emptyList(),
    /** Known spell ids per class id (known casters: Bard/Sorcerer/Warlock/Ranger + Wizard's spellbook). */
    val spellsKnown: Map<String, List<String>> = emptyMap(),
    /** Prepared spell ids per class id (prepared casters: Wizard/Cleric/Druid/Paladin). */
    val preparedSpells: Map<String, List<String>> = emptyMap(),
    /** Remaining regular spell slots by level; a missing/zero level means none left. */
    val currentSpellSlots: Map<Int, Int> = emptyMap(),
    /** Remaining warlock pact slots (refresh on a short rest). */
    val currentPactSlots: Int = 0,
    /**
     * Hit dice already spent (0 = a fresh, fully-rested pool). Stored as "spent"
     * rather than "remaining" so the default needs no reference to [level], which
     * is itself derived. [currentHitDice] / [maxHitDice] expose the usable view.
     */
    val hitDiceSpent: Int = 0,
    /**
     * Point-pool resources (Ki, Sorcery Points, Lay on Hands) keyed by pool id →
     * remaining value. A missing key means "full" (max comes from the catalogue
     * at the character's class level), so a fresh/just-rested character stores
     * nothing and reads as full.
     */
    val resourcePools: Map<String, Int> = emptyMap(),
    /**
     * Use-counter class features (Rage, Action Surge, …) keyed by feature id →
     * remaining + recovery recorded at use time. A missing key means "full".
     */
    val classFeatureUses: Map<String, FeatureUseEntry> = emptyMap(),
    /** Whether the character currently holds Inspiration (the DM-granted reroll). */
    val hasInspiration: Boolean = false,
    /** Death-save tracker; only meaningful while downed at 0 HP. Cleared on a long rest / heal. */
    val deathSaves: DeathSaves = DeathSaves.cleared,
    /** Active condition names (standard + house-rule), in application order. */
    val activeConditions: List<String> = emptyList(),
    /** Exhaustion track 0–6 (level 6 = death); a long rest sheds one level. */
    val exhaustionLevel: Int = 0,
    /** This encounter's rolled initiative; null when not in an encounter. */
    val initiative: Int? = null,
    /** In-play modifiers (spell/item/feature effects + DM buffs); folded into derived stats (A17). */
    val activeEffects: List<ActiveEffect> = emptyList(),
    /** The spell/feature the character is concentrating on; null = not concentrating. */
    val concentratingOn: String? = null,
    /** Half-Orc Relentless Endurance: spent when it keeps the character at 1 HP; resets on a long rest. */
    val relentlessEnduranceUsed: Boolean = false,
    /** Optional content sources enabled for this character (supplement toggles, A19). Empty = SRD/PHB only. */
    val enabledSources: List<String> = emptyList(),
    /** Feat ids taken in place of an Ability Score Improvement (A11). Resolved through [Feats]. */
    val chosenFeats: List<String> = emptyList(),
    /** Fighting Style ids chosen (Fighter L1, Paladin/Ranger L2). Resolved through [FightingStyles]. */
    val fightingStyles: List<String> = emptyList(),
) : CharacterPayload {

    /** Total character level = sum of all class entry levels, floored at 1. */
    val level: Int
        get() = maxOf(MIN_LEVEL, classes.sumOf { it.level })

    /** Primary class id (the first entry), or "" if no class chosen yet. */
    val characterClass: String
        get() = classes.firstOrNull()?.classId ?: ""

    /** Armor Class from equipped inventory (10 + DEX when unarmored). */
    val armorClass: Int
        get() = ArmorClassCalculator.armorClass(this)

    /** Total hit dice = character level (one die per level). */
    val maxHitDice: Int
        get() = level

    /** Hit dice still available to spend (max minus spent, floored at 0). */
    val currentHitDice: Int
        get() = maxOf(0, maxHitDice - hitDiceSpent)

    /** Downed at 0 HP and still rolling death saves (not yet stable or dead). */
    val isDying: Boolean
        get() = currentHp == 0 && !deathSaves.isStable && !deathSaves.isDead

    /** Dead — three death-save failures or six levels of exhaustion. */
    val isDead: Boolean
        get() = deathSaves.isDead || Exhaustion.isFatal(exhaustionLevel)

    companion object {
        const val MIN_LEVEL: Int = 1
        const val MAX_LEVEL: Int = 20
        const val MINIMUM_MAX_HP: Int = 1
    }
}

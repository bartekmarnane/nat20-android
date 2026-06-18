package au.com.evonet.nat20.dnd5e.core

import kotlinx.serialization.Serializable

/**
 * Edition-agnostic 5e spell-slot machinery — the caster progressions and the
 * per-level slot tables. Port of the iOS `DnD5eCore/SpellSlotTables.swift`.
 *
 * This layer is deliberately `ClassEntry`-free (that type lives in the 2014
 * module): it works on a [CastingProgression] + level, plus pure class-id
 * string helpers. The multiclass combination over a character's class list and
 * the subclass-gated (Eldritch Knight / Arcane Trickster) progression live in
 * the 2014 module's `Spellcasting`, which composes these primitives.
 */
@Serializable
enum class CastingProgression {
    FULL, HALF, THIRD, WARLOCK, NONE;

    /** Warlock pact slots refresh on a short rest; everything else waits for a long rest. */
    val restoresOnShortRest: Boolean get() = this == WARLOCK

    companion object {
        /** The progression for a base class id (subclass-gated casters resolve to [NONE] here). */
        fun forClass(classId: String): CastingProgression = when (classId.lowercase()) {
            "bard", "cleric", "druid", "sorcerer", "wizard" -> FULL
            "paladin", "ranger" -> HALF
            "warlock" -> WARLOCK
            else -> NONE
        }

        /** Prepared casters (Wizard/Cleric/Druid/Paladin) choose a daily list; others cast from known. */
        fun usesPreparation(classId: String): Boolean = when (classId.lowercase()) {
            "wizard", "cleric", "druid", "paladin" -> true
            else -> false
        }

        /** The spellcasting ability for a base class id (null for non-casters). */
        fun spellcastingAbility(classId: String): Ability? = when (classId.lowercase()) {
            "wizard" -> Ability.INTELLIGENCE
            "cleric", "druid", "ranger" -> Ability.WISDOM
            "sorcerer", "warlock", "bard", "paladin" -> Ability.CHARISMA
            else -> null
        }

        /**
         * Daily prepared-spell count: Wizard/Cleric/Druid = level + ability mod;
         * Paladin = level/2 + ability mod; floored at 1. Known casters return 0
         * (they have no prepare step).
         */
        fun preparedSpellLimit(classId: String, level: Int, castingAbilityMod: Int): Int =
            when (classId.lowercase()) {
                "wizard", "cleric", "druid" -> maxOf(1, level + castingAbilityMod)
                "paladin" -> maxOf(1, level / 2 + castingAbilityMod)
                else -> 0
            }
    }
}

/** The 5e spell-slot tables: progression + level → slots-by-level. */
object SpellSlotTable {

    /** Slots available at [level] under [progression] (warlock returns its single pact level). */
    fun slots(progression: CastingProgression, level: Int): Map<Int, Int> {
        val lvl = level.coerceIn(1, 20)
        return when (progression) {
            CastingProgression.FULL -> FULL_CASTER[lvl - 1]
            CastingProgression.HALF -> HALF_CASTER[lvl - 1]
            CastingProgression.THIRD -> THIRD_CASTER[lvl - 1]
            CastingProgression.WARLOCK -> {
                val count = warlockSlotCount(lvl)
                val slotLevel = warlockSlotLevel(lvl)
                if (count > 0 && slotLevel > 0) mapOf(slotLevel to count) else emptyMap()
            }
            CastingProgression.NONE -> emptyMap()
        }
    }

    /** Warlock pact-slot count: 1 / 2 / 3 / 4 at levels 1 / 2–10 / 11–16 / 17–20. */
    fun warlockSlotCount(level: Int): Int = when (level.coerceIn(1, 20)) {
        1 -> 1
        in 2..10 -> 2
        in 11..16 -> 3
        else -> 4
    }

    /** Warlock pact-slot level: 1 / 2 / 3 / 4 / 5 at levels 1–2 / 3–4 / 5–6 / 7–8 / 9+. */
    fun warlockSlotLevel(level: Int): Int = when (level.coerceIn(1, 20)) {
        in 1..2 -> 1
        in 3..4 -> 2
        in 5..6 -> 3
        in 7..8 -> 4
        else -> 5
    }

    // Full caster (Bard/Cleric/Druid/Sorcerer/Wizard), PHB pg 113, L1–L20.
    private val FULL_CASTER: List<Map<Int, Int>> = listOf(
        mapOf(1 to 2),
        mapOf(1 to 3),
        mapOf(1 to 4, 2 to 2),
        mapOf(1 to 4, 2 to 3),
        mapOf(1 to 4, 2 to 3, 3 to 2),
        mapOf(1 to 4, 2 to 3, 3 to 3),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 2),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2, 6 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2, 6 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2, 6 to 1, 7 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2, 6 to 1, 7 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2, 6 to 1, 7 to 1, 8 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2, 6 to 1, 7 to 1, 8 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2, 6 to 1, 7 to 1, 8 to 1, 9 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 1, 7 to 1, 8 to 1, 9 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 2, 7 to 1, 8 to 1, 9 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 2, 7 to 2, 8 to 1, 9 to 1),
    )

    // Half caster (Paladin/Ranger), L1–L20 — no slots at L1.
    private val HALF_CASTER: List<Map<Int, Int>> = listOf(
        emptyMap(),
        mapOf(1 to 2),
        mapOf(1 to 3),
        mapOf(1 to 3),
        mapOf(1 to 4, 2 to 2),
        mapOf(1 to 4, 2 to 2),
        mapOf(1 to 4, 2 to 3),
        mapOf(1 to 4, 2 to 3),
        mapOf(1 to 4, 2 to 3, 3 to 2),
        mapOf(1 to 4, 2 to 3, 3 to 2),
        mapOf(1 to 4, 2 to 3, 3 to 3),
        mapOf(1 to 4, 2 to 3, 3 to 3),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 2),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 2),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2),
    )

    // Third caster (Eldritch Knight / Arcane Trickster), L1–L20 — no slots until L3.
    private val THIRD_CASTER: List<Map<Int, Int>> = listOf(
        emptyMap(),
        emptyMap(),
        mapOf(1 to 2),
        mapOf(1 to 3),
        mapOf(1 to 3),
        mapOf(1 to 3),
        mapOf(1 to 4, 2 to 2),
        mapOf(1 to 4, 2 to 2),
        mapOf(1 to 4, 2 to 2),
        mapOf(1 to 4, 2 to 3),
        mapOf(1 to 4, 2 to 3),
        mapOf(1 to 4, 2 to 3),
        mapOf(1 to 4, 2 to 3, 3 to 2),
        mapOf(1 to 4, 2 to 3, 3 to 2),
        mapOf(1 to 4, 2 to 3, 3 to 2),
        mapOf(1 to 4, 2 to 3, 3 to 3),
        mapOf(1 to 4, 2 to 3, 3 to 3),
        mapOf(1 to 4, 2 to 3, 3 to 3),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 1),
        mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 1),
    )
}

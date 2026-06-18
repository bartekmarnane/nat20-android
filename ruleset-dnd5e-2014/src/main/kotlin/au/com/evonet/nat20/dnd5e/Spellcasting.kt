package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.CastingProgression
import au.com.evonet.nat20.dnd5e.core.SpellSlotTable

/**
 * The `ClassEntry`-aware spellcasting math, composing the `-core` slot tables
 * over a (possibly multiclass) character's class list. Port of the iOS
 * `DnD5ePayload` spell helpers + the subclass-gated progression that
 * `SpellSlotTables.swift` keeps next to the tables.
 *
 * Multiclass rule (PHB pg 164): full casters contribute their full level, half
 * casters `level / 2`, third casters `level / 3`; these sum into a single
 * caster level read against the **full**-caster table. Warlock pact magic is
 * tracked entirely separately (it never joins the combined level).
 */
object Spellcasting {

    /** The progression for a class line, resolving Eldritch Knight / Arcane Trickster to [CastingProgression.THIRD]. */
    fun progression(entry: ClassEntry): CastingProgression {
        val base = CastingProgression.forClass(entry.classId)
        if (base != CastingProgression.NONE) return base
        val sub = entry.subclass?.lowercase()?.replace('-', ' ')?.trim() ?: return CastingProgression.NONE
        val classId = entry.classId.lowercase()
        val isEK = classId == "fighter" && sub == "eldritch knight"
        val isAT = classId == "rogue" && sub == "arcane trickster"
        return if (isEK || isAT) CastingProgression.THIRD else CastingProgression.NONE
    }

    /** The spellcasting ability for a class line (third casters use Intelligence). */
    fun spellcastingAbility(entry: ClassEntry): Ability? =
        CastingProgression.spellcastingAbility(entry.classId)
            ?: if (progression(entry) == CastingProgression.THIRD) Ability.INTELLIGENCE else null

    /** Combined caster level across the class list (warlock excluded). */
    fun combinedCasterLevel(classes: List<ClassEntry>): Int = classes.sumOf { entry ->
        when (progression(entry)) {
            CastingProgression.FULL -> entry.level
            CastingProgression.HALF -> entry.level / 2
            CastingProgression.THIRD -> entry.level / 3
            CastingProgression.WARLOCK, CastingProgression.NONE -> 0
        }
    }

    /** Regular (non-pact) max slots, read off the full-caster table at the combined level. */
    fun combinedSpellSlots(classes: List<ClassEntry>): Map<Int, Int> {
        val casterLevel = combinedCasterLevel(classes)
        return if (casterLevel > 0) SpellSlotTable.slots(CastingProgression.FULL, casterLevel) else emptyMap()
    }

    /** Total warlock levels across the class list. */
    fun warlockClassLevel(classes: List<ClassEntry>): Int =
        classes.filter { progression(it) == CastingProgression.WARLOCK }.sumOf { it.level }

    fun maxPactSlots(classes: List<ClassEntry>): Int {
        val warlockLevel = warlockClassLevel(classes)
        return if (warlockLevel > 0) SpellSlotTable.warlockSlotCount(warlockLevel) else 0
    }

    fun pactSlotLevel(classes: List<ClassEntry>): Int {
        val warlockLevel = warlockClassLevel(classes)
        return if (warlockLevel > 0) SpellSlotTable.warlockSlotLevel(warlockLevel) else 0
    }
}

// ── Payload-level derived spell state ─────────────────────────────────────────

/** Class lines that can cast (progression ≠ NONE), in payload order. */
val DnD5ePayload.spellcastingClasses: List<ClassEntry>
    get() = classes.filter { Spellcasting.progression(it) != CastingProgression.NONE }

/** True if any class line is a spellcaster. */
val DnD5ePayload.isSpellcaster: Boolean get() = spellcastingClasses.isNotEmpty()

/** Maximum regular (non-pact) spell slots by level. */
val DnD5ePayload.maxSpellSlots: Map<Int, Int> get() = Spellcasting.combinedSpellSlots(classes)

/** Maximum warlock pact slots (0 for non-warlocks). */
val DnD5ePayload.maxPactSlots: Int get() = Spellcasting.maxPactSlots(classes)

/** The single level all pact slots cast at (0 for non-warlocks). */
val DnD5ePayload.pactSlotLevel: Int get() = Spellcasting.pactSlotLevel(classes)

/** UI-friendly remaining slots: regular remaining + remaining pact merged at the pact level. */
val DnD5ePayload.totalCurrentSlots: Map<Int, Int>
    get() {
        val merged = currentSpellSlots.toMutableMap()
        val level = pactSlotLevel
        if (level > 0 && currentPactSlots > 0) {
            merged[level] = (merged[level] ?: 0) + currentPactSlots
        }
        return merged
    }

/** UI-friendly max slots: regular max + pact max merged at the pact level. */
val DnD5ePayload.totalMaxSlots: Map<Int, Int>
    get() {
        val merged = maxSpellSlots.toMutableMap()
        val level = pactSlotLevel
        val maxPact = maxPactSlots
        if (level > 0 && maxPact > 0) {
            merged[level] = (merged[level] ?: 0) + maxPact
        }
        return merged
    }

/**
 * Every spell id the character can currently cast: all known cantrips, plus the
 * prepared list for prepared casters and the known list for known casters. (No
 * legacy flat `spellsKnownL1` bucket — Android is greenfield.)
 */
val DnD5ePayload.castableSpellIDs: Set<String>
    get() {
        val ids = cantripsKnown.toMutableSet()
        for (entry in spellcastingClasses) {
            if (CastingProgression.usesPreparation(entry.classId)) {
                ids += preparedSpells[entry.classId].orEmpty()
            } else {
                ids += spellsKnown[entry.classId].orEmpty()
            }
        }
        return ids
    }

/** A copy with all spell slots (regular + pact) reset to full — used at creation and on a long rest. */
fun DnD5ePayload.withFullSpellSlots(): DnD5ePayload =
    copy(currentSpellSlots = maxSpellSlots, currentPactSlots = maxPactSlots)

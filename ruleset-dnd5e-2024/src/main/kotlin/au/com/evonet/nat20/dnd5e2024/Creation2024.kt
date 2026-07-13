package au.com.evonet.nat20.dnd5e2024

/**
 * Creation-time rules the 2024 wizard needs beyond the catalogues: which
 * classes grant a Fighting Style (and when), and how many cantrips / leveled
 * spells a fresh caster knows or prepares at a given level. Port of the iOS
 * `DnD5e2024EditorView` helpers.
 */

/** The 2024 Fighting Style feature: Fighter at level 1, Paladin/Ranger at level 2. */
object FightingStyle2024 {
    /** The level at which [classId] gains its Fighting Style; null when the class never does. */
    fun grantLevel(classId: String): Int? = when (classId.lowercase()) {
        "fighter" -> 1
        "paladin", "ranger" -> 2
        else -> null
    }
}

/**
 * How many spells a newly-created 2024 caster picks in the wizard — cantrips
 * known plus level-1 spells prepared. Simplified per-class quotas (2024 PHB
 * class tables), scaled by character level; presentation caps at what the
 * bundled catalogue actually offers.
 */
object Spellcasting2024Creation {
    /** Cantrips known at [level]: per-class base, +1 at level 4 and +1 at level 10 (casters only). */
    fun cantripsKnown(classId: String, level: Int): Int {
        val base = when (classId.lowercase()) {
            "sorcerer" -> 4
            "cleric", "wizard" -> 3
            "bard", "druid", "warlock" -> 2
            else -> 0 // paladin / ranger / non-casters
        }
        if (base == 0) return 0
        return base + (if (level >= 4) 1 else 0) + (if (level >= 10) 1 else 0)
    }

    /**
     * Level-1+ spells known/prepared at [level]: full casters start at 4
     * (sorcerer 2) and gain +1 per level above 1; half casters (paladin from 0,
     * ranger from 2) and the warlock (from 2) gain +1 per two levels above 1.
     */
    fun leveledSpellsKnown(classId: String, level: Int): Int {
        val id = classId.lowercase()
        val base = when (id) {
            "bard", "cleric", "druid", "wizard" -> 4
            "sorcerer", "warlock", "ranger" -> 2
            else -> 0 // paladin starts at 0; non-casters stay 0
        }
        val scale = when (id) {
            "bard", "cleric", "druid", "wizard", "sorcerer" -> level - 1
            "paladin", "ranger", "warlock" -> (level - 1) / 2
            else -> 0
        }
        return base + scale
    }
}

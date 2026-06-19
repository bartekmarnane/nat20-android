package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save

/**
 * The **class-specific** half of Pathfinder 2e advancement (A22): when each
 * class's proficiency ranks jump (weapon mastery, save expertise, spell
 * proficiency, armor/perception/class-DC). Bumping the character level already
 * scales every check (`bonus = level + rank`); this table raises the *rank*
 * itself at the class-defined levels. Port of the iOS `ClassAdvancement` tables,
 * verified against Archives of Nethys (Remaster).
 *
 * Grain note (matching the existing model): the `DEFENSE` track covers unarmored
 * + each armor category the class trains in; `WEAPONS` covers the simple +
 * martial categories together. PF2e's finer grain (advanced weapons, single
 * weapon group to legendary) isn't represented.
 */
enum class ProfTrack { PERCEPTION, FORTITUDE, REFLEX, WILL, CLASS_DC, SPELL, DEFENSE, WEAPONS }

data class ProfBump(val level: Int, val track: ProfTrack, val rank: Proficiency)

object ClassProgression {
    private fun bumps(vararg entries: Triple<Int, ProfTrack, Proficiency>): List<ProfBump> =
        entries.map { ProfBump(it.first, it.second, it.third) }

    private val T = Proficiency.EXPERT
    private val M = Proficiency.MASTER
    private val L = Proficiency.LEGENDARY

    private val tables: Map<String, List<ProfBump>> = mapOf(
        "fighter" to bumps(
            Triple(7, ProfTrack.PERCEPTION, M), Triple(9, ProfTrack.FORTITUDE, M), Triple(15, ProfTrack.REFLEX, M),
            Triple(3, ProfTrack.WILL, T), Triple(11, ProfTrack.CLASS_DC, T), Triple(19, ProfTrack.CLASS_DC, M),
            Triple(11, ProfTrack.DEFENSE, T), Triple(17, ProfTrack.DEFENSE, M),
            Triple(13, ProfTrack.WEAPONS, M), Triple(19, ProfTrack.WEAPONS, L),
        ),
        "rogue" to bumps(
            Triple(7, ProfTrack.PERCEPTION, M), Triple(13, ProfTrack.PERCEPTION, L), Triple(9, ProfTrack.FORTITUDE, T),
            Triple(7, ProfTrack.REFLEX, M), Triple(13, ProfTrack.REFLEX, L), Triple(17, ProfTrack.WILL, M),
            Triple(11, ProfTrack.CLASS_DC, T), Triple(19, ProfTrack.CLASS_DC, M),
            Triple(13, ProfTrack.DEFENSE, T), Triple(19, ProfTrack.DEFENSE, M),
            Triple(5, ProfTrack.WEAPONS, T), Triple(13, ProfTrack.WEAPONS, M),
        ),
        "ranger" to bumps(
            Triple(7, ProfTrack.PERCEPTION, M), Triple(15, ProfTrack.PERCEPTION, L), Triple(11, ProfTrack.FORTITUDE, M),
            Triple(7, ProfTrack.REFLEX, M), Triple(15, ProfTrack.REFLEX, L), Triple(3, ProfTrack.WILL, T),
            Triple(9, ProfTrack.CLASS_DC, T), Triple(17, ProfTrack.CLASS_DC, M),
            Triple(11, ProfTrack.DEFENSE, T), Triple(19, ProfTrack.DEFENSE, M), Triple(13, ProfTrack.WEAPONS, M),
        ),
        "champion" to bumps(
            Triple(11, ProfTrack.PERCEPTION, T), Triple(9, ProfTrack.FORTITUDE, M), Triple(9, ProfTrack.REFLEX, T),
            Triple(11, ProfTrack.WILL, M), Triple(9, ProfTrack.CLASS_DC, T), Triple(17, ProfTrack.CLASS_DC, M),
            Triple(9, ProfTrack.DEFENSE, M), Triple(15, ProfTrack.DEFENSE, L), Triple(9, ProfTrack.WEAPONS, M),
        ),
        "wizard" to bumps(
            Triple(11, ProfTrack.PERCEPTION, T), Triple(9, ProfTrack.FORTITUDE, T), Triple(5, ProfTrack.REFLEX, T),
            Triple(17, ProfTrack.WILL, M), Triple(7, ProfTrack.SPELL, T), Triple(15, ProfTrack.SPELL, M),
            Triple(19, ProfTrack.SPELL, L), Triple(13, ProfTrack.DEFENSE, T), Triple(11, ProfTrack.WEAPONS, T),
        ),
        "cleric" to bumps(
            Triple(5, ProfTrack.PERCEPTION, T), Triple(3, ProfTrack.FORTITUDE, T), Triple(11, ProfTrack.REFLEX, T),
            Triple(9, ProfTrack.WILL, M), Triple(7, ProfTrack.SPELL, T), Triple(15, ProfTrack.SPELL, M),
            Triple(19, ProfTrack.SPELL, L), Triple(13, ProfTrack.DEFENSE, T),
        ),
        "sorcerer" to bumps(
            Triple(11, ProfTrack.PERCEPTION, T), Triple(5, ProfTrack.FORTITUDE, T), Triple(9, ProfTrack.REFLEX, T),
            Triple(17, ProfTrack.WILL, M), Triple(7, ProfTrack.SPELL, T), Triple(15, ProfTrack.SPELL, M),
            Triple(19, ProfTrack.SPELL, L), Triple(13, ProfTrack.DEFENSE, T), Triple(11, ProfTrack.WEAPONS, T),
        ),
        "bard" to bumps(
            Triple(11, ProfTrack.PERCEPTION, M), Triple(9, ProfTrack.FORTITUDE, T), Triple(3, ProfTrack.REFLEX, T),
            Triple(9, ProfTrack.WILL, M), Triple(17, ProfTrack.WILL, L), Triple(7, ProfTrack.SPELL, T),
            Triple(15, ProfTrack.SPELL, M), Triple(19, ProfTrack.SPELL, L), Triple(13, ProfTrack.DEFENSE, T),
            Triple(11, ProfTrack.WEAPONS, T),
        ),
    )

    /** Every proficiency increase a class earns (across all levels). */
    fun increases(classId: String): List<ProfBump> = tables[classId.lowercase()].orEmpty()

    /** The increases a class earns exactly *at* [level]. */
    fun increasesAt(classId: String, level: Int): List<ProfBump> = increases(classId).filter { it.level == level }
}

package au.com.evonet.nat20.pf2e

/**
 * The common Pathfinder 2e conditions (A22) — display names + whether each is
 * **valued** (carries a live number). The full rules text lands with the play
 * slices; this drives the conditions picker + sheet labels. Slugs match
 * [au.com.evonet.nat20.pf2e.core.ValuedCondition.id].
 */
object PathfinderConditions {
    data class Entry(val id: String, val displayName: String, val valued: Boolean, val summary: String)

    val all: List<Entry> = listOf(
        Entry("clumsy", "Clumsy", true, "Status penalty to Dexterity-based checks and AC equal to the value."),
        Entry("drained", "Drained", true, "Penalty to Constitution checks and Fortitude saves; lose HP equal to level × value."),
        Entry("enfeebled", "Enfeebled", true, "Status penalty to Strength-based checks and DCs equal to the value."),
        Entry("frightened", "Frightened", true, "Status penalty to all checks and DCs equal to the value; decreases by 1 each turn."),
        Entry("sickened", "Sickened", true, "Status penalty to all checks and DCs equal to the value; can't ingest."),
        Entry("stupefied", "Stupefied", true, "Status penalty to mental checks and DCs; spellcasting risks failure."),
        Entry("stunned", "Stunned", true, "Lose that many actions; reduces stunned, otherwise lose actions this turn."),
        Entry("slowed", "Slowed", true, "Lose that many actions at the start of your turn."),
        Entry("quickened", "Quickened", false, "Gain 1 extra action each turn (used for a limited set of activities)."),
        Entry("off-guard", "Off-Guard", false, "−2 circumstance penalty to AC."),
        Entry("prone", "Prone", false, "Off-Guard, −2 to attacks; must Crawl or Stand."),
        Entry("blinded", "Blinded", false, "All terrain is difficult; everything is concealed; fail sight-based checks."),
        Entry("dazzled", "Dazzled", false, "All creatures and objects are concealed from you."),
        Entry("deafened", "Deafened", false, "Critically fail Perception checks based on hearing."),
        Entry("fatigued", "Fatigued", false, "−1 status penalty to AC and saves; can't use exploration activities."),
        Entry("grabbed", "Grabbed", false, "Immobilized and Off-Guard."),
        Entry("immobilized", "Immobilized", false, "You can't use any action with the move trait."),
        Entry("restrained", "Restrained", false, "Immobilized and Off-Guard; can only attempt to Escape."),
        Entry("unconscious", "Unconscious", false, "Asleep or knocked out; Blinded, Off-Guard, and can't act."),
    )

    private val byId = all.associateBy { it.id }
    fun entry(id: String): Entry? = byId[id.lowercase()]
    fun displayName(id: String): String = byId[id.lowercase()]?.displayName ?: id.replaceFirstChar(Char::uppercase)
    fun isValued(id: String): Boolean = byId[id.lowercase()]?.valued ?: false
}

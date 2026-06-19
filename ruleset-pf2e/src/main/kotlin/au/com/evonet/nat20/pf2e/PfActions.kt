package au.com.evonet.nat20.pf2e

/**
 * The Pathfinder 2e **three-action economy** as static reference data (A22).
 * Per the companion-app scope this is a glossary of action costs — what each
 * common action costs and does — **not** a live turn tracker. Covers the Basic
 * Actions and the common Skill Actions every table reaches for. ORC content.
 */
enum class PfActionCost(val glyph: String, val label: String) {
    ONE("◆", "One action"),
    TWO("◆◆", "Two actions"),
    THREE("◆◆◆", "Three actions"),
    FREE("◇", "Free action"),
    REACTION("↺", "Reaction"),
    VARIABLE("◆–◆◆◆", "Variable"),
}

/** A single action with its cost, traits, and a short rules summary. */
data class PfAction(val id: String, val name: String, val cost: PfActionCost, val traits: List<String>, val summary: String, val skill: Boolean = false)

object PfActions {
    val all: List<PfAction> = listOf(
        // ── Basic Actions ──
        PfAction("stride", "Stride", PfActionCost.ONE, listOf("Move"), "Move up to your Speed."),
        PfAction("step", "Step", PfActionCost.ONE, listOf("Move"), "Move 5 feet without triggering reactions."),
        PfAction("strike", "Strike", PfActionCost.ONE, listOf("Attack"), "Attack with a weapon or unarmed; each Strike after the first takes the multiple-attack penalty."),
        PfAction("interact", "Interact", PfActionCost.ONE, listOf("Manipulate"), "Draw, stow, pick up, or manipulate an object."),
        PfAction("raise-a-shield", "Raise a Shield", PfActionCost.ONE, emptyList(), "Gain your shield's circumstance bonus to AC until your next turn."),
        PfAction("take-cover", "Take Cover", PfActionCost.ONE, emptyList(), "Increase the bonus from cover, or gain cover against ranged attacks."),
        PfAction("ready", "Ready", PfActionCost.TWO, listOf("Concentrate"), "Prepare an action to trigger on a chosen stimulus (uses your reaction)."),
        PfAction("release", "Release", PfActionCost.FREE, listOf("Manipulate"), "Let go of something you're holding."),
        PfAction("drop-prone", "Drop Prone", PfActionCost.ONE, listOf("Move"), "Fall prone, gaining cover-like benefits against ranged attacks."),
        PfAction("stand", "Stand", PfActionCost.ONE, listOf("Move"), "Stand up from prone."),
        PfAction("sustain", "Sustain a Spell", PfActionCost.ONE, listOf("Concentrate"), "Extend the duration of a spell or effect you're sustaining."),
        PfAction("aid", "Aid", PfActionCost.REACTION, listOf("Concentrate"), "After preparing, help an ally's check with a roll of your own."),
        PfAction("seek", "Seek", PfActionCost.ONE, listOf("Concentrate", "Secret"), "Perception to find hidden or undetected creatures/objects."),
        PfAction("point-out", "Point Out", PfActionCost.ONE, listOf("Auditory", "Manipulate"), "Indicate a creature you can see to your allies."),
        PfAction("recall-knowledge", "Recall Knowledge", PfActionCost.ONE, listOf("Concentrate", "Secret"), "A relevant skill check to remember facts about a subject."),
        PfAction("delay", "Delay", PfActionCost.FREE, emptyList(), "On your initiative, wait to act later in the round."),
        PfAction("escape", "Escape", PfActionCost.ONE, listOf("Attack"), "Break free of the Grabbed, Restrained, or Immobilized condition."),
        PfAction("ready-strike", "Avert Gaze", PfActionCost.ONE, emptyList(), "Look away from a creature to resist its gaze."),

        // ── Common Skill Actions ──
        PfAction("tumble-through", "Tumble Through", PfActionCost.ONE, listOf("Move"), "Acrobatics vs Reflex DC to move through an enemy's space.", skill = true),
        PfAction("balance", "Balance", PfActionCost.ONE, listOf("Move"), "Acrobatics to move across narrow surfaces or uneven ground.", skill = true),
        PfAction("demoralize", "Demoralize", PfActionCost.ONE, listOf("Auditory", "Concentrate", "Emotion", "Fear", "Mental"), "Intimidation vs Will DC to make a foe Frightened.", skill = true),
        PfAction("trip", "Trip", PfActionCost.ONE, listOf("Attack"), "Athletics vs Reflex DC to knock a foe Prone.", skill = true),
        PfAction("grapple", "Grapple", PfActionCost.ONE, listOf("Attack"), "Athletics vs Fortitude DC to Grab a foe.", skill = true),
        PfAction("shove", "Shove", PfActionCost.ONE, listOf("Attack"), "Athletics vs Fortitude DC to push a foe back.", skill = true),
        PfAction("disarm", "Disarm", PfActionCost.ONE, listOf("Attack"), "Athletics vs Reflex DC to knock an item from a foe's grasp.", skill = true),
        PfAction("feint", "Feint", PfActionCost.ONE, listOf("Mental"), "Deception vs Perception DC to make a foe Off-Guard against you.", skill = true),
        PfAction("create-a-diversion", "Create a Diversion", PfActionCost.ONE, listOf("Mental"), "Deception vs Perception DC to Hide or Sneak immediately after.", skill = true),
        PfAction("hide", "Hide", PfActionCost.ONE, listOf("Secret"), "Stealth vs Perception DC to become Hidden from foes.", skill = true),
        PfAction("sneak", "Sneak", PfActionCost.ONE, listOf("Move", "Secret"), "Stealth to move while staying Hidden or Undetected.", skill = true),
        PfAction("treat-wounds", "Treat Wounds", PfActionCost.VARIABLE, listOf("Exploration", "Healing", "Manipulate"), "Medicine over 10 minutes to restore HP and reduce Wounded.", skill = true),
        PfAction("administer-first-aid", "Administer First Aid", PfActionCost.TWO, listOf("Manipulate"), "Medicine to Stabilize a dying creature or stop bleeding.", skill = true),
    )

    fun by(id: String): PfAction? = all.firstOrNull { it.id == id }
    val basics: List<PfAction> get() = all.filter { !it.skill }
    val skills: List<PfAction> get() = all.filter { it.skill }
}

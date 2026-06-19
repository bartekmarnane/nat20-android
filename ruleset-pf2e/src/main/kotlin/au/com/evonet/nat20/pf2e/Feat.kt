package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.pf2e.core.AdvancementSchedule
import kotlinx.serialization.Serializable

/**
 * Pathfinder 2e feats (A22). A character gains them on different cadences:
 * ancestry + class feats from level 1, skill feats from even levels (every level
 * for a Rogue), general feats from level 3. Prerequisites beyond level/ancestry/
 * class are deferred — getting the *picks legal* matters more than catalogue
 * breadth, which grows additively. Port of the iOS `Feat`/`Feats`.
 */
@Serializable
enum class PfFeatType(val displayName: String) {
    ANCESTRY("Ancestry"), CLASS("Class"), SKILL("Skill"), GENERAL("General"),
}

data class PfFeat(
    val id: String,
    val name: String,
    val type: PfFeatType,
    val level: Int,
    val summary: String,
    /** Set for ancestry feats — the ancestry that grants it. */
    val ancestry: String? = null,
    /** Set for class feats — the class that grants it. */
    val className: String? = null,
)

object PfFeats {
    val all: List<PfFeat> = buildList {
        // ── Ancestry feats (level 1, a representative few per ancestry) ──
        add(PfFeat("natural-ambition", "Natural Ambition", PfFeatType.ANCESTRY, 1, "Gain a 1st-level class feat from your class.", ancestry = "human"))
        add(PfFeat("general-training", "General Training", PfFeatType.ANCESTRY, 1, "Gain a 1st-level general feat.", ancestry = "human"))
        add(PfFeat("adapted-cantrip", "Adapted Cantrip", PfFeatType.ANCESTRY, 1, "Learn a cantrip from a different tradition.", ancestry = "human"))
        add(PfFeat("dwarven-lore", "Dwarven Lore", PfFeatType.ANCESTRY, 1, "Become trained in Crafting, Religion, and Dwarven Lore.", ancestry = "dwarf"))
        add(PfFeat("rock-runner", "Rock Runner", PfFeatType.ANCESTRY, 1, "Ignore difficult terrain from stone and earth.", ancestry = "dwarf", ))
        add(PfFeat("ancestral-longevity", "Ancestral Longevity", PfFeatType.ANCESTRY, 9, "Become trained in a skill of your choice each day.", ancestry = "dwarf"))
        add(PfFeat("elven-lore", "Elven Lore", PfFeatType.ANCESTRY, 1, "Become trained in Arcana, Nature, and Elven Lore.", ancestry = "elf"))
        add(PfFeat("nimble-elf", "Nimble Elf", PfFeatType.ANCESTRY, 1, "Your Speed increases by 5 feet.", ancestry = "elf"))
        add(PfFeat("ageless-patience", "Ageless Patience", PfFeatType.ANCESTRY, 5, "Take extra time on a skill check for a bonus.", ancestry = "elf"))
        add(PfFeat("gnome-obsession", "Gnome Obsession", PfFeatType.ANCESTRY, 1, "Become trained in a Lore of your choice, growing with you.", ancestry = "gnome"))
        add(PfFeat("first-world-magic", "First World Magic", PfFeatType.ANCESTRY, 1, "Learn a primal cantrip.", ancestry = "gnome"))
        add(PfFeat("burn-it", "Burn It!", PfFeatType.ANCESTRY, 1, "Your fire spells and alchemical fire deal extra damage.", ancestry = "goblin"))
        add(PfFeat("goblin-song", "Goblin Song", PfFeatType.ANCESTRY, 1, "Sing to distract foes, making them Off-Guard.", ancestry = "goblin"))
        add(PfFeat("halfling-luck", "Halfling Luck", PfFeatType.ANCESTRY, 1, "Once per day, reroll a failed skill check or save.", ancestry = "halfling"))
        add(PfFeat("sure-feet", "Sure Feet", PfFeatType.ANCESTRY, 1, "Success on Acrobatics to Balance/Squeeze counts as a critical success.", ancestry = "halfling"))
        add(PfFeat("grow-leaves", "Cautious Curiosity", PfFeatType.ANCESTRY, 1, "Learn a stealth or deception cantrip.", ancestry = "leshy"))
        add(PfFeat("orc-ferocity", "Orc Ferocity", PfFeatType.ANCESTRY, 1, "Once per day, drop to 1 HP instead of 0.", ancestry = "orc"))
        add(PfFeat("hold-mark", "Hold Mark", PfFeatType.ANCESTRY, 1, "Bear the mark of an orc hold, gaining a skill.", ancestry = "orc"))

        // ── Class feats (level 1, a representative few per class) ──
        add(PfFeat("sudden-charge", "Sudden Charge", PfFeatType.CLASS, 1, "Stride twice, then make a melee Strike.", className = "fighter"))
        add(PfFeat("power-attack", "Power Attack", PfFeatType.CLASS, 1, "A melee Strike for an extra die of damage (two actions).", className = "fighter"))
        add(PfFeat("double-slice", "Double Slice", PfFeatType.CLASS, 1, "Strike with two weapons against one foe.", className = "fighter"))
        add(PfFeat("combat-grab", "Combat Grab", PfFeatType.CLASS, 4, "Grab a foe on a successful Strike.", className = "fighter"))
        add(PfFeat("nimble-dodge", "Nimble Dodge", PfFeatType.CLASS, 1, "Reaction: gain a +2 circumstance bonus to AC against an attack.", className = "rogue"))
        add(PfFeat("twin-feint", "Twin Feint", PfFeatType.CLASS, 1, "Strike with two weapons; the second target is Off-Guard.", className = "rogue"))
        add(PfFeat("you-re-next", "You're Next", PfFeatType.CLASS, 1, "Reaction after a kill: Demoralize a foe.", className = "rogue"))
        add(PfFeat("deadly-simplicity", "Deadly Simplicity", PfFeatType.CLASS, 1, "Your deity's simple favored weapon deals a larger die.", className = "cleric"))
        add(PfFeat("harming-hands", "Harming Hands", PfFeatType.CLASS, 1, "Your Harm spell uses a larger die.", className = "cleric"))
        add(PfFeat("counterspell", "Counterspell", PfFeatType.CLASS, 1, "Reaction: expend a slot to counter a spell you have prepared/known.", className = "wizard"))
        add(PfFeat("reach-spell", "Reach Spell", PfFeatType.CLASS, 1, "Extend a spell's range by 30 feet.", className = "wizard"))
        add(PfFeat("widen-spell", "Widen Spell", PfFeatType.CLASS, 1, "Increase the area of an area spell.", className = "sorcerer"))
        add(PfFeat("dangerous-sorcery", "Dangerous Sorcery", PfFeatType.CLASS, 1, "Bonus damage when casting a damaging spell from a slot.", className = "sorcerer"))
        add(PfFeat("hunted-shot", "Hunted Shot", PfFeatType.CLASS, 1, "Make two ranged Strikes against your hunted prey.", className = "ranger"))
        add(PfFeat("twin-takedown", "Twin Takedown", PfFeatType.CLASS, 1, "Strike your hunted prey with two weapons.", className = "ranger"))
        add(PfFeat("ranged-reprisal", "Ranged Reprisal", PfFeatType.CLASS, 1, "Make a Champion's Reaction at range.", className = "champion"))
        add(PfFeat("bardic-lore", "Bardic Lore", PfFeatType.CLASS, 1, "Become trained in Bardic Lore on every subject.", className = "bard"))
        add(PfFeat("lingering-composition", "Lingering Composition", PfFeatType.CLASS, 1, "Spend a focus point to make a composition last longer.", className = "bard"))

        // ── Skill feats (shared pool) ──
        add(PfFeat("intimidating-glare", "Intimidating Glare", PfFeatType.SKILL, 1, "Demoralize without speaking; only sight matters."))
        add(PfFeat("battle-medicine", "Battle Medicine", PfFeatType.SKILL, 1, "Use Medicine to heal in combat, once per target per day."))
        add(PfFeat("assurance", "Assurance", PfFeatType.SKILL, 1, "Forgo rolling a chosen skill to get a fixed result (10 + prof)."))
        add(PfFeat("cat-fall", "Cat Fall", PfFeatType.SKILL, 1, "Treat falls as shorter, taking less damage."))
        add(PfFeat("titan-wrestler", "Titan Wrestler", PfFeatType.SKILL, 1, "Grapple or Trip creatures larger than you."))
        add(PfFeat("quick-jump", "Quick Jump", PfFeatType.SKILL, 1, "High Jump and Long Jump as a single action."))
        add(PfFeat("group-impression", "Group Impression", PfFeatType.SKILL, 1, "Make an Impression on several people at once."))
        add(PfFeat("dubious-knowledge", "Dubious Knowledge", PfFeatType.SKILL, 1, "Learn true and false lore on a failed Recall Knowledge."))
        add(PfFeat("forager", "Forager", PfFeatType.SKILL, 2, "Provide for yourself and others using Survival."))
        add(PfFeat("experienced-tracker", "Experienced Tracker", PfFeatType.SKILL, 2, "Track at full Speed."))

        // ── General feats (shared pool) ──
        add(PfFeat("toughness", "Toughness", PfFeatType.GENERAL, 1, "Increase max HP by your level; reduce the dying recovery DC."))
        add(PfFeat("fleet", "Fleet", PfFeatType.GENERAL, 1, "Your Speed increases by 5 feet."))
        add(PfFeat("diehard", "Diehard", PfFeatType.GENERAL, 1, "You die at dying 5 instead of 4."))
        add(PfFeat("incredible-initiative", "Incredible Initiative", PfFeatType.GENERAL, 1, "+2 circumstance bonus to initiative rolls."))
        add(PfFeat("canny-acumen", "Canny Acumen", PfFeatType.GENERAL, 1, "Become an expert in Perception or a saving throw."))
        add(PfFeat("feather-step", "Feather Step", PfFeatType.GENERAL, 1, "Step into difficult terrain."))
        add(PfFeat("ride", "Ride", PfFeatType.GENERAL, 1, "Command an animal to move as part of your action."))
        add(PfFeat("untrained-improvisation", "Untrained Improvisation", PfFeatType.GENERAL, 3, "Add part of your level to untrained skill checks."))
    }

    fun by(id: String): PfFeat? = all.firstOrNull { it.id == id }

    fun ancestryFeats(ancestryId: String, level: Int): List<PfFeat> =
        all.filter { it.type == PfFeatType.ANCESTRY && it.ancestry == ancestryId && it.level <= level }
    fun classFeats(classId: String, level: Int): List<PfFeat> =
        all.filter { it.type == PfFeatType.CLASS && it.className == classId && it.level <= level }
    fun skillFeats(level: Int): List<PfFeat> = all.filter { it.type == PfFeatType.SKILL && it.level <= level }
    fun generalFeats(level: Int): List<PfFeat> = all.filter { it.type == PfFeatType.GENERAL && it.level <= level }

    /** Feats of [type] a character can legally take (filtered by level, ancestry, and class). */
    fun available(type: PfFeatType, ancestryId: String, classId: String, level: Int): List<PfFeat> = when (type) {
        PfFeatType.ANCESTRY -> ancestryFeats(ancestryId, level)
        PfFeatType.CLASS -> classFeats(classId, level)
        PfFeatType.SKILL -> skillFeats(level)
        PfFeatType.GENERAL -> generalFeats(level)
    }
}

/** How many feat slots of each type a character of a given level + class has earned. */
object FeatSlots {
    /** Class-feat cadence: martials get one at level 1 then every even level; the seeded casters start at 2. */
    private val MARTIAL = setOf("fighter", "rogue", "ranger", "champion")
    fun classFeatLevels(classId: String): Set<Int> =
        if (classId in MARTIAL) (setOf(1) + (2..20 step 2)) else (2..20 step 2).toSet()

    fun ancestry(level: Int): Int = AdvancementSchedule.ANCESTRY_FEAT_LEVELS.count { it <= level }
    fun general(level: Int): Int = AdvancementSchedule.GENERAL_FEAT_LEVELS.count { it <= level }
    fun skill(level: Int, rogue: Boolean): Int = AdvancementSchedule.skillFeatLevels(rogue).count { it <= level }
    fun classFeat(level: Int, classId: String): Int = classFeatLevels(classId).count { it <= level }
}

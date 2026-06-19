package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.domain.CharacterEvent
import au.com.evonet.nat20.domain.NoteKind
import kotlinx.serialization.Serializable

/** Journal events for the Pathfinder 2e foundation slice (A22). */

@Serializable
data class PfNoteEvent(val text: String, val kind: NoteKind? = null) : CharacterEvent {
    override val summary: String get() = text
}

@Serializable
data class PfDamageTakenEvent(
    val amount: Int,
    val previousHp: Int,
    val newHp: Int,
    val tempAbsorbed: Int = 0,
    val nowDying: Int? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val temp = if (tempAbsorbed > 0) " ($tempAbsorbed absorbed by temp)" else ""
            val dying = nowDying?.takeIf { it > 0 }?.let { " · Dying $it" } ?: ""
            return "Took $amount damage (HP $previousHp → $newHp)$temp$dying"
        }
}

@Serializable
data class PfHealedEvent(val amount: Int, val previousHp: Int, val newHp: Int, val recoveredFromDying: Boolean = false) : CharacterEvent {
    override val summary: String
        get() = "Healed $amount (HP $previousHp → $newHp)" + if (recoveredFromDying) " · no longer dying" else ""
}

@Serializable
data class PfTempHpGainedEvent(val amount: Int, val newValue: Int) : CharacterEvent {
    override val summary: String get() = "Gained $amount temporary HP (now $newValue)"
}

@Serializable
data class PfDyingChangedEvent(val previous: Int, val newValue: Int) : CharacterEvent {
    override val summary: String
        get() = when {
            newValue >= PathfinderPayload.DYING_MAX -> "Dying reached ${PathfinderPayload.DYING_MAX} — death"
            newValue == 0 -> "Stabilized — no longer dying"
            else -> "Dying ${if (newValue > previous) "rose" else "eased"} to $newValue"
        }
}

@Serializable
data class PfWoundedChangedEvent(val previous: Int, val newValue: Int) : CharacterEvent {
    override val summary: String get() = if (newValue == 0) "Cleared the Wounded condition" else "Wounded $newValue"
}

@Serializable
data class PfHeroPointsChangedEvent(val previous: Int, val newValue: Int) : CharacterEvent {
    override val summary: String
        get() = when {
            newValue > previous -> "Gained a Hero Point (now $newValue)"
            newValue == 0 -> "Spent the last Hero Point"
            else -> "Spent a Hero Point (now $newValue)"
        }
}

@Serializable
data class PfSpellCastEvent(val spellName: String, val slotRank: Int, val heightened: Boolean, val focus: Boolean = false) : CharacterEvent {
    override val summary: String
        get() {
            val at = when {
                focus -> " (focus)"
                slotRank == 0 -> ""
                heightened -> " (heightened to rank $slotRank)"
                else -> ""
            }
            return "Cast $spellName$at"
        }
}

@Serializable
data class PfFeatChangedEvent(val name: String, val type: String, val taken: Boolean) : CharacterEvent {
    override val summary: String get() = if (taken) "Took the $name $type feat" else "Removed the $name feat"
}

@Serializable
data class PfLeveledUpEvent(val newLevel: Int, val hpGained: Int, val skillIncrease: String? = null, val abilityBoosts: List<String> = emptyList()) : CharacterEvent {
    override val summary: String
        get() {
            val extras = buildList {
                skillIncrease?.let { add("$it ↑") }
                if (abilityBoosts.isNotEmpty()) add(abilityBoosts.joinToString(", ") { "+$it" })
            }
            val extra = if (extras.isEmpty()) "" else " · ${extras.joinToString(" · ")}"
            return "Reached level $newLevel — +$hpGained HP$extra"
        }
}

@Serializable
data class PfDailyPrepEvent(val slotsRestored: Int) : CharacterEvent {
    override val summary: String get() = "Daily preparations — ${if (slotsRestored > 0) "$slotsRestored spell slot${if (slotsRestored == 1) "" else "s"} + focus restored" else "rested and refocused"}"
}

@Serializable
data class PfStrikeEvent(val weaponName: String, val attackNumber: Int, val total: Int, val target: String? = null) : CharacterEvent {
    override val summary: String
        get() {
            val tgt = target?.takeIf { it.isNotBlank() }?.let { " on $it" } ?: ""
            val which = when (attackNumber) { 1 -> "Strike"; 2 -> "2nd Strike"; else -> "3rd Strike" }
            return "$which$tgt with $weaponName (total $total)"
        }
}

@Serializable
data class PfConditionChangedEvent(val name: String, val value: Int? = null, val applied: Boolean) : CharacterEvent {
    override val summary: String
        get() {
            val label = name + (value?.let { " $it" } ?: "")
            return if (applied) "Gained $label" else "Cleared $name"
        }
}

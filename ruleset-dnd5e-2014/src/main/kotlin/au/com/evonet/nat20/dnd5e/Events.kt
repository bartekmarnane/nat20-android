package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.dnd5e.core.HpChoice
import au.com.evonet.nat20.domain.CharacterEvent
import kotlinx.serialization.Serializable

/**
 * Journal events for the A3 intent slice. The full app has ~35 event types;
 * these are the four that pair with the vitals + level intents. `summary` is a
 * computed getter, so it isn't serialized — only the stored facts are.
 */

@Serializable
data class DamageTakenEvent(
    val amount: Int,
    val damageType: String? = null,
    val note: String? = null,
    val previousHp: Int,
    val newHp: Int,
    val tempAbsorbed: Int = 0,
    val maxHp: Int? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val head = if (!damageType.isNullOrEmpty()) {
                "Took $amount ${damageType.lowercase()} damage"
            } else {
                "Took $amount damage"
            }
            val narrative = if (!note.isNullOrEmpty()) " — $note" else ""
            val tempNote = if (tempAbsorbed > 0) " ($tempAbsorbed absorbed by temp)" else ""
            return "$head$narrative (HP $previousHp → $newHp)$tempNote"
        }
}

@Serializable
data class HealedEvent(
    val amount: Int,
    val source: String? = null,
    val previousHp: Int,
    val newHp: Int,
    val maxHp: Int? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val tail = source?.let { " from $it" } ?: ""
            return "Healed $amount$tail (HP $previousHp → $newHp)"
        }
}

@Serializable
data class TempHpGainedEvent(
    val amount: Int,
    val source: String? = null,
    val previous: Int,
    val newValue: Int,
) : CharacterEvent {
    override val summary: String
        get() {
            val src = source?.let { " from $it" } ?: ""
            return when {
                newValue == amount && previous == 0 -> "Gained $amount temp HP$src"
                newValue > previous -> "Gained $amount temp HP$src (now $newValue)"
                else -> "Considered $amount temp HP$src — kept existing $previous"
            }
        }
}

@Serializable
data class LeveledUpEvent(
    val classId: String,
    val className: String,
    val isNewClass: Boolean,
    val previousLevel: Int,
    val newLevel: Int,
    val classLevelAfter: Int,
    val hpChoice: HpChoice,
    val hpGained: Int,
) : CharacterEvent {
    override val summary: String
        get() {
            val displayClass = className.ifEmpty { classId }
            val classLabel = if (displayClass.isEmpty()) "level" else "$displayClass $classLevelAfter"
            val hpPart = if (hpGained > 0) " — +$hpGained HP" else ""
            val newPart = if (isNewClass) " (multiclass)" else ""
            return "Leveled up to $classLabel$newPart$hpPart"
        }
}

// ── Inventory & coins (A10) ───────────────────────────────────────────────────

@Serializable
data class ItemAcquiredEvent(
    val itemName: String,
    val quantity: Int = 1,
) : CharacterEvent {
    override val summary: String
        get() = if (quantity > 1) "Acquired $itemName ×$quantity" else "Acquired $itemName"
}

@Serializable
data class ItemDroppedEvent(
    val itemName: String,
    val quantity: Int = 1,
) : CharacterEvent {
    override val summary: String
        get() = if (quantity > 1) "Dropped $itemName ×$quantity" else "Dropped $itemName"
}

@Serializable
data class ItemUsedEvent(
    val itemName: String,
    val healingRolled: Int? = null,
    val previousHp: Int? = null,
    val newHp: Int? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val heal = if (healingRolled != null && healingRolled > 0) {
                val hpPart = if (previousHp != null && newHp != null) " (HP $previousHp → $newHp)" else ""
                " — restored $healingRolled HP$hpPart"
            } else {
                ""
            }
            return "Used $itemName$heal"
        }
}

@Serializable
data class CoinAdjustedEvent(
    val coin: Coin,
    val delta: Int,
    val source: String? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val verb = if (delta >= 0) "Gained" else "Spent"
            val tail = source?.let { " — $it" } ?: ""
            return "$verb ${kotlin.math.abs(delta)} ${coin.abbreviation}$tail"
        }
}

// ── Spellcasting (A10) ────────────────────────────────────────────────────────

/** "1st" / "2nd" / "3rd" / "4th" … for spell-slot levels. */
internal fun ordinalLevel(level: Int): String = when (level) {
    1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${level}th"
}

@Serializable
data class CastSpellEvent(
    val spellID: String,
    val spellName: String,
    val spellLevel: Int,
    val slotLevel: Int,
    val wasUpcast: Boolean = false,
    val wasRitual: Boolean = false,
    val fromScroll: Boolean = false,
    val fromPact: Boolean = false,
    val target: String? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val head = when {
                fromScroll -> "Cast $spellName from a scroll"
                wasRitual -> "Cast $spellName as a ritual"
                slotLevel == 0 -> "Cast $spellName" // cantrip
                wasUpcast -> "Cast $spellName at ${ordinalLevel(slotLevel)} level"
                else -> "Cast $spellName"
            }
            val pact = if (fromPact) " (pact slot)" else ""
            val tgt = target?.let { " on $it" } ?: ""
            return "$head$tgt$pact"
        }
}

@Serializable
data class SpellPreparedEvent(
    val spellID: String,
    val spellName: String,
    val classID: String,
    val wasNew: Boolean,
) : CharacterEvent {
    override val summary: String get() = "Prepared $spellName"
}

@Serializable
data class SpellUnpreparedEvent(
    val spellID: String,
    val spellName: String,
    val classID: String,
    val wasRemoved: Boolean,
) : CharacterEvent {
    override val summary: String get() = "Unprepared $spellName"
}

@Serializable
data class SpellSlotExpendedEvent(
    val slotLevel: Int,
    val fromPactPool: Boolean,
    val remaining: Int,
    val source: String? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val pool = if (fromPactPool) " pact" else ""
            val src = source?.let { " — $it" } ?: ""
            return "Expended a ${ordinalLevel(slotLevel)}-level$pool spell slot$src"
        }
}

@Serializable
data class ShortRestEvent(
    val pactSlotsRestored: Int = 0,
) : CharacterEvent {
    override val summary: String
        get() = if (pactSlotsRestored > 0) {
            "Took a short rest — recovered $pactSlotsRestored pact slot${if (pactSlotsRestored == 1) "" else "s"}"
        } else {
            "Took a short rest"
        }
}

@Serializable
data class HitDieSpentEvent(
    val healingRolled: Int,
    val previousHp: Int,
    val newHp: Int,
    val remaining: Int,
) : CharacterEvent {
    override val summary: String
        get() = "Spent a hit die — recovered $healingRolled HP (HP $previousHp → $newHp), $remaining left"
}

@Serializable
data class LongRestEvent(
    val hpRestored: Int = 0,
    val tempCleared: Int = 0,
    val slotsRestored: Int = 0,
    val hitDiceRegained: Int = 0,
) : CharacterEvent {
    override val summary: String
        get() {
            val parts = buildList {
                if (slotsRestored > 0) add("$slotsRestored spell slot${if (slotsRestored == 1) "" else "s"}")
                if (hitDiceRegained > 0) add("$hitDiceRegained hit di${if (hitDiceRegained == 1) "e" else "ce"}")
            }
            val extras = if (parts.isEmpty()) "" else " (${parts.joinToString(" + ")} restored)"
            return "Took a long rest — back to full health$extras"
        }
}

// ── Class resources (A10) ─────────────────────────────────────────────────────

@Serializable
data class ResourceSpentEvent(
    val poolID: String,
    val poolName: String,
    val amount: Int,
    val remaining: Int,
    val max: Int,
    val note: String? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val tail = note?.let { " — $it" } ?: ""
            return "Spent $amount $poolName ($remaining/$max left)$tail"
        }
}

@Serializable
data class ClassFeatureUsedEvent(
    val featureID: String,
    val featureName: String,
    val recovery: au.com.evonet.nat20.dnd5e.core.FeatureRecovery,
    val remainingAfter: Int? = null,
    val note: String? = null,
) : CharacterEvent {
    override val summary: String
        get() {
            val uses = remainingAfter?.let { " ($it left)" } ?: ""
            val tail = note?.let { " — $it" } ?: ""
            return "Used $featureName$uses$tail"
        }
}

@Serializable
data class InspirationChangedEvent(
    val hasInspiration: Boolean,
) : CharacterEvent {
    override val summary: String
        get() = if (hasInspiration) "Gained Inspiration" else "Used Inspiration"
}

/** Free-form journal note (used by [DnD5eRuleset.makeProseEvent]). */
@Serializable
data class NoteEvent(
    val text: String,
    val kind: au.com.evonet.nat20.domain.NoteKind? = null,
) : CharacterEvent {
    override val summary: String get() = text
}

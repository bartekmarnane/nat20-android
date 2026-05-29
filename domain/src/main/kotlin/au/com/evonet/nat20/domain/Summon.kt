package au.com.evonet.nat20.domain

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * A group of [Creature] instances spawned by the same cause — one Find
 * Familiar casting, one Conjure Animals (yielding 8 wolves), one Animate Dead
 * group. The group shares a dismissal lifecycle.
 *
 * Persistent companions (Beast Master pet, paladin's Find Steed) are also
 * summons — they share storage with ephemeral ones, and
 * [SummonLifecycle.Persistent] tells the rest of the system they don't
 * auto-dismiss. Spawn/dismiss behaviour lands at A15.
 */
@Serializable
data class Summon(
    @Serializable(with = UuidSerializer::class) val id: UUID = UUID.randomUUID(),
    /** Human-readable label for the journal/UI ("Conjure Animals (3rd) — Wolves"). */
    val label: String,
    val origin: SummonOrigin,
    val lifecycle: SummonLifecycle,
    val creatures: List<Creature>,
    @Serializable(with = InstantSerializer::class) val spawnedAt: Instant,
)

/** What caused the summon — powers UI labels, prose, and re-summon affordances. */
@Serializable
sealed class SummonOrigin {
    @Serializable
    data class Spell(val name: String, val castAtSlotLevel: Int) : SummonOrigin()

    @Serializable
    data class ClassFeature(val className: String, val featureName: String) : SummonOrigin()

    @Serializable
    data class RacialTrait(val name: String) : SummonOrigin()

    @Serializable
    data class Item(@Serializable(with = UuidSerializer::class) val itemId: UUID) : SummonOrigin()

    /** DM-granted or otherwise outside the ruleset's catalogues. */
    @Serializable
    data object Manual : SummonOrigin()
}

/** When and how the summon disappears. */
@Serializable
sealed class SummonLifecycle {
    /** Familiars, beast companions, find-steed mounts. Survives long rests. */
    @Serializable
    data object Persistent : SummonLifecycle()

    /** Tied to the caster's concentration; dropping it dismisses the group. */
    @Serializable
    data class Concentration(val maxDuration: GameDuration) : SummonLifecycle()

    /** Counts down until the duration expires (Find Steed, short-rest buffs). */
    @Serializable
    data class Timed(val maxDuration: GameDuration) : SummonLifecycle()

    /** Animate Dead: control reasserted on a cadence up to a fixed capacity. */
    @Serializable
    data class ControlReassertion(val cadence: Cadence, val capacity: Int) : SummonLifecycle()
}

/** In-fiction time — game-clock units, not real-time. */
@Serializable
sealed class GameDuration {
    @Serializable
    data class Rounds(val value: Int) : GameDuration()

    @Serializable
    data class Minutes(val value: Int) : GameDuration()

    @Serializable
    data class Hours(val value: Int) : GameDuration()
}

/**
 * Recurrence schedule for [SummonLifecycle.ControlReassertion]. Only [DAILY]
 * is needed for current 5e content (Animate Dead); add cases as mechanics demand.
 */
@Serializable
enum class Cadence { DAILY }

package au.com.evonet.nat20.dnd5e.core

import kotlinx.serialization.Serializable

/** The resolved outcome of an attack roll. */
@Serializable
enum class AttackOutcome { HIT, MISS, CRITICAL }

/** The target's result against a save-DC spell (recorded at the table, not simulated). */
@Serializable
enum class SaveOutcome { PASSED, FAILED }

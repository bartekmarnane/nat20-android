package au.com.evonet.nat20.dnd5e

import kotlinx.serialization.Serializable

/**
 * A single class line on a (possibly multiclass) character — the class id, its
 * level in that class, and an optional subclass. The character's overall level
 * is the sum of all entries' levels. Port of the iOS `ClassEntry`.
 */
@Serializable
data class ClassEntry(
    val classId: String,
    val level: Int = 1,
    val subclass: String? = null,
)

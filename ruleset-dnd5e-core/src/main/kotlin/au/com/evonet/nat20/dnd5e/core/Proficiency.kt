package au.com.evonet.nat20.dnd5e.core

/**
 * 5e proficiency bonus by character level — edition-agnostic (2014 and 2024
 * share the same +2…+6 progression), so it lives in the shared core. Extracted
 * from `DnD5eRuleset` at the multi-ruleset module split (A3.5). Port of the iOS
 * `DnD5eCore` proficiency math.
 */
object Proficiency {
    /**
     * +2 at L1–4, +3 at L5–8, +4 at L9–12, +5 at L13–16, +6 at L17–20.
     */
    fun bonus(level: Int): Int = 2 + (maxOf(level, 1) - 1) / 4
}

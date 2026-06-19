package au.com.evonet.nat20.pf2e

import kotlinx.serialization.Serializable

/**
 * Pathfinder 2e coins, carried items, and **Bulk** (A22). Bulk is the abstract
 * encumbrance unit: a whole number, **light** (`L`, ten make 1 Bulk), or
 * **negligible** (`—`). Modelled as a Double (negligible 0, light 0.1, coins
 * 0.001); summing and flooring reproduces the "ten light items = 1 Bulk" rule.
 * You're **encumbered** above `5 + Str mod` Bulk. Port of the iOS `Bulk`/`Wealth`.
 */
@Serializable
enum class PFCoin(val abbreviation: String, val copperValue: Int) {
    CP("cp", 1), SP("sp", 10), GP("gp", 100), PP("pp", 1000);
}

@Serializable
data class PFInventoryItem(val name: String, val quantity: Int = 1, val bulk: Double = 0.0)

object Bulk {
    /** One light item's Bulk (ten make 1 Bulk). */
    const val LIGHT: Double = 0.1
    /** One coin's Bulk (1,000 coins make 1 Bulk). */
    const val PER_COIN: Double = 0.001

    /** Render a single item's Bulk: 0 → "—", light → "L", otherwise the integer. */
    fun label(value: Double): String = when {
        value <= 0 -> "—"
        value < 1 -> "L"
        else -> Math.round(value).toString()
    }

    /** Effective Bulk for carrying limits — the raw total floored, per RAW. */
    fun effective(rawTotal: Double): Int = kotlin.math.floor(rawTotal).toInt()
}

object Wealth {
    fun totalGp(coins: Map<PFCoin, Int>): Double =
        coins.entries.sumOf { (coin, count) -> coin.copperValue.toDouble() * count } / 100.0

    /** "12 gp, 5 sp" — most-valuable-first, omitting empties. */
    fun purseLabel(coins: Map<PFCoin, Int>): String =
        PFCoin.entries.reversed().mapNotNull { c -> (coins[c] ?: 0).takeIf { it > 0 }?.let { "$it ${c.abbreviation}" } }
            .joinToString(", ").ifEmpty { "empty" }
}

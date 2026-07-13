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
    /**
     * PF2e starting wealth by character level, in gp — the GM Core "Treasure for
     * New Characters" lump sum (level 1 = 15 gp). Indexed 1..20, clamped.
     * Mirrors iOS `Wealth.startingWealthByLevel` (verified vs Archives of Nethys).
     */
    private val STARTING_WEALTH_BY_LEVEL = intArrayOf(
        15, 30, 75, 140, 270, 450, 720, 1100, 1600, 2300,
        3200, 4500, 6400, 9300, 13500, 20000, 30000, 45000, 69000, 112000,
    )

    /** Starting wealth in gp for a new character of the given level. */
    fun startingWealthGP(level: Int): Int = STARTING_WEALTH_BY_LEVEL[(level - 1).coerceIn(0, 19)]

    /** The gp cost of catalogue equipment picks (worn armor + shield + weapons). */
    fun kitCostGp(armorId: String?, shieldId: String?, weaponIds: Collection<String>): Double =
        (armorId?.let(PfArmors::by)?.priceGp ?: 0.0) +
            (shieldId?.let(PfShields::by)?.priceGp ?: 0.0) +
            weaponIds.sumOf { PfWeapons.by(it)?.priceGp ?: 0.0 }

    /** The total Bulk of catalogue equipment picks (raw; callers floor via [Bulk.effective]). */
    fun kitBulk(armorId: String?, shieldId: String?, weaponIds: Collection<String>): Double =
        (armorId?.let(PfArmors::by)?.bulk ?: 0.0) +
            (shieldId?.let(PfShields::by)?.bulk ?: 0.0) +
            weaponIds.sumOf { PfWeapons.by(it)?.bulk ?: 0.0 }

    fun totalGp(coins: Map<PFCoin, Int>): Double =
        coins.entries.sumOf { (coin, count) -> coin.copperValue.toDouble() * count } / 100.0

    /** "12 gp, 5 sp" — most-valuable-first, omitting empties. */
    fun purseLabel(coins: Map<PFCoin, Int>): String =
        PFCoin.entries.reversed().mapNotNull { c -> (coins[c] ?: 0).takeIf { it > 0 }?.let { "$it ${c.abbreviation}" } }
            .joinToString(", ").ifEmpty { "empty" }
}

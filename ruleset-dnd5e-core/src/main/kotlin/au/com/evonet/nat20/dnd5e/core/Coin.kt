package au.com.evonet.nat20.dnd5e.core

import kotlinx.serialization.Serializable

/**
 * The five 5e coin denominations, ordered most → least valuable. Edition-agnostic
 * (2014 and 2024 share it), so it lives in `-core` — port of the iOS
 * `DnD5eCore/Coin.swift`. A character's purse is a `Map<Coin, Int>`; a missing
 * key means zero of that coin.
 */
@Serializable
enum class Coin {
    PP, GP, EP, SP, CP;

    /** Display abbreviation, e.g. "GP". */
    val abbreviation: String get() = name

    /** Full denomination name, e.g. "Gold". */
    val fullName: String
        get() = when (this) {
            PP -> "Platinum"
            GP -> "Gold"
            EP -> "Electrum"
            SP -> "Silver"
            CP -> "Copper"
        }
}

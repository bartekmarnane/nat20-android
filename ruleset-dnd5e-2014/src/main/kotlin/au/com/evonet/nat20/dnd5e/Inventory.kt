package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.ACOverrideFormula
import au.com.evonet.nat20.dnd5e.core.Ability
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * The inventory model for the A10 content slice — a flat list of [InventoryItem]
 * on the payload, each carrying an [equipped] flag that drives the AC calculation
 * ([ArmorClassCalculator]) and, later, the attack picker. Port of the iOS
 * `DnD5e/Inventory.swift`, trimmed to the fields this slice needs: the scroll /
 * wondrous-charge / effect-rider properties (save & skill bonuses, resistances)
 * arrive with active effects (A17).
 */

/** What kind of thing an item is — drives grouping, equip rules, and AC. */
@Serializable
enum class ItemKind {
    WEAPON, ARMOR, SHIELD, AMMUNITION, POTION, SCROLL, WONDROUS, TOOL, GEAR, TREASURE;

    /** Whether this kind can be worn/wielded (shows an equip toggle). */
    val isEquippable: Boolean get() = this == WEAPON || this == ARMOR || this == SHIELD || this == WONDROUS

    companion object {
        /**
         * Best-guess kind for a homebrew item the player is typing by name, used to
         * pre-select the Add sheet's kind chip; null when nothing in the name is
         * recognisable (the caller falls back to [GEAR]).
         *
         * Deliberately noun-driven rather than clever: "Robe of the Archmagi" is
         * wondrous because *robe* is a worn thing, and that's the whole rule.
         * Anything the bundled SRD catalogue already matches never reaches here.
         * Mirrors iOS `ItemKind.inferred(fromName:)`.
         */
        fun inferred(fromName: String): ItemKind? {
            val tokens = fromName.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return null

            // Cheap plural fold so "robes" and "gauntlets" land too.
            fun mentions(vocabulary: Set<String>) = tokens.any { token ->
                token in vocabulary || (token.endsWith("s") && token.dropLast(1) in vocabulary)
            }

            // Order matters where vocabularies brush against each other: a Potion of
            // Giant Strength is a potion first, and Staff of Power is wondrous while a
            // quarterstaff is a weapon (so "staff" sits in the wondrous list).
            return when {
                mentions(CONSUMABLE_NOUNS) -> POTION
                mentions(setOf("scroll")) -> SCROLL
                mentions(setOf("shield")) -> SHIELD
                mentions(ARMOR_NOUNS) -> ARMOR
                mentions(WEAPON_NOUNS) -> WEAPON
                mentions(WONDROUS_NOUNS) -> WONDROUS
                else -> null
            }
        }

        private val CONSUMABLE_NOUNS = setOf("potion", "oil", "elixir", "philter", "draught", "dust", "salve")

        private val ARMOR_NOUNS = setOf("armor", "armour", "mail", "plate", "breastplate", "cuirass", "brigandine", "chainmail")

        private val WEAPON_NOUNS = setOf(
            "sword", "longsword", "shortsword", "greatsword", "scimitar", "rapier", "dagger",
            "axe", "greataxe", "handaxe", "mace", "maul", "hammer", "warhammer", "club",
            "greatclub", "quarterstaff", "spear", "javelin", "pike", "lance", "halberd",
            "glaive", "trident", "flail", "morningstar", "sickle", "whip", "bow", "longbow",
            "shortbow", "crossbow", "sling", "dart", "blade", "arrow", "bolt",
        )

        private val WONDROUS_NOUNS = setOf(
            "ring", "robe", "cloak", "mantle", "cape", "amulet", "necklace", "periapt",
            "brooch", "medallion", "talisman", "circlet", "crown", "helm", "headband", "hat",
            "mask", "goggles", "lenses", "eyes", "gauntlet", "glove", "bracer", "boot",
            "slipper", "sandal", "belt", "girdle", "wand", "rod", "staff", "orb", "tome",
            "manual", "horn", "figurine", "stone", "pearl", "ioun", "instrument", "decanter",
            "censer", "deck", "carpet", "broom", "bag", "quiver", "gem", "crystal", "idol",
            "sphere", "chime", "candle", "lantern",
        )
    }

    /** Singular label for chip pickers and preview headers ("Weapon", "Wondrous"). */
    val displayName: String
        get() = when (this) {
            WEAPON -> "Weapon"
            ARMOR -> "Armor"
            SHIELD -> "Shield"
            AMMUNITION -> "Ammunition"
            POTION -> "Potion"
            SCROLL -> "Scroll"
            WONDROUS -> "Wondrous"
            TOOL -> "Tool"
            GEAR -> "Gear"
            TREASURE -> "Treasure"
        }
}

/** Weapon-specific data, present only when [InventoryItem.kind] is [ItemKind.WEAPON]. */
@Serializable
data class WeaponProperties(
    val kind: Kind,
    val damageDice: String,
    val damageType: String,
    val properties: List<String> = emptyList(),
    /** Feet — ranged / thrown only. */
    val normalRange: Int? = null,
    val longRange: Int? = null,
) {
    /** Lowercase serial names match the bundled catalogue JSON (`"melee"`). */
    @Serializable
    enum class Kind {
        @SerialName("melee") MELEE,
        @SerialName("ranged") RANGED,
    }

    /** "1d8 slashing" summary line for the edit-sheet property card. */
    val damageLine: String get() = "$damageDice $damageType".trim()
}

/** Armor-specific data, present only when [InventoryItem.kind] is [ItemKind.ARMOR]. */
@Serializable
data class ArmorProperties(
    val kind: Kind,
    val baseAC: Int,
    /**
     * DEX bonus cap under this armor: `null` = uncapped (light), `2` = medium,
     * `0` = heavy (DEX ignored entirely).
     */
    val dexCap: Int? = null,
    val stealthDisadvantage: Boolean = false,
    val strengthRequirement: Int? = null,
) {
    /** Lowercase serial names match the bundled catalogue JSON (`"light"`). */
    @Serializable
    enum class Kind {
        @SerialName("light") LIGHT,
        @SerialName("medium") MEDIUM,
        @SerialName("heavy") HEAVY,
    }

    /**
     * Effective AC for a given DEX modifier under this armor's cap rule:
     * - **Light** (`dexCap == null`): full DEX, positive or negative.
     * - **Medium** (`dexCap == 2`): DEX capped at +2; a negative DEX still applies.
     * - **Heavy** (`dexCap == 0`): DEX ignored entirely.
     */
    fun effectiveAC(dexModifier: Int): Int {
        val dexContribution = when {
            dexCap == 0 -> 0
            dexCap != null -> minOf(dexCap, dexModifier)
            else -> dexModifier
        }
        return baseAC + dexContribution
    }
}

/**
 * The spell a scroll discharges when used. Denormalised ([spellName]/[spellLevel])
 * so the codex shows it offline without a catalogue lookup; [spellID] links back to
 * the SRD spell. [useScrollDC] is stored for the (deferred) scroll-DC cast path —
 * runtime mechanics are out of scope for this slice.
 */
@Serializable
data class ScrollProperties(
    val spellID: String,
    val spellName: String,
    val spellLevel: Int,
    val useScrollDC: Boolean = false,
)

/**
 * Charge meter for wondrous items (wands, rings, horns). [rechargeDice] is the dice
 * formula rolled when the item recharges (`1d6+1` for a wand). Charge-consume /
 * recharge runtime is deferred — this slice only stores the data. The clamp keeps
 * `current` within `0..max`.
 */
@Serializable
data class WondrousProperties(
    val currentCharges: Int = 0,
    val maxCharges: Int = 0,
    val rechargeDice: String = "1d6+1",
) {
    /** Max floored at 0; current clamped into `0..max`. */
    val normalized: WondrousProperties
        get() {
            val cappedMax = maxOf(0, maxCharges)
            return copy(maxCharges = cappedMax, currentCharges = currentCharges.coerceIn(0, cappedMax))
        }
}

/**
 * A single carried item. Weapons/armor carry their typed sub-properties; mundane
 * gear, potions, and treasure leave them null. [equipped] only matters for
 * equippable kinds. [catalogueID] links back to the bundled SRD entry it came from.
 *
 * The magic-item rider fields fold into live stats while the item is [equipped]:
 * [acBonus] / [acOverride] through [ArmorClassCalculator], [saveBonus] /
 * [saveBonusByAbility] through `savingThrowBonus`, [skillBonus] through the skills
 * tab, and [spellDcBonus] / [spellAttackBonus] through the casting stats. The
 * free-text lists ([damageResistances], [advantageOn], …) stay display-only —
 * the player decides when a situational rider applies.
 */
@Serializable
data class InventoryItem(
    val id: String,
    val name: String,
    val kind: ItemKind,
    val weapon: WeaponProperties? = null,
    val armor: ArmorProperties? = null,
    val quantity: Int = 1,
    val equipped: Boolean = false,
    val notes: String = "",
    val catalogueID: String? = null,
    val weight: Double? = null,
    /** Flat AC bonus from an equipped item (shield +2, cloak/ring of protection +1). */
    val acBonus: Int? = null,
    /** Linked spell for scrolls; null for narration-only scrolls / non-scrolls. */
    val scroll: ScrollProperties? = null,
    /** Charge meter for wondrous items; null for non-charged / non-wondrous items. */
    val wondrous: WondrousProperties? = null,
    /** Flat bonus applied to every saving throw (Cloak of Protection). */
    val saveBonus: Int? = null,
    /** Attack-roll bonus for this weapon (+1 longsword). */
    val attackBonus: Int? = null,
    /** Per-ability save bonuses, on top of the flat [saveBonus]; only non-zero entries kept. */
    val saveBonusByAbility: Map<Ability, Int> = emptyMap(),
    /** Skill-check bonuses keyed by skill id (e.g. `"stealth"`); only non-zero entries kept. */
    val skillBonus: Map<String, Int> = emptyMap(),
    /** Damage types this item grants resistance to (half damage). */
    val damageResistances: List<String> = emptyList(),
    /** Damage types this item grants immunity to (no damage). */
    val damageImmunities: List<String> = emptyList(),
    /** Conditions this item grants immunity to (frightened, charmed, …). */
    val conditionImmunities: List<String> = emptyList(),
    /** Free-text descriptors this item grants advantage on ("Perception checks"). */
    val advantageOn: List<String> = emptyList(),
    /**
     * Replaces the unarmored AC base while equipped — Robe of the Archmagi (15 + DEX),
     * Bracers of Defense (13 + DEX). Competes with spell / class overrides (Mage Armor,
     * Unarmored Defense) highest-wins and, per RAW, drops out entirely once armor is
     * worn. Distinct from [acBonus], which stacks on top of whichever base won.
     */
    val acOverride: ACOverrideFormula? = null,
    /** Spell save DC bonus while equipped — Robe of the Archmagi +2, Rod of the Pact Keeper +1. */
    val spellDcBonus: Int? = null,
    /** Spell attack bonus while equipped. Separate from [attackBonus], which is weapon-scoped. */
    val spellAttackBonus: Int? = null,
) {
    companion object {
        /** Fresh random id for a newly-created item (catalogue `makeItem`, UI add). */
        fun newId(): String = UUID.randomUUID().toString()
    }
}

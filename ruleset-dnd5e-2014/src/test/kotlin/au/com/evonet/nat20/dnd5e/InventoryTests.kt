package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")

// ── Helpers ───────────────────────────────────────────────────────────────────

private val ruleset = DnD5eRuleset()

private fun characterWith(
    dex: Int = 10,
    maxHp: Int = 20,
    currentHp: Int = maxHp,
    inventory: List<InventoryItem> = emptyList(),
    coins: Map<Coin, Int> = emptyMap(),
): Character = Character.new(
    name = "Aria",
    ruleset = ruleset,
    payload = DnD5ePayload(
        classes = listOf(ClassEntry("fighter", 1)),
        abilityScores = AbilityScores(dexterity = dex),
        maxHp = maxHp,
        currentHp = currentHp,
        inventory = inventory,
        coins = coins,
    ),
    timestamp = NOW,
)

private fun Character.payload() = payload as DnD5ePayload

private fun gear(id: String, catalogueID: String? = id, quantity: Int = 1) = InventoryItem(
    id = id, name = id.replaceFirstChar { it.uppercase() }, kind = ItemKind.GEAR,
    quantity = quantity, catalogueID = catalogueID,
)

// ── Armor properties: the DEX-cap rule ────────────────────────────────────────

class ArmorPropertiesTests {
    @ParameterizedTest
    @CsvSource(
        // dexCap, baseAC, dexMod, expectedAC
        ", 11, 4, 15",      // light (uncapped): full DEX
        ", 11, -1, 10",     // light: negative DEX still applies
        "2, 14, 4, 16",     // medium: capped at +2
        "2, 14, 1, 15",     // medium: under the cap → full
        "2, 14, -1, 13",    // medium: negative still applies
        "0, 16, 4, 16",     // heavy: DEX ignored
        "0, 16, -2, 16",    // heavy: negative DEX ignored too
    )
    fun `effective AC honours the DEX cap by armor kind`(dexCap: Int?, baseAC: Int, dexMod: Int, expected: Int) {
        val props = ArmorProperties(kind = ArmorProperties.Kind.MEDIUM, baseAC = baseAC, dexCap = dexCap)
        assertEquals(expected, props.effectiveAC(dexMod))
    }
}

// ── ArmorClassCalculator ──────────────────────────────────────────────────────

class ArmorClassCalculatorTests {
    private fun armorItem(name: String, kind: ArmorProperties.Kind, baseAC: Int, dexCap: Int?, equipped: Boolean) =
        InventoryItem(
            id = name, name = name, kind = ItemKind.ARMOR, equipped = equipped,
            armor = ArmorProperties(kind = kind, baseAC = baseAC, dexCap = dexCap),
        )

    private fun shield(equipped: Boolean) = InventoryItem(
        id = "shield", name = "Shield", kind = ItemKind.SHIELD, equipped = equipped, acBonus = 2,
    )

    @Test
    fun `unarmored AC is 10 plus DEX`() {
        assertEquals(13, characterWith(dex = 16).payload().armorClass)
    }

    @Test
    fun `equipped heavy armor ignores DEX`() {
        val plate = armorItem("Plate", ArmorProperties.Kind.HEAVY, baseAC = 18, dexCap = 0, equipped = true)
        // DEX +3 would push unarmored to 13, but heavy ignores it: flat 18.
        assertEquals(18, characterWith(dex = 16, inventory = listOf(plate)).payload().armorClass)
    }

    @Test
    fun `medium armor caps DEX at plus two`() {
        val halfPlate = armorItem("Half Plate", ArmorProperties.Kind.MEDIUM, baseAC = 15, dexCap = 2, equipped = true)
        assertEquals(17, characterWith(dex = 18, inventory = listOf(halfPlate)).payload().armorClass) // 15 + 2
    }

    @Test
    fun `light armor adds full DEX`() {
        val leather = armorItem("Leather", ArmorProperties.Kind.LIGHT, baseAC = 11, dexCap = null, equipped = true)
        assertEquals(15, characterWith(dex = 18, inventory = listOf(leather)).payload().armorClass) // 11 + 4
    }

    @Test
    fun `shield stacks on top of armor`() {
        val chain = armorItem("Chain Mail", ArmorProperties.Kind.HEAVY, baseAC = 16, dexCap = 0, equipped = true)
        val ac = characterWith(dex = 12, inventory = listOf(chain, shield(equipped = true))).payload().armorClass
        assertEquals(18, ac) // 16 + 2 shield
    }

    @Test
    fun `unequipped armor and shield do not count`() {
        val leather = armorItem("Leather", ArmorProperties.Kind.LIGHT, baseAC = 11, dexCap = null, equipped = false)
        val ac = characterWith(dex = 14, inventory = listOf(leather, shield(equipped = false))).payload().armorClass
        assertEquals(12, ac) // unarmored 10 + 2 DEX
    }

    @Test
    fun `breakdown rows explain the total`() {
        val leather = armorItem("Leather", ArmorProperties.Kind.LIGHT, baseAC = 11, dexCap = null, equipped = true)
        val breakdown = ArmorClassCalculator.compute(
            characterWith(dex = 14, inventory = listOf(leather, shield(equipped = true))).payload(),
        )
        assertEquals(listOf("Leather", "DEX", "Shield"), breakdown.rows.map { it.label })
        assertEquals(15, breakdown.total) // 11 + 2 + 2
    }
}

// ── Equipment catalogues ──────────────────────────────────────────────────────

class EquipmentCatalogueTests {
    @Test
    fun `weapons load and a longsword makes a versatile melee item`() {
        assertTrue(DnD5eCatalog.weapons.isNotEmpty())
        val longsword = DnD5eCatalog.weapon("longsword")
        assertNotNull(longsword)
        val item = longsword!!.makeItem(equipped = true)
        assertEquals(ItemKind.WEAPON, item.kind)
        assertEquals(WeaponProperties.Kind.MELEE, item.weapon?.kind)
        assertEquals("1d8", item.weapon?.damageDice)
        assertEquals("longsword", item.catalogueID)
        assertTrue(item.equipped)
    }

    @Test
    fun `armor loads and chain mail decodes as heavy with a zero dex cap`() {
        val chain = DnD5eCatalog.armorPiece("chain-mail")
        assertNotNull(chain)
        assertEquals(ArmorProperties.Kind.HEAVY, chain!!.kind)
        assertEquals(0, chain.dexCap)
        val item = chain.makeItem(equipped = true)
        assertEquals(16, item.armor?.baseAC)
    }

    @Test
    fun `a shield from gear is a SHIELD item carrying a plus two AC bonus`() {
        val shield = DnD5eCatalog.gearPiece("shield")
        assertNotNull(shield)
        val item = shield!!.makeItem(equipped = true)
        assertEquals(ItemKind.SHIELD, item.kind)
        assertEquals(2, item.acBonus)
    }

    @Test
    fun `gear kinds map onto ItemKind`() {
        assertEquals(ItemKind.POTION, DnD5eCatalog.gearPiece("potion-of-healing")?.itemKind)
    }
}

// ── Inventory intents ─────────────────────────────────────────────────────────

class InventoryIntentTests {
    @Test
    fun `acquiring stacks non-equippable items by catalogue id`() {
        val start = characterWith(inventory = listOf(gear("arrows", catalogueID = "arrows-20", quantity = 20)))
        val more = AcquireItem(gear("arrows2", catalogueID = "arrows-20", quantity = 20))
            .applyTo(start, ruleset).character
        assertEquals(1, more.payload().inventory.size)
        assertEquals(40, more.payload().inventory.first().quantity)
    }

    @Test
    fun `acquiring weapons appends distinct lines even when identical`() {
        val dagger = DnD5eCatalog.weapon("dagger")!!.makeItem()
        var c = characterWith()
        c = AcquireItem(dagger).applyTo(c, ruleset).character
        c = AcquireItem(DnD5eCatalog.weapon("dagger")!!.makeItem()).applyTo(c, ruleset).character
        assertEquals(2, c.payload().inventory.size) // equippable → separate lines
    }

    @Test
    fun `acquire emits a summary`() {
        val result = AcquireItem(gear("rope", quantity = 1)).applyTo(characterWith(), ruleset)
        assertEquals("Acquired Rope", result.event.summary)
    }

    @Test
    fun `dropping part of a stack reduces quantity`() {
        val start = characterWith(inventory = listOf(gear("arrows", quantity = 20)))
        val result = DropItem(itemID = "arrows", quantity = 5).applyTo(start, ruleset)
        assertEquals(15, result.character.payload().inventory.first().quantity)
        assertEquals("Dropped Arrows ×5", result.event.summary)
    }

    @Test
    fun `dropping the whole stack removes the line`() {
        val start = characterWith(inventory = listOf(gear("torch", quantity = 2)))
        val result = DropItem(itemID = "torch", quantity = 99).applyTo(start, ruleset)
        assertTrue(result.character.payload().inventory.isEmpty())
        assertEquals("Dropped Torch ×2", result.event.summary) // clamped to what was held
    }

    @Test
    fun `dropping an unknown item is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            DropItem(itemID = "ghost").applyTo(characterWith(), ruleset)
        }
    }

    @Test
    fun `using a healing potion consumes one and restores HP clamped to max`() {
        val potion = InventoryItem(id = "potion", name = "Potion of Healing", kind = ItemKind.POTION, quantity = 2)
        val start = characterWith(maxHp = 20, currentHp = 16, inventory = listOf(potion))
        val result = UseItem(itemID = "potion", healingRolled = 7).applyTo(start, ruleset)
        val p = result.character.payload()
        assertEquals(1, p.inventory.first().quantity) // one consumed
        assertEquals(20, p.currentHp) // 16 + 7 clamped to 20
        assertEquals("Used Potion of Healing — restored 7 HP (HP 16 → 20)", result.event.summary)
    }

    @Test
    fun `using the last of an item removes it`() {
        val scroll = InventoryItem(id = "scroll", name = "Scroll", kind = ItemKind.SCROLL, quantity = 1)
        val result = UseItem(itemID = "scroll").applyTo(characterWith(inventory = listOf(scroll)), ruleset)
        assertTrue(result.character.payload().inventory.isEmpty())
        assertEquals("Used Scroll", result.event.summary)
    }
}

// ── Coins ─────────────────────────────────────────────────────────────────────

class CoinIntentTests {
    @Test
    fun `gaining coins adds to the pouch`() {
        val result = AdjustCoin(Coin.GP, delta = 15, source = "quest reward").applyTo(characterWith(), ruleset)
        assertEquals(15, result.character.payload().coins[Coin.GP])
        assertEquals("Gained 15 GP — quest reward", result.event.summary)
    }

    @Test
    fun `spending reduces the pouch and removes a zeroed denomination`() {
        val start = characterWith(coins = mapOf(Coin.GP to 10))
        val result = AdjustCoin(Coin.GP, delta = -10).applyTo(start, ruleset)
        assertNull(result.character.payload().coins[Coin.GP]) // removed at zero
        assertEquals("Spent 10 GP", result.event.summary)
    }

    @Test
    fun `overspending is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            AdjustCoin(Coin.GP, delta = -5).applyTo(characterWith(coins = mapOf(Coin.GP to 3)), ruleset)
        }
    }

    @Test
    fun `a zero adjustment is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            AdjustCoin(Coin.SP, delta = 0).applyTo(characterWith(), ruleset)
        }
    }
}

// ── Codec ─────────────────────────────────────────────────────────────────────

class InventoryCodecTests {
    @Test
    fun `payload with inventory and coins round-trips`() {
        val payload = DnD5ePayload(
            classes = listOf(ClassEntry("fighter", 1)),
            inventory = listOf(
                DnD5eCatalog.weapon("longsword")!!.makeItem(equipped = true).copy(id = "w1"),
                DnD5eCatalog.armorPiece("chain-mail")!!.makeItem(equipped = true).copy(id = "a1"),
                DnD5eCatalog.gearPiece("potion-of-healing")!!.makeItem(quantity = 3).copy(id = "g1"),
            ),
            coins = mapOf(Coin.GP to 25, Coin.SP to 8),
        )
        val decoded = ruleset.decodePayload(ruleset.encodePayload(payload))
        assertEquals(payload, decoded)
    }

    @Test
    fun `inventory and coin events round-trip with their type ids`() {
        val events = listOf(
            ItemAcquiredEvent(itemName = "Longsword"),
            ItemDroppedEvent(itemName = "Arrows", quantity = 10),
            ItemUsedEvent(itemName = "Potion of Healing", healingRolled = 7, previousHp = 4, newHp = 11),
            CoinAdjustedEvent(coin = Coin.GP, delta = -5, source = "bribe"),
        )
        for (event in events) {
            val typeId = ruleset.eventTypeId(event)
            assertFalse(typeId == "dnd5e.unknown")
            val decoded = ruleset.decodeEvent(ruleset.encodeEvent(event), typeId)
            assertEquals(event, decoded)
        }
    }
}

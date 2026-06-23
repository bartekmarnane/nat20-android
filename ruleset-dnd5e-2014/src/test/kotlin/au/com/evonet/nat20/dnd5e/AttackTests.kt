package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.AttackOutcome
import au.com.evonet.nat20.dnd5e.core.RollSpec
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val ruleset = DnD5eRuleset()

private fun fighter(str: Int = 16, dex: Int = 12, level: Int = 5, inventory: List<InventoryItem> = emptyList()): Character =
    Character.new(
        "Bron", ruleset,
        DnD5ePayload(
            classes = listOf(ClassEntry("fighter", level)),
            abilityScores = AbilityScores(strength = str, dexterity = dex),
            inventory = inventory,
        ),
        NOW,
    )

private fun Character.payload() = payload as DnD5ePayload

class DamageRiderTests {
    @Test
    fun `sneak attack scales one d6 per two rogue levels rounded up`() {
        assertEquals(1, DamageRiders.sneakAttackDice(1))
        assertEquals(1, DamageRiders.sneakAttackDice(2))
        assertEquals(2, DamageRiders.sneakAttackDice(3))
        assertEquals(3, DamageRiders.sneakAttackDice(5))
        assertEquals(10, DamageRiders.sneakAttackDice(20))
        assertEquals(0, DamageRiders.sneakAttackDice(0))
    }

    @Test
    fun `sneak attack needs a finesse or ranged weapon`() {
        val dagger = WeaponProperties(WeaponProperties.Kind.MELEE, "1d4", "piercing", listOf("Finesse", "Light"))
        val club = WeaponProperties(WeaponProperties.Kind.MELEE, "1d4", "bludgeoning", emptyList())
        val bow = WeaponProperties(WeaponProperties.Kind.RANGED, "1d8", "piercing", listOf("Ammunition"))
        assertEquals(true, DamageRiders.sneakAttackEligible(dagger))
        assertEquals(false, DamageRiders.sneakAttackEligible(club))
        assertEquals(true, DamageRiders.sneakAttackEligible(bow))
    }

    @Test
    fun `divine smite is two d8 plus one per slot level above first, capped at five, plus one vs fiend or undead`() {
        assertEquals(2, DamageRiders.divineSmiteDice(1))
        assertEquals(3, DamageRiders.divineSmiteDice(2))
        assertEquals(5, DamageRiders.divineSmiteDice(4))
        assertEquals(5, DamageRiders.divineSmiteDice(5)) // capped
        assertEquals(6, DamageRiders.divineSmiteDice(4, vsUndeadOrFiend = true)) // exceeds the cap, RAW
    }

    private fun paladin(slots: Map<Int, Int>) = Character.new(
        "Dame", ruleset,
        DnD5ePayload(
            classes = listOf(ClassEntry("paladin", 5)),
            abilityScores = AbilityScores(strength = 16),
            currentSpellSlots = slots,
        ),
        NOW,
    )

    @Test
    fun `a smite attack expends the slot and journals the rider`() {
        val result = MakeAttack(
            weaponName = "Longsword", attackTotal = 18, outcome = AttackOutcome.HIT,
            damage = 20, damageType = "slashing",
            riders = listOf("Divine Smite (2nd)"), expendSlotLevel = 2,
        ).applyTo(paladin(mapOf(1 to 4, 2 to 3)), ruleset)
        assertEquals(2, result.character.payload().currentSpellSlots[2]) // 3 → 2
        assertTrue(result.event.summary.contains("Divine Smite (2nd)"))
    }

    @Test
    fun `a smite with no slot of that level is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            MakeAttack("Longsword", 18, AttackOutcome.HIT, damage = 20, expendSlotLevel = 3)
                .applyTo(paladin(mapOf(1 to 2)), ruleset)
        }
    }

    @Test
    fun `a plain attack leaves slots untouched`() {
        val result = MakeAttack("Club", 12, AttackOutcome.HIT, damage = 5).applyTo(paladin(mapOf(1 to 2)), ruleset)
        assertEquals(2, result.character.payload().currentSpellSlots[1])
    }

    @Test
    fun `a ranged attack spends one piece of ammunition and removes the empty stack`() {
        val arrows = InventoryItem("ar", "Arrows", ItemKind.AMMUNITION, quantity = 2)
        val archer = fighter(level = 3, inventory = listOf(arrows))
        val once = MakeAttack("Shortbow", 15, AttackOutcome.HIT, damage = 6, ammoItemId = "ar").applyTo(archer, ruleset)
        assertEquals(1, once.character.payload().inventory.first { it.id == "ar" }.quantity)
        // Spend the last arrow → the stack is removed.
        val empty = MakeAttack("Shortbow", 15, AttackOutcome.MISS, ammoItemId = "ar").applyTo(once.character, ruleset)
        assertFalse(empty.character.payload().inventory.any { it.id == "ar" })
    }
}

class FightingStyleTests {
    @Test
    fun `grant levels match the base classes`() {
        assertEquals(1, FightingStyles.grantLevel("fighter"))
        assertEquals(2, FightingStyles.grantLevel("paladin"))
        assertEquals(2, FightingStyles.grantLevel("ranger"))
        assertNull(FightingStyles.grantLevel("wizard"))
        assertTrue(FightingStyles.grantsBy("fighter", 1))
        assertFalse(FightingStyles.grantsBy("paladin", 1))
        assertTrue(FightingStyles.grantsBy("paladin", 2))
    }

    private fun styledFighter(styles: List<String>, inventory: List<InventoryItem> = emptyList()) = Character.new(
        "Bron", ruleset,
        DnD5ePayload(
            classes = listOf(ClassEntry("fighter", 3)),
            abilityScores = AbilityScores(strength = 16, dexterity = 16),
            fightingStyles = styles,
            inventory = inventory,
        ),
        NOW,
    ).payload()

    @Test
    fun `defense adds plus one AC only while armored`() {
        val leather = DnD5eCatalog.armorPiece("leather")!!.makeItem(equipped = true)
        val unarmored = styledFighter(listOf("defense"))
        val armored = styledFighter(listOf("defense"), inventory = listOf(leather))
        // Unarmored: no Defense bonus (RAW gate). Armored: leather 11 + DEX 3 + Defense 1 = 15.
        assertEquals(13, unarmored.armorClass) // 10 + DEX 3, no Defense
        assertEquals(15, armored.armorClass)
    }

    @Test
    fun `archery adds plus two to ranged attacks and dueling adds plus two melee damage`() {
        val bow = InventoryItem("b", "Shortbow", ItemKind.WEAPON, equipped = true, weapon = WeaponProperties(WeaponProperties.Kind.RANGED, "1d6", "piercing", listOf("Ammunition")))
        val sword = InventoryItem("s", "Longsword", ItemKind.WEAPON, equipped = true, weapon = WeaponProperties(WeaponProperties.Kind.MELEE, "1d8", "slashing", emptyList()))

        val archer = AttackMath.forWeapon(bow, styledFighter(listOf("archery"), inventory = listOf(bow)))!!
        assertTrue(archer.attackBonuses.any { it.label == "Archery" && it.value == 2 })

        val duelist = AttackMath.forWeapon(sword, styledFighter(listOf("dueling"), inventory = listOf(sword)))!!
        assertTrue(duelist.damageBonuses.any { it.label == "Dueling" && it.value == 2 })
        // Dueling doesn't apply to a ranged weapon.
        val duelistBow = AttackMath.forWeapon(bow, styledFighter(listOf("dueling"), inventory = listOf(bow)))!!
        assertFalse(duelistBow.damageBonuses.any { it.label == "Dueling" })
    }

    @Test
    fun `a level-up records the fighting style and rejects an unknown one`() {
        val paladin = Character.new("Dame", ruleset, DnD5ePayload(classes = listOf(ClassEntry("paladin", 1)), abilityScores = AbilityScores(strength = 16)), NOW)
        val result = LevelUp("paladin", fightingStyle = "dueling", className = "Paladin").applyTo(paladin, ruleset)
        assertTrue("dueling" in result.character.payload().fightingStyles)
        assertThrows(CharacterIntentError.Invalid::class.java) {
            LevelUp("paladin", fightingStyle = "nope").applyTo(paladin, ruleset)
        }
    }

    @Test
    fun `fightingStyles round-trip through the payload codec`() {
        val payload = DnD5ePayload(classes = listOf(ClassEntry("fighter", 1)), fightingStyles = listOf("defense"))
        assertEquals(payload, ruleset.decodePayload(ruleset.encodePayload(payload)))
    }
}

class DiceParseTests {
    @Test
    fun `parses standard weapon notation`() {
        assertEquals(RollSpec.d(1, 8), RollSpec.parse("1d8"))
        assertEquals(RollSpec.d(2, 6), RollSpec.parse("2d6"))
        assertEquals(RollSpec(1, 10, 2), RollSpec.parse("1d10+2"))
        assertNull(RollSpec.parse("sword"))
    }

    @Test
    fun `crit doubles the dice count`() {
        assertEquals(RollSpec.d(2, 8), RollSpec.d(1, 8).critDoubled())
    }
}

class AttackMathTests {
    @Test
    fun `a longsword uses STR and proficiency`() {
        val sword = DnD5eCatalog.weapon("longsword")!!.makeItem(equipped = true)
        val attack = AttackMath.forWeapon(sword, fighter(str = 16, level = 5).payload())!!
        // STR 16 → +3, fighter L5 proficiency +3.
        assertEquals("STR", attack.abilityLabel)
        assertEquals(listOf(3, 3), attack.attackBonuses.map { it.value })
        assertEquals(RollSpec.d(1, 8), attack.damageSpec)
        assertEquals(3, attack.damageBonuses.single().value)
        assertEquals("slashing", attack.damageType)
    }

    @Test
    fun `a finesse weapon takes the better of STR and DEX`() {
        val dagger = DnD5eCatalog.weapon("dagger")!!.makeItem(equipped = true)
        val attack = AttackMath.forWeapon(dagger, fighter(str = 10, dex = 18).payload())!!
        assertEquals("DEX", attack.abilityLabel) // DEX +4 beats STR +0
    }

    @Test
    fun `a ranged weapon uses DEX`() {
        val bow = DnD5eCatalog.weapon("shortbow")!!.makeItem(equipped = true)
        val attack = AttackMath.forWeapon(bow, fighter(str = 16, dex = 14).payload())!!
        assertEquals("DEX", attack.abilityLabel)
    }

    @Test
    fun `equipped-weapons and initiative bonus read off the payload`() {
        val sword = DnD5eCatalog.weapon("longsword")!!.makeItem(equipped = true)
        val p = fighter(dex = 14, inventory = listOf(sword)).payload()
        assertEquals(1, p.equippedWeapons.size)
        assertEquals(2, p.initiativeBonus) // DEX 14 → +2
    }
}

class AttackIntentTests {
    @Test
    fun `a hit logs damage`() {
        val result = MakeAttack("Longsword", attackTotal = 18, outcome = AttackOutcome.HIT, damage = 11, damageType = "Slashing").applyTo(fighter(), ruleset)
        assertEquals("Hit with Longsword for 11 slashing damage (rolled 18)", result.event.summary)
    }

    @Test
    fun `a miss drops any damage`() {
        val result = MakeAttack("Longsword", attackTotal = 7, outcome = AttackOutcome.MISS, damage = 11).applyTo(fighter(), ruleset)
        assertEquals("Attacked with Longsword — missed (rolled 7)", result.event.summary)
        assertNull((result.event as AttackEvent).damage)
    }

    @Test
    fun `a crit reads as a critical hit with target`() {
        val result = MakeAttack("Dagger", attackTotal = 24, outcome = AttackOutcome.CRITICAL, damage = 9, damageType = "Piercing", target = "the goblin").applyTo(fighter(), ruleset)
        assertEquals("Critical hit the goblin with Dagger for 9 piercing damage!", result.event.summary)
    }

    @Test
    fun `a blank weapon is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            MakeAttack("  ", attackTotal = 10, outcome = AttackOutcome.HIT).applyTo(fighter(), ruleset)
        }
    }

    @Test
    fun `the attacker is unchanged`() {
        val before = fighter()
        val after = MakeAttack("Longsword", 18, AttackOutcome.HIT, 11).applyTo(before, ruleset).character
        assertEquals(before.payload, after.payload)
    }
}

class InitiativeIntentTests {
    @Test
    fun `setting and clearing initiative`() {
        var c = SetInitiative(17).applyTo(fighter(), ruleset).character
        assertEquals(17, c.payload().initiative)
        val cleared = SetInitiative(null).applyTo(c, ruleset)
        assertNull(cleared.character.payload().initiative)
        assertEquals("Cleared initiative", cleared.event.summary)
    }
}

class CombatCodecTests {
    @Test
    fun `payload with initiative round-trips`() {
        val payload = DnD5ePayload(classes = listOf(ClassEntry("fighter", 5)), initiative = 17)
        assertEquals(payload, ruleset.decodePayload(ruleset.encodePayload(payload)))
    }

    @Test
    fun `attack and initiative events round-trip`() {
        val events = listOf(
            AttackEvent("Longsword", 18, AttackOutcome.HIT, damage = 11, damageType = "Slashing", target = "orc"),
            InitiativeEvent(17),
        )
        for (event in events) {
            val typeId = ruleset.eventTypeId(event)
            assertFalse(typeId == "dnd5e.unknown")
            assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
        }
    }
}

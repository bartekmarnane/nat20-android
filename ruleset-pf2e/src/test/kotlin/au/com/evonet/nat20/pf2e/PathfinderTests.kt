package au.com.evonet.nat20.pf2e

import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfAbilityScores
import au.com.evonet.nat20.pf2e.core.PfSkill
import au.com.evonet.nat20.pf2e.core.Proficiency
import au.com.evonet.nat20.pf2e.core.Save
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val ruleset = PathfinderRuleset()

private fun character(payload: PathfinderPayload): Character = Character.new("Seoni", ruleset, payload, NOW)
private fun Character.p() = payload as PathfinderPayload

class PathfinderIdentityTests {
    @Test
    fun `identity is the first non-D&D ruleset`() {
        assertEquals("pf2e-remaster", ruleset.id)
        assertEquals("Pathfinder 2e (Remaster)", ruleset.displayName)
        assertTrue(ruleset.makeInitialPayload("Seoni") is PathfinderPayload)
    }
}

class PathfinderStatsTests {
    private val sorcerer = PathfinderPayload(
        className = "sorcerer", level = 3, keyAbility = PfAbility.CHARISMA,
        abilityScores = PfAbilityScores(dexterity = 14, wisdom = 11, charisma = 18),
        perception = Proficiency.TRAINED, unarmoredProficiency = Proficiency.TRAINED, classDC = Proficiency.TRAINED,
        saves = mapOf(Save.WILL to Proficiency.EXPERT),
        skills = mapOf(PfSkill.DIPLOMACY to Proficiency.EXPERT),
    )

    @Test
    fun `derived stats are ability modifier plus level plus rank`() {
        // Perception: WIS 0 + (3 + 2 trained) = 5.
        assertEquals(5, sorcerer.perceptionBonus)
        // AC: 10 + DEX 2 + (3 + 2 trained) = 17.
        assertEquals(17, sorcerer.armorClass)
        // Will save: WIS 0 + (3 + 4 expert) = 7. Untrained Fortitude: CON 0 + 0 = 0.
        assertEquals(7, sorcerer.saveBonus(Save.WILL))
        assertEquals(0, sorcerer.saveBonus(Save.FORTITUDE))
        // Diplomacy (expert): CHA 4 + (3 + 4) = 11. Untrained Athletics: STR 0 + 0 = 0.
        assertEquals(11, sorcerer.skillBonus(PfSkill.DIPLOMACY))
        assertEquals(0, sorcerer.skillBonus(PfSkill.ATHLETICS))
        // Class DC: 10 + CHA 4 + (3 + 2) = 19.
        assertEquals(19, sorcerer.classDcValue)
    }

    @Test
    fun `frightened applies a blanket status penalty`() {
        val scared = sorcerer.copy(conditions = listOf(au.com.evonet.nat20.pf2e.core.ValuedCondition("frightened", 2)))
        assertEquals(sorcerer.perceptionBonus - 2, scared.perceptionBonus)
        assertEquals(sorcerer.skillBonus(PfSkill.DIPLOMACY) - 2, scared.skillBonus(PfSkill.DIPLOMACY))
        assertEquals(sorcerer.armorClass - 2, scared.armorClass)
    }
}

class PathfinderVitalsTests {
    private fun fresh(currentHp: Int = 30, maxHp: Int = 30, dying: Int = 0, wounded: Int = 0) =
        character(PathfinderPayload(maxHp = maxHp, currentHp = currentHp, dying = dying, wounded = wounded))

    @Test
    fun `dropping to 0 HP starts dying at one plus wounded`() {
        val downed = PfTakeDamage(40).applyTo(fresh(wounded = 1), ruleset)
        assertEquals(0, downed.character.p().currentHp)
        assertEquals(2, downed.character.p().dying) // 1 + wounded 1
        assertTrue(downed.event.summary.contains("Dying 2"))
        // Further damage while down increases dying.
        val worse = PfTakeDamage(5).applyTo(downed.character, ruleset)
        assertEquals(3, worse.character.p().dying)
    }

    @Test
    fun `recovering from dying clears it and increases wounded`() {
        val downed = PfTakeDamage(40).applyTo(fresh(), ruleset).character // dying 1
        val healed = PfHeal(10).applyTo(downed, ruleset)
        assertEquals(10, healed.character.p().currentHp)
        assertEquals(0, healed.character.p().dying)
        assertEquals(1, healed.character.p().wounded)
        assertTrue(healed.event.summary.contains("no longer dying"))
    }

    @Test
    fun `hero points clamp and conditions apply or update`() {
        val gained = PfAdjustHeroPoints(1).applyTo(fresh(), ruleset) // 1 -> 2
        assertEquals(2, gained.character.p().heroPoints)
        assertThrows(CharacterIntentError.Invalid::class.java) { PfAdjustHeroPoints(1).applyTo(character(PathfinderPayload(heroPoints = 3)), ruleset) }
        var c = PfApplyCondition("frightened", 2).applyTo(fresh(), ruleset).character
        assertEquals(2, c.p().conditions.first().value)
        c = PfApplyCondition("frightened", 1).applyTo(c, ruleset).character // updates, not duplicates
        assertEquals(1, c.p().conditions.size)
        assertEquals(1, c.p().conditions.first().value)
        c = PfClearCondition("frightened").applyTo(c, ruleset).character
        assertTrue(c.p().conditions.isEmpty())
    }
}

class PathfinderCodecTests {
    @Test
    fun `a full payload round-trips`() {
        val payload = PathfinderPayload(
            ancestry = "human", heritage = "skilled-heritage", background = "noble", className = "sorcerer",
            level = 3, keyAbility = PfAbility.CHARISMA, abilityScores = PfAbilityScores(charisma = 18),
            maxHp = 30, currentHp = 22, dying = 1, wounded = 1, heroPoints = 2,
            perception = Proficiency.TRAINED, classDC = Proficiency.TRAINED,
            saves = mapOf(Save.WILL to Proficiency.EXPERT),
            skills = mapOf(PfSkill.DIPLOMACY to Proficiency.EXPERT),
            loreSkills = mapOf("Genealogy Lore" to Proficiency.TRAINED),
            conditions = listOf(au.com.evonet.nat20.pf2e.core.ValuedCondition("frightened", 1)),
        )
        assertEquals(payload, ruleset.decodePayload(ruleset.encodePayload(payload)))
    }

    @Test
    fun `events round-trip with their type ids`() {
        val events = listOf(
            PfDamageTakenEvent(10, 30, 20),
            PfHealedEvent(5, 0, 5, recoveredFromDying = true),
            PfDyingChangedEvent(1, 0),
            PfHeroPointsChangedEvent(1, 2),
            PfConditionChangedEvent("Frightened", 2, applied = true),
        )
        for (e in events) {
            val typeId = ruleset.eventTypeId(e)
            assertFalse(typeId == "pf2e.unknown")
            assertEquals(e, ruleset.decodeEvent(ruleset.encodeEvent(e), typeId))
        }
    }
}

class PathfinderEquipmentTests {
    private fun fighter(str: Int = 18, dex: Int = 14, level: Int = 1) = PathfinderPayload(
        className = "fighter", level = level,
        abilityScores = PfAbilityScores(strength = str, dexterity = dex),
        unarmoredProficiency = Proficiency.TRAINED,
        armorProficiencies = mapOf(ArmorCategory.UNARMORED to Proficiency.TRAINED, ArmorCategory.HEAVY to Proficiency.TRAINED, ArmorCategory.MEDIUM to Proficiency.TRAINED),
        weaponProficiencies = mapOf(WeaponCategory.SIMPLE to Proficiency.EXPERT, WeaponCategory.MARTIAL to Proficiency.EXPERT),
    )

    @org.junit.jupiter.api.Test
    fun `worn armor caps DEX and adds the category proficiency`() {
        // Unarmored L1 fighter: 10 + DEX 2 + (1 + 2 trained) = 15.
        assertEquals(15, fighter().armorClass)
        // Breastplate (medium, +4, dex cap 1): 10 + min(2,1)=1 + (1+2) + 4 = 18.
        assertEquals(18, fighter().copy(armor = "breastplate").armorClass)
        // Full plate (heavy, +6, dex cap 0): 10 + 0 + 3 + 6 = 19.
        assertEquals(19, fighter().copy(armor = "full-plate").armorClass)
    }

    @org.junit.jupiter.api.Test
    fun `a raised shield adds its circumstance bonus`() {
        val withShield = fighter().copy(shield = "steel-shield")
        assertEquals(withShield.armorClass + 2, withShield.armorClassRaised)
        assertEquals(null, fighter().armorClassRaised)
    }

    @org.junit.jupiter.api.Test
    fun `a Strike uses the weapon category proficiency, the right ability, and the MAP`() {
        // Longsword (martial, expert): STR 4 + (1 + 4) = 9; MAP −5/−10 → 9 / 4 / −1. Damage 1d8+4.
        val s = fighter().strike(PfWeapons.by("longsword")!!)
        assertEquals(listOf(9, 4, -1), s.attackMods)
        assertEquals("1d8+4", s.damage)
        // Agile shortsword: −4/−8 MAP. STR 4 + (1+4) = 9 → 9 / 5 / 1.
        assertEquals(listOf(9, 5, 1), fighter().strike(PfWeapons.by("shortsword")!!).attackMods)
        // Ranged longbow uses DEX and adds no STR to damage.
        val bow = fighter().strike(PfWeapons.by("longbow")!!)
        assertEquals(2 + 5, bow.attackMods.first()) // DEX 2 + (1+4)
        assertEquals("1d8", bow.damage)
    }

    @org.junit.jupiter.api.Test
    fun `equip and strike intents work and the strike event round-trips`() {
        val c = character(fighter())
        val armed = PfAddWeapon("longsword").applyTo(c, ruleset).character
        assertTrue("longsword" in armed.p().weapons)
        assertEquals(1, armed.p().strikes.size)
        val armored = PfEquipArmor("breastplate").applyTo(armed, ruleset).character
        assertEquals("breastplate", armored.p().armor)
        val raised = PfRaiseShield(true).applyTo(armored.copy(payload = armored.p().copy(shield = "buckler")), ruleset).character
        assertTrue(raised.p().shieldRaised)
        val struck = PfStrike("longsword", 2, 14, "the goblin").applyTo(armed, ruleset)
        assertTrue(struck.event.summary.contains("2nd Strike"))
        val typeId = ruleset.eventTypeId(struck.event)
        assertEquals(struck.event, ruleset.decodeEvent(ruleset.encodeEvent(struck.event), typeId))
    }
}

class PathfinderSpellTests {
    private fun sorcerer(level: Int = 3) = PathfinderPayload(
        className = "sorcerer", level = level, keyAbility = PfAbility.CHARISMA,
        abilityScores = PfAbilityScores(charisma = 18),
        spellTradition = au.com.evonet.nat20.pf2e.core.SpellTradition.ARCANE,
        castingAbility = PfAbility.CHARISMA, spellProficiency = Proficiency.TRAINED,
        cantrips = listOf("electric-arc"), knownSpells = mapOf(1 to listOf("magic-missile")),
    ).withFullSpellSlots()

    @org.junit.jupiter.api.Test
    fun `slot table, spell attack, and DC follow the full-caster maths`() {
        val s = sorcerer(3)
        // Level 3 full caster: rank 1 = 3 slots, rank 2 = 2 (just unlocked).
        assertEquals(mapOf(1 to 3, 2 to 2), s.maxSpellSlots)
        // Spell attack: CHA 4 + (3 + 2 trained) = 9; DC = 19.
        assertEquals(9, s.spellAttack)
        assertEquals(19, s.spellDc)
    }

    @org.junit.jupiter.api.Test
    fun `casting consumes a slot of the chosen rank and heightening uses a higher slot`() {
        val c = character(sorcerer(3))
        val cast = PfCastSpell("magic-missile", "Magic Missile", 1, 1).applyTo(c, ruleset)
        assertEquals(2, cast.character.p().currentSpellSlots[1]) // 3 -> 2
        // Heighten Magic Missile into a rank-2 slot.
        val up = PfCastSpell("magic-missile", "Magic Missile", 1, 2).applyTo(cast.character, ruleset)
        assertEquals(1, up.character.p().currentSpellSlots[2]) // 2 -> 1
        assertTrue(up.event.summary.contains("heightened to rank 2"))
        // Cantrips never consume a slot.
        val cantrip = PfCastSpell("electric-arc", "Electric Arc", 0, 0).applyTo(up.character, ruleset)
        assertEquals(up.character.p().currentSpellSlots, cantrip.character.p().currentSpellSlots)
    }

    @org.junit.jupiter.api.Test
    fun `daily preparations refill slots and the focus pool, refocus caps at max`() {
        val spent = sorcerer(3).copy(currentSpellSlots = mapOf(1 to 0), focusPoints = 0, maxFocusPoints = 1)
        val prepped = PfDailyPreparations().applyTo(character(spent), ruleset).character.p()
        assertEquals(3, prepped.currentSpellSlots[1])
        assertEquals(1, prepped.focusPoints)
        // Refocus is rejected when the pool is full.
        assertThrows(CharacterIntentError.Invalid::class.java) { PfRefocus().applyTo(character(prepped), ruleset) }
    }

    @org.junit.jupiter.api.Test
    fun `a built caster gets a tradition, casting ability, and full slots`() {
        val choices = PathfinderBuilder.Choices("Ezren", "human", null, "scholar", "wizard", PfAbility.INTELLIGENCE,
            freeBoosts = listOf(PfAbility.INTELLIGENCE, PfAbility.DEXTERITY, PfAbility.CONSTITUTION, PfAbility.WISDOM))
        val p = PathfinderBuilder.build(choices)
        assertTrue(p.isCaster)
        assertEquals(au.com.evonet.nat20.pf2e.core.SpellTradition.ARCANE, p.spellTradition)
        assertEquals(PfAbility.INTELLIGENCE, p.castingAbility)
        assertEquals(p.maxSpellSlots, p.currentSpellSlots) // seeded full
    }
}

class PathfinderLevelUpTests {
    private fun fighter(level: Int = 2, con: Int = 14) = character(PathfinderPayload(
        className = "fighter", level = level, abilityScores = PfAbilityScores(strength = 18, constitution = con, charisma = 10),
        maxHp = 30, currentHp = 30, skills = mapOf(PfSkill.ATHLETICS to Proficiency.TRAINED),
    ))

    @org.junit.jupiter.api.Test
    fun `leveling adds fixed HP and bumps the level`() {
        // Fighter 2 -> 3: +10 (class) + 2 (CON) = 12 HP.
        val up = PfLevelUp().applyTo(fighter(), ruleset)
        assertEquals(3, up.character.p().level)
        assertEquals(42, up.character.p().maxHp)
        assertTrue(up.event.summary.contains("+12 HP"))
    }

    @org.junit.jupiter.api.Test
    fun `a skill increase raises one rank and is gated by the level cap`() {
        // Level 3 grants a skill increase, capped at expert.
        val up = PfLevelUp(skillIncrease = PfSkill.ATHLETICS).applyTo(fighter(level = 2), ruleset)
        assertEquals(Proficiency.EXPERT, up.character.p().skills[PfSkill.ATHLETICS])
        // Trying to take a master jump at level 3 (only expert allowed) is rejected.
        val expertAlready = fighter(level = 2).copy(payload = fighter(level = 2).p().copy(skills = mapOf(PfSkill.ATHLETICS to Proficiency.EXPERT)))
        assertThrows(CharacterIntentError.Invalid::class.java) { PfLevelUp(skillIncrease = PfSkill.ATHLETICS).applyTo(expertAlready, ruleset) }
    }

    @org.junit.jupiter.api.Test
    fun `ability boosts apply only at boost levels and reject duplicates`() {
        val atFour = fighter(level = 4) // -> 5, a boost level
        val up = PfLevelUp(abilityBoosts = listOf(PfAbility.STRENGTH, PfAbility.DEXTERITY, PfAbility.CONSTITUTION, PfAbility.WISDOM)).applyTo(atFour, ruleset)
        assertEquals(19, up.character.p().abilityScores.strength) // 18 -> +1 (already 18+)
        assertEquals(16, up.character.p().abilityScores.constitution) // 14 -> +2
        // Boosts at a non-boost level are rejected.
        assertThrows(CharacterIntentError.Invalid::class.java) { PfLevelUp(abilityBoosts = listOf(PfAbility.STRENGTH)).applyTo(fighter(level = 2), ruleset) }
        // Duplicate boost rejected.
        assertThrows(CharacterIntentError.Invalid::class.java) { PfLevelUp(abilityBoosts = listOf(PfAbility.STRENGTH, PfAbility.STRENGTH, PfAbility.DEXTERITY, PfAbility.WISDOM)).applyTo(atFour, ruleset) }
    }

    @org.junit.jupiter.api.Test
    fun `leveling a caster grants newly-unlocked spell slots and the event round-trips`() {
        val wiz = character(PathfinderPayload(className = "wizard", level = 2, abilityScores = PfAbilityScores(intelligence = 18),
            spellTradition = au.com.evonet.nat20.pf2e.core.SpellTradition.ARCANE, castingAbility = PfAbility.INTELLIGENCE,
            spellProficiency = Proficiency.TRAINED, currentSpellSlots = mapOf(1 to 1)))
        val up = PfLevelUp().applyTo(wiz, ruleset).character.p()
        assertEquals(mapOf(1 to 3, 2 to 2), up.maxSpellSlots) // level 3
        assertEquals(1, up.currentSpellSlots[1]) // rank 1 was already maxed at level 2 → the remaining 1 is preserved
        assertEquals(2, up.currentSpellSlots[2]) // newly unlocked → full
        val evt = PfLevelUp().applyTo(wiz, ruleset).event
        val typeId = ruleset.eventTypeId(evt)
        assertEquals(evt, ruleset.decodeEvent(ruleset.encodeEvent(evt), typeId))
    }
}

class PathfinderFeatTests {
    @org.junit.jupiter.api.Test
    fun `feat slots follow the cadence per type and class`() {
        // Fighter (martial): class feats at 1 then every even level.
        assertEquals(1, FeatSlots.classFeat(1, "fighter"))
        assertEquals(2, FeatSlots.classFeat(2, "fighter"))
        // Caster: class feats at even levels only.
        assertEquals(0, FeatSlots.classFeat(1, "wizard"))
        assertEquals(1, FeatSlots.classFeat(2, "wizard"))
        // Ancestry feats at 1/5/9/13/17; general at 3/7/11/15/19.
        assertEquals(2, FeatSlots.ancestry(5))
        assertEquals(1, FeatSlots.general(3))
        // Rogue gains a skill feat every level; others on even levels.
        assertEquals(5, FeatSlots.skill(5, rogue = true))
        assertEquals(2, FeatSlots.skill(5, rogue = false))
    }

    @org.junit.jupiter.api.Test
    fun `the picker only offers legal feats by type, ancestry, class, and level`() {
        val elfAncestry = PfFeats.available(PfFeatType.ANCESTRY, "elf", "wizard", 1)
        assertTrue(elfAncestry.all { it.ancestry == "elf" && it.level <= 1 })
        assertTrue(elfAncestry.any { it.id == "elven-lore" })
        // A higher-level elf feat isn't offered at level 1.
        assertTrue(PfFeats.available(PfFeatType.ANCESTRY, "elf", "wizard", 1).none { it.id == "ageless-patience" })
        assertTrue(PfFeats.available(PfFeatType.ANCESTRY, "elf", "wizard", 5).any { it.id == "ageless-patience" })
        // Class feats are gated to the class.
        assertTrue(PfFeats.available(PfFeatType.CLASS, "elf", "wizard", 1).all { it.className == "wizard" })
    }

    @org.junit.jupiter.api.Test
    fun `taking and removing a feat journals and the event round-trips`() {
        val c = character(PathfinderPayload(ancestry = "human", className = "sorcerer", level = 3))
        val took = PfTakeFeat("dangerous-sorcery").applyTo(c, ruleset)
        assertTrue("dangerous-sorcery" in took.character.p().feats)
        assertTrue(took.event.summary.contains("Dangerous Sorcery"))
        // De-dupe: taking it again is a no-op on the list.
        assertEquals(1, PfTakeFeat("dangerous-sorcery").applyTo(took.character, ruleset).character.p().feats.size)
        val removed = PfRemoveFeat("dangerous-sorcery").applyTo(took.character, ruleset)
        assertTrue(removed.character.p().feats.isEmpty())
        val typeId = ruleset.eventTypeId(took.event)
        assertEquals(took.event, ruleset.decodeEvent(ruleset.encodeEvent(took.event), typeId))
    }
}

class PFMonsterCatalogTests {
    @org.junit.jupiter.api.Test
    fun `the ORC Monster Core bestiary loads, sorted by level then name`() {
        val all = PFMonsterCatalog.all
        assertEquals(445, all.size)
        assertTrue(all.zipWithNext().all { (a, b) -> a.level <= b.level })
        val broom = PFMonsterCatalog.monster("animated-broom")!!
        assertEquals(-1, broom.level)
        assertTrue(broom.subtitle.contains("Construct"))
        assertTrue(broom.statblock.isNotEmpty())
    }
}

class PathfinderClassProgressionTests {
    private fun fighter(level: Int) = character(PathfinderPayload(
        className = "fighter", level = level, abilityScores = PfAbilityScores(constitution = 12),
        maxHp = 50, currentHp = 50, perception = Proficiency.EXPERT, classDC = Proficiency.TRAINED,
        saves = mapOf(Save.FORTITUDE to Proficiency.EXPERT, Save.REFLEX to Proficiency.EXPERT, Save.WILL to Proficiency.TRAINED),
        weaponProficiencies = mapOf(WeaponCategory.SIMPLE to Proficiency.EXPERT, WeaponCategory.MARTIAL to Proficiency.EXPERT),
    ))

    @org.junit.jupiter.api.Test
    fun `leveling raises the class's proficiency ranks at the right levels`() {
        // Fighter Will: Trained → Expert at level 3.
        assertEquals(Proficiency.EXPERT, PfLevelUp().applyTo(fighter(2), ruleset).character.p().saves[Save.WILL])
        // Fighter weapons: Expert → Master at level 13.
        assertEquals(Proficiency.MASTER, PfLevelUp().applyTo(fighter(12), ruleset).character.p().weaponProficiencies[WeaponCategory.MARTIAL])
        // Class DC: Trained → Expert at 11.
        assertEquals(Proficiency.EXPERT, PfLevelUp().applyTo(fighter(10), ruleset).character.p().classDC)
        // A non-jump level leaves ranks untouched.
        assertEquals(Proficiency.TRAINED, PfLevelUp().applyTo(fighter(3), ruleset).character.p().classDC)
    }

    @org.junit.jupiter.api.Test
    fun `a caster's spell proficiency advances on its schedule`() {
        val wiz = character(PathfinderPayload(className = "wizard", level = 6, abilityScores = PfAbilityScores(intelligence = 18),
            spellTradition = au.com.evonet.nat20.pf2e.core.SpellTradition.ARCANE, castingAbility = PfAbility.INTELLIGENCE,
            spellProficiency = Proficiency.TRAINED, maxHp = 30, currentHp = 30))
        // Wizard spell: Trained → Expert at level 7.
        assertEquals(Proficiency.EXPERT, PfLevelUp().applyTo(wiz, ruleset).character.p().spellProficiency)
    }

    @org.junit.jupiter.api.Test
    fun `the progression table covers every seed class`() {
        listOf("fighter", "rogue", "ranger", "champion", "wizard", "cleric", "sorcerer", "bard").forEach {
            assertTrue(ClassProgression.increases(it).isNotEmpty(), "no progression for $it")
        }
    }
}

class PathfinderRunesWealthTests {
    private fun fighter() = PathfinderPayload(
        className = "fighter", level = 1, abilityScores = PfAbilityScores(strength = 18, dexterity = 14, constitution = 10),
        armor = "breastplate", armorProficiencies = mapOf(ArmorCategory.MEDIUM to Proficiency.TRAINED),
        saves = mapOf(Save.FORTITUDE to Proficiency.EXPERT),
        weapons = listOf("longsword"), weaponProficiencies = mapOf(WeaponCategory.MARTIAL to Proficiency.EXPERT),
    )

    @org.junit.jupiter.api.Test
    fun `runes fold into AC, saves, attack, and damage dice`() {
        val base = fighter()
        val runed = base.copy(armorRunes = au.com.evonet.nat20.pf2e.ArmorRunes(potency = 2, resilient = 1))
        assertEquals(base.armorClass + 2, runed.armorClass)             // armor potency +2 AC
        assertEquals(base.saveBonus(Save.FORTITUDE) + 1, runed.saveBonus(Save.FORTITUDE)) // resilient +1 saves
        // Weapon potency +1 attack, striking 1 → 2 damage dice.
        val withWeaponRunes = base.copy(weaponRunes = mapOf("longsword" to au.com.evonet.nat20.pf2e.WeaponRunes(potency = 1, striking = 1)))
        val s = withWeaponRunes.strike(PfWeapons.by("longsword")!!)
        assertEquals(base.strike(PfWeapons.by("longsword")!!).attackMods.first() + 1, s.attackMods.first())
        assertEquals("2d8+4", s.damage)
    }

    @org.junit.jupiter.api.Test
    fun `bulk sums items and coins, encumbrance keys off Strength`() {
        val p = fighter().copy(
            inventory = listOf(PFInventoryItem("Plate", bulk = 4.0), PFInventoryItem("Torch", quantity = 3, bulk = Bulk.LIGHT)),
            coins = mapOf(PFCoin.GP to 1000),
        )
        // 4 + 3×0.1 + 1000×0.001 = 5.3 → floored 5.
        assertEquals(5, Bulk.effective(p.totalBulk))
        assertEquals(5 + 4, p.encumberedThreshold) // 5 + STR mod (+4)
        assertFalse(p.isEncumbered)
        assertTrue(p.copy(inventory = p.inventory + PFInventoryItem("Anvil", bulk = 6.0)).isEncumbered)
    }

    @org.junit.jupiter.api.Test
    fun `rune, coin, and inventory intents apply and clamp`() {
        var c = character(fighter())
        c = PfSetWeaponRunes("longsword", potency = 5, striking = 2).applyTo(c, ruleset).character // clamps to 3
        assertEquals(3, c.p().weaponRunes["longsword"]!!.potency)
        c = PfAdjustCoin(PFCoin.GP, 10).applyTo(c, ruleset).character
        assertEquals(10, c.p().coins[PFCoin.GP])
        assertThrows(CharacterIntentError.Invalid::class.java) { PfAdjustCoin(PFCoin.GP, -20).applyTo(c, ruleset) }
        c = PfAddInventoryItem(PFInventoryItem("Rope", bulk = Bulk.LIGHT)).applyTo(c, ruleset).character
        c = PfAddInventoryItem(PFInventoryItem("Rope")).applyTo(c, ruleset).character // merges by name
        assertEquals(2, c.p().inventory.first { it.name == "Rope" }.quantity)
        assertTrue(PfRemoveInventoryItem("Rope").applyTo(c, ruleset).character.p().inventory.none { it.name == "Rope" })
    }
}

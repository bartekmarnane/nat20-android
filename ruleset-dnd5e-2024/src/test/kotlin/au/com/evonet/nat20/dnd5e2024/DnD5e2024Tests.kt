package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.Coin
import au.com.evonet.nat20.dnd5e.core.DeathSaves
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.EffectSource
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val ruleset = DnD5e2024Ruleset()

private fun character(payload: DnD5e2024Payload): Character = Character.new("Nyx", ruleset, payload, NOW)
private fun Character.p() = payload as DnD5e2024Payload

class RulesetIdentityTests {
    @Test
    fun `identity is the 2024 sibling`() {
        assertEquals("dnd-5e-2024", ruleset.id)
        assertEquals("D&D 5e (2024)", ruleset.displayName)
        assertTrue(ruleset.makeInitialPayload("Nyx") is DnD5e2024Payload)
    }

    @Test
    fun `level and primary class derive from the class lines`() {
        val p = DnD5e2024Payload(classes = listOf(ClassEntry2024("rogue", 3), ClassEntry2024("wizard", 2)))
        assertEquals(5, p.level)
        assertEquals("rogue", p.characterClass)
    }
}

class Exhaustion2024Tests {
    @ParameterizedTest
    @CsvSource("0,0,0", "1,-2,5", "3,-6,15", "6,-12,30")
    fun `numeric exhaustion penalties scale per level`(level: Int, expectedD20: Int, expectedSpeed: Int) {
        assertEquals(expectedD20, Exhaustion2024.d20Modifier(level))
        assertEquals(expectedSpeed, Exhaustion2024.speedPenaltyFeet(level))
    }

    @Test
    fun `level six is fatal`() {
        assertTrue(Exhaustion2024.isFatal(6))
        assertFalse(Exhaustion2024.isFatal(5))
        assertEquals(6, Exhaustion2024.clamp(9))
    }
}

class SharedCoreReuseTests {
    @Test
    fun `the 2024 payload reuses the core spell-slot tables`() {
        // Wizard 5 (full caster) reads the same -core full-caster row as 2014.
        val p = DnD5e2024Payload(classes = listOf(ClassEntry2024("wizard", 5)))
        assertEquals(mapOf(1 to 4, 2 to 3, 3 to 2), p.maxSpellSlots)
    }

    @Test
    fun `effective score folds core active-effect deltas`() {
        val effect = ActiveEffect("e", "Enlarge", EffectSource.Custom, listOf(EffectModifier.AbilityDelta(Ability.STRENGTH, 2)), EffectDuration.UntilCancelled)
        val p = DnD5e2024Payload(abilityScores = AbilityScores(strength = 14), activeEffects = listOf(effect))
        assertEquals(16, p.effectiveScore(Ability.STRENGTH))
    }
}

class Intents2024Tests {
    private fun nyx(currentHp: Int = 27, maxHp: Int = 27, exhaustion: Int = 0) = character(
        DnD5e2024Payload(classes = listOf(ClassEntry2024("rogue", 4)), maxHp = maxHp, currentHp = currentHp, exhaustionLevel = exhaustion),
    )

    @Test
    fun `damage and heal move HP`() {
        val hurt = TakeDamage2024(8, "piercing").applyTo(nyx(), ruleset)
        assertEquals(19, hurt.character.p().currentHp)
        assertEquals("Took 8 piercing damage (HP 27 → 19)", hurt.event.summary)
        val healed = Heal2024(5).applyTo(hurt.character, ruleset)
        assertEquals(24, healed.character.p().currentHp)
    }

    @Test
    fun `exhaustion clamps and is rejected at the edges`() {
        val tired = ChangeExhaustion2024(1).applyTo(nyx(), ruleset)
        assertEquals(1, tired.character.p().exhaustionLevel)
        assertThrows(CharacterIntentError.Invalid::class.java) { ChangeExhaustion2024(-1).applyTo(nyx(exhaustion = 0), ruleset) }
    }

    @Test
    fun `level up adds a class level and HP`() {
        val result = LevelUp2024("rogue", className = "Rogue").applyTo(nyx(), ruleset)
        assertEquals(5, result.character.p().level)
        assertTrue(result.event.summary.startsWith("Leveled up to Rogue 5"))
    }

    @Test
    fun `level up applies the subclass, the ASI capped at 20, and new slots`() {
        // Rogue 2 → 3: pick a subclass + ASI.
        val rogue = character(DnD5e2024Payload(classes = listOf(ClassEntry2024("rogue", 2)), abilityScores = AbilityScores(dexterity = 19), maxHp = 14, currentHp = 14))
        val result = LevelUp2024("rogue", subclass = "thief", abilityIncreases = mapOf(Ability.DEXTERITY to 2), className = "Rogue").applyTo(rogue, ruleset)
        val p = result.character.p()
        assertEquals("thief", p.classes.first().subclass)
        assertEquals(20, p.abilityScores.dexterity) // 19 + 2 capped at 20
        assertTrue(result.event.summary.contains("thief"))
    }

    @Test
    fun `leveling a caster grants newly unlocked slots`() {
        val wiz = character(DnD5e2024Payload(classes = listOf(ClassEntry2024("wizard", 2)), maxHp = 12, currentHp = 12, currentSpellSlots = mapOf(1 to 2)))
        val p = LevelUp2024("wizard").applyTo(wiz, ruleset).character.p()
        assertEquals(mapOf(1 to 4, 2 to 2), p.maxSpellSlots) // L3 wizard
        assertEquals(3, p.currentSpellSlots[1]) // 2 remaining + 1 newly granted
        assertEquals(2, p.currentSpellSlots[2]) // newly unlocked → full
    }

    @Test
    fun `classes carry subclasses chosen at level 3`() {
        val fighter = DnD5e2024Catalog.characterClass("fighter")!!
        assertEquals(3, fighter.subclassLevel)
        assertTrue(fighter.subclasses.any { it.id == "champion" })
    }
}

class Catalog2024Tests {
    @Test
    fun `the SRD 5_2 spell library loads, sorted by level then name`() {
        val spells = DnD5e2024Catalog.spellLibrary
        assertEquals(339, spells.size)
        assertEquals(0, spells.first().level) // cantrips first
        val fireball = DnD5e2024Catalog.spell("fireball")!!
        assertEquals(3, fireball.level)
        assertEquals("Evocation", fireball.school)
        assertTrue(fireball.description.isNotEmpty())
    }
}

class SpellIntents2024Tests {
    private fun wizard() = character(DnD5e2024Payload(classes = listOf(ClassEntry2024("wizard", 3))).withFullSpellSlots())

    @Test
    fun `casting consumes the matching slot`() {
        val result = CastSpell2024("magic-missile", "Magic Missile", 1, 1).applyTo(wizard(), ruleset)
        assertEquals(3, result.character.p().currentSpellSlots[1]) // wizard L3 has 4 at level 1
        assertEquals("Cast Magic Missile", result.event.summary)
    }

    @Test
    fun `upcasting drains the higher slot`() {
        val result = CastSpell2024("magic-missile", "Magic Missile", 1, 2).applyTo(wizard(), ruleset)
        assertEquals(1, result.character.p().currentSpellSlots[2]) // L3 wizard has 2 at level 2 → 1
        assertEquals("Cast Magic Missile at 2nd level", result.event.summary)
    }

    @Test
    fun `long rest refills slots and HP`() {
        val spent = character(DnD5e2024Payload(classes = listOf(ClassEntry2024("wizard", 3)), maxHp = 17, currentHp = 6, currentSpellSlots = mapOf(1 to 1)))
        val p = LongRest2024().applyTo(spent, ruleset).character.p()
        assertEquals(17, p.currentHp)
        assertEquals(p.maxSpellSlots, p.currentSpellSlots)
    }

    @Test
    fun `full-slot seeding matches the derived max`() {
        val p = DnD5e2024Payload(classes = listOf(ClassEntry2024("wizard", 3))).withFullSpellSlots()
        assertEquals(p.maxSpellSlots, p.currentSpellSlots)
    }
}

class CreationCatalog2024Tests {
    @Test
    fun `species carry traits but no ability bonuses`() {
        val elf = DnD5e2024Catalog.species("elf")!!
        assertTrue(elf.traits.any { it.contains("Fey Ancestry") })
    }

    @Test
    fun `backgrounds grant ability options, an origin feat, and skills`() {
        val acolyte = DnD5e2024Catalog.background("acolyte")!!
        assertEquals(listOf(Ability.INTELLIGENCE, Ability.WISDOM, Ability.CHARISMA), acolyte.abilityOptions)
        assertEquals("magic-initiate-cleric", acolyte.originFeat)
        assertEquals(listOf("insight", "religion"), acolyte.skills)
    }

    @Test
    fun `classes load with hit die, saves, and caster flag`() {
        val wizard = DnD5e2024Catalog.characterClass("wizard")!!
        assertEquals(6, wizard.hitDie)
        assertTrue(wizard.isCaster)
        assertEquals(12, DnD5e2024Catalog.classes.size)
        assertEquals(8, DnD5e2024Catalog.species.size)
        assertEquals(6, DnD5e2024Catalog.backgrounds.size)
    }
}

class Effects2024Tests {
    @Test
    fun `mage armor sets the unarmored AC base to 13 plus DEX`() {
        val mageArmor = au.com.evonet.nat20.dnd5e.core.ActiveEffect("m", "Mage Armor", au.com.evonet.nat20.dnd5e.core.EffectSource.Spell("mage-armor"), listOf(au.com.evonet.nat20.dnd5e.core.EffectModifier.AcOverride(au.com.evonet.nat20.dnd5e.core.ACOverrideFormula.BaseDex(13))), au.com.evonet.nat20.dnd5e.core.EffectDuration.UntilCancelled)
        val p = DnD5e2024Payload(abilityScores = AbilityScores(dexterity = 16), activeEffects = listOf(mageArmor))
        assertEquals(16, p.armorClass) // 13 + 3
    }

    @Test
    fun `barbarian unarmored defense uses CON via the passive effect`() {
        val p = DnD5e2024Payload(classes = listOf(ClassEntry2024("barbarian", 3)), abilityScores = AbilityScores(dexterity = 14, constitution = 16))
        assertEquals(15, p.armorClass) // 10 + DEX 2 + CON 3
    }

    @Test
    fun `casting a concentration spell applies its effect and sets focus`() {
        val cleric = character(DnD5e2024Payload(classes = listOf(ClassEntry2024("cleric", 3))).withFullSpellSlots())
        val result = CastSpell2024("bless", "Bless", 1, 1, requiresConcentration = true, applyToSelf = true).applyTo(cleric, ruleset)
        val p = result.character.p()
        assertEquals("Bless", p.concentratingOn)
        assertTrue(p.activeEffects.any { it.name == "Bless" })
        assertEquals(2, p.temporarySaveBonus(Ability.WISDOM))
    }

    @Test
    fun `damage while concentrating prompts a save and resistance halves it`() {
        val raging = character(DnD5e2024Payload(classes = listOf(ClassEntry2024("barbarian", 3)), maxHp = 30, currentHp = 30, concentratingOn = "Hex", activeEffects = listOf(au.com.evonet.nat20.dnd5e.core.ActiveEffect("r", "Rage", au.com.evonet.nat20.dnd5e.core.EffectSource.Feature("rage"), listOf(au.com.evonet.nat20.dnd5e.core.EffectModifier.DamageResistance("slashing")), au.com.evonet.nat20.dnd5e.core.EffectDuration.UntilCancelled))))
        val result = TakeDamage2024(10, "Slashing").applyTo(raging, ruleset)
        val event = result.event as DamageTaken2024Event
        assertEquals(25, result.character.p().currentHp) // 10 resisted → 5
        assertTrue(event.resistanceApplied)
        assertEquals(10, event.concentrationCheckDC)
    }

    @Test
    fun `ending concentration clears the focus and its effects`() {
        val c = character(DnD5e2024Payload(activeEffects = listOf(au.com.evonet.nat20.dnd5e.core.ActiveEffect("b", "Bless", au.com.evonet.nat20.dnd5e.core.EffectSource.Custom, listOf(au.com.evonet.nat20.dnd5e.core.EffectModifier.AttackBonus(2)), au.com.evonet.nat20.dnd5e.core.EffectDuration.Concentration, concentrationOwner = true)), concentratingOn = "Bless"))
        val p = EndConcentration2024().applyTo(c, ruleset).character.p()
        assertEquals(null, p.concentratingOn)
        assertTrue(p.activeEffects.isEmpty())
    }
}

class SpeciesTraits2024Tests {
    @Test
    fun `dwarf poison resistance folds into damage`() {
        val dwarf = character(DnD5e2024Payload(species = "dwarf", classes = listOf(ClassEntry2024("fighter", 1)), maxHp = 12, currentHp = 12))
        assertTrue("poison" in dwarf.p().effectiveDamageResistances)
        val hurt = TakeDamage2024(8, "poison").applyTo(dwarf, ruleset)
        assertEquals(8, hurt.character.p().currentHp) // 8 poison halved to 4
        assertTrue(hurt.event.summary.contains("resisted"))
    }

    @Test
    fun `elf Keen Senses auto-grants Perception proficiency`() {
        val elf = DnD5e2024Payload(species = "elf", skillProficiencies = listOf("stealth"))
        assertTrue("perception" in elf.effectiveSkillProficiencies)
        assertEquals(listOf("stealth", "perception"), elf.effectiveSkillProficiencies)
        // A non-elf gets no auto skill.
        assertFalse("perception" in DnD5e2024Payload(species = "human", skillProficiencies = listOf("stealth")).effectiveSkillProficiencies)
    }

    @Test
    fun `orc Relentless Endurance keeps the character at 1 HP once per long rest`() {
        val orc = character(DnD5e2024Payload(species = "orc", classes = listOf(ClassEntry2024("barbarian", 1)), maxHp = 14, currentHp = 6))
        val downed = TakeDamage2024(10).applyTo(orc, ruleset) // leftover 4 < max ⇒ not overkill
        assertEquals(1, downed.character.p().currentHp)
        assertTrue(downed.character.p().relentlessEnduranceUsed)
        assertTrue(downed.event.summary.contains("Relentless Endurance"))
        // Already used → drops to 0 this time.
        val again = TakeDamage2024(10).applyTo(downed.character.copy(payload = downed.character.p().copy(currentHp = 6)), ruleset)
        assertEquals(0, again.character.p().currentHp)
        // Long rest restores the feature.
        val rested = LongRest2024().applyTo(again.character, ruleset)
        assertFalse(rested.character.p().relentlessEnduranceUsed)
    }

    @Test
    fun `massive overkill kills outright despite Relentless Endurance`() {
        val orc = character(DnD5e2024Payload(species = "orc", classes = listOf(ClassEntry2024("barbarian", 1)), maxHp = 14, currentHp = 6))
        val dead = TakeDamage2024(6 + 14).applyTo(orc, ruleset) // current + max ⇒ instant death
        assertEquals(0, dead.character.p().currentHp)
        assertFalse(dead.character.p().relentlessEnduranceUsed)
    }

    @Test
    fun `every catalogue species has surfaced reminders`() {
        DnD5e2024Catalog.species.forEach { s ->
            assertTrue(SpeciesTraits2024.reminders(s.id).isNotEmpty(), "no reminders for ${s.id}")
        }
    }
}

class Feats2024Tests {
    @Test
    fun `feats are tiered with the right minimum levels`() {
        assertEquals(1, FeatCategory2024.ORIGIN.minimumLevel)
        assertEquals(4, FeatCategory2024.GENERAL.minimumLevel)
        assertEquals(19, FeatCategory2024.EPIC_BOON.minimumLevel)
        assertTrue(Feats2024.inCategory(FeatCategory2024.ORIGIN).isNotEmpty())
        assertEquals(FeatCategory2024.GENERAL, Feats2024.feat("great-weapon-master")!!.category)
    }

    @Test
    fun `availability honours level, ability prereqs, and spellcasting`() {
        val gwm = Feats2024.feat("great-weapon-master")!!
        // General feat needs level 4 and STR 13.
        assertFalse(gwm.isAvailable(3, AbilityScores(strength = 16)))
        assertFalse(gwm.isAvailable(4, AbilityScores(strength = 12)))
        assertTrue(gwm.isAvailable(4, AbilityScores(strength = 13)))
        assertTrue(gwm.grantsAbilityIncrease)
        // War Caster needs spellcasting.
        val wc = Feats2024.feat("war-caster")!!
        assertFalse(wc.isAvailable(4, AbilityScores(), isSpellcaster = false))
        assertTrue(wc.isAvailable(4, AbilityScores(), isSpellcaster = true))
        // Epic boon gated at 19.
        assertFalse(Feats2024.feat("boon-of-fate")!!.isAvailable(18, AbilityScores()))
        assertTrue(Feats2024.feat("boon-of-fate")!!.isAvailable(20, AbilityScores()))
    }

    @Test
    fun `advancement choice validity covers ASI and feat`() {
        val scores = AbilityScores(strength = 19)
        assertTrue(AdvancementChoice2024.AbilityScoreImprovement(mapOf(Ability.DEXTERITY to 2)).isValid(4, scores))
        assertTrue(AdvancementChoice2024.AbilityScoreImprovement(mapOf(Ability.DEXTERITY to 1, Ability.WISDOM to 1)).isValid(4, scores))
        // ASI that overflows past 20 is rejected (not silently clamped).
        assertFalse(AdvancementChoice2024.AbilityScoreImprovement(mapOf(Ability.STRENGTH to 2)).isValid(4, scores))
        // Only General feats are valid advancement picks.
        assertTrue(AdvancementChoice2024.Feat("great-weapon-master").isValid(4, scores))
        assertFalse(AdvancementChoice2024.Feat("alert").isValid(4, scores)) // origin feat, not a General pick
    }

    @Test
    fun `level up records a chosen feat and journals it`() {
        val fighter = character(DnD5e2024Payload(classes = listOf(ClassEntry2024("fighter", 3)), abilityScores = AbilityScores(strength = 15), maxHp = 28, currentHp = 28))
        val result = LevelUp2024("fighter", feat = "great-weapon-master", abilityIncreases = mapOf(Ability.STRENGTH to 1), className = "Fighter").applyTo(fighter, ruleset)
        val p = result.character.p()
        assertEquals(4, p.level)
        assertTrue("great-weapon-master" in p.chosenFeats)
        assertEquals(16, p.abilityScores.strength) // half-feat +1
        assertTrue(result.event.summary.contains("Great Weapon Master"))
        assertTrue("great-weapon-master" in p.allFeats)
    }

    @Test
    fun `Tough and Dwarven Toughness raise the effective max HP and Alert boosts initiative`() {
        // Tough: +2 per level. Level-3 fighter base 28 → 34 effective.
        val tough = DnD5e2024Payload(classes = listOf(ClassEntry2024("fighter", 3)), maxHp = 28, chosenFeats = listOf("tough"))
        assertEquals(34, tough.effectiveMaxHp)
        // Dwarven Toughness: +1 per level, stacking with Tough.
        val dwarf = tough.copy(species = "dwarf")
        assertEquals(37, dwarf.effectiveMaxHp) // 28 + (2+1)*3
        // Healing clamps to the *effective* max, not the stored base.
        val hurt = character(dwarf.copy(currentHp = 10))
        assertEquals(37, Heal2024(100).applyTo(hurt, ruleset).character.p().currentHp)
        // Alert adds the Proficiency Bonus to initiative.
        val alert = DnD5e2024Payload(classes = listOf(ClassEntry2024("fighter", 5)), abilityScores = AbilityScores(dexterity = 16), originFeat = "alert")
        assertEquals(3 + 3, alert.initiativeBonus) // +3 DEX, +3 prof at level 5
        assertEquals(3, DnD5e2024Payload(classes = listOf(ClassEntry2024("fighter", 5)), abilityScores = AbilityScores(dexterity = 16)).initiativeBonus)
    }

    @Test
    fun `leveling up keeps a full character at the new effective max`() {
        // Dwarf fighter at full (28 base + 1*3 = 31). Level 3 → 4 with average HP.
        val dwarf = character(DnD5e2024Payload(species = "dwarf", classes = listOf(ClassEntry2024("fighter", 3)), maxHp = 28, currentHp = 31))
        val p = LevelUp2024("fighter", className = "Fighter").applyTo(dwarf, ruleset).character.p()
        assertEquals(p.effectiveMaxHp, p.currentHp) // stays full: base+6 HP + 1 dwarf share
    }

    @Test
    fun `the feat catalogue spans every tier and resolves consistently`() {
        FeatCategory2024.entries.forEach { c -> assertTrue(Feats2024.inCategory(c).isNotEmpty(), "no feats in $c") }
        // Every feat resolves by id and a representative half-feat / prereq is honoured.
        assertTrue(Feats2024.all.all { Feats2024.feat(it.id) === it })
        assertTrue(Feats2024.feat("polearm-master")!!.grantsAbilityIncrease)
        assertTrue(Feats2024.feat("crossbow-expert")!!.isAvailable(4, AbilityScores(dexterity = 14)))
        assertFalse(Feats2024.feat("crossbow-expert")!!.isAvailable(4, AbilityScores(dexterity = 10)))
        // Expanded General pool is sizeable.
        assertTrue(Feats2024.inCategory(FeatCategory2024.GENERAL).size >= 12)
        assertTrue(Feats2024.inCategory(FeatCategory2024.FIGHTING_STYLE).size >= 6)
    }

    @Test
    fun `spell-effect producers resolve and apply on cast`() {
        // The catalogue grew to cover more buffs; each resolves to a named effect.
        listOf("bless", "haste", "barkskin", "death-ward", "greater-invisibility", "enlargereduce").forEach {
            assertTrue(SpellEffectCatalog2024.template(it) != null, "no template for $it")
        }
        // Barkskin sets an AC floor of 16 when cast on self.
        val druid = character(DnD5e2024Payload(classes = listOf(ClassEntry2024("druid", 5)), abilityScores = AbilityScores(dexterity = 10)).withFullSpellSlots())
        val cast = CastSpell2024("barkskin", "Barkskin", 2, 2, applyToSelf = true, requiresConcentration = true).applyTo(druid, ruleset)
        assertEquals(16, cast.character.p().armorClass) // override floor beats 10 + 0 DEX
    }

    @Test
    fun `Defense fighting style adds plus one AC only while armored`() {
        val unarmored = DnD5e2024Payload(classes = listOf(ClassEntry2024("fighter", 1)), abilityScores = AbilityScores(dexterity = 14), fightingStyle = "defense")
        assertEquals(12, unarmored.armorClass) // no armor → no Defense bonus
        val armored = unarmored.copy(equippedArmor = "chain-mail")
        assertEquals(17, armored.armorClass) // 16 base + 0 DEX + 1 Defense
    }
}

class Attacks2024Tests {
    private fun fighter(str: Int = 16, dex: Int = 12, level: Int = 5, feats: List<String> = emptyList()) = DnD5e2024Payload(
        classes = listOf(ClassEntry2024("fighter", level)),
        abilityScores = AbilityScores(strength = str, dexterity = dex),
        chosenFeats = feats,
    )

    @Test
    fun `melee attack uses STR plus proficiency, damage adds STR`() {
        val atk = AttackMath2024.forWeapon(Weapons2024.weapon("longsword")!!, fighter())
        assertEquals("STR", atk.abilityLabel)
        assertEquals(3 + 3, atk.attackBonuses.sumOf { it.value }) // +3 STR, +3 prof (level 5)
        assertEquals(3, atk.damageBonuses.sumOf { it.value })
        assertEquals("slashing", atk.damageType)
        assertEquals(WeaponMastery2024.SAP, atk.mastery)
    }

    @Test
    fun `ranged uses DEX and finesse takes the better mod`() {
        val ranged = AttackMath2024.forWeapon(Weapons2024.weapon("shortbow")!!, fighter(str = 16, dex = 18))
        assertEquals("DEX", ranged.abilityLabel)
        val finesse = AttackMath2024.forWeapon(Weapons2024.weapon("rapier")!!, fighter(str = 10, dex = 18))
        assertEquals("DEX", finesse.abilityLabel) // DEX 18 (+4) beats STR 10 (0)
    }

    @Test
    fun `Great Weapon Master adds proficiency to a heavy weapon's damage`() {
        val withGwm = AttackMath2024.forWeapon(Weapons2024.weapon("greatsword")!!, fighter(feats = listOf("great-weapon-master")))
        assertTrue(withGwm.damageBonuses.any { it.label == "Great Weapon Master" && it.value == 3 })
        // Not a heavy weapon → no GWM rider even with the feat.
        val dagger = AttackMath2024.forWeapon(Weapons2024.weapon("dagger")!!, fighter(feats = listOf("great-weapon-master")))
        assertFalse(dagger.damageBonuses.any { it.label == "Great Weapon Master" })
    }

    @Test
    fun `MakeAttack journals without mutating the character`() {
        val c = character(fighter())
        val result = MakeAttack2024("Greatsword", 18, au.com.evonet.nat20.dnd5e.core.AttackOutcome.CRITICAL, 21, "slashing", "graze", "the ogre").applyTo(c, ruleset)
        assertEquals(c.payload, result.character.payload) // unchanged
        assertTrue(result.event.summary.startsWith("Critical hit the ogre with Greatsword for 21 slashing damage"))
    }

    @Test
    fun `the weapons accessor resolves catalogue-linked pack weapons`() {
        val p = fighter().copy(inventory = listOf(
            InventoryItem2024("a", "Longsword", kind = ItemKind2024.WEAPON, catalogueID = "longsword"),
            InventoryItem2024("b", "Apple", kind = ItemKind2024.CONSUMABLE),
        ))
        assertEquals(listOf("longsword"), p.weapons.map { it.id })
    }
}

class MonsterCatalog2024Tests {
    @Test
    fun `the SRD 5_2 monster catalogue loads, sorted by CR then name`() {
        val monsters = MonsterCatalog2024.all
        assertEquals(330, monsters.size)
        // sorted ascending by CR
        assertTrue(monsters.zipWithNext().all { (a, b) -> a.challengeRating <= b.challengeRating })
        val aboleth = MonsterCatalog2024.monster("aboleth")!!
        assertEquals("Aboleth", aboleth.name)
        assertEquals(10.0, aboleth.challengeRating)
        assertTrue(aboleth.actions.isNotEmpty())
        assertTrue(aboleth.subtitle.contains("Aberration"))
    }
}

class Inventory2024Tests {
    private fun fighter(dex: Int = 14, armor: String? = null, shield: Boolean = false) = character(
        DnD5e2024Payload(
            classes = listOf(ClassEntry2024("fighter", 1)),
            abilityScores = AbilityScores(dexterity = dex),
            equippedArmor = armor, hasShield = shield,
        ),
    )

    @ParameterizedTest
    @CsvSource(
        // armor, dex, shield, expectedAC
        "null,14,false,12",            // unarmored: 10 + 2 DEX
        "leather,14,false,13",         // light: 11 + full DEX(2)
        "scale-mail,16,false,16",      // medium: 14 + DEX capped at +2 (3→2)
        "scale-mail,10,false,14",      // medium: 14 + DEX(0)
        "plate,16,false,18",           // heavy: 18 + DEX ignored
        "plate,16,true,20",            // heavy + shield +2
        "leather,14,true,15",          // light + shield
    )
    fun `armor class folds worn armor, the DEX cap, and a shield`(armor: String, dex: Int, shield: Boolean, expectedAC: Int) {
        val id = armor.takeIf { it != "null" }
        assertEquals(expectedAC, fighter(dex = dex, armor = id, shield = shield).p().armorClass)
    }

    @Test
    fun `unarmored Barbarian uses Constitution, ignoring overrides once armored`() {
        val barb = character(DnD5e2024Payload(classes = listOf(ClassEntry2024("barbarian", 1)), abilityScores = AbilityScores(dexterity = 14, constitution = 16)))
        assertEquals(15, barb.p().armorClass) // 10 + 2 DEX + 3 CON
        val armored = character((barb.p()).copy(equippedArmor = "chain-mail"))
        assertEquals(16, armored.p().armorClass) // heavy base only; Unarmored Defense suppressed
    }

    @Test
    fun `acquiring stacks consumables by catalogue id but keeps weapons distinct`() {
        val potion = InventoryItem2024(InventoryItem2024.newId(), "Potion of Healing", kind = ItemKind2024.CONSUMABLE, catalogueID = "potion-healing")
        var c = AcquireItem2024(potion).applyTo(fighter(), ruleset).character
        c = AcquireItem2024(potion.copy(id = InventoryItem2024.newId(), quantity = 2)).applyTo(c, ruleset).character
        assertEquals(1, c.p().inventory.size)
        assertEquals(3, c.p().inventory.first().quantity)
    }

    @Test
    fun `equipping resolves the catalogue and toggling a shield drives AC`() {
        val equipped = EquipArmor2024("studded-leather").applyTo(fighter(dex = 12), ruleset).character
        assertEquals("studded-leather", equipped.p().equippedArmor)
        assertEquals(13, equipped.p().armorClass) // 12 base + DEX 1
        val shielded = SetShield2024(true).applyTo(equipped, ruleset).character
        assertEquals(15, shielded.p().armorClass)
        assertThrows(CharacterIntentError.Invalid::class.java) { EquipArmor2024("nonsense").applyTo(fighter(), ruleset) }
    }

    @Test
    fun `weapon mastery progression caps the picks`() {
        assertEquals(3, WeaponMasteryProgression2024.slots("fighter", 1))
        assertEquals(6, WeaponMasteryProgression2024.slots("fighter", 16))
        assertEquals(2, WeaponMasteryProgression2024.slots("rogue", 5))
        assertEquals(0, WeaponMasteryProgression2024.slots("wizard", 20))
        // A barbarian (2 slots) keeps only the first two, case-insensitively de-duped.
        val barb = character(DnD5e2024Payload(classes = listOf(ClassEntry2024("barbarian", 3))))
        val set = SetWeaponMasteries2024(listOf("Cleave", "topple", "VEX", "cleave")).applyTo(barb, ruleset).character
        assertEquals(listOf("cleave", "topple"), set.p().weaponMasteries)
    }

    @Test
    fun `every 2024 weapon carries a mastery and the catalogue resolves by id`() {
        assertTrue(Weapons2024.all.all { WeaponMastery2024.entries.contains(it.mastery) })
        assertEquals(WeaponMastery2024.CLEAVE, Weapons2024.weapon("greataxe")!!.mastery)
    }
}

class Codec2024Tests {
    @Test
    fun `a full 2024 payload round-trips, including core sealed effects`() {
        val payload = DnD5e2024Payload(
            species = "elf",
            classes = listOf(ClassEntry2024("rogue", 4, "thief")),
            background = "criminal",
            abilityScores = AbilityScores(dexterity = 17),
            backgroundASI = mapOf(Ability.DEXTERITY to 2, Ability.INTELLIGENCE to 1),
            maxHp = 27, currentHp = 20,
            deathSaves = DeathSaves(successes = 1),
            activeEffects = listOf(ActiveEffect("e", "Bless", EffectSource.Spell("bless"), listOf(EffectModifier.AttackBonus(2)), EffectDuration.Concentration, concentrationOwner = true)),
            activeConditions = listOf("Poisoned"),
            exhaustionLevel = 2,
            coins = mapOf(Coin.GP to 12),
            equippedArmor = "studded-leather",
            hasShield = true,
            inventory = listOf(InventoryItem2024("i1", "Thieves' Tools", kind = ItemKind2024.GEAR, catalogueID = "thieves-tools")),
            weaponMasteries = listOf("vex", "nick"),
            originFeat = "alert",
        )
        assertEquals(payload, ruleset.decodePayload(ruleset.encodePayload(payload)))
    }

    @Test
    fun `2024 events round-trip with their type ids`() {
        val events = listOf(
            DamageTaken2024Event(8, "piercing", 27, 19),
            ExhaustionChanged2024Event(0, 1),
            LeveledUp2024Event("rogue", "Rogue", 5, 5, 6),
            ItemAcquired2024Event("Longsword", 1),
            ItemDropped2024Event("Torch", 2),
            ArmorEquipped2024Event("Chain Mail"),
            ShieldChanged2024Event(true),
            CoinAdjusted2024Event(Coin.GP, -5, "tavern"),
            WeaponMasteries2024Event(listOf("vex", "nick")),
            Attack2024Event("Greatsword", 18, au.com.evonet.nat20.dnd5e.core.AttackOutcome.CRITICAL, 21, "slashing", "graze", "ogre"),
        )
        for (event in events) {
            val typeId = ruleset.eventTypeId(event)
            assertFalse(typeId == "dnd5e2024.unknown")
            assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
        }
    }
}

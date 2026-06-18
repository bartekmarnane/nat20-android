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
        )
        for (event in events) {
            val typeId = ruleset.eventTypeId(event)
            assertFalse(typeId == "dnd5e2024.unknown")
            assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
        }
    }
}

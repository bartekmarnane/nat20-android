package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import au.com.evonet.nat20.domain.SummonLifecycle
import au.com.evonet.nat20.domain.SummonOrigin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

private val CR_NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val crRuleset = DnD5eRuleset()

private fun crChar(): Character = Character.new(
    "Caster",
    crRuleset,
    DnD5ePayload(race = "human", classes = listOf(ClassEntry("wizard", 5)), abilityScores = AbilityScores()),
    CR_NOW,
)

class CreatureCatalogTests {
    @Test
    fun `the library covers familiars, mounts, companions, and summons`() {
        assertTrue(CreatureCatalog.familiars.any { it.id == "owl" })
        assertTrue(CreatureCatalog.mounts.any { it.id == "warhorse" })
        assertTrue(CreatureCatalog.companions.any { it.id == "wolf" })
        assertTrue(CreatureCatalog.summons.any { it.id == "brown-bear" })
        assertEquals(CreatureKind.FAMILIAR, CreatureCatalog.template("owl")!!.kind)
    }

    @Test
    fun `a creature's statblock round-trips through its ruleset payload`() {
        val wolf = CreatureCatalog.template("wolf")!!
        val creature = wolf.makeCreature("Shadow")
        assertEquals("Shadow", creature.name)
        assertEquals(wolf.maxHp, creature.currentHp)
        val decoded = DnD5eCreaturePayload.decode(creature.rulesetPayload)
        assertEquals(wolf.armorClass, decoded.armorClass)
        assertEquals("wolf", decoded.catalogueId)
        assertTrue(decoded.attacks.first().display.contains("Bite"))
    }
}

class SummonIntentsTests {
    private fun summon(count: Int = 1) = CreatureCatalog.buildSummon(
        CreatureCatalog.template("wolf")!!,
        SummonOrigin.Spell("Conjure Animals", 3),
        SummonLifecycle.Concentration(au.com.evonet.nat20.domain.GameDuration.Hours(1)),
        CR_NOW,
        count = count,
    )

    @Test
    fun `summoning attaches a group and dismissing detaches it`() {
        val s = summon(count = 3)
        val summoned = SummonCreature(s).applyTo(crChar(), crRuleset)
        assertEquals(1, summoned.character.summons.size)
        assertEquals(3, summoned.character.summons.first().creatures.size)
        assertTrue(summoned.event.summary.contains("3 creatures"))
        val dismissed = DismissSummon(s.id).applyTo(summoned.character, crRuleset)
        assertTrue(dismissed.character.summons.isEmpty())
        assertThrows(CharacterIntentError.Invalid::class.java) { DismissSummon(UUID.randomUUID()).applyTo(crChar(), crRuleset) }
    }

    @Test
    fun `setting a creature's HP clamps to 0 and max`() {
        val s = summon()
        val c = SummonCreature(s).applyTo(crChar(), crRuleset).character
        val creatureId = c.summons.first().creatures.first().id
        val hurt = SetCreatureHp(s.id, creatureId, -5).applyTo(c, crRuleset)
        assertEquals(0, hurt.character.summons.first().creatures.first().currentHp)
        val over = SetCreatureHp(s.id, creatureId, 999).applyTo(hurt.character, crRuleset)
        val max = CreatureCatalog.template("wolf")!!.maxHp
        assertEquals(max, over.character.summons.first().creatures.first().currentHp)
    }

    @Test
    fun `summon events round-trip through the ruleset codec`() {
        val events = listOf(
            CreatureSummonedEvent("Wolves", 3),
            SummonDismissedEvent("Owl"),
            CreatureHpEvent("Wolf", 11, 4, 11),
        )
        for (e in events) {
            val typeId = crRuleset.eventTypeId(e)
            assertFalse(typeId == "dnd5e.unknown")
            assertEquals(e, crRuleset.decodeEvent(crRuleset.encodeEvent(e), typeId))
        }
    }
}

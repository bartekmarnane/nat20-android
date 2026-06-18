package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.ClassFeatureCatalog
import au.com.evonet.nat20.dnd5e.core.ClassResourceCatalog
import au.com.evonet.nat20.dnd5e.core.FeatureRecovery
import au.com.evonet.nat20.dnd5e.core.FeatureUseEntry
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterIntentError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val ruleset = DnD5eRuleset()

private fun character(
    vararg classes: ClassEntry,
    cha: Int = 10,
    pools: Map<String, Int> = emptyMap(),
    features: Map<String, FeatureUseEntry> = emptyMap(),
): Character = Character.new(
    "Hero", ruleset,
    DnD5ePayload(
        classes = classes.toList(),
        abilityScores = AbilityScores(charisma = cha),
        resourcePools = pools,
        classFeatureUses = features,
    ),
    NOW,
)

private fun Character.payload() = payload as DnD5ePayload

// ── Catalogue formulas ────────────────────────────────────────────────────────

class ClassResourceCatalogTests {
    @ParameterizedTest
    @CsvSource("1,2", "3,3", "6,4", "12,5", "17,6") // L20 → unlimited (null), tested separately
    fun `rage uses by barbarian level`(level: Int, expected: Int) {
        assertEquals(expected, ClassFeatureCatalog.feature("rage")!!.maxUsesFor(level))
    }

    @Test
    fun `rage is unlimited at level twenty`() {
        assertNull(ClassFeatureCatalog.feature("rage")!!.maxUsesFor(20))
    }

    @ParameterizedTest
    @CsvSource("5,1", "6,2", "17,2", "18,3")
    fun `channel divinity uses by level`(level: Int, expected: Int) {
        assertEquals(expected, ClassFeatureCatalog.feature("channel-divinity")!!.maxUsesFor(level))
    }

    @Test
    fun `action surge is one then two at seventeen`() {
        assertEquals(1, ClassFeatureCatalog.feature("action-surge")!!.maxUsesFor(16))
        assertEquals(2, ClassFeatureCatalog.feature("action-surge")!!.maxUsesFor(17))
    }

    @Test
    fun `point pools scale by level`() {
        assertEquals(0, ClassResourceCatalog.pool("ki")!!.maxValue(1))      // online at L2
        assertEquals(5, ClassResourceCatalog.pool("ki")!!.maxValue(5))
        assertEquals(25, ClassResourceCatalog.pool("lay-on-hands")!!.maxValue(5)) // 5 × level
    }

    @ParameterizedTest
    @CsvSource("4,6", "5,8", "10,10", "15,12")
    fun `bardic inspiration die scales`(bardLevel: Int, expectedFaces: Int) {
        assertEquals(expectedFaces, ClassResourceCatalog.bardicInspirationDie(bardLevel))
    }
}

// ── Resolved accessors ────────────────────────────────────────────────────────

class ResolvedResourceTests {
    @Test
    fun `monk exposes a ki pool at its level, full when untracked`() {
        val monk = character(ClassEntry("monk", 5)).payload()
        val ki = monk.availableResourcePools().single { it.id == "ki" }
        assertEquals(5, ki.max)
        assertEquals(5, ki.current) // untouched = full
        assertEquals(FeatureRecovery.SHORT_REST, ki.recovery)
    }

    @Test
    fun `bardic inspiration cap is CHA mod and recovery flips at level five`() {
        val youngBard = character(ClassEntry("bard", 3), cha = 16).payload() // CHA +3
        val bi = youngBard.availableClassFeatures().single { it.id == "bardic-inspiration" }
        assertEquals(3, bi.max)
        assertEquals(FeatureRecovery.LONG_REST, bi.recovery) // < L5
        val seniorBard = character(ClassEntry("bard", 6), cha = 16).payload()
        assertEquals(FeatureRecovery.SHORT_REST, seniorBard.availableClassFeatures().single { it.id == "bardic-inspiration" }.recovery)
    }

    @Test
    fun `level twenty barbarian rage shows as unlimited`() {
        val barb = character(ClassEntry("barbarian", 20)).payload()
        val rage = barb.availableClassFeatures().single { it.id == "rage" }
        assertNull(rage.max)
        assertNull(rage.current)
    }

    @Test
    fun `non-resource class exposes nothing`() {
        assertFalse(character(ClassEntry("wizard", 5)).payload().hasClassResources())
    }

    @Test
    fun `paladin gets channel divinity only from level three`() {
        assertTrue(character(ClassEntry("paladin", 2)).payload().availableClassFeatures().none { it.id == "channel-divinity" })
        assertTrue(character(ClassEntry("paladin", 3)).payload().availableClassFeatures().any { it.id == "channel-divinity" })
    }
}

// ── Intents ───────────────────────────────────────────────────────────────────

class ResourceIntentTests {
    @Test
    fun `spending ki drains the pool and the rest reads spent-down`() {
        val monk = character(ClassEntry("monk", 5))
        val result = SpendResource("ki", 2, note = "Flurry of Blows").applyTo(monk, ruleset)
        val p = result.character.payload()
        assertEquals(3, p.resourcePools["ki"])
        assertEquals(3, p.currentResource("ki"))
        assertEquals("Spent 2 Ki (3/5 left) — Flurry of Blows", result.event.summary)
    }

    @Test
    fun `overspending a pool is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            SpendResource("ki", 99).applyTo(character(ClassEntry("monk", 5)), ruleset)
        }
    }

    @Test
    fun `spending a pool the character lacks is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            SpendResource("ki", 1).applyTo(character(ClassEntry("fighter", 5)), ruleset)
        }
    }

    @Test
    fun `using a feature decrements its counter`() {
        val barb = character(ClassEntry("barbarian", 3)) // 3 rages
        val result = UseClassFeature("rage", "Rage", FeatureRecovery.LONG_REST, usesRemaining = 3).applyTo(barb, ruleset)
        assertEquals(2, result.character.payload().classFeatureUses["rage"]?.remaining)
        assertEquals("Used Rage (2 left)", result.event.summary)
    }

    @Test
    fun `using a feature with no uses left is rejected`() {
        assertThrows(CharacterIntentError.Invalid::class.java) {
            UseClassFeature("rage", "Rage", FeatureRecovery.LONG_REST, usesRemaining = 0).applyTo(character(ClassEntry("barbarian", 3)), ruleset)
        }
    }

    @Test
    fun `an unlimited feature fires without tracking`() {
        val result = UseClassFeature("rage", "Rage", FeatureRecovery.LONG_REST, usesRemaining = null).applyTo(character(ClassEntry("barbarian", 20)), ruleset)
        assertTrue(result.character.payload().classFeatureUses.isEmpty())
        assertEquals("Used Rage", result.event.summary)
    }
}

// ── Rest resets ───────────────────────────────────────────────────────────────

class ResourceRestTests {
    @Test
    fun `short rest restores ki and short-rest features but keeps long-rest ones`() {
        // Monk/Barbarian: ki (short) spent, action-surge analog not here; rage (long) used.
        val hero = character(
            ClassEntry("monk", 5), ClassEntry("barbarian", 3),
            pools = mapOf("ki" to 1),
            features = mapOf(
                "rage" to FeatureUseEntry(1, FeatureRecovery.LONG_REST),
                "second-wind" to FeatureUseEntry(0, FeatureRecovery.SHORT_REST),
            ),
        )
        val p = ShortRest().applyTo(hero, ruleset).character.payload()
        assertNull(p.resourcePools["ki"]) // reset to full (key removed)
        assertEquals(5, p.currentResource("ki"))
        assertNull(p.classFeatureUses["second-wind"]) // short-rest feature cleared
        assertEquals(1, p.classFeatureUses["rage"]?.remaining) // long-rest feature survives
    }

    @Test
    fun `long rest wipes every pool and feature counter`() {
        val hero = character(
            ClassEntry("paladin", 6),
            pools = mapOf("lay-on-hands" to 5),
            features = mapOf("channel-divinity" to FeatureUseEntry(0, FeatureRecovery.SHORT_REST)),
        )
        val p = LongRest().applyTo(hero, ruleset).character.payload()
        assertTrue(p.resourcePools.isEmpty())
        assertTrue(p.classFeatureUses.isEmpty())
        assertEquals(30, p.currentResource("lay-on-hands")) // back to full (6 × 5)
    }
}

// ── Codec ─────────────────────────────────────────────────────────────────────

class ResourceCodecTests {
    @Test
    fun `payload with resource state round-trips`() {
        val payload = DnD5ePayload(
            classes = listOf(ClassEntry("monk", 5), ClassEntry("barbarian", 3)),
            resourcePools = mapOf("ki" to 2),
            classFeatureUses = mapOf("rage" to FeatureUseEntry(1, FeatureRecovery.LONG_REST)),
        )
        assertEquals(payload, ruleset.decodePayload(ruleset.encodePayload(payload)))
    }

    @Test
    fun `resource events round-trip with their type ids`() {
        val events = listOf(
            ResourceSpentEvent("ki", "Ki", 2, 3, 5, note = "Flurry"),
            ClassFeatureUsedEvent("rage", "Rage", FeatureRecovery.LONG_REST, remainingAfter = 2),
        )
        for (event in events) {
            val typeId = ruleset.eventTypeId(event)
            assertFalse(typeId == "dnd5e.unknown")
            assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
        }
    }
}

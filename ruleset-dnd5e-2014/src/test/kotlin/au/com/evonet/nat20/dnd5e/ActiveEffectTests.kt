package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.ACOverrideFormula
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.dnd5e.core.ActiveEffect
import au.com.evonet.nat20.dnd5e.core.EffectDuration
import au.com.evonet.nat20.dnd5e.core.EffectModifier
import au.com.evonet.nat20.dnd5e.core.EffectSource
import au.com.evonet.nat20.dnd5e.core.RestKind
import au.com.evonet.nat20.domain.Character
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-06-18T00:00:00Z")
private val ruleset = DnD5eRuleset()

private fun effect(
    name: String,
    vararg modifiers: EffectModifier,
    duration: EffectDuration = EffectDuration.UntilCancelled,
    concentration: Boolean = false,
    id: String = name,
) = ActiveEffect(id, name, EffectSource.Custom, modifiers.toList(), duration, concentration)

private fun character(
    classId: String = "wizard",
    level: Int = 5,
    scores: AbilityScores = AbilityScores(),
    inventory: List<InventoryItem> = emptyList(),
    effects: List<ActiveEffect> = emptyList(),
    concentratingOn: String? = null,
    currentHp: Int = 30,
    maxHp: Int = 30,
    tempHp: Int = 0,
): Character = Character.new(
    "Aria", ruleset,
    DnD5ePayload(
        classes = listOf(ClassEntry(classId, level)),
        abilityScores = scores,
        maxHp = maxHp, currentHp = currentHp, temporaryHp = tempHp,
        inventory = inventory,
        activeEffects = effects,
        concentratingOn = concentratingOn,
    ).withFullSpellSlots(),
    NOW,
)

private fun Character.payload() = payload as DnD5ePayload

// ── AC integration ────────────────────────────────────────────────────────────

class EffectAcTests {
    @Test
    fun `mage armor sets the unarmored base to 13 plus DEX`() {
        val p = character(scores = AbilityScores(dexterity = 16), effects = listOf(effect("Mage Armor", EffectModifier.AcOverride(ACOverrideFormula.BaseDex(13))))).payload()
        assertEquals(16, p.armorClass) // 13 + 3
    }

    @Test
    fun `barbarian unarmored defense is 10 plus DEX plus CON`() {
        val p = character(classId = "barbarian", scores = AbilityScores(dexterity = 14, constitution = 16)).payload()
        assertEquals(15, p.armorClass) // 10 + 2 + 3
    }

    @Test
    fun `monk unarmored defense drops when a shield is equipped`() {
        val shield = InventoryItem("s", "Shield", ItemKind.SHIELD, equipped = true, acBonus = 2)
        val withShield = character(classId = "monk", scores = AbilityScores(dexterity = 14, wisdom = 16), inventory = listOf(shield)).payload()
        // Monk UD void with a shield → unarmored 10 + DEX 2 + shield 2 = 14 (not 10+DEX+WIS).
        assertEquals(14, withShield.armorClass)
    }

    @Test
    fun `shield spell adds a flat plus five`() {
        val p = character(scores = AbilityScores(dexterity = 12), effects = listOf(effect("Shield", EffectModifier.AcBonus(5)))).payload()
        assertEquals(16, p.armorClass) // 10 + 1 + 5
    }

    @Test
    fun `an override is ignored while wearing armor`() {
        val leather = DnD5eCatalog.armorPiece("leather")!!.makeItem(equipped = true)
        val p = character(scores = AbilityScores(dexterity = 14), inventory = listOf(leather), effects = listOf(effect("Mage Armor", EffectModifier.AcOverride(ACOverrideFormula.BaseDex(13))))).payload()
        assertEquals(13, p.armorClass) // leather 11 + DEX 2; override doesn't apply
    }
}

// ── Effective scores / resistances ────────────────────────────────────────────

class EffectStatTests {
    @Test
    fun `ability delta and set fold into the effective score`() {
        val p = character(scores = AbilityScores(strength = 12), effects = listOf(effect("Bull's Strength", EffectModifier.AbilityDelta(Ability.STRENGTH, 4)))).payload()
        assertEquals(16, p.effectiveScore(Ability.STRENGTH))

        val giant = character(scores = AbilityScores(strength = 18), effects = listOf(effect("Giant Strength", EffectModifier.AbilitySet(Ability.STRENGTH, 21)))).payload()
        assertEquals(21, giant.effectiveScore(Ability.STRENGTH)) // set wins when higher
    }

    @Test
    fun `save and skill bonuses sum across effects`() {
        val p = character(effects = listOf(effect("Bless", EffectModifier.SaveBonus(null, 2), EffectModifier.AttackBonus(2)))).payload()
        assertEquals(2, p.temporarySaveBonus(Ability.WISDOM))
        assertEquals(2, p.effectAttackBonus)
    }

    @Test
    fun `resistance halves typed damage and concentration prompts a save`() {
        val raging = character(
            classId = "barbarian",
            effects = listOf(effect("Rage", EffectModifier.DamageResistance("slashing"), EffectModifier.DamageBonus(2))),
            concentratingOn = "Hex", currentHp = 30,
        )
        val result = TakeDamage(amount = 10, damageType = "Slashing").applyTo(raging, ruleset)
        val event = result.event as DamageTakenEvent
        assertEquals(25, result.character.payload().currentHp) // 10 resisted → 5
        assertTrue(event.resistanceApplied)
        assertEquals(10, event.concentrationCheckDC) // half of 5 = 2 → floored to min 10
    }
}

// ── Concentration lifecycle ───────────────────────────────────────────────────

class ConcentrationTests {
    @Test
    fun `casting a concentration spell sets focus and drops the prior one`() {
        val hexed = character(effects = listOf(effect("Hex", EffectModifier.DamageBonus(3), concentration = true)), concentratingOn = "Hex")
        val result = CastSpell("bless", "Bless", 1, 1, requiresConcentration = true, applyToSelf = true).applyTo(hexed, ruleset)
        val p = result.character.payload()
        assertEquals("Bless", p.concentratingOn)
        assertTrue(p.activeEffects.none { it.name == "Hex" }) // prior concentration dropped
        assertTrue(p.activeEffects.any { it.name == "Bless" })
    }

    @Test
    fun `a target-picked buff only self-applies when opted in`() {
        val without = CastSpell("bless", "Bless", 1, 1, requiresConcentration = true, applyToSelf = false).applyTo(character(), ruleset)
        assertTrue(without.character.payload().activeEffects.none { it.name == "Bless" })
        assertEquals("Bless", without.character.payload().concentratingOn) // still concentrating, effect just not on self
    }

    @Test
    fun `ending concentration clears the focus and its effects`() {
        val c = character(effects = listOf(effect("Bless", EffectModifier.AttackBonus(2), concentration = true)), concentratingOn = "Bless")
        val p = EndConcentration().applyTo(c, ruleset).character.payload()
        assertNull(p.concentratingOn)
        assertTrue(p.activeEffects.isEmpty())
    }

    @Test
    fun `cancelling the concentration effect also clears focus`() {
        val c = character(effects = listOf(effect("Bless", EffectModifier.AttackBonus(2), concentration = true, id = "bless1")), concentratingOn = "Bless")
        val result = CancelEffect("bless1").applyTo(c, ruleset)
        assertNull(result.character.payload().concentratingOn)
        assertEquals("Lost the Bless effect", result.event.summary)
    }
}

// ── Producers + rest cleanup ──────────────────────────────────────────────────

class EffectLifecycleTests {
    @Test
    fun `raging applies the resistance-and-damage effect at the level-correct tier`() {
        val barb = character(classId = "barbarian", level = 9) // L9 → +3 rage damage
        val result = UseClassFeature("rage", "Rage", au.com.evonet.nat20.dnd5e.core.FeatureRecovery.LONG_REST, usesRemaining = 3).applyTo(barb, ruleset)
        val rage = result.character.payload().activeEffects.single { it.name == "Rage" }
        assertTrue(rage.modifiers.contains(EffectModifier.DamageBonus(3)))
        assertEquals(setOf("bludgeoning", "piercing", "slashing"), result.character.payload().effectiveDamageResistances)
    }

    @Test
    fun `short rest drops until-short-rest effects but keeps cancellable ones`() {
        val c = character(effects = listOf(
            effect("Bardic boost", EffectModifier.AttackBonus(1), duration = EffectDuration.UntilRest(RestKind.SHORT)),
            effect("Rage", EffectModifier.DamageBonus(2), duration = EffectDuration.UntilCancelled),
        ))
        val p = ShortRest().applyTo(c, ruleset).character.payload()
        assertTrue(p.activeEffects.none { it.name == "Bardic boost" })
        assertTrue(p.activeEffects.any { it.name == "Rage" })
    }

    @Test
    fun `long rest ends concentration and timed effects but keeps until-cancelled`() {
        val c = character(effects = listOf(
            effect("Bless", EffectModifier.AttackBonus(2), duration = EffectDuration.Concentration, concentration = true),
            effect("Mage Armor", EffectModifier.AcOverride(ACOverrideFormula.BaseDex(13)), duration = EffectDuration.UntilRest(RestKind.LONG)),
            effect("Rage", EffectModifier.DamageBonus(2), duration = EffectDuration.UntilCancelled),
        ), concentratingOn = "Bless")
        val p = LongRest().applyTo(c, ruleset).character.payload()
        assertNull(p.concentratingOn)
        assertEquals(listOf("Rage"), p.activeEffects.map { it.name })
    }
}

// ── Catalogue + codec ─────────────────────────────────────────────────────────

class EffectCatalogueCodecTests {
    @Test
    fun `the spell catalogue resolves known buffs`() {
        val bless = SpellEffectCatalog.template("bless")!!
        assertEquals(EffectScope.TARGET_PICKED, bless.scope)
        assertTrue(bless.concentrationOwner)
        assertTrue(bless.modifiers.contains(EffectModifier.AttackBonus(2)))
        assertNull(SpellEffectCatalog.template("not-a-spell"))
    }

    @Test
    fun `a payload with active effects round-trips through the sealed-polymorphic codec`() {
        val payload = DnD5ePayload(
            classes = listOf(ClassEntry("wizard", 5)),
            activeEffects = listOf(
                ActiveEffect("e1", "Mage Armor", EffectSource.Spell("mage-armor"), listOf(EffectModifier.AcOverride(ACOverrideFormula.BaseDex(13))), EffectDuration.UntilRest(RestKind.LONG)),
                ActiveEffect("e2", "Bless", EffectSource.Custom, listOf(EffectModifier.AttackBonus(2), EffectModifier.SaveBonus(null, 2)), EffectDuration.Concentration, concentrationOwner = true),
            ),
            concentratingOn = "Bless",
        )
        assertEquals(payload, ruleset.decodePayload(ruleset.encodePayload(payload)))
    }

    @Test
    fun `effect events round-trip with their type ids`() {
        val events = listOf(
            ConcentrationEndedEvent("Bless"),
            EffectAppliedEvent("Rage"),
            EffectCancelledEvent("Hex"),
        )
        for (event in events) {
            val typeId = ruleset.eventTypeId(event)
            assertFalse(typeId == "dnd5e.unknown")
            assertEquals(event, ruleset.decodeEvent(ruleset.encodeEvent(event), typeId))
        }
    }
}

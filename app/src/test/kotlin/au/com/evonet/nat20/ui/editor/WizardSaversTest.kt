package au.com.evonet.nat20.ui.editor

import au.com.evonet.nat20.dnd5e.InventoryItem
import au.com.evonet.nat20.dnd5e.ItemKind
import au.com.evonet.nat20.dnd5e.core.Ability
import au.com.evonet.nat20.dnd5e.core.AbilityScores
import au.com.evonet.nat20.pf2e.FeatSlotKey
import au.com.evonet.nat20.pf2e.PfFeatType
import au.com.evonet.nat20.pf2e.core.PfAbility
import au.com.evonet.nat20.pf2e.core.PfSkill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The wizards keep their in-flight character build in `rememberSaveable` via
 * [jsonStateSaver], so a rotation or a low-memory kill mid-build doesn't throw
 * the player back to step one. Compilation proves each type has a serializer;
 * these cover the shapes that can only fail at runtime — notably the maps keyed
 * by something other than a String.
 */
class WizardSaversTest {

    private inline fun <reified T> roundTrip(value: T): T {
        val encoded = WizardJson.encodeToString(kotlinx.serialization.serializer<T>(), value)
        return WizardJson.decodeFromString(kotlinx.serialization.serializer<T>(), encoded)
    }

    @Test
    fun `ability scores survive a round trip`() {
        val scores = AbilityScores(strength = 15, dexterity = 14, constitution = 13, intelligence = 12, wisdom = 10, charisma = 8)
        assertEquals(scores, roundTrip(scores))
    }

    @Test
    fun `maps keyed by an enum survive a round trip`() {
        val increases = mapOf(Ability.STRENGTH to 2, Ability.WISDOM to 1)
        assertEquals(increases, roundTrip(increases))
    }

    @Test
    fun `maps keyed by a data class survive a round trip`() {
        // Needs Json.allowStructuredMapKeys — the PF2e wizard's per-slot feat picks.
        val feats = mapOf(
            FeatSlotKey(level = 1, type = PfFeatType.ANCESTRY) to "natural-ambition",
            FeatSlotKey(level = 2, type = PfFeatType.CLASS) to "dangerous-sorcery",
        )
        assertEquals(feats, roundTrip(feats))
    }

    @Test
    fun `pf2e enum collections and nullables survive a round trip`() {
        assertEquals(listOf(PfAbility.STRENGTH, PfAbility.CHARISMA), roundTrip(listOf(PfAbility.STRENGTH, PfAbility.CHARISMA)))
        assertEquals(setOf(PfSkill.ARCANA), roundTrip(setOf(PfSkill.ARCANA)))
        assertEquals(null, roundTrip<PfSkill?>(null))
    }

    @Test
    fun `inventory survives a round trip`() {
        val inventory = listOf(InventoryItem(id = "i1", name = "Longsword", kind = ItemKind.WEAPON, quantity = 1))
        assertEquals(inventory, roundTrip(inventory))
    }
}

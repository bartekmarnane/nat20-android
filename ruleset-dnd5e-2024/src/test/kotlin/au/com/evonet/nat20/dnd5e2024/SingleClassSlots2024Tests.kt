package au.com.evonet.nat20.dnd5e2024

import au.com.evonet.nat20.domain.Character
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SingleClassSlots2024Tests {
    @Test
    fun `a lone paladin reads the half-caster table`() {
        val p = DnD5e2024Payload(classes = listOf(ClassEntry2024("paladin", 5)))
        assertEquals(mapOf(1 to 4, 2 to 2), p.maxSpellSlots)
        assertEquals(emptyMap<Int, Int>(), DnD5e2024Payload(classes = listOf(ClassEntry2024("ranger", 1))).maxSpellSlots)
    }

    @Test
    fun `multiclass combines against the full-caster table`() {
        val p = DnD5e2024Payload(classes = listOf(ClassEntry2024("paladin", 3), ClassEntry2024("wizard", 3)))
        assertEquals(mapOf(1 to 4, 2 to 3), p.maxSpellSlots)
    }

    @Test
    fun `long rest regains up to half the total hit dice`() {
        val ruleset = DnD5e2024Ruleset()
        val p = DnD5e2024Payload(classes = listOf(ClassEntry2024("fighter", 10)), maxHp = 80, currentHp = 40, hitDiceSpent = 5)
        val c = Character.new("Bram", ruleset, p, java.time.Instant.EPOCH)
        val after = LongRest2024().applyTo(c, ruleset).character.payload as DnD5e2024Payload
        assertEquals(0, after.hitDiceSpent)
        val drained = Character.new("Bram", ruleset, p.copy(hitDiceSpent = 10), java.time.Instant.EPOCH)
        assertEquals(5, (LongRest2024().applyTo(drained, ruleset).character.payload as DnD5e2024Payload).hitDiceSpent)
    }

    @Test
    fun `campaign end sheds in-play state`() {
        val p = DnD5e2024Payload(
            classes = listOf(ClassEntry2024("cleric", 3)), maxHp = 20, currentHp = 14,
            temporaryHp = 4, concentratingOn = "Bless", activeConditions = listOf("prone"),
        )
        val after = DnD5e2024Ruleset().payloadAfterCampaignEnd(p) as DnD5e2024Payload
        assertEquals(0, after.temporaryHp)
        assertEquals(null, after.concentratingOn)
        assertEquals(emptyList<String>(), after.activeConditions)
        assertEquals(14, after.currentHp)
    }
}

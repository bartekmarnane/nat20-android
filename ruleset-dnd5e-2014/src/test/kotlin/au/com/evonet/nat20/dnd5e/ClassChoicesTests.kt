package au.com.evonet.nat20.dnd5e

import au.com.evonet.nat20.dnd5e.core.AbilityScores
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClassChoicesTests {

    private val rs = DnD5eRuleset()

    @Test
    fun `the deep-class catalogues decode from the bundled SRD json`() {
        assertEquals(8, Metamagics.all.size)
        assertEquals(4, PactBoons.all.size)
        assertTrue(Invocations.all.size >= 30)
        assertNotNull(Metamagics.option("careful-spell"))
        assertNotNull(PactBoons.boon("pact-of-the-blade"))
        assertNotNull(Invocations.invocation("agonizing-blast"))
    }

    @Test
    fun `metamagic known follows the sorcerer table 2 at 3, 3 at 10, 4 at 17`() {
        fun sorc(l: Int) = DnD5ePayload(classes = listOf(ClassEntry("sorcerer", l))).metamagicKnownCount
        assertEquals(0, sorc(2))
        assertEquals(2, sorc(3))
        assertEquals(2, sorc(9))
        assertEquals(3, sorc(10))
        assertEquals(4, sorc(17))
        assertEquals(4, sorc(20))
    }

    @Test
    fun `warlock invocations and pact boon track the warlock table`() {
        fun lock(l: Int) = DnD5ePayload(classes = listOf(ClassEntry("warlock", l)))
        assertEquals(0, lock(1).invocationsKnownCount)
        assertEquals(2, lock(2).invocationsKnownCount)
        assertEquals(8, lock(20).invocationsKnownCount)
        assertFalse(lock(2).pactBoonAvailable)
        assertTrue(lock(3).pactBoonAvailable)
    }

    @Test
    fun `expertise slots come from rogue and bard levels and stack on a multiclass`() {
        assertEquals(2, DnD5ePayload(classes = listOf(ClassEntry("rogue", 1))).expertiseSlots)
        assertEquals(4, DnD5ePayload(classes = listOf(ClassEntry("rogue", 6))).expertiseSlots)
        assertEquals(2, DnD5ePayload(classes = listOf(ClassEntry("bard", 3))).expertiseSlots)
        assertEquals(6, DnD5ePayload(classes = listOf(ClassEntry("rogue", 6), ClassEntry("bard", 3))).expertiseSlots)
    }

    @Test
    fun `expertise doubles the proficiency multiplier for a proficient skill`() {
        val payload = DnD5ePayload(
            classes = listOf(ClassEntry("rogue", 1)),
            selectedSkills = listOf("stealth"),
            expertiseSkills = listOf("stealth"),
        )
        assertEquals(2, payload.skillProficiencyMultiplier("stealth"))
        assertEquals(0, payload.skillProficiencyMultiplier("arcana")) // not proficient
    }

    @Test
    fun `invocation prerequisites gate on warlock level, pact boon and known spell`() {
        val ancient = Invocations.invocation("book-of-ancient-secrets")!!
        assertFalse(ancient.isAvailable(2, pactBoon = null, knownSpells = emptySet()))
        assertTrue(ancient.isAvailable(2, pactBoon = "pact-of-the-tome", knownSpells = emptySet()))

        val agonizing = Invocations.invocation("agonizing-blast")!!
        assertFalse(agonizing.isAvailable(2, pactBoon = null, knownSpells = emptySet()))
        assertTrue(agonizing.isAvailable(2, pactBoon = null, knownSpells = setOf("eldritch-blast")))
    }

    @Test
    fun `payload with deep-class choices round-trips through the codec`() {
        val payload = DnD5ePayload(
            classes = listOf(ClassEntry("warlock", 5)),
            metamagicKnown = emptyList(),
            invocationsKnown = listOf("agonizing-blast", "devils-sight"),
            pactBoon = "pact-of-the-blade",
            expertiseSkills = listOf("deception"),
        )
        assertEquals(payload, rs.decodePayload(rs.encodePayload(payload)))
    }
}

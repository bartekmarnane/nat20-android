package au.com.evonet.nat20.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class CampaignTests {
    private val ruleset = FakeRuleset()
    private val now: Instant = Instant.parse("2026-06-18T10:00:00Z")
    private val later: Instant = Instant.parse("2026-06-18T12:00:00Z")

    private fun buildingCharacter(): Character =
        Character.new(name = "Aria", ruleset = ruleset, timestamp = now)

    /** A character committed to [campaign]. */
    private fun inCampaign(character: Character, campaign: Campaign): Character =
        character.copy(phase = CharacterPhase.InCampaign(campaign.id))

    @Test
    fun `start captures the snapshot and opens an empty active log`() {
        val character = buildingCharacter()
        val campaign = Campaign.start(character, name = "The Sunless Citadel", startedAt = now)

        assertTrue(campaign.isActive)
        assertEquals(character.id, campaign.characterId)
        assertEquals(character, campaign.startSnapshot)
        assertTrue(campaign.log.isEmpty())
    }

    @Test
    fun `apply appends to the log and bumps the character`() {
        val character = buildingCharacter()
        val campaign = Campaign.start(character, name = "Quest", startedAt = now)
        val committed = inCampaign(character, campaign)

        val result = campaign.apply(RenameIntent("Aria the Bold"), committed, ruleset, later)

        assertEquals(1, result.campaign.log.size)
        assertEquals("Renamed to Aria the Bold", result.logged.event.summary)
        assertEquals("Aria the Bold", result.character.name)
        assertEquals(later, result.character.updatedAt)
        // Original campaign value is untouched (immutability).
        assertTrue(campaign.log.isEmpty())
    }

    @Test
    fun `apply rejects a building character`() {
        val character = buildingCharacter()
        val campaign = Campaign.start(character, name = "Quest", startedAt = now)
        assertThrows(CampaignError.CharacterNotInCampaign::class.java) {
            campaign.apply(RenameIntent("X"), character, ruleset, later)
        }
    }

    @Test
    fun `apply rejects a character committed to a different campaign`() {
        val character = buildingCharacter()
        val campaign = Campaign.start(character, name = "Quest", startedAt = now)
        val elsewhere = character.copy(phase = CharacterPhase.InCampaign(java.util.UUID.randomUUID()))
        assertThrows(CampaignError.CharacterNotInCampaign::class.java) {
            campaign.apply(RenameIntent("X"), elsewhere, ruleset, later)
        }
    }

    @Test
    fun `apply rejects a ruleset mismatch`() {
        val character = buildingCharacter()
        val campaign = Campaign.start(character, name = "Quest", startedAt = now)
        val committed = inCampaign(character, campaign)
        assertThrows(CampaignError.RulesetMismatch::class.java) {
            campaign.apply(RenameIntent("X"), committed, FakeRuleset(id = "other"), later)
        }
    }

    @Test
    fun `apply rejects an ended campaign`() {
        val character = buildingCharacter()
        val campaign = Campaign.start(character, name = "Quest", startedAt = now)
        val committed = inCampaign(character, campaign)
        val ended = campaign.end(later, finalSnapshot = committed)
        assertThrows(CampaignError.AlreadyEnded::class.java) {
            ended.apply(RenameIntent("X"), committed, ruleset, later)
        }
    }

    @Test
    fun `end captures the final snapshot and is idempotent`() {
        val character = buildingCharacter()
        val campaign = Campaign.start(character, name = "Quest", startedAt = now)
        val committed = inCampaign(character, campaign)

        val ended = campaign.end(later, finalSnapshot = committed)
        assertFalse(ended.isActive)
        assertEquals(later, ended.endedAt)
        assertNotNull(ended.endSnapshot)

        // Ending again is a no-op (returns the same already-ended value).
        val again = ended.end(now, finalSnapshot = character)
        assertSame(ended, again)
    }
}

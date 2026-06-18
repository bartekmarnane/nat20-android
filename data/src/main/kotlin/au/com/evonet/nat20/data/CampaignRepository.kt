package au.com.evonet.nat20.data

import au.com.evonet.nat20.domain.Campaign
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Persistence seam for campaigns (A7a). Like [CharacterRepository], the
 * store/UI depend only on this interface, never on Room.
 */
interface CampaignRepository {
    /** Live stream of a character's campaigns, newest-started first. */
    fun campaignsForCharacter(characterId: UUID): Flow<List<Campaign>>

    /** One campaign by id, or null. */
    suspend fun campaign(id: UUID): Campaign?

    /** Insert or update a campaign (start, every applied intent, end). */
    suspend fun upsert(campaign: Campaign)

    /** Remove a campaign by id. */
    suspend fun delete(id: UUID)
}

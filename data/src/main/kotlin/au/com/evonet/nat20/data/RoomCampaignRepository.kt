package au.com.evonet.nat20.data

import au.com.evonet.nat20.domain.Campaign
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Room-backed [CampaignRepository]. Maps rows ↔ domain through [CampaignCodec]. */
class RoomCampaignRepository(
    private val dao: CampaignDao,
    private val codec: CampaignCodec,
) : CampaignRepository {

    override fun campaignsForCharacter(characterId: UUID): Flow<List<Campaign>> =
        dao.observeForCharacter(characterId.toString())
            .map { rows -> rows.map(codec::toDomain) }

    override suspend fun campaign(id: UUID): Campaign? =
        dao.byId(id.toString())?.let(codec::toDomain)

    override suspend fun upsert(campaign: Campaign) {
        dao.upsert(codec.toEntity(campaign))
    }

    override suspend fun delete(id: UUID) {
        dao.delete(id.toString())
    }
}

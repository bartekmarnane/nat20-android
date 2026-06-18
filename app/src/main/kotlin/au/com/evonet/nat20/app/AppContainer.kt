package au.com.evonet.nat20.app

import android.content.Context
import au.com.evonet.nat20.data.CampaignRepository
import au.com.evonet.nat20.data.CharacterRepository
import au.com.evonet.nat20.data.Nat20Data
import au.com.evonet.nat20.domain.RulesetRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container — the composition root. Builds the singletons the
 * app wires together by hand (no DI framework yet; Hilt can slot in later if
 * the graph grows). Mirrors the role of the iOS app's store/environment setup.
 */
class AppContainer(context: Context) {
    /** Lives for the whole process; used for app-scoped background work (seeding). */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val rulesetRegistry: RulesetRegistry = DefaultRulesetRegistry()

    private val repositories = Nat20Data.create(context, rulesetRegistry)
    val characterRepository: CharacterRepository = repositories.characters
    val campaignRepository: CampaignRepository = repositories.campaigns
}

package au.com.evonet.nat20.app

import android.content.Context
import au.com.evonet.nat20.chronicle.ChronicleService
import au.com.evonet.nat20.chronicle.GeminiNanoChronicleGenerator
import au.com.evonet.nat20.data.CampaignRepository
import au.com.evonet.nat20.data.CharacterRepository
import au.com.evonet.nat20.data.CustomRaceRepository
import au.com.evonet.nat20.data.Nat20Data
import au.com.evonet.nat20.store.CustomRaceSync
import au.com.evonet.nat20.store.CustomCreatureSync
import au.com.evonet.nat20.domain.RulesetRegistry
import au.com.evonet.nat20.patron.PatronStore
import au.com.evonet.nat20.settings.AppSettings
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

    /** Persisted app preferences (appearance, …). */
    val appSettings: AppSettings = AppSettings(context)

    /** The Patron unlock (Play Billing). `isPatron` is the single gate every
     *  premium feature keys off (A14). */
    val patronStore: PatronStore = PatronStore(context, applicationScope)

    private val repositories = Nat20Data.create(context, rulesetRegistry)
    val characterRepository: CharacterRepository = repositories.characters
    val campaignRepository: CampaignRepository = repositories.campaigns
    val customRaceRepository: CustomRaceRepository = repositories.customRaces

    /** Hydrates + write-through-persists the homebrew race library (parity #11). */
    val customRaceSync: CustomRaceSync = CustomRaceSync(customRaceRepository, applicationScope)

    /** Hydrates + write-through-persists the homebrew creature library (parity #44). */
    val customCreatureSync: CustomCreatureSync = CustomCreatureSync(repositories.customCreatures, applicationScope)

    /** On-device chronicler. Gemini Nano is hardware-gated, so this is inert
     *  (isAvailable == false) until run on supported hardware — see the generator. */
    val chronicleService: ChronicleService = ChronicleService(GeminiNanoChronicleGenerator(context))
}

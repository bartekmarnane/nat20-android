package au.com.evonet.nat20.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.evonet.nat20.BuildConfig
import au.com.evonet.nat20.app.Nat20Application
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.SourceCatalog
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterPhase
import au.com.evonet.nat20.domain.PartyMember
import au.com.evonet.nat20.store.CharacterStore
import au.com.evonet.nat20.ui.editor.CreationWizardScreen
import au.com.evonet.nat20.ui.editor.DnD5eWizardScreen
import au.com.evonet.nat20.ui.journal.JournalScreen
import au.com.evonet.nat20.ui.past.PastAdventuresScreen
import au.com.evonet.nat20.ui.reference.ItemCatalogShell
import au.com.evonet.nat20.ui.reference.MonsterCodexShell
import au.com.evonet.nat20.ui.patron.PatronScreen
import au.com.evonet.nat20.patron.PatronStore
import au.com.evonet.nat20.ui.reference.SpellLibraryShell
import au.com.evonet.nat20.ui.roster.RosterScreen
import au.com.evonet.nat20.ui.roll.LocalDiceInput
import au.com.evonet.nat20.ui.settings.CharacterSettingsScreen
import au.com.evonet.nat20.ui.settings.ContentSourcesScreen
import au.com.evonet.nat20.ui.settings.CreditsScreen
import au.com.evonet.nat20.ui.settings.SettingsScreen
import au.com.evonet.nat20.ui.sheet.CharacterSheetScreen
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * App root: the Navigation Compose graph, the iOS `NavigationStack` equivalent.
 * The [CharacterStore] is the shared source of truth; routes carry ids and
 * resolve against the live roster / campaign streams, so create/edit/delete and
 * campaign changes reflect at once. The journal route is keyed by campaign id,
 * so it serves both the active journal and read-only Past Adventures.
 */
private object Routes {
    const val ROSTER = "roster"
    const val SHEET = "sheet/{id}"
    const val CREATE = "create"
    const val EDIT = "editor/{id}"
    const val JOURNAL = "journal/{id}/{campaignId}"
    const val PAST = "past/{id}"
    const val CHARACTER_SETTINGS = "character-settings/{id}"
    const val CONTENT_SOURCES = "content-sources/{id}"
    const val SETTINGS = "settings"
    const val CREDITS = "credits"
    const val SPELL_LIBRARY = "spell-library"
    const val MONSTER_CODEX = "monster-codex"
    const val CUSTOM_CREATURES = "custom-creatures"
    const val ITEM_CATALOG = "item-catalog"
    const val PATRON = "patron"
    const val ARG_ID = "id"
    const val ARG_CAMPAIGN_ID = "campaignId"
    fun sheet(id: UUID) = "sheet/$id"
    fun edit(id: UUID) = "editor/$id"
    fun journal(characterId: UUID, campaignId: UUID) = "journal/$characterId/$campaignId"
    fun past(id: UUID) = "past/$id"
    fun characterSettings(id: UUID) = "character-settings/$id"
    fun contentSources(id: UUID) = "content-sources/$id"
}

@Composable
fun NatApp() {
    val container = (LocalContext.current.applicationContext as Nat20Application).container
    val store: CharacterStore = viewModel(
        factory = CharacterStore.factory(
            container.characterRepository,
            container.campaignRepository,
            container.rulesetRegistry,
            container.chronicleService,
            narrationStyle = { container.appSettings.narrationStyle.value },
        ),
    )
    val nav = rememberNavController()
    val characters by store.roster.collectAsState()
    // Rejected intents / campaign gating used to vanish silently while the
    // picker had already closed as if it worked.
    val context = LocalContext.current
    LaunchedEffect(store) {
        store.errors.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    val patron = container.patronStore
    val isPatron by patron.isPatron.collectAsState()

    fun find(id: UUID?): Character? = id?.let { wanted -> characters.firstOrNull { it.id == wanted } }

    // Every RollResultView reads this to decide whether to open on the ROLL
    // button or the face pad (A25). One provider here covers all three rulesets.
    val diceInput by container.appSettings.diceInput.collectAsState()

    CompositionLocalProvider(LocalDiceInput provides diceInput) {
    NavHost(navController = nav, startDestination = Routes.ROSTER) {
        composable(Routes.ROSTER) {
            val campaignNames by store.activeCampaignNames.collectAsState()
            RosterScreen(
                characters = characters,
                campaignNames = campaignNames,
                canCreate = PatronStore.canCreateMore(characters.size, isPatron),
                onSelect = { nav.navigate(Routes.sheet(it.id)) },
                onNew = { nav.navigate(Routes.CREATE) },
                onUpgrade = { nav.navigate(Routes.PATRON) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                appSettings = container.appSettings,
                narrationAvailable = container.chronicleService.isAvailable,
                isPatron = isPatron,
                onOpenPatron = { nav.navigate(Routes.PATRON) },
                onOpenSpellLibrary = { nav.navigate(Routes.SPELL_LIBRARY) },
                onOpenMonsterCodex = { nav.navigate(Routes.MONSTER_CODEX) },
                onOpenItemCatalog = { nav.navigate(Routes.ITEM_CATALOG) },
                onOpenCustomCreatures = { nav.navigate(Routes.CUSTOM_CREATURES) },
                onOpenCredits = { nav.navigate(Routes.CREDITS) },
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.CREDITS) {
            CreditsScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.PATRON) {
            PatronScreen(patron = patron, onBack = { nav.popBackStack() })
        }

        composable(Routes.SPELL_LIBRARY) {
            SpellLibraryShell(onBack = { nav.popBackStack() })
        }

        composable(Routes.CUSTOM_CREATURES) {
            au.com.evonet.nat20.ui.reference.CustomCreaturesScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.MONSTER_CODEX) {
            MonsterCodexShell(onBack = { nav.popBackStack() })
        }

        composable(Routes.ITEM_CATALOG) {
            ItemCatalogShell(onBack = { nav.popBackStack() })
        }

        composable(
            Routes.SHEET,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val character = find(entry.uuidArg(Routes.ARG_ID))
            if (character == null) {
                MissingEntry(nav)
            } else {
                val campaigns by remember(character.id) { store.campaignsForCharacter(character.id) }
                    .collectAsState(initial = emptyList())
                val active = activeCampaign(character, campaigns)
                CharacterSheetScreen(
                    character = character,
                    activeCampaign = active,
                    hasPastAdventures = campaigns.any { !it.isActive },
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.edit(character.id)) },
                    onStartCampaign = { name, party -> store.startCampaign(character, name, party) },
                    onRenameCampaign = { name -> active?.let { c -> store.renameCampaign(c, name) } },
                    onUpdateParty = { party -> active?.let { c -> store.updateParty(c, party) } },
                    onLeaveCampaign = { active?.let { c -> store.endCampaign(character, c) } },
                    onOpenJournal = { active?.let { c -> nav.navigate(Routes.journal(character.id, c.id)) } },
                    onOpenPastAdventures = { nav.navigate(Routes.past(character.id)) },
                    onOpenCharacterSettings = { nav.navigate(Routes.characterSettings(character.id)) },
                    onBrowseSpells = { nav.navigate(Routes.SPELL_LIBRARY) },
                    // Phase-aware: journaled in-campaign, a direct edit while building.
                    onApplyIntent = { intent -> store.applyIntent(intent, character, active) },
                    onSave = { store.save(it) },
                )
            }
        }

        composable(
            Routes.JOURNAL,
            arguments = listOf(
                navArgument(Routes.ARG_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_CAMPAIGN_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val character = find(entry.uuidArg(Routes.ARG_ID))
            val campaignId = entry.uuidArg(Routes.ARG_CAMPAIGN_ID)
            if (character == null || campaignId == null) {
                MissingEntry(nav)
            } else {
                val campaigns by remember(character.id) { store.campaignsForCharacter(character.id) }
                    .collectAsState(initial = emptyList())
                val generating by store.generatingChronicles.collectAsState()
                val campaign = campaigns.firstOrNull { it.id == campaignId }
                LaunchedEffect(campaign?.id, campaign?.log?.size) {
                    if (campaign != null) store.generateChronicles(campaign)
                }
                JournalScreen(
                    campaign = campaign,
                    characterName = character.name,
                    chronicleAvailable = store.isChronicleAvailable,
                    chronicling = campaignId in generating,
                    readOnly = campaign?.isActive == false,
                    onBack = { nav.popBackStack() },
                    onRename = { name -> campaign?.let { store.renameCampaign(it, name) } },
                    onUpdateParty = { party -> campaign?.let { store.updateParty(it, party) } },
                    onLeave = { campaign?.let { store.endCampaign(character, it) }; nav.popBackStack() },
                )
            }
        }

        composable(
            Routes.PAST,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val character = find(entry.uuidArg(Routes.ARG_ID))
            if (character == null) {
                MissingEntry(nav)
            } else {
                val campaigns by remember(character.id) { store.campaignsForCharacter(character.id) }
                    .collectAsState(initial = emptyList())
                PastAdventuresScreen(
                    endedCampaigns = campaigns.filter { !it.isActive },
                    onOpen = { nav.navigate(Routes.journal(character.id, it.id)) },
                    onBack = { nav.popBackStack() },
                )
            }
        }

        composable(
            Routes.CHARACTER_SETTINGS,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val character = find(entry.uuidArg(Routes.ARG_ID))
            if (character == null) {
                MissingEntry(nav)
            } else {
                CharacterSettingsScreen(
                    character = character,
                    onBack = { nav.popBackStack() },
                    // Delete removes the character and pops all the way back to the roster.
                    onDelete = {
                        store.delete(character.id)
                        nav.popBackStack(Routes.ROSTER, inclusive = false)
                    },
                    onEditSources = { nav.navigate(Routes.contentSources(character.id)) },
                )
            }
        }

        composable(
            Routes.CONTENT_SOURCES,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val character = find(entry.uuidArg(Routes.ARG_ID))
            val payload = character?.payload as? DnD5ePayload
            if (character == null || payload == null) {
                MissingEntry(nav)
            } else {
                ContentSourcesScreen(
                    characterName = character.name,
                    payload = payload,
                    onBack = { nav.popBackStack() },
                    // Bookkeeping rather than an in-play event — saved straight
                    // through the store with no journal entry, matching the way
                    // identity and inventory edits are persisted.
                    onCommit = { sources ->
                        store.save(
                            character.copy(
                                payload = payload.copy(
                                    enabledSources = (sources + SourceCatalog.lockedIds).toList(),
                                ),
                            ),
                        )
                    },
                )
            }
        }

        composable(Routes.CREATE) {
            // The unified creation flow: Ruleset step first, then the chosen
            // edition's wizard inside the same shell (iOS CharacterCreationWizard).
            CreationWizardScreen(
                onSave = { store.save(it); nav.popBackStack() },
                onCancel = { nav.popBackStack() },
            )
        }

        composable(
            Routes.EDIT,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val character = find(entry.uuidArg(Routes.ARG_ID))
            if (character == null) {
                MissingEntry(nav)
            } else {
                DnD5eWizardScreen(
                    existing = character,
                    onSave = { store.save(it); nav.popBackStack() },
                    onCancel = { nav.popBackStack() },
                )
            }
        }
    }
    if (BuildConfig.DEBUG) ScreenshotRouter(nav, store, characters)
    }
}

/**
 * DEBUG screenshot harness: once the seeded roster has landed, navigate to the
 * screen named by [ScreenshotRoute.shot], starting (or ending) a campaign for
 * the demo character first where the shot needs one. Runs once per launch.
 */
@Composable
private fun ScreenshotRouter(nav: NavHostController, store: CharacterStore, characters: List<Character>) {
    val shot = ScreenshotRoute.shot ?: return
    var routed by remember { mutableStateOf(false) }
    LaunchedEffect(characters.isNotEmpty()) {
        if (routed) return@LaunchedEffect
        if (characters.isEmpty()) return@LaunchedEffect // seed still landing
        routed = true
        if (shot == "emptyRoster") {
            // Wait for the seed first, then clear it: the blank-page state.
            characters.forEach { store.delete(it.id) }
            return@LaunchedEffect
        }

        fun named(prefix: String): Character = characters.first { it.name.startsWith(prefix) }

        /** The character committed to an active campaign (started here if needed). */
        suspend fun inCampaign(character: Character): Pair<Character, Campaign> {
            val existing = store.campaignsForCharacter(character.id).first().firstOrNull { it.isActive }
            if (existing != null) return character to existing
            store.startCampaign(
                character,
                "The Lantern Company",
                listOf(
                    PartyMember(name = "Bram Holloway", characterClass = "Fighter", race = "Human", level = 6),
                    PartyMember(name = "Sister Ives", characterClass = "Cleric", race = "Hill Dwarf", level = 6),
                ),
            )
            val campaign = store.campaignsForCharacter(character.id)
                .first { list -> list.any { it.isActive } }
                .first { it.isActive }
            val updated = store.roster
                .first { roster -> roster.firstOrNull { it.id == character.id }?.phase is CharacterPhase.InCampaign }
                .first { it.id == character.id }
            return updated to campaign
        }

        val lyra = named("Lyra")
        val thorgar = named("Thorgar")
        val nyx = named("Nyx")
        val seoni = named("Seoni")
        when (shot) {
            "roster", "onboarding" -> Unit
            "stats", "skills", "combat", "spells", "items", "lore", "actions", "levelUp" -> {
                val (c, _) = inCampaign(lyra)
                nav.navigate(Routes.sheet(c.id))
            }
            "journal" -> {
                val (c, campaign) = inCampaign(lyra)
                nav.navigate(Routes.sheet(c.id))
                nav.navigate(Routes.journal(c.id, campaign.id))
            }
            "journal2024" -> {
                val (c, campaign) = inCampaign(nyx)
                nav.navigate(Routes.sheet(c.id))
                nav.navigate(Routes.journal(c.id, campaign.id))
            }
            "past" -> {
                val (c, campaign) = inCampaign(thorgar)
                store.endCampaign(c, campaign)
                store.campaignsForCharacter(c.id).first { list -> list.none { it.isActive } }
                nav.navigate(Routes.sheet(c.id))
                nav.navigate(Routes.past(c.id))
            }
            "building" -> nav.navigate(Routes.sheet(thorgar.id))
            "editor" -> nav.navigate(Routes.edit(thorgar.id))
            "characterSettings" -> {
                nav.navigate(Routes.sheet(lyra.id))
                nav.navigate(Routes.characterSettings(lyra.id))
            }
            "contentSources" -> {
                nav.navigate(Routes.sheet(lyra.id))
                nav.navigate(Routes.characterSettings(lyra.id))
                nav.navigate(Routes.contentSources(lyra.id))
            }
            "create" -> nav.navigate(Routes.CREATE)
            "settings" -> nav.navigate(Routes.SETTINGS)
            "credits" -> { nav.navigate(Routes.SETTINGS); nav.navigate(Routes.CREDITS) }
            "patron" -> nav.navigate(Routes.PATRON)
            "spellLibrary" -> { nav.navigate(Routes.SETTINGS); nav.navigate(Routes.SPELL_LIBRARY) }
            "itemCatalog" -> { nav.navigate(Routes.SETTINGS); nav.navigate(Routes.ITEM_CATALOG) }
            "monsterCodex" -> { nav.navigate(Routes.SETTINGS); nav.navigate(Routes.MONSTER_CODEX) }
            "customCreatures" -> { nav.navigate(Routes.SETTINGS); nav.navigate(Routes.CUSTOM_CREATURES) }
            "sheet2024", "combat2024", "spells2024", "items2024" -> nav.navigate(Routes.sheet(nyx.id))
            "sheetPF2e" -> nav.navigate(Routes.sheet(seoni.id))
            else -> Unit
        }
    }
}

/**
 * Rendered in place of a route whose character has gone (deleted, or not yet
 * loaded after process death). Pops once, as an effect — calling
 * `popBackStack()` from the composition body re-ran on every recomposition
 * during the exit transition and could pop the roster itself, leaving an
 * empty NavHost.
 */
@Composable
private fun MissingEntry(nav: NavHostController) {
    LaunchedEffect(Unit) {
        if (nav.previousBackStackEntry != null) nav.popBackStack()
    }
}

/** The character's active campaign, if it's committed to one. */
private fun activeCampaign(character: Character, campaigns: List<Campaign>): Campaign? =
    (character.phase as? CharacterPhase.InCampaign)?.let { phase ->
        campaigns.firstOrNull { it.id == phase.campaignId }
    }

/** Parse a path arg into a UUID, or null if absent/malformed. */
private fun NavBackStackEntry.uuidArg(name: String): UUID? =
    arguments?.getString(name)?.let { runCatching { UUID.fromString(it) }.getOrNull() }

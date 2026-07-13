package au.com.evonet.nat20.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.evonet.nat20.app.Nat20Application
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.domain.CharacterPhase
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
import au.com.evonet.nat20.ui.settings.CharacterSettingsScreen
import au.com.evonet.nat20.ui.settings.CreditsScreen
import au.com.evonet.nat20.ui.settings.SettingsScreen
import au.com.evonet.nat20.ui.sheet.CharacterSheetScreen
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
    const val SETTINGS = "settings"
    const val CREDITS = "credits"
    const val SPELL_LIBRARY = "spell-library"
    const val MONSTER_CODEX = "monster-codex"
    const val ITEM_CATALOG = "item-catalog"
    const val PATRON = "patron"
    const val ARG_ID = "id"
    const val ARG_CAMPAIGN_ID = "campaignId"
    fun sheet(id: UUID) = "sheet/$id"
    fun edit(id: UUID) = "editor/$id"
    fun journal(characterId: UUID, campaignId: UUID) = "journal/$characterId/$campaignId"
    fun past(id: UUID) = "past/$id"
    fun characterSettings(id: UUID) = "character-settings/$id"
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
    val patron = container.patronStore
    val isPatron by patron.isPatron.collectAsState()

    fun find(id: UUID?): Character? = id?.let { wanted -> characters.firstOrNull { it.id == wanted } }

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
                nav.popBackStack()
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
                nav.popBackStack()
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
                nav.popBackStack()
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
                nav.popBackStack()
            } else {
                CharacterSettingsScreen(
                    character = character,
                    onBack = { nav.popBackStack() },
                    // Delete removes the character and pops all the way back to the roster.
                    onDelete = {
                        store.delete(character.id)
                        nav.popBackStack(Routes.ROSTER, inclusive = false)
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
                nav.popBackStack()
            } else {
                DnD5eWizardScreen(
                    existing = character,
                    onSave = { store.save(it); nav.popBackStack() },
                    onCancel = { nav.popBackStack() },
                )
            }
        }
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

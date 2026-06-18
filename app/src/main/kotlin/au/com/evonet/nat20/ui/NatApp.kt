package au.com.evonet.nat20.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
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
import au.com.evonet.nat20.ui.editor.DnD5eEditorScreen
import au.com.evonet.nat20.ui.journal.JournalScreen
import au.com.evonet.nat20.ui.roster.RosterScreen
import au.com.evonet.nat20.ui.sheet.CharacterSheetScreen
import java.util.UUID

/**
 * App root: the Navigation Compose graph (roster → sheet → journal, plus the
 * create/edit editor), the iOS `NavigationStack` equivalent. The
 * [CharacterStore] is the shared source of truth; routes carry a character id
 * and resolve against the live roster, so create/edit/delete reflect at once.
 */
private object Routes {
    const val ROSTER = "roster"
    const val SHEET = "sheet/{id}"
    const val JOURNAL = "journal/{id}"
    const val CREATE = "editor"
    const val EDIT = "editor/{id}"
    const val ARG_ID = "id"
    fun sheet(id: UUID) = "sheet/$id"
    fun journal(id: UUID) = "journal/$id"
    fun edit(id: UUID) = "editor/$id"
}

@Composable
fun NatApp() {
    val container = (LocalContext.current.applicationContext as Nat20Application).container
    val store: CharacterStore = viewModel(
        factory = CharacterStore.factory(
            container.characterRepository,
            container.campaignRepository,
            container.rulesetRegistry,
        ),
    )
    val nav = rememberNavController()
    val characters by store.roster.collectAsState()

    fun find(id: UUID?): Character? = id?.let { wanted -> characters.firstOrNull { it.id == wanted } }

    NavHost(navController = nav, startDestination = Routes.ROSTER) {
        composable(Routes.ROSTER) {
            RosterScreen(
                characters = characters,
                onSelect = { nav.navigate(Routes.sheet(it.id)) },
                onNew = { nav.navigate(Routes.CREATE) },
                onDelete = { store.delete(it.id) },
            )
        }

        composable(
            Routes.SHEET,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val character = find(entry.characterId())
            if (character == null) {
                nav.popBackStack()
            } else {
                val campaigns by remember(character.id) { store.campaignsForCharacter(character.id) }
                    .collectAsState(initial = emptyList())
                val active = activeCampaign(character, campaigns)
                CharacterSheetScreen(
                    character = character,
                    activeCampaign = active,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.edit(character.id)) },
                    onStartCampaign = { store.startCampaign(character, it) },
                    onEndCampaign = { active?.let { c -> store.endCampaign(character, c) } },
                    onOpenJournal = { nav.navigate(Routes.journal(character.id)) },
                    onApplyIntent = { intent -> active?.let { c -> store.applyIntent(intent, character, c) } },
                )
            }
        }

        composable(
            Routes.JOURNAL,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val character = find(entry.characterId())
            if (character == null) {
                nav.popBackStack()
            } else {
                val campaigns by remember(character.id) { store.campaignsForCharacter(character.id) }
                    .collectAsState(initial = emptyList())
                JournalScreen(
                    campaign = activeCampaign(character, campaigns),
                    characterName = character.name,
                    onBack = { nav.popBackStack() },
                )
            }
        }

        composable(Routes.CREATE) {
            DnD5eEditorScreen(
                existing = null,
                onSave = { store.save(it); nav.popBackStack() },
                onCancel = { nav.popBackStack() },
            )
        }

        composable(
            Routes.EDIT,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val character = find(entry.characterId())
            if (character == null) {
                nav.popBackStack()
            } else {
                DnD5eEditorScreen(
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

/** Parse the `id` path arg into a UUID, or null if absent/malformed. */
private fun androidx.navigation.NavBackStackEntry.characterId(): UUID? =
    arguments?.getString(Routes.ARG_ID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }

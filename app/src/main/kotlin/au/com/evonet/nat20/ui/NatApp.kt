package au.com.evonet.nat20.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.evonet.nat20.app.Nat20Application
import au.com.evonet.nat20.store.CharacterStore
import au.com.evonet.nat20.ui.journal.JournalScreen
import au.com.evonet.nat20.ui.roster.RosterScreen
import au.com.evonet.nat20.ui.sheet.CharacterSheetScreen
import java.util.UUID

/**
 * App root: the Navigation Compose graph (roster → sheet → journal), the iOS
 * `NavigationStack` equivalent. The [CharacterStore] is the shared source of
 * truth; routes carry a character id and resolve against it.
 */
private object Routes {
    const val ROSTER = "roster"
    const val SHEET = "sheet/{id}"
    const val JOURNAL = "journal/{id}"
    fun sheet(id: UUID) = "sheet/$id"
    fun journal(id: UUID) = "journal/$id"
    const val ARG_ID = "id"
}

@Composable
fun NatApp() {
    val container = (LocalContext.current.applicationContext as Nat20Application).container
    val store: CharacterStore = viewModel(factory = CharacterStore.factory(container.characterRepository))
    val nav = rememberNavController()
    val characters by store.characters.collectAsState()

    NavHost(navController = nav, startDestination = Routes.ROSTER) {
        composable(Routes.ROSTER) {
            RosterScreen(
                characters = characters,
                onSelect = { nav.navigate(Routes.sheet(it.id)) },
            )
        }

        composable(
            Routes.SHEET,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val character = entry.characterId()?.let(store::character)
            if (character == null) {
                nav.popBackStack()
            } else {
                CharacterSheetScreen(
                    character = character,
                    onBack = { nav.popBackStack() },
                    onOpenJournal = { nav.navigate(Routes.journal(character.id)) },
                )
            }
        }

        composable(
            Routes.JOURNAL,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val character = entry.characterId()?.let(store::character)
            if (character == null) {
                nav.popBackStack()
            } else {
                JournalScreen(characterName = character.name, onBack = { nav.popBackStack() })
            }
        }
    }
}

/** Parse the `id` path arg into a UUID, or null if absent/malformed. */
private fun androidx.navigation.NavBackStackEntry.characterId(): UUID? =
    arguments?.getString(Routes.ARG_ID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }

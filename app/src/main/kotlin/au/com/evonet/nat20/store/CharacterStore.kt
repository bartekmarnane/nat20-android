package au.com.evonet.nat20.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import au.com.evonet.nat20.data.CharacterRepository
import au.com.evonet.nat20.domain.Character
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The roster's single source of truth for the UI. Port of the iOS
 * `CharacterStore` (`@Observable @MainActor`); a `ViewModel` projecting the
 * repository's `Flow` into a `StateFlow` the composables collect.
 *
 * As of A5 it's Room-backed through [CharacterRepository] — the UI is unchanged
 * from A4 because it always talked to this store, not the data layer. Create /
 * edit / delete (A6) add write methods here that delegate to the repository.
 */
class CharacterStore(private val repository: CharacterRepository) : ViewModel() {
    val characters: StateFlow<List<Character>> = repository.characters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Look a character up by id (the sheet/journal routes carry the id). */
    fun character(id: UUID): Character? = characters.value.firstOrNull { it.id == id }

    /**
     * Persist a created or edited character (A6). The editor builds the whole
     * [Character] (it owns the ruleset-specific payload), so the store just
     * hands it to the repository. The roster/sheet update via the Flow.
     */
    fun save(character: Character) {
        viewModelScope.launch { repository.upsert(character) }
    }

    /** Delete a character by id (swipe-to-delete on the roster). */
    fun delete(id: UUID) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        /** Factory that injects the repository from the app's container. */
        fun factory(repository: CharacterRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { CharacterStore(repository) }
            }
    }
}

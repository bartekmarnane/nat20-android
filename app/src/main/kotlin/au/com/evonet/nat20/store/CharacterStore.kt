package au.com.evonet.nat20.store

import androidx.lifecycle.ViewModel
import au.com.evonet.nat20.domain.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * The roster's single source of truth for the UI. Port of the iOS
 * `CharacterStore` (`@Observable @MainActor`); here it's a `ViewModel` exposing
 * a `StateFlow` the composables collect.
 *
 * **A4 scope:** read-only, seeded in memory. Create/edit/delete (A6) and the
 * Room-backed `CharacterRepository` (A5) replace the in-memory list without the
 * UI changing — it already talks to this store, not the data layer.
 */
class CharacterStore : ViewModel() {
    private val _characters = MutableStateFlow(DemoCharacters.seed())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()

    /** Look a character up by id (the sheet/journal routes carry the id). */
    fun character(id: UUID): Character? = _characters.value.firstOrNull { it.id == id }
}

package au.com.evonet.nat20.data

import au.com.evonet.nat20.domain.Character
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * The roster's persistence seam. The UI/store depend on this interface, never
 * on Room — so a future sync backend (or a different local store) slots in
 * without touching the domain or the UI. This is the insertion point the
 * README's "Open decisions" reserve for cross-device sync.
 *
 * Deals only in domain [Character]s; the Room ↔ domain translation is the
 * implementation's job (via `CharacterCodec`).
 */
interface CharacterRepository {
    /** Live stream of the full roster. */
    val characters: Flow<List<Character>>

    /** Insert or update a character. */
    suspend fun upsert(character: Character)

    /** Remove a character by id. */
    suspend fun delete(id: UUID)

    /**
     * Seed the store with [characters] only if it's currently empty. Used for
     * the debug-only first-launch demo roster; a no-op once anything exists.
     */
    suspend fun seedIfEmpty(characters: List<Character>)
}

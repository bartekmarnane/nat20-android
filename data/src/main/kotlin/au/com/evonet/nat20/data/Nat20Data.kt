package au.com.evonet.nat20.data

import android.content.Context
import androidx.room.Room
import au.com.evonet.nat20.domain.RulesetRegistry

/**
 * Composition entry point for the persistence layer. The app calls this to get
 * a [CharacterRepository] without ever importing Room itself — Room stays
 * confined to `:data`, so the rest of the app talks only to the interface.
 */
object Nat20Data {
    private const val DATABASE_NAME = "nat20.db"

    fun characterRepository(
        context: Context,
        registry: RulesetRegistry,
    ): CharacterRepository {
        val database = Room.databaseBuilder(
            context.applicationContext,
            Nat20Database::class.java,
            DATABASE_NAME,
        ).build()
        return RoomCharacterRepository(database.characterDao(), CharacterCodec(registry))
    }
}

package au.com.evonet.nat20.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's Room database. Version 1 holds only characters (A5); campaign +
 * journal tables are added in a later version at A7a, migrated cleanly off the
 * exported schema (see `data/schemas`).
 */
@Database(entities = [PersistentCharacter::class], version = 1, exportSchema = true)
abstract class Nat20Database : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
}

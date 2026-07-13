package au.com.evonet.nat20.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's Room database.
 *
 * - v1 (A5): characters.
 * - v2 (A7a): adds the campaigns table.
 * - v3 (A7d): adds `campaigns.chronicleJson` for generated chronicle prose.
 * - v4 (A18): adds `characters.summonsJson` for familiars/companions/summons.
 * - v5 (parity #11): adds the `custom_races` table for homebrew races.
 *
 * Each step is an additive migration (see `Migrations`), validated against the
 * exported schema in `data/schemas`.
 */
@Database(
    entities = [PersistentCharacter::class, PersistentCampaign::class, PersistentCustomRace::class],
    version = 5,
    exportSchema = true,
)
abstract class Nat20Database : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun campaignDao(): CampaignDao
    abstract fun customRaceDao(): CustomRaceDao
}

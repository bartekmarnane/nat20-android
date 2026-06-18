package au.com.evonet.nat20.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's Room database.
 *
 * - v1 (A5): characters.
 * - v2 (A7a): adds the campaigns table.
 * - v3 (A7d): adds `campaigns.chronicleJson` for generated chronicle prose.
 *
 * Each step is an additive migration (see `Migrations`), validated against the
 * exported schema in `data/schemas`.
 */
@Database(
    entities = [PersistentCharacter::class, PersistentCampaign::class],
    version = 3,
    exportSchema = true,
)
abstract class Nat20Database : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun campaignDao(): CampaignDao
}

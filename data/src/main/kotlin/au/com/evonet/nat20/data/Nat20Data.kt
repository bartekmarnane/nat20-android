package au.com.evonet.nat20.data

import android.content.Context
import androidx.room.Room
import au.com.evonet.nat20.domain.RulesetRegistry

/**
 * Composition entry point for the persistence layer. The app calls [create] to
 * get the repositories without ever importing Room itself — Room stays confined
 * to `:data`, so the rest of the app talks only to the interfaces.
 */
object Nat20Data {
    private const val DATABASE_NAME = "nat20.db"

    /** The repositories the app wires into its container, sharing one database. */
    class Repositories internal constructor(
        val characters: CharacterRepository,
        val campaigns: CampaignRepository,
        val customRaces: CustomRaceRepository,
        val customCreatures: CustomCreatureRepository,
    )

    fun create(context: Context, registry: RulesetRegistry): Repositories {
        val database = Room.databaseBuilder(
            context.applicationContext,
            Nat20Database::class.java,
            DATABASE_NAME,
        )
            .addMigrations(
                Migrations.MIGRATION_1_2,
                Migrations.MIGRATION_2_3,
                Migrations.MIGRATION_3_4,
                Migrations.MIGRATION_4_5,
                Migrations.MIGRATION_5_6,
                Migrations.MIGRATION_6_7,
                Migrations.MIGRATION_7_8,
            )
            .build()

        val characterCodec = CharacterCodec(registry)
        val campaignCodec = CampaignCodec(registry, characterCodec)
        return Repositories(
            characters = RoomCharacterRepository(database.characterDao(), characterCodec),
            campaigns = RoomCampaignRepository(database.campaignDao(), campaignCodec),
            customRaces = RoomCustomRaceRepository(database.customRaceDao()),
            customCreatures = RoomCustomCreatureRepository(database.customCreatureDao()),
        )
    }
}

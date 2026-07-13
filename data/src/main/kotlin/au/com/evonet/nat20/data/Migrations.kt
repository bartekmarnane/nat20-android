package au.com.evonet.nat20.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations. Greenfield, but we still version cleanly (README A5) so that
 * once there are real users a schema change never wipes data. The SQL is copied
 * verbatim from the exported schema (`data/schemas`), so Room's runtime
 * validation against the v2 schema passes.
 */
internal object Migrations {
    /** v1 → v2 (A7a): additive — create the campaigns table + its index. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `campaigns` (" +
                    "`id` TEXT NOT NULL, `characterId` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`rulesetId` TEXT NOT NULL, `startedAt` TEXT NOT NULL, `endedAt` TEXT, " +
                    "`startSnapshotJson` TEXT NOT NULL, `endSnapshotJson` TEXT, " +
                    "`logJson` TEXT NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_campaigns_characterId` " +
                    "ON `campaigns` (`characterId`)",
            )
        }
    }

    /** v2 → v3 (A7d): additive — chronicle prose column on campaigns. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `campaigns` ADD COLUMN `chronicleJson` TEXT NOT NULL DEFAULT '[]'")
        }
    }

    /** v3 → v4 (A18): additive — summoned-creature column on characters. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `characters` ADD COLUMN `summonsJson` TEXT NOT NULL DEFAULT '[]'")
        }
    }

    /** v4 → v5 (parity #11): additive — create the homebrew custom_races table. */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `custom_races` (" +
                    "`id` TEXT NOT NULL, `raceId` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`payloadJson` TEXT NOT NULL, `createdAt` TEXT NOT NULL, " +
                    "`updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

    /** v5 → v6 (parity #23): additive — nullable portrait BLOB column on characters. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `characters` ADD COLUMN `portraitData` BLOB")
        }
    }

    /** v6 → v7 (parity #37): additive — party-roster column on campaigns. */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `campaigns` ADD COLUMN `partyJson` TEXT NOT NULL DEFAULT '[]'")
        }
    }
}

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
}

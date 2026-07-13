package au.com.evonet.nat20.store

import au.com.evonet.nat20.data.CustomRaceRecord
import au.com.evonet.nat20.data.CustomRaceRepository
import au.com.evonet.nat20.dnd5e.CustomRaceJson
import au.com.evonet.nat20.dnd5e.CustomRaceLibrary
import au.com.evonet.nat20.dnd5e.Race
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * Bridges the in-memory [CustomRaceLibrary] (pure-JVM, `:ruleset-dnd5e-2014`)
 * to Room (parity #11). At startup, saved rows hydrate the library through
 * `replaceAll` (which does NOT fire `onChange`, so hydration never writes
 * back); afterwards every library mutation reconciles the table write-through:
 * changed rows update by raceId, new homebrews insert, removed ones delete.
 *
 * Only the library row is touched on delete — saved characters keep a dangling
 * `race` id, matching iOS.
 */
class CustomRaceSync(
    private val repository: CustomRaceRepository,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            val rows = repository.all()
            val races = rows.mapNotNull { row -> CustomRaceJson.decode(row.payloadJson) }
            CustomRaceLibrary.replaceAll(races)
            CustomRaceLibrary.onChange = { updated ->
                scope.launch { reconcile(updated) }
            }
        }
    }

    private suspend fun reconcile(races: List<Race>) {
        val existing = repository.all().associateBy { it.raceId }
        val now = Instant.now()
        for (race in races) {
            val payload = CustomRaceJson.encode(race)
            val row = existing[race.id]
            when {
                row == null -> repository.upsert(
                    CustomRaceRecord(
                        id = UUID.randomUUID(),
                        raceId = race.id,
                        name = race.name,
                        payloadJson = payload,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                row.payloadJson != payload || row.name != race.name -> repository.upsert(
                    row.copy(name = race.name, payloadJson = payload, updatedAt = now),
                )
            }
        }
        val kept = races.mapTo(mutableSetOf()) { it.id }
        existing.keys.filterNot { it in kept }.forEach { repository.deleteByRaceId(it) }
    }
}

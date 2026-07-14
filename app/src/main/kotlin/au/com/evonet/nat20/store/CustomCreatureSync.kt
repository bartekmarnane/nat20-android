package au.com.evonet.nat20.store

import au.com.evonet.nat20.data.CustomCreatureRecord
import au.com.evonet.nat20.data.CustomCreatureRepository
import au.com.evonet.nat20.dnd5e.CustomCreature
import au.com.evonet.nat20.dnd5e.CustomCreatureJson
import au.com.evonet.nat20.dnd5e.CustomCreatureLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * Bridges the in-memory [CustomCreatureLibrary] to Room (parity #44), the same
 * hydrate-then-write-through shape as [CustomRaceSync].
 */
class CustomCreatureSync(
    private val repository: CustomCreatureRepository,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            val creatures = repository.all().mapNotNull { CustomCreatureJson.decode(it.payloadJson) }
            CustomCreatureLibrary.replaceAll(creatures)
            CustomCreatureLibrary.onChange = { updated -> scope.launch { reconcile(updated) } }
        }
    }

    private suspend fun reconcile(creatures: List<CustomCreature>) {
        val existing = repository.all().associateBy { it.creatureId }
        val now = Instant.now()
        for (creature in creatures) {
            val payload = CustomCreatureJson.encode(creature)
            val row = existing[creature.id]
            when {
                row == null -> repository.upsert(
                    CustomCreatureRecord(
                        id = UUID.randomUUID(),
                        creatureId = creature.id,
                        name = creature.name,
                        payloadJson = payload,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                row.payloadJson != payload || row.name != creature.name -> repository.upsert(
                    row.copy(name = creature.name, payloadJson = payload, updatedAt = now),
                )
            }
        }
        val kept = creatures.mapTo(mutableSetOf()) { it.id }
        existing.keys.filterNot { it in kept }.forEach { repository.deleteByCreatureId(it) }
    }
}

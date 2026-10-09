package au.com.evonet.nat20.dnd5e

/**
 * One catalogue entry a character has already selected, tagged with the kind
 * of slot it fills. Rendered in the warning shown when a player turns a
 * content source off.
 */
data class SourceUsage(val category: String, val name: String)

/**
 * Answers "what has this character already taken from that book?" so the
 * Content Sources editor can warn before a source is switched off.
 *
 * Why it matters: [DnD5ePayload.enabledSources] only filters *pickers* — see
 * [filteredBySources]. Turning Tasha's off doesn't strip a Peace Cleric's
 * subclass or unlearn Silvery Barbs; those stay on the sheet and keep
 * working. What changes is that they stop appearing in lists, so a player who
 * swaps a spell out can't swap it back in. That's a surprise worth spelling
 * out rather than discovering three sessions later.
 *
 * Deliberately limited to catalogue-tagged selections — exactly the entries
 * [filteredBySources] acts on. Port of the iOS `SourceUsageAudit`.
 */
object SourceUsageAudit {
    /**
     * Every selection on [payload] that came from [sourceId], in a stable
     * order: identity first (race → class → subclass → background), then the
     * per-level picks, then spells alphabetically. Empty when the character
     * owes nothing to that book.
     */
    fun usages(sourceId: String, payload: DnD5ePayload): List<SourceUsage> {
        val found = LinkedHashSet<SourceUsage>()

        fun note(category: String, name: String) {
            found += SourceUsage(category, name)
        }

        DnD5eCatalog.race(payload.race)
            ?.takeIf { it.sourceId == sourceId }
            ?.let { note("Race", it.name) }

        payload.classes.forEach { entry ->
            val klass = DnD5eCatalog.characterClass(entry.classId) ?: return@forEach
            if (klass.sourceId == sourceId) note("Class", klass.name)
            // The payload holds the display name, but older or hand-edited
            // data can carry the catalogue slug — match either.
            val chosen = entry.subclass
            if (chosen != null) {
                klass.subclasses
                    .firstOrNull { it.name.equals(chosen, ignoreCase = true) || it.id == chosen }
                    ?.takeIf { it.sourceId == sourceId }
                    ?.let { note("Subclass", it.name) }
            }
        }

        DnD5eCatalog.background(payload.background)
            ?.takeIf { it.sourceId == sourceId }
            ?.let { note("Background", it.name) }

        payload.chosenFeats.forEach { id ->
            Feats.feat(id)?.takeIf { it.sourceId == sourceId }?.let { note("Feat", it.name) }
        }
        payload.fightingStyles.forEach { id ->
            FightingStyles.style(id)?.takeIf { it.sourceId == sourceId }
                ?.let { note("Fighting Style", it.name) }
        }
        payload.metamagicKnown.forEach { id ->
            Metamagics.option(id)?.takeIf { it.sourceId == sourceId }
                ?.let { note("Metamagic", it.name) }
        }
        payload.invocationsKnown.forEach { id ->
            Invocations.invocation(id)?.takeIf { it.sourceId == sourceId }
                ?.let { note("Invocation", it.name) }
        }
        payload.pactBoon
            ?.let(PactBoons::boon)
            ?.takeIf { it.sourceId == sourceId }
            ?.let { note("Pact Boon", it.name) }

        // Spell ids arrive from maps, so sort by display name — otherwise the
        // warning reshuffles itself between presentations.
        spellIds(payload)
            .mapNotNull(DnD5eCatalog::spell)
            .filter { it.sourceId == sourceId }
            .map { it.name }
            .distinct()
            .sorted()
            .forEach { note("Spell", it) }

        return found.toList()
    }

    /**
     * Every spell id the character has any claim on — cantrips, known spells,
     * and the prepared lists, which for prepared casters hold picks that never
     * appear in `spellsKnown`.
     */
    private fun spellIds(payload: DnD5ePayload): List<String> =
        payload.cantripsKnown +
            payload.spellsKnown.values.flatten() +
            payload.preparedSpells.values.flatten()
}

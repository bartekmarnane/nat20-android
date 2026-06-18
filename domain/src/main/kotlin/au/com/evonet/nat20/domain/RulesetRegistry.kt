package au.com.evonet.nat20.domain

/**
 * Resolves a [RulesetId] to its [Ruleset] implementation. The persistence
 * layer holds a character's payload as opaque JSON tagged with its ruleset id;
 * to decode it back into a concrete [CharacterPayload] it needs the matching
 * ruleset's codec — but the data layer must stay ruleset-agnostic, so it asks
 * a registry rather than referencing any concrete ruleset.
 *
 * The concrete registry (which knows about `DnD5eRuleset`, and later the 2024 /
 * PF2e rulesets) is built in the app's composition root and injected down.
 */
interface RulesetRegistry {
    /** The ruleset for [id], or null if no ruleset with that id is registered. */
    fun ruleset(id: RulesetId): Ruleset?
}

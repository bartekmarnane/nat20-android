package au.com.evonet.nat20.app

import au.com.evonet.nat20.dnd5e.DnD5eRuleset
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Ruleset
import au.com.evonet.nat20.pf2e.PathfinderRuleset
import au.com.evonet.nat20.domain.Ruleset
import au.com.evonet.nat20.domain.RulesetId
import au.com.evonet.nat20.domain.RulesetRegistry

/**
 * The app's concrete [RulesetRegistry]. This is the one place that knows which
 * rulesets exist; new editions (5e 2024, PF2e) register here and nothing else
 * changes. Each ruleset is a stateless value, so a single shared instance is
 * fine.
 */
class DefaultRulesetRegistry : RulesetRegistry {
    private val byId: Map<RulesetId, Ruleset> = listOf(
        DnD5eRuleset(),
        DnD5e2024Ruleset(),
        PathfinderRuleset(),
    ).associateBy { it.id }

    override fun ruleset(id: RulesetId): Ruleset? = byId[id]
}

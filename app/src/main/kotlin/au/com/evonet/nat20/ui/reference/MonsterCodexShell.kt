package au.com.evonet.nat20.ui.reference

import androidx.compose.runtime.Composable

/**
 * The unified Monster Codex (matching iOS): one reference browser with ruleset
 * tabs — D&D 5e 2014, D&D 5e 2024, Pathfinder 2e — rather than three separate
 * Settings entries. Each tab embeds that ruleset's chromeless bestiary body.
 */
@Composable
fun MonsterCodexShell(onBack: () -> Unit) {
    ReferenceTabShell(
        eyebrow = "Bestiary",
        title = "Monster Codex",
        onBack = onBack,
        tabs = listOf(
            ReferenceTab("5e 2014") { MonsterCodexBody() },
            ReferenceTab("5e 2024") { MonsterCodex2024Body() },
            ReferenceTab("PF2e") { PathfinderMonsterCodexBody() },
        ),
    )
}

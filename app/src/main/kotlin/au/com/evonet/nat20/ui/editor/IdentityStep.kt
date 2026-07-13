package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The shared first step of every ruleset's creation wizard — the character's
 * identity. Today that's the name field on the parchment atoms; portrait + AI
 * "describe" affordances land here when those features arrive (mirrors iOS's
 * `IdentityStep`), so every edition picks them up in one place instead of three.
 *
 * The 5e (2014), 5e (2024), and Pathfinder wizards all render this for their NAME
 * step. Each passes the [modifier] that matches its own content layout: the 5e
 * wizards size + pad here (their step area doesn't), while Pathfinder's step area
 * already scrolls + pads, so it passes the default. The 2014 wizard appends its
 * starting-level stepper below (iOS keeps level on the Name step).
 */
@Composable
fun IdentityStep(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        WizardStepSection("Identity", "Tell us who walks the road.")
        WizardFieldLabel("Name", required = true)
        WizardTextField(placeholder = "Their name…", value = name, onValueChange = onNameChange)
    }
}

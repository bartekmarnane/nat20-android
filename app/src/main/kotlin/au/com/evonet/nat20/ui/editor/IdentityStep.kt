package au.com.evonet.nat20.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization

/**
 * The shared first step of every ruleset's creation wizard — the character's
 * identity. Today that's just the name field; portrait + AI "describe" affordances
 * land here when those features arrive (mirrors iOS's `IdentityStep`), so every
 * edition picks them up in one place instead of three.
 *
 * The 5e (2014), 5e (2024), and Pathfinder wizards all render this for their NAME
 * step. Each passes the [modifier] that matches its own content layout: the 5e
 * wizards size + pad here (their step area doesn't), while Pathfinder's step area
 * already scrolls + pads, so it passes the default.
 */
@Composable
fun IdentityStep(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Character name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

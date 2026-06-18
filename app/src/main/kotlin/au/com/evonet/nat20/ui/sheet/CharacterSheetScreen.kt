package au.com.evonet.nat20.ui.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5eRuleset
import au.com.evonet.nat20.domain.Character

/**
 * The character sheet host. Owns the universal chrome (title + Journal action)
 * and dispatches the body by `rulesetId` — the iOS `CharacterSheetView` pattern.
 * A hand-written `when` is fine while one ruleset exists; it becomes a registry
 * when the second lands (A21), exactly as the iOS README notes for its dispatch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    character: Character,
    onBack: () -> Unit,
    onOpenJournal: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(character.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = onOpenJournal) { Text("Journal") } },
            )
        },
    ) { inner ->
        val body = Modifier.padding(inner)
        when (character.rulesetId) {
            DnD5eRuleset.RULESET_ID -> DnD5eSheetView(character, body)
            else -> UnsupportedRuleset(character.rulesetId, body)
        }
    }
}

@Composable
private fun UnsupportedRuleset(rulesetId: String, modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            "No sheet for ruleset \"$rulesetId\" yet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

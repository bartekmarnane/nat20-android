package au.com.evonet.nat20.ui.journal

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The campaign journal, reached from the sheet. In A4 it's an empty state:
 * journals live on a `Campaign` (A7a) and nothing has started a campaign yet,
 * so there's deliberately nothing to show until then.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(characterName: String, onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Journal") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { inner ->
        Box(
            Modifier.fillMaxSize().padding(inner).padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "No adventures yet.\nStart a campaign to begin $characterName's chronicle.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

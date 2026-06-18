package au.com.evonet.nat20.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Credits & Licenses (A9). Carries the same attributions as the iOS Credits
 * screen — required by the open licenses the bundled content + fonts ship under.
 * Keep verbatim when adding sources.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Credits & Licenses") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CreditCard(
                "Game content",
                "Game mechanics and content from the System Reference Document 5.1 " +
                    "(\"SRD 5.1\") by Wizards of the Coast LLC, used under the Creative " +
                    "Commons Attribution 4.0 International License (CC BY 4.0).\n\n" +
                    "Catalogue data derived from the open-source 5e-bits/5e-database " +
                    "project, also under CC BY 4.0.",
            )
            CreditCard(
                "Typefaces",
                "Cinzel, Cormorant Garamond, EB Garamond, and IM Fell English " +
                    "(Regular, Italic, and Small Caps) are used under the SIL Open " +
                    "Font License 1.1 (OFL).",
            )
            CreditCard(
                "Trademarks",
                "Dungeons & Dragons and D&D are trademarks of Wizards of the Coast. " +
                    "Nat20 is an independent companion app and is not affiliated with " +
                    "or endorsed by Wizards of the Coast.",
            )
        }
    }
}

@Composable
private fun CreditCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

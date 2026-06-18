package au.com.evonet.nat20.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.settings.AppSettings
import au.com.evonet.nat20.settings.AppearanceMode

/**
 * Settings (A9): appearance (light / dark / system, applied live) and an entry
 * to Credits & Licenses. Narration style and the reference catalogues (Spell
 * Library / Item Catalog) join once that content lands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    onOpenSpellLibrary: () -> Unit,
    onOpenMonsterCodex: () -> Unit,
    onOpenItemCatalog: () -> Unit,
    onOpenCredits: () -> Unit,
    onBack: () -> Unit,
) {
    val appearance by appSettings.appearance.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingSection("Appearance") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppearanceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = appearance == mode,
                            onClick = { appSettings.setAppearance(mode) },
                            label = { Text(mode.label()) },
                        )
                    }
                }
            }

            SettingSection("Reference") {
                Row(Modifier.fillMaxWidth().clickable(onClick = onOpenSpellLibrary).padding(vertical = 8.dp)) {
                    Text("Spell Library", style = MaterialTheme.typography.bodyLarge)
                }
                Row(Modifier.fillMaxWidth().clickable(onClick = onOpenMonsterCodex).padding(vertical = 8.dp)) {
                    Text("Monster Codex (2024)", style = MaterialTheme.typography.bodyLarge)
                }
                Row(Modifier.fillMaxWidth().clickable(onClick = onOpenItemCatalog).padding(vertical = 8.dp)) {
                    Text("Item Catalog (2024)", style = MaterialTheme.typography.bodyLarge)
                }
            }

            SettingSection("About") {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onOpenCredits).padding(vertical = 8.dp),
                ) {
                    Text("Credits & Licenses", style = MaterialTheme.typography.bodyLarge)
                }
                Row(
                    Modifier.fillMaxWidth().clickable { appSettings.setOnboardingComplete(false); onBack() }.padding(vertical = 8.dp),
                ) {
                    Text("Replay introduction", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

private fun AppearanceMode.label(): String = when (this) {
    AppearanceMode.SYSTEM -> "System"
    AppearanceMode.LIGHT -> "Light"
    AppearanceMode.DARK -> "Dark"
}

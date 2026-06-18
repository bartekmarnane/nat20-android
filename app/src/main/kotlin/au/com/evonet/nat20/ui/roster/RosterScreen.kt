package au.com.evonet.nat20.ui.roster

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.slugToTitle

/**
 * The character roster — the app's home screen. Port of the iOS
 * `CharacterListView`. Read-only in A4; the New-Character CTA + swipe-to-delete
 * arrive with the create/edit flows (A6).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    characters: List<Character>,
    onSelect: (Character) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Nat20") }) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + 8.dp,
                bottom = inner.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(characters, key = { it.id }) { character ->
                CharacterRow(character, onClick = { onSelect(character) })
            }
        }
    }
}

@Composable
private fun CharacterRow(character: Character, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = character.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = character.subtitle(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Mountain Dwarf · Fighter 3" — a quick identity line for the roster row. */
private fun Character.subtitle(): String {
    val payload = payload as? DnD5ePayload ?: return rulesetId
    val race = payload.race.takeIf { it.isNotEmpty() }?.slugToTitle()
    val cls = payload.characterClass.takeIf { it.isNotEmpty() }?.slugToTitle()
    val classLine = cls?.let { "$it ${payload.level}" }
    return listOfNotNull(race, classLine).joinToString(" · ").ifEmpty { "Level ${payload.level}" }
}

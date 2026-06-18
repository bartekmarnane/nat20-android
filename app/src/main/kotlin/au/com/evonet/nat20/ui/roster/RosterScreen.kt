package au.com.evonet.nat20.ui.roster

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.slugToTitle

/**
 * The character roster — the app's home screen. Port of the iOS
 * `CharacterListView`. A6 adds a New-Character action and swipe-to-delete with
 * a confirmation dialog. (The free-tier "Upgrade" CTA swap is A14.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    characters: List<Character>,
    onSelect: (Character) -> Unit,
    onNew: () -> Unit,
    onDelete: (Character) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Character?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Nat20") },
                actions = { TextButton(onClick = onOpenSettings) { Text("Settings") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNew) { Text("New Character") }
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + 8.dp,
                bottom = inner.calculateBottomPadding() + 88.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(characters, key = { it.id }) { character ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { target ->
                        // Don't auto-dismiss: arm the confirmation dialog and snap back.
                        if (target == SwipeToDismissBoxValue.EndToStart) pendingDelete = character
                        false
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = { DeleteSwipeBackground() },
                ) {
                    CharacterRow(character, onClick = { onSelect(character) })
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${target.name}?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
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

@Composable
private fun DeleteSwipeBackground() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
    }
}

/** "Mountain Dwarf · Fighter 3" — a quick identity line for the roster row. */
private fun Character.subtitle(): String {
    (payload as? DnD5e2024Payload)?.let { p ->
        val species = p.species.takeIf { it.isNotEmpty() }?.slugToTitle()
        val cls = p.characterClass.takeIf { it.isNotEmpty() }?.slugToTitle()?.let { "$it ${p.level}" }
        return listOfNotNull(species, cls).joinToString(" · ").ifEmpty { "Level ${p.level}" } + " · 2024"
    }
    val payload = payload as? DnD5ePayload ?: return rulesetId
    val race = payload.race.takeIf { it.isNotEmpty() }?.slugToTitle()
    val cls = payload.characterClass.takeIf { it.isNotEmpty() }?.slugToTitle()
    val classLine = cls?.let { "$it ${payload.level}" }
    return listOfNotNull(race, classLine).joinToString(" · ").ifEmpty { "Level ${payload.level}" }
}

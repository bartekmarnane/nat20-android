package au.com.evonet.nat20.ui.past

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
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.slugToTitle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Past Adventures (A7c): the build-phase index of a character's ended campaigns,
 * newest-ended first. Each row summarises the run (level arc, primary class,
 * ended date); tapping opens that campaign's journal read-only (the journal has
 * no editing affordances, so reuse `JournalScreen`). Port of the iOS
 * `PastAdventuresView`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastAdventuresScreen(
    endedCampaigns: List<Campaign>,
    onOpen: (Campaign) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Past Adventures") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { inner ->
        val campaigns = endedCampaigns.sortedByDescending { it.endedAt }
        if (campaigns.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No finished adventures yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
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
                items(campaigns, key = { it.id }) { campaign ->
                    PastAdventureRow(campaign, onClick = { onOpen(campaign) })
                }
            }
        }
    }
}

@Composable
private fun PastAdventureRow(campaign: Campaign, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                campaign.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                campaign.subtitle(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            campaign.endedAt?.let { ended ->
                Text(
                    "Ended ${ended.formattedDate()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Lyra Brightwood · Wizard · Lv 2 → 4" from the start/end snapshots. */
private fun Campaign.subtitle(): String {
    val start = startSnapshot.payload as? DnD5ePayload
    val end = (endSnapshot ?: startSnapshot).payload as? DnD5ePayload
    val name = startSnapshot.name
    val cls = start?.characterClass?.takeIf { it.isNotEmpty() }?.slugToTitle()
    val arc = if (start != null && end != null) {
        if (start.level == end.level) "Lv ${end.level}" else "Lv ${start.level} → ${end.level}"
    } else {
        null
    }
    return listOfNotNull(name, cls, arc).joinToString(" · ")
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())

private fun Instant.formattedDate(): String =
    atZone(ZoneId.systemDefault()).format(dateFormatter)

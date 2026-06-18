package au.com.evonet.nat20.ui.journal

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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.CampaignSession
import au.com.evonet.nat20.domain.LoggedEvent
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * The campaign journal (A7b), now grouped into play sessions with per-session
 * AI chronicle prose (A7d). Sessions newest-first; within a session, entries in
 * the order they happened (the chronicle reads chronologically). When on-device
 * AI is unavailable the prose simply doesn't appear — the entries stand alone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    campaign: Campaign?,
    characterName: String,
    chronicleAvailable: Boolean,
    chronicling: Boolean,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(campaign?.name ?: "Journal") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { inner ->
        val sessions = campaign?.sessions.orEmpty()
        if (sessions.isEmpty()) {
            EmptyJournal(characterName, Modifier.padding(inner))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = inner.calculateTopPadding() + 8.dp,
                    bottom = inner.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                sessions.forEach { session ->
                    item(key = "session-${session.id}") {
                        SessionHeader(
                            session = session,
                            chronicle = campaign?.chronicle(session.id)?.paragraph,
                            chronicling = chronicling && chronicleAvailable,
                        )
                    }
                    items(session.events, key = { it.id }) { entry -> JournalRow(entry) }
                }
            }
        }
    }
}

@Composable
private fun SessionHeader(session: CampaignSession, chronicle: String?, chronicling: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Session ${session.number} · ${session.startedAt.formattedDate()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        when {
            chronicle != null -> Text(
                chronicle,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
            )
            chronicling -> Text(
                "Chronicling this session…",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun JournalRow(entry: LoggedEvent) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.displaySummary, style = MaterialTheme.typography.bodyLarge)
            Text(
                entry.timestamp.formattedTime(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyJournal(characterName: String, modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            "The page is blank.\n$characterName's tale begins with the first deed.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())

private fun java.time.Instant.formattedTime(): String =
    atZone(ZoneId.systemDefault()).format(timeFormatter)

private fun java.time.Instant.formattedDate(): String =
    atZone(ZoneId.systemDefault()).format(dateFormatter)

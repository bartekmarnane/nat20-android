package au.com.evonet.nat20.ui.past

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.ui.slugToTitle
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.natPalette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Past Adventures (parity #39): the build-phase index of a character's ended
 * campaigns, newest-ended first, on the parchment chrome. Each card shows the
 * run's level arc, primary class, party, and ended date; tapping opens that
 * campaign's journal read-only. Port of the iOS `PastAdventuresView`.
 */
@Composable
fun PastAdventuresScreen(
    endedCampaigns: List<Campaign>,
    onOpen: (Campaign) -> Unit,
    onBack: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp, start = 22.dp, end = 22.dp)) {
            BackCircle(onBack, Modifier.align(Alignment.CenterStart))
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ADVENTURES", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 3.sp, color = palette.inkMute)
                Text("Past", fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = palette.ink)
            }
        }
        OrnamentalDivider(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), opacity = 0.4f)

        val campaigns = endedCampaigns.sortedByDescending { it.endedAt }
        if (campaigns.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No finished adventures yet.",
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    color = palette.inkSoft,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(campaigns, key = { it.id }) { PastCard(it, onClick = { onOpen(it) }) }
            }
        }
    }
}

@Composable
private fun BackCircle(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.natPalette
    Box(
        modifier.size(38.dp).clip(CircleShape).background(palette.tileStrong).border(1.dp, palette.accent, CircleShape).clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = palette.accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PastCard(campaign: Campaign, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile.copy(alpha = 0.66f))
            .border(1.dp, palette.ink.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            campaign.name,
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = palette.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(campaign.arcLine(), fontFamily = Cormorant, fontSize = 15.sp, color = palette.inkSoft, maxLines = 1)
        if (campaign.party.isNotEmpty()) {
            Text(
                "with " + campaign.party.joinToString(", ") { it.name },
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = palette.inkMute,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        campaign.endedAt?.let {
            Text("Ended ${it.formattedDate()}", fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkMute)
        }
    }
}

/** "Wizard · Lv 2 → 4" from the start/end snapshots (2014 or 2024 payload). */
private fun Campaign.arcLine(): String {
    fun lvlAndClass(payload: Any?): Pair<Int, String?>? = when (payload) {
        is DnD5ePayload -> payload.level to payload.characterClass.takeIf { it.isNotEmpty() }?.slugToTitle()
        is DnD5e2024Payload -> payload.level to payload.characterClass.takeIf { it.isNotEmpty() }?.slugToTitle()
        else -> null
    }
    val start = lvlAndClass(startSnapshot.payload)
    val end = lvlAndClass((endSnapshot ?: startSnapshot).payload)
    val cls = end?.second ?: start?.second ?: "Adventurer"
    val arc = when {
        start != null && end != null && start.first != end.first -> "Lv ${start.first} → ${end.first}"
        end != null -> "Lv ${end.first}"
        else -> null
    }
    return listOfNotNull(cls, arc).joinToString(" · ")
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())

private fun Instant.formattedDate(): String = atZone(ZoneId.systemDefault()).format(dateFormatter)

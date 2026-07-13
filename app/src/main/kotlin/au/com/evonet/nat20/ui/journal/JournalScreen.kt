package au.com.evonet.nat20.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.domain.Campaign
import au.com.evonet.nat20.domain.CampaignSession
import au.com.evonet.nat20.domain.LoggedEvent
import au.com.evonet.nat20.domain.PartyMember
import au.com.evonet.nat20.ui.campaign.CampaignSetupScreen
import au.com.evonet.nat20.ui.codex.DashedNotice
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.Diamond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.natPalette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * The campaign journal (parity #38), rebuilt on the parchment chrome. Sessions
 * are shown one at a time (newest first) with an Earlier/Later pager; a
 * Chronicle/Log toggle appears once the AI chronicle is unlocked (≥5 entries).
 * The cog opens the campaign settings (edit) — rename / party / leave. Read-only
 * mode (a past adventure) hides the cog. Port of the iOS `CampaignJournalView`.
 */
@Composable
fun JournalScreen(
    campaign: Campaign?,
    characterName: String,
    chronicleAvailable: Boolean,
    chronicling: Boolean,
    readOnly: Boolean,
    onBack: () -> Unit,
    onRename: (String) -> Unit = {},
    onUpdateParty: (List<PartyMember>) -> Unit = {},
    onLeave: () -> Unit = {},
) {
    val palette = MaterialTheme.natPalette
    val sessions = campaign?.sessions.orEmpty()
    var sessionIdx by remember(campaign?.id, sessions.size) { mutableIntStateOf(0) }
    var chronicleMode by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val unlocked = chronicleAvailable && (campaign?.log?.size ?: 0) >= 5

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Top nav: back / kicker+title / cog.
        Box(Modifier.fillMaxWidth().padding(top = 8.dp, start = 22.dp, end = 22.dp)) {
            BackCircle(onBack, Modifier.align(Alignment.CenterStart))
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (readOnly) "PAST CAMPAIGN" else "CAMPAIGN", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 3.sp, color = palette.inkMute)
                Text(campaign?.name ?: "Journal", fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!readOnly && campaign != null) {
                Box(
                    Modifier.align(Alignment.CenterEnd).size(38.dp).clip(CircleShape).background(palette.tileStrong).border(1.dp, palette.accent, CircleShape).clickable { editing = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Campaign settings", tint = palette.accent, modifier = Modifier.size(16.dp))
                }
            }
        }
        OrnamentalDivider(Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = if (unlocked) 12.dp else 4.dp), opacity = 0.4f)

        if (unlocked) {
            ChronicleToggle(chronicleMode, onChange = { chronicleMode = it })
            Spacer(Modifier.height(12.dp))
        }

        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(horizontal = 22.dp).padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                DashedNotice("The journal lies open and unmarked. $characterName's tale begins with the first deed.")
            }
        } else {
            val idx = sessionIdx.coerceIn(0, sessions.lastIndex)
            val session = sessions[idx]
            SessionBody(
                session = session,
                chronicle = campaign?.chronicle(session.id)?.paragraph,
                chronicling = chronicling,
                chronicleMode = chronicleMode,
                modifier = Modifier.weight(1f),
            )
            if (sessions.size > 1) {
                SessionPager(
                    count = sessions.size,
                    // sessions are newest-first; Session I (oldest) sits at the right end.
                    currentIdx = idx,
                    onEarlier = { if (idx < sessions.lastIndex) sessionIdx = idx + 1 },
                    onLater = { if (idx > 0) sessionIdx = idx - 1 },
                    onJump = { sessionIdx = it },
                )
            }
        }
    }

    if (editing && campaign != null) {
        CampaignSetupScreen(
            create = false,
            initialName = campaign.name,
            initialParty = campaign.party,
            onCancel = { editing = false },
            onRename = onRename,
            onPartyChange = onUpdateParty,
            onLeave = { editing = false; onLeave() },
        )
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
private fun ChronicleToggle(chronicleMode: Boolean, onChange: (Boolean) -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .padding(horizontal = 22.dp)
            .clip(RoundedCornerShape(50))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.33f), RoundedCornerShape(50)),
    ) {
        listOf("Chronicle" to true, "Log" to false).forEach { (label, mode) ->
            val active = chronicleMode == mode
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) palette.accent else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onChange(mode) }
                    .padding(horizontal = 18.dp, vertical = 7.dp),
            ) {
                Text(
                    label.uppercase(),
                    fontFamily = Cinzel,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                    letterSpacing = 2.5.sp,
                    color = if (active) palette.cream else palette.ink,
                )
            }
        }
    }
}

@Composable
private fun SessionBody(
    session: CampaignSession,
    chronicle: String?,
    chronicling: Boolean,
    chronicleMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.natPalette
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 4.dp),
    ) {
        item(key = "header") { SessionHeader(session) }
        if (chronicleMode) {
            item(key = "chronicle") {
                Box(Modifier.padding(top = 12.dp)) {
                    when {
                        chronicle != null -> Text(chronicle, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 17.sp, lineHeight = 24.sp, color = palette.ink)
                        chronicling -> Text("Scribing the chronicle…", fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkMute)
                        else -> DashedNotice("Not yet chronicled.")
                    }
                }
            }
        } else {
            items(session.events, key = { it.id }) { EntryRow(it) }
        }
    }
}

@Composable
private fun SessionHeader(session: CampaignSession) {
    val palette = MaterialTheme.natPalette
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HeaderRule(Modifier.weight(1f), toRight = true)
            Text(
                "SESSION ${roman(session.number)}",
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                color = palette.accent,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            HeaderRule(Modifier.weight(1f), toRight = false)
        }
        Text(session.startedAt.formattedDate(), fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = palette.inkSoft)
    }
}

@Composable
private fun HeaderRule(modifier: Modifier, toRight: Boolean) {
    val accent = MaterialTheme.natPalette.accent.copy(alpha = 0.4f)
    val colors = if (toRight) listOf(androidx.compose.ui.graphics.Color.Transparent, accent) else listOf(accent, androidx.compose.ui.graphics.Color.Transparent)
    Box(modifier.height(1.dp).background(androidx.compose.ui.graphics.Brush.horizontalGradient(colors)))
}

@Composable
private fun EntryRow(entry: LoggedEvent) {
    val palette = MaterialTheme.natPalette
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(entry.timestamp.formattedTime(), fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 1.sp, color = palette.inkMute, modifier = Modifier.width(44.dp))
        Box(Modifier.width(18.dp).padding(top = 5.dp), contentAlignment = Alignment.TopCenter) {
            Diamond(size = 6.dp, fill = palette.accent)
        }
        Text(entry.displaySummary, fontFamily = Cormorant, fontSize = 15.sp, lineHeight = 19.sp, color = palette.ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SessionPager(count: Int, currentIdx: Int, onEarlier: () -> Unit, onLater: () -> Unit, onJump: (Int) -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .background(palette.cream.copy(alpha = 0.9f))
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PagerStep("◀ Earlier", enabled = currentIdx < count - 1, onClick = onEarlier)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            // Session I (oldest = highest index) on the left → walk indices high→low.
            (count - 1 downTo 0).forEach { i ->
                val active = i == currentIdx
                Box(
                    Modifier.padding(horizontal = 4.dp).size(if (active) 8.dp else 5.dp).clip(CircleShape)
                        .background(if (active) palette.accent else androidx.compose.ui.graphics.Color.Transparent)
                        .border(if (active) 0.dp else 1.dp, if (active) androidx.compose.ui.graphics.Color.Transparent else palette.ink.copy(alpha = 0.4f), CircleShape)
                        .clickable { onJump(i) },
                )
            }
        }
        PagerStep("Later ▶", enabled = currentIdx > 0, onClick = onLater)
    }
}

@Composable
private fun PagerStep(label: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Text(
        label.uppercase(),
        fontFamily = Cinzel,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        color = if (enabled) palette.accent else palette.inkMute.copy(alpha = 0.4f),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(4.dp),
    )
}

private fun roman(n: Int): String {
    if (n <= 0) return n.toString()
    val vals = listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
    var rem = n
    val sb = StringBuilder()
    for ((v, s) in vals) while (rem >= v) { sb.append(s); rem -= v }
    return sb.toString()
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withLocale(Locale.getDefault())
private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault())

private fun Instant.formattedTime(): String = atZone(ZoneId.systemDefault()).format(timeFormatter)
private fun Instant.formattedDate(): String = atZone(ZoneId.systemDefault()).format(dateFormatter)

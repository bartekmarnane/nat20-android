package au.com.evonet.nat20.ui.settings

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e.Source
import au.com.evonet.nat20.dnd5e.SourceCatalog
import au.com.evonet.nat20.dnd5e.SourceUsage
import au.com.evonet.nat20.dnd5e.SourceUsageAudit
import au.com.evonet.nat20.dnd5e.effectiveSources
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.EbGaramond
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * Per-character Content Sources editor, pushed from the character's settings
 * page. The post-creation counterpart to the creation wizard's Content Sources
 * disclosure — until this existed, the source set chosen at creation was frozen
 * for the life of the character, so a player who forgot to tick Tasha's had no
 * way in short of rerolling.
 *
 * Enabling a source commits immediately. Turning one off asks first: the set
 * only filters *pickers*, so anything already chosen from that book stays on
 * the sheet and keeps working — it just stops being offered. [SourceUsageAudit]
 * names exactly what falls into that gap so the warning is specific rather than
 * a generic "are you sure".
 *
 * 2014 only — the 2024 ruleset and PF2e ship core content with no supplement
 * gating. Port of the iOS `ContentSourcesView`.
 */
@Composable
fun ContentSourcesScreen(
    characterName: String,
    payload: DnD5ePayload,
    onBack: () -> Unit,
    onCommit: (Set<String>) -> Unit,
) {
    val palette = MaterialTheme.natPalette
    var enabled by remember { mutableStateOf(payload.effectiveSources) }
    var pendingDisable by remember { mutableStateOf<Source?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp, start = 22.dp, end = 22.dp)) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(palette.tileStrong)
                    .border(1.dp, palette.accent, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                Modifier.align(Alignment.Center).widthIn(max = 220.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    characterName.uppercase(),
                    fontFamily = Cinzel,
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                    color = palette.inkMute,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Content Sources",
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = palette.ink,
                )
            }
        }
        OrnamentalDivider(Modifier.padding(horizontal = 22.dp, vertical = 18.dp), opacity = 0.4f)

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Choose which books $characterName draws on. Pickers throughout the " +
                    "sheet — races, subclasses, feats, spells — only offer content from " +
                    "the sources ticked here.",
                fontFamily = EbGaramond,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = palette.inkSoft,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceCatalog.all.forEach { source ->
                    SourceToggleRow(
                        source = source,
                        isOn = source.isLocked || source.id in enabled,
                    ) { turningOn ->
                        if (source.isLocked) return@SourceToggleRow
                        // Enabling is harmless and commits straight away;
                        // turning off routes through the warning first.
                        if (turningOn) {
                            enabled = enabled + source.id
                            onCommit(enabled)
                        } else {
                            pendingDisable = source
                        }
                    }
                }
            }
        }
    }

    pendingDisable?.let { source ->
        AlertDialog(
            onDismissRequest = { pendingDisable = null },
            title = { Text("Turn off ${source.name}?", fontFamily = Cormorant, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    disableWarning(source, characterName, payload),
                    fontFamily = EbGaramond,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    enabled = enabled - source.id
                    pendingDisable = null
                    onCommit(enabled)
                }) { Text("Turn Off", color = palette.danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDisable = null }) { Text("Cancel") }
            },
        )
    }
}

/** Spells alone can run to dozens; past this the list stops informing. */
private const val USAGE_LIST_LIMIT = 8

private fun disableWarning(source: Source, characterName: String, payload: DnD5ePayload): String {
    val usages = SourceUsageAudit.usages(source.id, payload)
    val lines = mutableListOf(
        "${source.name} content stops being offered — its races, subclasses, feats " +
            "and spells drop out of every picker on the sheet.",
    )
    if (usages.isEmpty()) {
        lines += "Nothing $characterName has taken comes from it, so nothing on the sheet changes."
    } else {
        val noun = if (usages.size == 1) "entry" else "entries"
        lines += "$characterName already has ${usages.size} $noun from it:"
        lines += usageList(usages)
        lines += "Those stay on the sheet and keep working — but if you swap one out, " +
            "you won't be able to pick it again until you turn this back on."
    }
    return lines.joinToString("\n\n")
}

private fun usageList(usages: List<SourceUsage>): String {
    val shown = usages.take(USAGE_LIST_LIMIT).map { "· ${it.category} — ${it.name}" }
    val overflow = usages.size - USAGE_LIST_LIMIT
    return (if (overflow > 0) shown + "· and $overflow more" else shown).joinToString("\n")
}

/**
 * One source row — abbreviation capsule, full name, and an on/off seal. Shared
 * by the creation wizard's Content Sources disclosure and this editor so the
 * two surfaces can't drift apart.
 */
@Composable
fun SourceToggleRow(source: Source, isOn: Boolean, onChange: (Boolean) -> Unit) {
    val palette = MaterialTheme.natPalette
    val shape = RoundedCornerShape(4.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.tile)
            .border(
                1.dp,
                if (isOn) palette.accent.copy(alpha = 0.5f) else palette.ink.copy(alpha = 0.12f),
                shape,
            )
            .clickable(enabled = !source.isLocked) { onChange(!isOn) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .widthIn(min = 64.dp)
                .clip(CircleShape)
                .background(if (isOn) palette.accent else palette.tileStrong)
                .border(1.dp, if (isOn) palette.accent else palette.accent.copy(alpha = 0.4f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                source.abbreviation,
                fontFamily = Cinzel,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                color = if (isOn) palette.cream else palette.accent,
            )
        }
        Text(
            source.name,
            fontFamily = Cormorant,
            fontSize = 14.sp,
            color = palette.ink,
            modifier = Modifier.weight(1f),
        )
        // Locked reads as a padlock, enabled as a filled seal; the off state is
        // an empty ring — the bundled icon set has no hollow-circle glyph, so
        // it's drawn rather than imported.
        when {
            source.isLocked -> Icon(
                Icons.Filled.Lock,
                contentDescription = "Always enabled — this is a core source",
                tint = palette.ink.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp),
            )
            isOn -> Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Enabled",
                tint = palette.accent,
                modifier = Modifier.size(20.dp),
            )
            else -> Box(
                Modifier
                    .size(20.dp)
                    .border(1.4.dp, palette.ink.copy(alpha = 0.3f), CircleShape)
                    .semantics { contentDescription = "Disabled" },
            )
        }
    }
}

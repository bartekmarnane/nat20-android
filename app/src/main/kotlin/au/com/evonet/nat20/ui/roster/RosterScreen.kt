package au.com.evonet.nat20.ui.roster

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import au.com.evonet.nat20.dnd5e.DnD5ePayload
import au.com.evonet.nat20.dnd5e2024.DnD5e2024Payload
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.slugToTitle
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.Diamond
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.Rosette
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The character roster — the app's home screen, a faithful port of the iOS
 * `CharacterIndexView`: a centred welcome hero (Cormorant italic title + IM Fell
 * subtitle + rosette flourish) with a gear in the top corner, an ornamental
 * divider, parchment-tile character cards (bordered drop-cap + name/race/class +
 * campaign line + chevron), and a dashed-gold "New Character" CTA. Past the free
 * glance count the CTA tucks inline at the end of the list (matching iOS).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    characters: List<Character>,
    campaignNames: Map<UUID, String>,
    canCreate: Boolean,
    onSelect: (Character) -> Unit,
    onNew: () -> Unit,
    onUpgrade: () -> Unit,
    onDelete: (Character) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Character?>(null) }
    val palette = MaterialTheme.natPalette
    val inlineCta = characters.size > 3

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            // Once the CTA lives inline at the end of the list, drop the pinned bar.
            if (!inlineCta) {
                val hairline = palette.ink.copy(alpha = 0.13f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    palette.cream.copy(alpha = 0.5f),
                                    palette.cream.copy(alpha = 0.98f),
                                    palette.cream.copy(alpha = 0.98f),
                                ),
                            ),
                        )
                        .drawBehind {
                            drawRect(color = hairline, size = Size(size.width, 1.dp.toPx()))
                        }
                        .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 10.dp),
                ) {
                    BottomCta(canCreate, onNew, onUpgrade)
                }
            }
        },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            HeroHeader(characters.size, onOpenSettings)
            OrnamentalDivider(Modifier.padding(horizontal = 22.dp, vertical = 16.dp), opacity = 0.45f)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (characters.isEmpty()) {
                    item { Box(Modifier.padding(top = 40.dp)) { EmptyStateTile() } }
                }
                items(characters, key = { it.id }) { character ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { target ->
                            if (target == SwipeToDismissBoxValue.EndToStart) pendingDelete = character
                            false
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = { DeleteSwipeBackground(dismissState) },
                    ) {
                        CharacterIndexCard(character, campaignNames[character.id], onClick = { onSelect(character) })
                    }
                }
                if (inlineCta) {
                    item {
                        Box(Modifier.padding(top = 6.dp, bottom = 12.dp)) {
                            BottomCta(canCreate, onNew, onUpgrade)
                        }
                    }
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

// ── Hero ────────────────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(count: Int, onOpenSettings: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(Modifier.fillMaxWidth().padding(top = 12.dp, start = 22.dp, end = 22.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (count == 0) "Welcome." else "Welcome back.",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = 34.sp,
                color = palette.accent,
            )
            Text(
                heroSubtitle(count),
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
                color = palette.inkSoft,
                modifier = Modifier.padding(top = 6.dp),
            )
            Box(Modifier.padding(top = 14.dp)) { Rosette() }
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = palette.inkSoft,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun heroSubtitle(count: Int): String = when (count) {
    0 -> "No characters yet. Begin a new story."
    1 -> "One soul under your hand."
    else -> "$count souls under your hand."
}

// ── Empty state ────────────────────────────────────────────────────────────────

/** The blank-roster tile: dashed accent border, matching the iOS `emptyStateTile`. */
@Composable
private fun EmptyStateTile() {
    val palette = MaterialTheme.natPalette
    val accent = palette.accent.copy(alpha = 0.33f)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile.copy(alpha = 0.66f))
            .drawBehind {
                drawRoundRect(
                    color = accent,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(
                        width = 1.4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "The page is blank.",
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontSize = 20.sp,
            color = palette.ink,
        )
        Text(
            "Forge your first character and the codex will fill itself.",
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = palette.inkSoft,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Character card ─────────────────────────────────────────────────────────────

@Composable
private fun CharacterIndexCard(character: Character, campaignName: String?, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.tile)
            .border(BorderStroke(1.2.dp, palette.ink.copy(alpha = 0.13f)), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DropCapSquare(character.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
        Column(Modifier.weight(1f)) {
            Text(
                character.name,
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = 22.sp,
                color = palette.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val race = character.race()
            if (race.isNotEmpty()) {
                Text(race, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = palette.inkSoft, maxLines = 1)
            }
            Text(character.classAndLevel(), fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = palette.inkSoft, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            if (campaignName != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Diamond(size = 4.dp, fill = palette.accent)
                    Text(campaignName, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.ink, maxLines = 1)
                }
            } else {
                Text("No campaign yet", fontFamily = ImFell, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkMute, maxLines = 1)
            }
        }
        // iOS renders a footnote-weight chevron pinned to the row's top edge.
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(18.dp).padding(top = 2.dp),
        )
    }
}

/** The bordered drop-cap initial: a hairline-ringed square with an accent gradient fill. */
@Composable
private fun DropCapSquare(letter: String) {
    val palette = MaterialTheme.natPalette
    Box(Modifier.size(width = 58.dp, height = 70.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 58.dp, height = 70.dp)
                .border(0.8.dp, palette.accent.copy(alpha = 0.53f)),
        )
        Box(
            Modifier
                .size(width = 52.dp, height = 64.dp)
                .background(
                    Brush.linearGradient(listOf(palette.accent.copy(alpha = 0.20f), palette.accent.copy(alpha = 0.067f))),
                )
                .border(1.4.dp, palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter,
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = 34.sp,
                color = palette.accent,
            )
            // Inset cream highlight ring inside the accent border (iOS "inner glow").
            Box(
                Modifier
                    .matchParentSize()
                    .border(2.dp, Color(0xFFFFFADC).copy(alpha = 0.6f)),
            )
        }
    }
}

// ── Bottom CTA ─────────────────────────────────────────────────────────────────

@Composable
private fun BottomCta(canCreate: Boolean, onNew: () -> Unit, onUpgrade: () -> Unit) {
    val palette = MaterialTheme.natPalette
    val title = if (canCreate) "New Character" else "Upgrade"
    val dashColor = palette.accent.copy(alpha = 0.53f)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.linearGradient(listOf(palette.accent.copy(alpha = 0.10f), palette.accent.copy(alpha = 0.06f))),
            )
            .drawBehind {
                // iOS draws the CTA outline dashed (StrokeStyle dash [4,3]).
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            }
            .clickable(onClick = if (canCreate) onNew else onUpgrade)
            .padding(vertical = 14.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(30.dp).clip(androidx.compose.foundation.shape.CircleShape).background(palette.cream.copy(alpha = 0.6f)).border(1.4.dp, palette.accent, androidx.compose.foundation.shape.CircleShape))
            Icon(
                if (canCreate) Icons.Filled.Add else Icons.Filled.Lock,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            title.uppercase(),
            fontFamily = Cinzel,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 3.sp,
            color = palette.accent,
        )
    }
}

@Composable
private fun DeleteSwipeBackground(state: androidx.compose.material3.SwipeToDismissBoxState) {
    // Only paint while the card is actually being swiped left — otherwise the
    // label bleeds through the translucent parchment card at rest.
    if (state.dismissDirection != SwipeToDismissBoxValue.EndToStart) return
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
    }
}

// ── Identity helpers ────────────────────────────────────────────────────────────

private fun Character.race(): String {
    (payload as? DnD5e2024Payload)?.let { return it.species.takeIf { s -> s.isNotEmpty() }?.slugToTitle().orEmpty() }
    return (payload as? DnD5ePayload)?.race?.takeIf { it.isNotEmpty() }?.slugToTitle().orEmpty()
}

private fun Character.classAndLevel(): String {
    (payload as? DnD5e2024Payload)?.let { p ->
        val cls = p.characterClass.takeIf { it.isNotEmpty() }?.slugToTitle()
        return cls?.let { "$it, Level ${p.level}" } ?: "Level ${p.level}"
    }
    (payload as? DnD5ePayload)?.let { p ->
        val cls = p.characterClass.takeIf { it.isNotEmpty() }?.slugToTitle()
        return cls?.let { "$it, Level ${p.level}" } ?: "Level ${p.level}"
    }
    return rulesetId
}

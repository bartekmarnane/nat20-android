package au.com.evonet.nat20.ui.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.domain.Character
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * Per-character admin (parity #40), reached from the codex gear: export the
 * sheet as a PDF and delete the character. Port of the iOS
 * `CharacterSettingsView`. Delete is delegated up so the owner can pop the whole
 * codex stack back to the roster.
 */
@Composable
fun CharacterSettingsScreen(
    character: Character,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    val context = LocalContext.current
    var confirming by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp, start = 22.dp, end = 22.dp)) {
            Box(
                Modifier.align(Alignment.CenterStart).size(38.dp).clip(CircleShape).background(palette.tileStrong).border(1.dp, palette.accent, CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = palette.accent, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CHARACTER", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 3.sp, color = palette.inkMute)
                Text("Settings", fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = palette.ink)
            }
        }
        OrnamentalDivider(Modifier.padding(horizontal = 22.dp, vertical = 18.dp), opacity = 0.4f)

        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(
                icon = Icons.Filled.Share,
                title = "Export as PDF",
                subtitle = "Share a printable character sheet",
                tint = palette.ink,
            ) { CharacterSheetPdf.exportAndShare(context, character) }
            ActionTile(
                icon = Icons.Filled.Delete,
                title = "Delete Character",
                subtitle = "Permanently remove this character",
                tint = palette.danger,
            ) { confirming = true }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Delete ${character.name}?") },
            text = { Text("This permanently removes ${character.name} and every campaign tied to them. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirming = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ActionTile(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(tint.copy(alpha = 0.067f)).border(1.dp, tint.copy(alpha = 0.33f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = tint)
            Text(subtitle, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkSoft)
        }
    }
}

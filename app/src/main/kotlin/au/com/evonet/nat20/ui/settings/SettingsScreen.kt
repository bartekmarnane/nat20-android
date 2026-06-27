package au.com.evonet.nat20.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.chronicle.NarrationStyle
import au.com.evonet.nat20.settings.AppSettings
import au.com.evonet.nat20.settings.AppearanceMode
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.SectionLabel
import au.com.evonet.nat20.ui.theme.SegmentedPills
import au.com.evonet.nat20.ui.theme.SettingsTile
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * Settings (A9) — a faithful port of the iOS `SettingsView`: a centred
 * "Preferences / Settings" header with a Done affordance + ornamental divider,
 * then Narration / Appearance pickers (segmented parchment pills), and Patron /
 * Reference / About tile rows. Appearance + narration apply live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    isPatron: Boolean,
    onOpenPatron: () -> Unit,
    onOpenSpellLibrary: () -> Unit,
    onOpenMonsterCodex2014: () -> Unit,
    onOpenItemCatalog2014: () -> Unit,
    onOpenMonsterCodex: () -> Unit,
    onOpenItemCatalog: () -> Unit,
    onOpenPfMonsterCodex: () -> Unit,
    onOpenPfItemCatalog: () -> Unit,
    onOpenPfActions: () -> Unit,
    onOpenCredits: () -> Unit,
    onBack: () -> Unit,
) {
    val appearance by appSettings.appearance.collectAsState()
    val narration by appSettings.narrationStyle.collectAsState()
    val palette = MaterialTheme.natPalette

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopNav(onDone = onBack)
        OrnamentalDivider(Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 4.dp), opacity = 0.4f)

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 28.dp),
        ) {
            item {
                SectionLabel("Narration", Modifier.padding(top = 6.dp, bottom = 8.dp))
                Text(
                    "The voice the AI chronicler writes session recaps in.",
                    fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 15.sp,
                    color = palette.inkSoft, modifier = Modifier.padding(bottom = 10.dp),
                )
                SegmentedPills(
                    options = NarrationStyle.entries,
                    selected = narration,
                    label = { it.label },
                    onSelect = { appSettings.setNarrationStyle(it) },
                    equalWeight = false,
                )
            }

            item {
                SectionLabel("Appearance", Modifier.padding(top = 18.dp, bottom = 8.dp))
                SegmentedPills(
                    options = AppearanceMode.entries,
                    selected = appearance,
                    label = { it.label() },
                    onSelect = { appSettings.setAppearance(it) },
                    equalWeight = true,
                )
                Text(
                    "Choose a fixed light or dark codex, or follow your device's appearance.",
                    fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 15.sp,
                    color = palette.inkSoft, modifier = Modifier.padding(top = 10.dp),
                )
            }

            item {
                SectionLabel("Patron", Modifier.padding(top = 18.dp, bottom = 8.dp))
                if (isPatron) {
                    PatronBadge()
                } else {
                    SettingsTile("Become a Patron", "Unlock unlimited characters and perks to come", onOpenPatron)
                }
            }

            item {
                SectionLabel("Reference", Modifier.padding(top = 18.dp, bottom = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsTile("Spell Library", "Browse every SRD spell", onOpenSpellLibrary)
                    SettingsTile("Monster Codex (2014)", "Browse the SRD bestiary", onOpenMonsterCodex2014)
                    SettingsTile("Item Catalog (2014)", "Browse SRD weapons, armor, and gear", onOpenItemCatalog2014)
                    SettingsTile("Monster Codex (2024)", "Browse the 2024 SRD bestiary", onOpenMonsterCodex)
                    SettingsTile("Item Catalog (2024)", "Browse 2024 weapons, armor, and masteries", onOpenItemCatalog)
                    SettingsTile("Monster Core (PF2e)", "Browse the ORC bestiary", onOpenPfMonsterCodex)
                    SettingsTile("Item Catalog (PF2e)", "Browse weapons, armor, and shields", onOpenPfItemCatalog)
                    SettingsTile("Actions (PF2e)", "The three-action economy reference", onOpenPfActions)
                }
            }

            item {
                SectionLabel("About", Modifier.padding(top = 18.dp, bottom = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsTile("Credits & Licenses", "SRD 5.1, game data, and typeface attributions", onOpenCredits)
                    SettingsTile("Replay introduction", "See the first-run welcome again", { appSettings.setOnboardingComplete(false); onBack() }, showChevron = false)
                }
            }
        }
    }
}

/** "DONE" on the left, a centred "Preferences / Settings" title block (iOS `topNav`). */
@Composable
private fun TopNav(onDone: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(Modifier.fillMaxWidth().padding(top = 8.dp, start = 22.dp, end = 22.dp)) {
        Text(
            "DONE",
            fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.inkSoft,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onDone)
                .padding(vertical = 10.dp, horizontal = 4.dp),
        )
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PREFERENCES", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 3.sp, color = palette.inkMute)
            Text(
                "Settings",
                fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp, color = palette.ink,
            )
        }
    }
}

@Composable
private fun PatronBadge() {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Nat20 Patron", fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = palette.ink)
            Text("Unlimited characters unlocked — thank you.", fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkSoft)
        }
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = palette.accent)
    }
}

private fun AppearanceMode.label(): String = when (this) {
    AppearanceMode.SYSTEM -> "System"
    AppearanceMode.LIGHT -> "Light"
    AppearanceMode.DARK -> "Dark"
}

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.SectionLabel
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * Credits & Licenses (A9). Carries the same attributions as the iOS `CreditsView`
 * — required verbatim by the open licenses the bundled content + fonts ship
 * under (SRD 5.1/5.2 CC BY 4.0, Pathfinder Monster Core ORC, SIL OFL 1.1). Keep
 * the wording verbatim when adding sources. Reached from Settings → About.
 */
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Header: back circle on the left, centred eyebrow + title.
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
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ATTRIBUTION", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 3.sp, color = palette.inkMute)
                Text(
                    "Credits & Licenses",
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = palette.ink,
                )
            }
        }

        OrnamentalDivider(Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 4.dp), opacity = 0.4f)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 6.dp, bottom = 24.dp),
        ) {
            IntroLine(
                "Nat20 is built on openly-licensed game content and typefaces. " +
                    "The notices below satisfy their attribution terms.",
            )

            SectionLabel("Game Content", Modifier.padding(top = 18.dp, bottom = 8.dp))
            GAME_CONTENT.forEach { CreditCard(it) }

            SectionLabel("Typefaces", Modifier.padding(top = 18.dp, bottom = 8.dp))
            IntroLine("All five display faces are licensed under the SIL Open Font License 1.1.")
            TYPEFACES.forEach { CreditCard(it) }
            Box(Modifier.padding(top = 4.dp)) {
                LicenseLink("SIL Open Font License 1.1", OFL_URL)
            }
        }
    }
}

@Composable
private fun IntroLine(text: String) {
    val palette = MaterialTheme.natPalette
    Text(
        text,
        fontFamily = Cormorant,
        fontStyle = FontStyle.Italic,
        fontSize = 15.sp,
        color = palette.inkSoft,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun CreditCard(entry: CreditEntry) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(entry.title, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = palette.ink)
        entry.attribution?.let {
            Text(it, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkSoft)
        }
        if (entry.links.isNotEmpty()) {
            Column(Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                entry.links.forEach { LicenseLink(it.label, it.url) }
            }
        }
    }
}

@Composable
private fun LicenseLink(label: String, url: String) {
    val palette = MaterialTheme.natPalette
    val uriHandler = LocalUriHandler.current
    Row(
        Modifier.clickable { uriHandler.openUri(url) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("↗", fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = palette.accent)
        Text(label, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = palette.accent)
    }
}

// ── Data (verbatim from iOS CreditsView) ─────────────────────────────────────

private class CreditLink(val label: String, val url: String)
private class CreditEntry(val title: String, val attribution: String?, val links: List<CreditLink> = emptyList())

private const val OFL_URL = "https://openfontlicense.org"
private const val CC_BY_URL = "https://creativecommons.org/licenses/by/4.0/legalcode"
private const val SRD_URL = "https://dnd.wizards.com/resources/systems-reference-document"

private val GAME_CONTENT = listOf(
    CreditEntry(
        "System Reference Document 5.1",
        "This work includes material from the System Reference Document 5.1 (“SRD 5.1”) by " +
            "Wizards of the Coast LLC, available at dnd.wizards.com/resources/systems-reference-document. " +
            "The SRD 5.1 is licensed under the Creative Commons Attribution 4.0 International License.",
        listOf(CreditLink("SRD 5.1", SRD_URL), CreditLink("CC BY 4.0", CC_BY_URL)),
    ),
    CreditEntry(
        "5e-bits / 5e-database",
        "Machine-readable SRD 5.1 data powering the spell, monster, race, class, background, and " +
            "equipment catalogues. Licensed under CC BY 4.0.",
        listOf(CreditLink("github.com/5e-bits/5e-database", "https://github.com/5e-bits/5e-database"), CreditLink("CC BY 4.0", CC_BY_URL)),
    ),
    CreditEntry(
        "System Reference Document 5.2",
        "This work includes material from the System Reference Document 5.2 (“SRD 5.2”) by " +
            "Wizards of the Coast LLC, available at dnd.wizards.com/resources/systems-reference-document. " +
            "The SRD 5.2 is licensed under the Creative Commons Attribution 4.0 International License. " +
            "It supplies the 5th Edition (2024) spell and monster catalogues.",
        listOf(CreditLink("SRD 5.2", SRD_URL), CreditLink("CC BY 4.0", CC_BY_URL)),
    ),
    CreditEntry(
        "Open5e",
        "Machine-readable SRD 5.2 data (the “srd-2024” dataset) used to build the D&D 5e (2024) " +
            "spell and monster catalogues. Licensed under CC BY 4.0.",
        listOf(CreditLink("open5e.com", "https://open5e.com"), CreditLink("CC BY 4.0", CC_BY_URL)),
    ),
    CreditEntry(
        "Pathfinder Monster Core",
        "The Pathfinder 2e (Remaster) Monster Core bestiary is used under the ORC License. Monster Core " +
            "© Paizo Inc.; Pathfinder is a registered trademark of Paizo Inc. This work is not published, " +
            "endorsed, or specifically approved by Paizo. The ORC License is administered by Azora Law on behalf of Paizo.",
        listOf(CreditLink("ORC License", "https://paizo.com/orclicense"), CreditLink("paizo.com", "https://paizo.com")),
    ),
    CreditEntry(
        "Archives of Nethys",
        "Machine-readable Pathfinder 2e (Remaster) Monster Core statblocks used to build the Pathfinder " +
            "bestiary, sourced from Archives of Nethys (the official Pathfinder rules reference).",
        listOf(CreditLink("2e.aonprd.com", "https://2e.aonprd.com")),
    ),
)

private val TYPEFACES = listOf(
    CreditEntry("Cinzel", "Designed by Natanael Gama."),
    CreditEntry("Cormorant Garamond", "Designed by Christian Thalmann (Catharsis Fonts)."),
    CreditEntry("EB Garamond", "Designed by Georg Duffner & Octavio Pardo."),
    CreditEntry("IM Fell English & IM Fell English SC", "Digitised by Igino Marini."),
)

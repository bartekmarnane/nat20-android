package au.com.evonet.nat20.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * The first-run onboarding flow (A12): a four-page parchment walk-through —
 * Welcome / Characters / Campaigns / The Seal — shown once on launch. The
 * Characters and Campaigns pages carry **AI-aware copy** when on-device AI is
 * available. Port of the iOS onboarding (step 25); [onComplete] persists the
 * completion flag and dismisses.
 */
@Composable
fun OnboardingScreen(aiAvailable: Boolean, onComplete: () -> Unit) {
    val pages = onboardingPages(aiAvailable)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    fun advance() {
        if (isLast) onComplete() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            // Skip.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onComplete) {
                    Text("SKIP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
                PageContent(pages[index])
            }

            // Diamond page indicators.
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.indices.forEach { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 9.dp else 5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (active) 1f else 0.3f)),
                    )
                }
            }

            Button(onClick = { advance() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isLast) "Begin" else "Continue")
            }
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        Modifier.fillMaxSize().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RuledLabel(page.label)
        Spacer(Modifier.height(20.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Box(Modifier.height(180.dp), contentAlignment = Alignment.Center) { page.art() }
        Spacer(Modifier.height(28.dp))
        Text(
            page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun RuledLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(width = 28.dp, height = 1.dp).background(MaterialTheme.colorScheme.outline))
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.size(width = 28.dp, height = 1.dp).background(MaterialTheme.colorScheme.outline))
    }
}

// ── Art motifs (Compose primitives — no bundled illustration assets) ──────────

@Composable
private fun SealDisc(diameter: Int = 96) {
    Box(
        Modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text("20", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Black, fontSize = (diameter / 2.4f).sp, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun DieFace() {
    Box(
        Modifier
            .size(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text("d20", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun JournalCard(entries: List<String>) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SESSION II", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            entries.forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SealedSheet() {
    Box(contentAlignment = Alignment.BottomEnd) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sera Vane", style = MaterialTheme.typography.titleMedium)
                Text("Fighter · Level 5", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(28.dp))
            }
        }
        Box(Modifier.padding(end = 16.dp)) { SealDisc(diameter = 64) }
    }
}

// ── Page data (AI-aware) ──────────────────────────────────────────────────────

private data class OnboardingPage(val label: String, val title: String, val body: String, val art: @Composable () -> Unit)

private fun onboardingPages(ai: Boolean): List<OnboardingPage> = listOf(
    OnboardingPage(
        label = "Welcome",
        title = "Every legend starts as a blank page.",
        body = "Nat20 keeps your characters, campaigns, and stories in one codex. A few minutes from now, you'll have a hero.",
        art = { SealDisc() },
    ),
    OnboardingPage(
        label = "Characters",
        title = if (ai) "Forge a hero by hand — or conjure one whole." else "Forge a hero, choice by choice.",
        body = if (ai) {
            "Build by hand, step by step, or let Nat20 conjure a ready-to-play hero in moments. Until a campaign begins, every detail is yours to change."
        } else {
            "Build your hero by hand, choice by choice. Until a campaign begins, every detail is yours to change."
        },
        art = { DieFace() },
    ),
    OnboardingPage(
        label = "Campaigns",
        title = if (ai) "Play on, and the story writes itself." else "Play on, and the journal keeps the tale.",
        body = if (ai) {
            "Start a campaign and your actions become journal entries — damage taken, treasure found, nights survived. Entry by entry, they bind into a storied chronicle of the whole adventure."
        } else {
            "Start a campaign and your actions become journal entries — damage taken, treasure found, nights survived — kept in order, session by session, for the whole adventure."
        },
        art = {
            JournalCard(
                if (ai) {
                    listOf(
                        "A goblin's blade nicked Sparks' arm — a scratch that wounded his pride far more than his flesh.",
                        "With a flick of the wrist he loosed a bolt of fire, and the goblin was unmade in ash.",
                    )
                } else {
                    listOf("Took 4 slashing damage — goblin.", "Cast Fire Bolt — goblin defeated.")
                },
            )
        },
    ),
    OnboardingPage(
        label = "The Seal",
        title = "When the adventure begins, the ink dries.",
        body = "Once your character enters a campaign, the sheet is sealed — no edits until the campaign ends. Change happens the honest way: through play.",
        art = { SealedSheet() },
    ),
)

package au.com.evonet.nat20.ui.patron

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.patron.PatronStore
import au.com.evonet.nat20.ui.theme.Cinzel
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.Nat20MarkD20Iso
import au.com.evonet.nat20.ui.theme.OrnamentalDivider
import au.com.evonet.nat20.ui.theme.natPalette

/**
 * The Patron paywall (A14). A single, one-time purchase that lifts the
 * one-character free limit and folds in future cross-table perks. A faithful
 * port of the iOS `PatronView`: CLOSE/UNLOCK top nav, ornamental divider,
 * d20 crest, parchment perk tiles, and the accent pledge button + restore
 * footer. Reached from the roster's "Upgrade" CTA and from Settings.
 *
 * If the unlock lands while open (purchase, restore, or a pending approval that
 * clears), it auto-dismisses — there's nothing left to offer.
 */
@Composable
fun PatronScreen(patron: PatronStore, onBack: () -> Unit) {
    val isPatron by patron.isPatron.collectAsState()
    val product by patron.product.collectAsState()
    val inFlight by patron.purchaseInFlight.collectAsState()
    val error by patron.lastError.collectAsState()
    val activity = LocalContext.current.findActivity()
    val palette = MaterialTheme.natPalette

    // If the unlock lands while open (purchase, restore, or a pending approval
    // that clears), dismiss — the paywall has nothing left to offer.
    LaunchedEffect(isPatron) {
        if (isPatron) onBack()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopNav(onClose = onBack)
        OrnamentalDivider(Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 4.dp), opacity = 0.4f)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Crest()

            Column(Modifier.fillMaxWidth().padding(top = 26.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PerkTile(
                    title = "Unlimited characters",
                    blurb = "Keep a whole party — every hero, henchman, and what-if build in one codex.",
                )
                // Only live perks are listed — App-review parity with iOS: don't
                // pitch the purchase on a not-yet-shipped feature (the shared
                // journal unlocks from the same isPatron gate when it lands).
            }

            error?.let {
                Text(
                    it,
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = palette.danger,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                )
            }
        }

        Footer(
            inFlight = inFlight,
            canBuy = !inFlight && product != null && activity != null,
            hasProduct = product != null,
            buyLabel = buyLabel(inFlight, patron.priceText),
            onBuy = { activity?.let { patron.purchase(it) } },
            onRestore = { patron.restore() },
        )
    }
}

// ── Chrome ────────────────────────────────────────────────────────────────────

/** "CLOSE" on the left, a centred "UNLOCK" eyebrow (iOS `topNav`). */
@Composable
private fun TopNav(onClose: () -> Unit) {
    val palette = MaterialTheme.natPalette
    Box(Modifier.fillMaxWidth().padding(top = 8.dp, start = 22.dp, end = 22.dp)) {
        Text(
            "CLOSE",
            fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 2.5.sp, color = palette.inkSoft,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClose)
                .padding(vertical = 10.dp, horizontal = 4.dp),
        )
        Text(
            "UNLOCK",
            fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 3.sp, color = palette.inkMute,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

// ── Crest ─────────────────────────────────────────────────────────────────────

@Composable
private fun Crest() {
    val palette = MaterialTheme.natPalette
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Nat20MarkD20Iso(size = 76.dp, fg = palette.accent)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("PATRON", fontFamily = Cinzel, fontSize = 11.sp, letterSpacing = 4.sp, color = palette.inkMute)
            Text(
                "Become a Patron",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                color = palette.accent,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            "One pledge unlocks your full roster.",
            fontFamily = ImFell,
            fontStyle = FontStyle.Italic,
            fontSize = 15.sp,
            color = palette.inkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

// ── Perks ─────────────────────────────────────────────────────────────────────

@Composable
private fun PerkTile(title: String, blurb: String) {
    val palette = MaterialTheme.natPalette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.tile)
            .border(1.dp, palette.ink.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(26.dp).padding(top = 2.dp), contentAlignment = Alignment.Center) {
            PartyGlyph()
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = palette.ink)
            Text(blurb, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = palette.inkSoft)
        }
    }
}

/** Three filled silhouettes in a row (stands in for the iOS `person.3.fill` glyph). */
@Composable
private fun PartyGlyph() {
    val accent = MaterialTheme.natPalette.accent
    Canvas(Modifier.size(width = 26.dp, height = 18.dp)) {
        val h = size.height
        // Flanking figures slightly smaller and behind; centre figure in front.
        listOf(
            Triple(size.width * 0.18f, 0.82f, 0.55f),
            Triple(size.width * 0.82f, 0.82f, 0.55f),
            Triple(size.width * 0.5f, 1f, 1f),
        ).forEach { (cx, scale, _) ->
            val headR = h * 0.19f * scale
            val headCy = h * 0.28f
            drawCircle(color = accent, radius = headR, center = Offset(cx, headCy))
            // Shoulders: a filled arc-ish dome under the head.
            drawArc(
                color = accent,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - headR * 1.8f, headCy + headR * 1.2f),
                size = androidx.compose.ui.geometry.Size(headR * 3.6f, headR * 3.2f),
            )
        }
    }
}

// ── Footer (buy + restore) ────────────────────────────────────────────────────

@Composable
private fun Footer(
    inFlight: Boolean,
    canBuy: Boolean,
    hasProduct: Boolean,
    buyLabel: String,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer(alpha = if (hasProduct) 1f else 0.5f)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.accent)
                .border(1.dp, palette.accentDeep, RoundedCornerShape(8.dp))
                .clickable(enabled = canBuy, onClick = onBuy)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (inFlight) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = palette.cream,
                )
                Spacer(Modifier.size(10.dp))
            }
            Text(
                buyLabel.uppercase(),
                fontFamily = Cinzel,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.5.sp,
                color = palette.cream,
                maxLines = 1,
            )
        }

        Text(
            "RESTORE PURCHASE",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = palette.inkSoft,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(enabled = !inFlight, onClick = onRestore)
                .padding(vertical = 6.dp, horizontal = 8.dp),
        )

        Text(
            "A one-time purchase. Restores free on every device signed in to your Google account.",
            fontFamily = Cormorant,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp,
            color = palette.inkMute,
            textAlign = TextAlign.Center,
        )
    }
}

private fun buyLabel(inFlight: Boolean, price: String?): String = when {
    inFlight -> "Pledging…"
    price != null -> "Become a Patron · $price"
    else -> "Become a Patron"
}

/** Walk the context-wrapper chain to the hosting Activity (Play's billing flow
 *  needs one to present its sheet). */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

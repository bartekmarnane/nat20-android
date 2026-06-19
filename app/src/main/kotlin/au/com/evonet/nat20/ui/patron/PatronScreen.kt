package au.com.evonet.nat20.ui.patron

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.evonet.nat20.patron.PatronStore

/**
 * The Patron paywall (A14). A single, one-time purchase that lifts the
 * one-character free limit and folds in future cross-table perks. Port of the
 * iOS `PatronView`. Reached from the roster's "Upgrade" CTA and from Settings.
 *
 * If the unlock lands while open (purchase, restore, or a pending approval that
 * clears), it auto-dismisses — there's nothing left to offer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatronScreen(patron: PatronStore, onBack: () -> Unit) {
    val isPatron by patron.isPatron.collectAsState()
    val product by patron.product.collectAsState()
    val inFlight by patron.purchaseInFlight.collectAsState()
    val error by patron.lastError.collectAsState()
    val activity = LocalContext.current.findActivity()

    // If the unlock lands while open (purchase, restore, or a pending approval
    // that clears), dismiss — the paywall has nothing left to offer.
    LaunchedEffect(isPatron) {
        if (isPatron) onBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Become a Patron") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Close") } },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "One pledge unlocks your full roster.",
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            // Only live perks are listed — App-review parity with iOS: don't
            // pitch the purchase on a not-yet-shipped feature (the shared
            // journal lands with A23 and unlocks from the same isPatron gate).
            PerkCard(
                title = "Unlimited characters",
                blurb = "Keep a whole party — every hero, henchman, and what-if build in one codex.",
            )

            error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { activity?.let { patron.purchase(it) } },
                enabled = !inFlight && product != null && activity != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (inFlight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.size(10.dp))
                }
                Text(buyLabel(inFlight, patron.priceText))
            }

            TextButton(
                onClick = { patron.restore() },
                enabled = !inFlight,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restore Purchase")
            }

            Text(
                "A one-time purchase. Restores free on every device signed in to your Google account.",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PerkCard(title: String, blurb: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    blurb,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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

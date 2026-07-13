package au.com.evonet.nat20.ui.identity

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.evonet.nat20.ui.actions.ActionPickerShell
import au.com.evonet.nat20.ui.editor.WizardFieldLabel
import au.com.evonet.nat20.ui.editor.WizardPrimaryButton
import au.com.evonet.nat20.ui.editor.WizardTextField
import au.com.evonet.nat20.ui.theme.Cormorant
import au.com.evonet.nat20.ui.theme.DropCapSquare
import au.com.evonet.nat20.ui.theme.ImFell
import au.com.evonet.nat20.ui.theme.natPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Lightweight in-campaign editor for the two roleplay-metadata fields a player
 * can sensibly change mid-game — their character's name and portrait (parity
 * #23, port of iOS `IdentitySheet`). Mechanical state (level, race, class,
 * stats) stays in the full builder editor and only makes sense while building.
 *
 * Chrome is the shared [ActionPickerShell]; the footer's single Save button
 * lights up only once the name is non-empty and something actually changed.
 */
@Composable
fun IdentitySheet(
    initialName: String,
    initialPortrait: ByteArray?,
    onCancel: () -> Unit,
    onCommit: (name: String, portraitData: ByteArray?) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var portrait by remember { mutableStateOf(initialPortrait) }

    val trimmed = name.trim()
    val nameChanged = trimmed != initialName.trim()
    val portraitChanged = !portrait.contentEquals(initialPortrait)
    val canCommit = trimmed.isNotEmpty() && (nameChanged || portraitChanged)

    ActionPickerShell(
        kicker = "Identity",
        title = "Edit identity",
        onCancel = onCancel,
        footer = {
            WizardPrimaryButton("Save", enabled = canCommit) { onCommit(trimmed, portrait) }
        },
    ) {
        PortraitPickerControl(
            portraitData = portrait,
            onChange = { portrait = it },
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(16.dp))
        WizardFieldLabel("Name", required = true)
        WizardTextField("Their name…", value = name, onValueChange = { name = it })
    }
}

// ── Portrait picker ─────────────────────────────────────────────────────────────

/**
 * Parchment-styled portrait tile that opens the photo picker on tap. The chosen
 * image is decoded through a bounded sampler and downscaled to a 512px JPEG
 * before being stored so persisted BLOBs stay small (Kotlin analogue of iOS
 * `PortraitPickerControl.downscaledJPEG`). Shared by the identity sheet; the
 * creation wizards can adopt it later.
 */
@Composable
fun PortraitPickerControl(
    portraitData: ByteArray?,
    onChange: (ByteArray?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.natPalette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val image = remember(portraitData) { portraitData?.decodeToImageBitmap() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    downscaledJpeg(context.contentResolver, uri)
                }
                if (bytes != null) onChange(bytes)
            }
        }
    }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        PortraitTileFrame(
            modifier = Modifier.clickable {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        ) {
            if (image != null) {
                Image(image, contentDescription = "Portrait", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            } else {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                listOf(palette.accent.copy(alpha = 0.20f), palette.accent.copy(alpha = 0.067f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (portraitData == null) "Add a portrait" else "Portrait chosen",
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = palette.ink,
            )
            Text(
                "Optional — a photo, an illustration, anything that captures their face.",
                fontFamily = ImFell,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = palette.inkMute,
            )
            if (portraitData != null) {
                Text(
                    "Remove",
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = palette.danger,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onChange(null) }
                        .padding(vertical = 2.dp, horizontal = 2.dp),
                )
            }
        }
    }
}

// ── Shared portrait frame ───────────────────────────────────────────────────────

/**
 * The 58×70 portrait chrome for the hero row and roster cards: renders the
 * stored portrait when present, otherwise falls back to the bordered
 * [DropCapSquare] initial (parity #2's deferred portrait-in-card, resolved here).
 */
@Composable
fun CharacterPortraitFrame(portraitData: ByteArray?, fallbackLetter: String) {
    val image = remember(portraitData) { portraitData?.decodeToImageBitmap() }
    if (image != null) {
        PortraitTileFrame {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
    } else {
        DropCapSquare(fallbackLetter)
    }
}

/**
 * Shared double-stroked 58×70 tile: hairline outer rect, a clipped 52×64 inner
 * well carrying [content], then the accent frame + cream inset glow overlaid on
 * top (so a filled image doesn't paint over them). Mirrors [DropCapSquare].
 */
@Composable
private fun PortraitTileFrame(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val palette = MaterialTheme.natPalette
    Box(modifier.size(width = 58.dp, height = 70.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 58.dp, height = 70.dp)
                .border(0.8.dp, palette.accent.copy(alpha = 0.53f)),
        )
        Box(
            Modifier
                .size(width = 52.dp, height = 64.dp)
                .clip(RectangleShape),
            contentAlignment = Alignment.Center,
        ) {
            content()
            Box(Modifier.matchParentSize().border(1.4.dp, palette.accent))
            Box(Modifier.matchParentSize().border(2.dp, Color(0xFFFFFADC).copy(alpha = 0.6f)))
        }
    }
}

// ── Bitmap helpers ──────────────────────────────────────────────────────────────

/** Decode stored JPEG bytes to a Compose [ImageBitmap] (null if undecodable). */
internal fun ByteArray.decodeToImageBitmap(): ImageBitmap? =
    BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap()

/**
 * Decode the picked image with a bounded sampler, downscale its longest edge to
 * [maxDimension], and re-encode as JPEG at [quality]. Kotlin analogue of iOS's
 * `downscaledJPEG` — keeps persisted portrait BLOBs small.
 */
private fun downscaledJpeg(
    resolver: ContentResolver,
    uri: Uri,
    maxDimension: Int = 512,
    quality: Int = 80,
): ByteArray? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val srcW = bounds.outWidth
    val srcH = bounds.outHeight
    if (srcW <= 0 || srcH <= 0) return null

    // Coarse power-of-two subsample so the full-res bitmap never lands in memory.
    var sample = 1
    while (srcW / (sample * 2) >= maxDimension && srcH / (sample * 2) >= maxDimension) {
        sample *= 2
    }
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOpts)
    } ?: return null

    // Fine downscale to the exact cap.
    val longest = maxOf(decoded.width, decoded.height)
    val scaled = if (longest > maxDimension) {
        val ratio = maxDimension.toFloat() / longest
        Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * ratio).toInt().coerceAtLeast(1),
            (decoded.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        decoded
    }

    return ByteArrayOutputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.toByteArray()
    }
}

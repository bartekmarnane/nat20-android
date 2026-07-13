package au.com.evonet.nat20.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import au.com.evonet.nat20.R

/**
 * Brand marks + splash, ported from iOS `Styling/Brand.swift`.
 */

/**
 * Isometric d20 silhouette (iOS `Nat20MarkD20Iso`): outer hexagon filled in
 * [fg] with the internal facet lines cut out as transparent slits so the
 * parchment shows through.
 */
@Composable
fun Nat20MarkD20Iso(size: Dp, fg: Color) {
    Canvas(Modifier.size(size)) {
        val s = size.toPx() / 100f
        fun pt(x: Float, y: Float) = Offset(x * s, y * s)
        val t = pt(50f, 5f)
        val ur = pt(90f, 28f)
        val lr = pt(90f, 72f)
        val b = pt(50f, 95f)
        val ll = pt(10f, 72f)
        val ul = pt(10f, 28f)
        val p1 = pt(28f, 31f)
        val p2 = pt(72f, 31f)
        val q = pt(50f, 70f)
        val gap = 2.4f * s

        val outline = Path().apply {
            moveTo(t.x, t.y)
            lineTo(ur.x, ur.y)
            lineTo(lr.x, lr.y)
            lineTo(b.x, b.y)
            lineTo(ll.x, ll.y)
            lineTo(ul.x, ul.y)
            close()
        }
        val segments = listOf(
            t to p1, t to p2, p1 to p2,
            p1 to ul, p2 to ur,
            p1 to q, p2 to q,
            p1 to ll, p2 to lr,
            q to ll, q to lr, q to b,
        )

        // Fill the silhouette in an offscreen layer, then knock the facet
        // lines out of it (destination-out) so the slits are transparent.
        drawIntoCanvas { canvas ->
            canvas.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, this.size), Paint())
            drawPath(outline, color = fg)
            segments.forEach { (a, bEnd) ->
                drawLine(
                    color = Color.Black,
                    start = a,
                    end = bEnd,
                    strokeWidth = gap,
                    cap = StrokeCap.Round,
                    blendMode = BlendMode.DstOut,
                )
            }
            canvas.restore()
        }
    }
}

/** "NAT20" in tracked Cinzel small-caps (iOS `Nat20Wordmark`), optionally flanked by diamonds. */
@Composable
fun Nat20Wordmark(size: TextUnit, color: Color, withDiamonds: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(size.value.dp * 0.45f),
    ) {
        if (withDiamonds) Diamond(size = size.value.dp * 0.32f, fill = color)
        Text(
            "NAT20",
            fontFamily = Cinzel,
            fontWeight = FontWeight.Bold,
            fontSize = size,
            letterSpacing = size * 0.22f,
            color = color,
        )
        if (withDiamonds) Diamond(size = size.value.dp * 0.32f, fill = color)
    }
}

/** Stacked mark-above-wordmark composition with optional italic tagline (iOS `Nat20Lockup`). */
@Composable
fun Nat20Lockup(size: Dp, fg: Color, tagline: String? = null, taglineColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(size * 0.18f),
    ) {
        Nat20MarkD20Iso(size = size, fg = fg)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Nat20Wordmark(size = (size.value * 0.30f).sp, color = fg, withDiamonds = false)
            if (tagline != null) {
                Text(
                    tagline,
                    fontFamily = ImFell,
                    fontStyle = FontStyle.Italic,
                    fontSize = (size.value * 0.14f).sp,
                    letterSpacing = 0.5.sp,
                    color = taglineColor,
                )
            }
        }
    }
}

/**
 * Parchment splash (iOS `Nat20SplashView`): its own opaque parchment ground
 * (texture + fox-marks in light, candle-glow in dark), the lockup centred,
 * and a tiny Cinzel inscription pinned to the bottom. Overlaid above the app
 * for ~1.2s at launch, then faded out.
 */
@Composable
fun SplashView(darkTheme: Boolean) {
    val palette = LocalNatPalette.current
    Box(Modifier.fillMaxSize().background(palette.parchment)) {
        if (darkTheme) {
            // Candle-glow rising from the lower centre (iOS: centre 0.5/0.62, radius 0.9w).
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(this.size.width * 0.5f, this.size.height * 0.62f)
                val r = this.size.width * 0.9f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.accent.copy(alpha = 0.12f), Color.Transparent),
                        center = c,
                        radius = r,
                    ),
                    radius = r,
                    center = c,
                )
            }
        } else {
            Image(
                painter = painterResource(R.drawable.parchment_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.92f,
                modifier = Modifier.fillMaxSize(),
            )
            // Soft fox-marks for warmth (multiply-blended radial stains).
            Canvas(Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF46280F).copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(w * 0.25f, h * 0.20f),
                        radius = w * 0.6f,
                    ),
                    radius = w * 0.6f,
                    center = Offset(w * 0.25f, h * 0.20f),
                    blendMode = BlendMode.Multiply,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF6E3C14).copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(w * 0.75f, h * 0.75f),
                        radius = w * 0.6f,
                    ),
                    radius = w * 0.6f,
                    center = Offset(w * 0.75f, h * 0.75f),
                    blendMode = BlendMode.Multiply,
                )
            }
        }

        Box(Modifier.align(Alignment.Center)) {
            Nat20Lockup(
                size = 140.dp,
                fg = palette.accent,
                tagline = "Your codex of heroes.",
                taglineColor = palette.inkSoft,
            )
        }

        Text(
            "NAT · TWENTY",
            fontFamily = Cinzel,
            fontSize = 11.sp,
            letterSpacing = 4.sp,
            color = palette.inkMute,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 54.dp),
        )
    }
}

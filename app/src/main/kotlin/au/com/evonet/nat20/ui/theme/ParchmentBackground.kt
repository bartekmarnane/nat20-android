package au.com.evonet.nat20.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import au.com.evonet.nat20.R

/**
 * App-wide parchment ground (iOS `ParchmentBackground`). In light mode the
 * `parchment_background` texture sits on the parchment base fill; in dark mode
 * the texture can't sit on near-black, so it's dropped for a faint candle-glow
 * rising from the lower centre. Apply at each root container (and at the root of
 * sheets) behind scroll content.
 *
 * Implemented as a composable wrapper rather than a pure `Modifier` because the
 * light texture is an `Image` painter; call it as the outermost box of a screen
 * and put the content inside.
 */
@Composable
fun ParchmentSurface(
    modifier: Modifier = Modifier,
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val palette = LocalNatPalette.current
    androidx.compose.foundation.layout.Box(modifier.fillMaxSize()) {
        if (darkTheme) {
            // Candle glow rising from the lower centre over the near-black ground.
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxSize()
                    .background(palette.parchment)
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(palette.accent.copy(alpha = 0.10f), Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height * 0.92f),
                                radius = size.maxDimension * 0.6f,
                            ),
                        )
                    },
            )
        } else {
            Image(
                painter = painterResource(R.drawable.parchment_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        content()
    }
}

/**
 * A translucent parchment tile fill + gold hairline, the chrome iOS uses for its
 * codex section cards. Use as the container modifier for a card so it reads as
 * inked parchment rather than an elevated Material surface.
 */
@Composable
fun Modifier.parchmentTile(strong: Boolean = false): Modifier {
    val palette = LocalNatPalette.current
    return background(if (strong) palette.tileStrong else palette.tile)
}

package au.com.evonet.nat20.ui.theme

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * The host activity's system-bar insets, provided at the root of `MainActivity`.
 * Compose reports zero insets inside a dialog window on the devices we tested,
 * so full-screen dialogs pad from these instead.
 */
val LocalHostInsets = staticCompositionLocalOf<WindowInsets> { WindowInsets(0, 0, 0, 0) }

/**
 * A `Dialog` that really is full-screen and edge-to-edge.
 *
 * `usePlatformDefaultWidth = false` alone leaves the window `WRAP_CONTENT` in
 * height and laid out below the status bar, so a `fillMaxSize` body measured
 * to the display height overflowed the bottom by exactly one status bar — the
 * wizard / picker footers sat under the gesture bar. Sizing the window
 * `MATCH_PARENT` both ways makes it cover the screen, and
 * `decorFitsSystemWindows = false` lets the body's `statusBarsPadding()` /
 * `navigationBarsPadding()` do the inset work.
 */
@Composable
fun FullScreenDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.apply {
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                // The dialog window otherwise still "fits" the status bar (laid out
                // below it) while its content is measured at display height, so
                // the bottom overflowed by one status bar. Stop fitting any inset:
                // the window then spans the screen and the body's paddings apply.
                setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    attributes = attributes.also { it.fitInsetsTypes = 0; it.fitInsetsSides = 0 }
                }
                WindowCompat.setDecorFitsSystemWindows(this, false)
            }
        }
        // Pad from the host's insets and consume them, so the body's own
        // statusBarsPadding / navigationBarsPadding (shared with non-dialog
        // screens) don't add a second band where a device does report them.
        val host = LocalHostInsets.current
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.natPalette.parchment)
                .windowInsetsPadding(host)
                .consumeWindowInsets(host),
        ) {
            content()
        }
    }
}

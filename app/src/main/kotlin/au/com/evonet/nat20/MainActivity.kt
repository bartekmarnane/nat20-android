package au.com.evonet.nat20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import au.com.evonet.nat20.app.Nat20Application
import au.com.evonet.nat20.settings.AppearanceMode
import au.com.evonet.nat20.ui.NatApp
import au.com.evonet.nat20.ui.onboarding.OnboardingScreen
import au.com.evonet.nat20.ui.theme.Nat20Theme
import au.com.evonet.nat20.ui.theme.ParchmentSurface
import au.com.evonet.nat20.ui.theme.SplashView
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as Nat20Application).container
        val appSettings = container.appSettings
        setContent {
            val appearance by appSettings.appearance.collectAsState()
            val dark = when (appearance) {
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
            }
            val onboardingComplete by appSettings.onboardingComplete.collectAsState()
            Nat20Theme(darkTheme = dark) {
                // The parchment ground (texture in light, candle-glow in dark) sits behind
                // everything; screens that paint a transparent Scaffold let it show through.
                ParchmentSurface(darkTheme = dark) {
                    // First run shows onboarding once; AI-aware copy reflects on-device AI
                    // availability. In debug builds the "Always Show Onboarding" developer
                    // toggle replays it every launch (iOS `alwaysShowOnboarding`) — finishing
                    // or skipping still dismisses it for the rest of this launch.
                    val alwaysShowOnboarding by appSettings.alwaysShowOnboarding.collectAsState()
                    var onboardingDismissed by remember { mutableStateOf(false) }
                    val showOnboarding = !onboardingDismissed &&
                        ((BuildConfig.DEBUG && alwaysShowOnboarding) || !onboardingComplete)
                    Crossfade(targetState = showOnboarding, label = "onboardingGate") { show ->
                        if (!show) {
                            NatApp()
                        } else {
                            OnboardingScreen(
                                aiAvailable = container.chronicleService.isAvailable,
                                onComplete = {
                                    appSettings.setOnboardingComplete(true)
                                    onboardingDismissed = true
                                },
                            )
                        }
                    }

                    // Brand splash over everything for ~1.2s (iOS `Nat20SplashView`),
                    // with the roster/onboarding ready behind the fade.
                    var splashVisible by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        delay(1200)
                        splashVisible = false
                    }
                    AnimatedVisibility(
                        visible = splashVisible,
                        enter = EnterTransition.None,
                        exit = fadeOut(animationSpec = tween(durationMillis = 400, easing = EaseOut)),
                    ) {
                        SplashView(darkTheme = dark)
                    }
                }
            }
        }
    }
}

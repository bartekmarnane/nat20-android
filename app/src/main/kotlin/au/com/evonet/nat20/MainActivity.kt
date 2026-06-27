package au.com.evonet.nat20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import au.com.evonet.nat20.app.Nat20Application
import au.com.evonet.nat20.settings.AppearanceMode
import au.com.evonet.nat20.ui.NatApp
import au.com.evonet.nat20.ui.onboarding.OnboardingScreen
import au.com.evonet.nat20.ui.theme.Nat20Theme
import au.com.evonet.nat20.ui.theme.ParchmentSurface

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
                    // First run shows onboarding once; AI-aware copy reflects on-device AI availability.
                    Crossfade(targetState = onboardingComplete, label = "onboardingGate") { complete ->
                        if (complete) {
                            NatApp()
                        } else {
                            OnboardingScreen(
                                aiAvailable = container.chronicleService.isAvailable,
                                onComplete = { appSettings.setOnboardingComplete(true) },
                            )
                        }
                    }
                }
            }
        }
    }
}

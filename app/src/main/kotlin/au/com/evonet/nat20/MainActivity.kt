package au.com.evonet.nat20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import au.com.evonet.nat20.app.Nat20Application
import au.com.evonet.nat20.settings.AppearanceMode
import au.com.evonet.nat20.ui.NatApp
import au.com.evonet.nat20.ui.theme.Nat20Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appSettings = (application as Nat20Application).container.appSettings
        setContent {
            val appearance by appSettings.appearance.collectAsState()
            val dark = when (appearance) {
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
            }
            Nat20Theme(darkTheme = dark) {
                NatApp()
            }
        }
    }
}

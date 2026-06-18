package au.com.evonet.nat20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import au.com.evonet.nat20.ui.NatApp
import au.com.evonet.nat20.ui.theme.Nat20Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Nat20Theme {
                NatApp()
            }
        }
    }
}

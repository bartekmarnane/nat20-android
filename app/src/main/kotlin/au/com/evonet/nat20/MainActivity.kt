package au.com.evonet.nat20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Welcome(modifier = Modifier.padding(innerPadding).padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun Welcome(modifier: Modifier = Modifier) {
    Text(text = "Nat20 — roll for initiative.", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    MaterialTheme {
        Welcome()
    }
}

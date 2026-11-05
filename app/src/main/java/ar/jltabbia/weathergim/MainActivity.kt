package ar.jltabbia.weathergim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ar.jltabbia.weathergim.ui.screen.WeatherScreen
import ar.jltabbia.weathergim.ui.theme.WeatherGimTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherGimTheme {
                WeatherScreen()
            }
        }
    }
}

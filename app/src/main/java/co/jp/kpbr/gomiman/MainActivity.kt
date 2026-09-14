package co.jp.kpbr.gomiman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import co.jp.kpbr.gomiman.ui.screens.main.MainScreen
import co.jp.kpbr.gomiman.ui.theme.GomimanTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GomimanApp

        setContent {
            GomimanTheme {
                MainScreen(
                    garbageRepository = app.garbageRepository,
                    calendarRepository = app.calendarRepository,
                    preferencesManager = app.preferencesManager,
                    syncRepository = app.syncRepository
                )
            }
        }
    }
}

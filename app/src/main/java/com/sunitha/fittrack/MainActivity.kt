package com.sunitha.fittrack

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.sunitha.fittrack.data.datastore.ThemeMode
import com.sunitha.fittrack.notifications.NotificationHelper
import com.sunitha.fittrack.ui.navigation.AppNavigation
import com.sunitha.fittrack.ui.theme.FitTrackTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as FitTrackApp
            val themeMode by app.themeStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
            }
            FitTrackTheme(darkTheme = darkTheme) {
                AppNavigation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Clears any pending reminder notification regardless of how the app was
        // opened (notification tap, app drawer, recents) — not just the tap path,
        // which is the only case setAutoCancel(true) covers on its own.
        NotificationHelper.cancelAllReminders(this)
    }
}

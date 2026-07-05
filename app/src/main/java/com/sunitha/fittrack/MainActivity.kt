package com.sunitha.fittrack

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.sunitha.fittrack.ui.navigation.AppNavigation
import com.sunitha.fittrack.ui.theme.FitTrackTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitTrackTheme {
                AppNavigation()
            }
        }
    }
}

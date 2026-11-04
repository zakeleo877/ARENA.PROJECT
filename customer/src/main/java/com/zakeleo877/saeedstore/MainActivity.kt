package com.zakeleo877.saeedstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zakeleo877.saeedcore.designsystem.SaeedTheme
import com.zakeleo877.saeedstore.ui.CustomerApp
import dagger.hilt.android.AndroidEntryPoint

/** Customer-facing entry point: splash screen, Neon Void theme and the RTL catalog. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splashScreen.setKeepOnScreenCondition { false }
        setContent {
            SaeedTheme {
                CustomerApp()
            }
        }
    }
}

package com.zakeleo877.saeedadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zakeleo877.saeedcore.designsystem.SaeedTheme
import com.zakeleo877.saeedadmin.ui.AdminApp
import dagger.hilt.android.AndroidEntryPoint

/** Admin entry point: splash screen, Neon Void theme and the management shell. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splashScreen.setKeepOnScreenCondition { false }
        setContent {
            SaeedTheme {
                AdminApp()
            }
        }
    }
}

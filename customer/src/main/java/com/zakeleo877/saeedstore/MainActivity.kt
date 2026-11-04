package com.zakeleo877.saeedstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zakeleo877.saeedcore.designsystem.SaeedTheme

/** Temporary scaffold only; real screens are implemented in later phases. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SaeedTheme {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.scaffold_message))
                }
            }
        }
    }
}

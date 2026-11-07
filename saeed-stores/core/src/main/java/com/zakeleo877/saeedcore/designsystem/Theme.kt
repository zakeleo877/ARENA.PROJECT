package com.zakeleo877.saeedcore.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Fixed dark palette; it intentionally does not follow dynamic device colors. */
@Composable
fun SaeedTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalSaeedSpacing provides SaeedSpacing(),
    ) {
        MaterialTheme(
            colorScheme = NeonVoidColorScheme,
            typography = SaeedTypography,
            shapes = SaeedShapes,
            content = content,
        )
    }
}

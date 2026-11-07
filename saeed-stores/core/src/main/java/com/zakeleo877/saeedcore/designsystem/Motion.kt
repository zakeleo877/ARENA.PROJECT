package com.zakeleo877.saeedcore.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object SaeedMotion {
    val press = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val transition = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
    )
}

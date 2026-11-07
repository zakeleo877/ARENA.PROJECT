package com.zakeleo877.saeedcore.designsystem

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object Haptics {
    fun tap(feedback: HapticFeedback) {
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun confirm(feedback: HapticFeedback) {
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

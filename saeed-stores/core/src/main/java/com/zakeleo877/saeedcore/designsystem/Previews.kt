package com.zakeleo877.saeedcore.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.zakeleo877.saeedcore.R
import com.zakeleo877.saeedcore.model.OrderStatus

@Preview(showBackground = true, locale = "ar", widthDp = 360, heightDp = 760, name = "Neon Void RTL")
@Composable
private fun NeonVoidPreview() {
    SaeedTheme {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(LocalSaeedSpacing.current.large),
            verticalArrangement = Arrangement.spacedBy(LocalSaeedSpacing.current.large),
        ) {
            Text(stringResource(R.string.preview_title), style = MaterialTheme.typography.headlineMedium)
            SaeedCard(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.preview_empty_message),
                    Modifier.padding(LocalSaeedSpacing.current.large),
                )
            }
            OrderStatus.entries.forEach { SaeedStatusChip(it) }
            SaeedButton(stringResource(R.string.action_confirm), onClick = {})
        }
    }
}

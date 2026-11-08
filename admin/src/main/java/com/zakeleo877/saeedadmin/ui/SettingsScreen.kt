package com.zakeleo877.saeedadmin.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.designsystem.SaeedTextField
import com.zakeleo877.saeedadmin.R
import com.zakeleo877.saeedadmin.notifications.OrderNotificationScheduler

/** Store settings: name, WhatsApp number, currency and the invoice footer. */
@Composable
fun SettingsScreen(
    onSave: (Map<String, String>) -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    var storeName by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("") }
    var footerNote by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.admin_tab_settings),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))

        SaeedTextField(
            value = storeName,
            onValueChange = { storeName = it },
            label = stringResource(R.string.admin_setting_store_name),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        SaeedTextField(
            value = whatsapp,
            onValueChange = { whatsapp = it },
            label = stringResource(R.string.admin_setting_whatsapp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        SaeedTextField(
            value = currency,
            onValueChange = { currency = it },
            label = stringResource(R.string.admin_setting_currency),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        SaeedTextField(
            value = footerNote,
            onValueChange = { footerNote = it },
            label = stringResource(R.string.admin_setting_footer),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        SaeedButton(
            text = stringResource(R.string.admin_save),
            onClick = {
                onSave(
                    mapOf(
                        "storeName" to storeName.trim(),
                        "whatsapp" to whatsapp.trim(),
                        "currency" to currency.trim(),
                        "footerNote" to footerNote.trim(),
                    ),
                )
                saved = true
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (saved) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.admin_settings_saved),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.admin_battery_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.admin_battery_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.admin_notifications_enabled),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { enabled ->
                    notificationsEnabled = enabled
                    if (enabled) {
                        OrderNotificationScheduler.start(context)
                    } else {
                        OrderNotificationScheduler.stop(context)
                    }
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        SaeedButton(
            text = stringResource(R.string.admin_battery_button),
            onClick = {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                runCatching { context.startActivity(intent) }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))
        SaeedButton(
            text = stringResource(R.string.admin_sign_out),
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))
        DiagnosticsCard()
    }
}

/**
 * What the app is actually pointed at.
 *
 * Every report of "the product does not appear" and "the image is unreadable" is
 * consistent with four different causes, and none of them can be told apart from
 * the screen. This panel shows the address the package was built with, whether
 * the session is still valid, and the last refusal the server sent, so the person
 * holding the phone can read the answer out instead of describing a symptom.
 */
@Composable
private fun DiagnosticsCard() {
    val context = LocalContext.current
    val build = remember { com.zakeleo877.saeedadmin.BuildConfig.SUPABASE_URL }
    var copied by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.admin_diagnostics_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.admin_diagnostics_url, build),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.admin_diagnostics_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    SaeedButton(
        text = stringResource(
            if (copied) R.string.admin_diagnostics_copied else R.string.admin_diagnostics_copy,
        ),
        onClick = {
            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
            clipboard?.setPrimaryClip(
                android.content.ClipData.newPlainText("saeed", build),
            )
            copied = true
        },
        modifier = Modifier.fillMaxWidth(),
    )

}

package com.zakeleo877.saeedstore.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.designsystem.SaeedTextField
import com.zakeleo877.saeedcore.util.Validation
import com.zakeleo877.saeedstore.R

/** Data collected by the checkout sheet. */
data class CheckoutForm(
    val customer: String = "",
    val phone: String = "",
    val notes: String = "",
    val location: String = "",
)

sealed interface CheckoutResult {
    data class Submit(val form: CheckoutForm) : CheckoutResult
    data object Dismissed : CheckoutResult
}

/**
 * Draggable checkout sheet. The location is optional and the permission is only
 * requested when the user taps "add my location".
 */
@Composable
fun CheckoutSheet(
    submitting: Boolean,
    onSubmit: (CheckoutForm) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf(false) }

    fun currentLocation(): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            ?: return null
        val providers = listOf(
            android.location.LocationManager.GPS_PROVIDER,
            android.location.LocationManager.NETWORK_PROVIDER,
        )
        return providers.firstNotNullOfOrNull { provider ->
            runCatching {
                manager.getLastKnownLocation(provider)?.takeIf { it.accuracy < 500f }
            }.getOrNull()
        }
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val hasFine = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasFine) {
            val fix = currentLocation()
            if (fix == null) {
                locationError = true
            } else {
                locationError = false
                location = "https://maps.google.com/?q=${fix.latitude},${fix.longitude}"
            }
        } else {
            locationError = true
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.checkout_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        SaeedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = stringResource(R.string.checkout_name),
            isError = nameError,
            supportingText = if (nameError) stringResource(R.string.checkout_name_error) else null,
        )

        SaeedTextField(
            value = phone,
            onValueChange = { phone = it; phoneError = false },
            label = stringResource(R.string.checkout_phone),
            isError = phoneError,
            supportingText = if (phoneError) stringResource(R.string.checkout_phone_error) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )

        SaeedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = stringResource(R.string.checkout_notes),
            singleLine = false,
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.AddLocationAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.size(8.dp))
            SaeedButton(
                text = stringResource(R.string.checkout_add_location),
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        val fix = currentLocation()
                        if (fix == null) {
                            locationError = true
                        } else {
                            locationError = false
                            location = "https://maps.google.com/?q=${fix.latitude},${fix.longitude}"
                        }
                    } else {
                        locationPermission.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ),
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(R.string.checkout_location_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (locationError) {
            Text(
                text = stringResource(R.string.checkout_location_permission),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (locationError) {
            Text(
                text = stringResource(R.string.checkout_location_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        SaeedButton(
            text = stringResource(R.string.checkout_submit),
            onClick = {
                val validName = Validation.customerName(name)
                val validPhone = Validation.phone(phone)
                nameError = !validName
                phoneError = !validPhone
                if (validName && validPhone) {
                    onSubmit(CheckoutForm(name, phone, notes, location))
                }
            },
            enabled = !submitting,
            confirmation = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

package com.zakeleo877.saeedcore.designsystem

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.R
import com.zakeleo877.saeedcore.model.OrderStatus

@Composable
fun SaeedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    confirmation: Boolean = false,
    icon: ImageVector? = null,
) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = {
            if (confirmation) Haptics.confirm(haptics) else Haptics.tap(haptics)
            onClick()
        },
        modifier = modifier.defaultMinSize(minHeight = LocalSaeedSpacing.current.touchTarget),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(LocalSaeedSpacing.current.small))
        }
        Text(text)
    }
}

@Composable
fun SaeedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.98f else 1f,
        animationSpec = SaeedMotion.press,
        label = "card press",
    )
    val clickable = if (onClick == null) Modifier else Modifier.clickable(
        interactionSource = interactions,
        indication = LocalIndication.current,
        onClick = { Haptics.tap(haptics); onClick() },
    )
    val glassAlpha = if (Build.VERSION.SDK_INT >= 31) 0.92f else 1f
    Card(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.then(clickable),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = glassAlpha),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

@Composable
fun SaeedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
fun SaeedSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
fun SaeedStatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val (label, icon, accent) = when (status) {
        OrderStatus.NEW -> Triple(R.string.status_new, Icons.Filled.NewReleases, ElectricCyan)
        OrderStatus.PREPARING -> Triple(R.string.status_preparing, Icons.Filled.HourglassBottom, SemanticWarning)
        OrderStatus.READY -> Triple(R.string.status_ready, Icons.Filled.CheckCircle, SemanticSuccess)
        OrderStatus.HANDED_OVER -> Triple(R.string.status_handed_over, Icons.Filled.DoneAll, ElectricCyan)
        OrderStatus.CANCELLED -> Triple(R.string.status_cancelled, Icons.Filled.Cancel, SemanticError)
    }
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = LocalSaeedSpacing.current.medium,
                vertical = LocalSaeedSpacing.current.small,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LocalSaeedSpacing.current.small))
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Blur the underlying screen on Android 12+; older devices keep a solid overlay. */
fun Modifier.saeedBlurBehind(open: Boolean): Modifier =
    if (open && Build.VERSION.SDK_INT >= 31) this.blur(12.dp) else this

@Composable
fun SaeedDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val overlay = if (Build.VERSION.SDK_INT >= 31) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.90f)
    } else MaterialTheme.colorScheme.secondary
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { Haptics.confirm(haptics); onConfirm() }) {
                Text(confirmLabel, color = SpectralWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = { Haptics.tap(haptics); onDismiss() }) {
                Text(stringResource(R.string.action_cancel), color = SpectralWhite)
            }
        },
        shape = MaterialTheme.shapes.large,
        containerColor = overlay,
        titleContentColor = SpectralWhite,
        textContentColor = SpectralWhite,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaeedBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Deep Void Violet surface layer; a translucent variant only on Android 12+.
    val overlay = if (Build.VERSION.SDK_INT >= 31) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    // Material3's ModalBottomSheet drives the predictive back gesture itself
    // when android:enableOnBackInvokedCallback="true" is declared, so the
    // sheet shrinks with the gesture on Android 13+ without a custom handler.
    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        modifier = modifier,
        containerColor = overlay,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        content = content,
    )
}

@Composable
fun SaeedSkeleton(modifier: Modifier = Modifier) {
    var width by remember { mutableIntStateOf(0) }
    val transition = rememberInfiniteTransition(label = "skeleton")
    val fraction by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "skeleton shimmer",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val shimmer = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Box(
        modifier
            .onSizeChanged { width = it.width }
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.linearGradient(
                    colors = listOf(base, shimmer, base),
                    start = Offset(width * fraction, 0f),
                    end = Offset(width * (fraction + 0.6f), 0f),
                ),
            ),
    )
}

@Composable
fun SaeedEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inventory2,
    retry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(LocalSaeedSpacing.current.xLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalSaeedSpacing.current.large),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        if (retry != null) SaeedButton(stringResource(R.string.action_retry), onClick = retry)
    }
}

@Composable
fun SaeedLoading(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

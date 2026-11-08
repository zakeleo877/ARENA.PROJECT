package com.zakeleo877.saeedstore.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.zakeleo877.saeedcore.designsystem.saeedBlurBehind
import com.zakeleo877.saeedstore.R
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

/**
 * Full-screen image viewer with pinch-to-zoom.
 *
 * The viewer is a custom window rather than a Material sheet, so it drives the
 * predictive back gesture itself: the image shrinks toward the centre while
 * the gesture is in flight and the window closes once it is committed.
 */
@Composable
fun ProductImageDialog(
    imageUrl: String,
    contentDescription: String,
    onDismiss: () -> Unit,
) {
    // The gesture progress drives the shrink so the exit is animated.
    var backProgress by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler { backEvents ->
        try {
            // The flow must be collected so the handler can tell the start of
            // the gesture apart from its completion.
            backEvents.collect { event ->
                backProgress = event.progress
            }
            // The gesture was committed, so the viewer closes.
            onDismiss()
        } catch (cancellation: CancellationException) {
            // The gesture was abandoned, so the viewer returns to its resting
            // state and stays open.
            backProgress = 0f
            throw cancellation
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .saeedBlurBehind(open = true)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        // Panning only makes sense once the image is zoomed in.
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                // Built once per URL. Inside the composition it was rebuilt on
                // every pinch/scale frame, and each rebuild restarted the
                // request: the full-size photo never settled while the customer
                // was zooming.
                model = remember(imageUrl) {
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build()
                },
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer {
                        val shrink = 1f - backProgress * 0.25f
                        scaleX = scale * shrink
                        scaleY = scale * shrink
                        translationX = offsetX * (1f - backProgress)
                        translationY = offsetY * (1f - backProgress)
                        alpha = 1f - backProgress * 0.4f
                    },
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = Color.White,
                )
            }

            if (abs(scale - 1f) > 0.01f) {
                IconButton(
                    onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(50)),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_reset_zoom),
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

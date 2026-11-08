package com.zakeleo877.saeedcore.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * A product photograph from Storage.
 *
 * Three things the plain [AsyncImage] call sites kept getting wrong:
 *
 * 1. **The decoded size.** Without a size the loader decodes at the source
 *    resolution, which for a phone camera is 4000px wide - sixteen times the
 *    memory a card needs. A shop with forty products then collects hundreds of
 *    megabytes of bitmaps and the app is killed on a low-end phone. The request
 *    is bounded to the box it will be drawn in.
 * 2. **A load that failed.** An HTTP 404 or a dead connection used to leave an
 *    empty grey square, indistinguishable from "this product has no photo". It
 *    now says so.
 * 3. **A transparent spinner vs. a placeholder.** The tinted box shows through
 *    while loading instead of flashing white.
 *
 * @param size the largest edge the image will be drawn at; it must be the real
 *   on-screen size, because it is the decode bound.
 */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var failed by remember(url) { mutableStateOf(false) }
    // A retry needs a new request identity: the loader caches the failure, so
    // rebuilding the same request would replay it instead of refetching.
    var attempt by remember(url) { mutableIntStateOf(0) }

    val request = remember(url, size, density, attempt) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            // Without a per-attempt memory key the loader would answer the
            // retry from its failure entry. The disk cache key stays the URL,
            // so a photo already downloaded is not fetched twice.
            .apply { if (attempt > 0) memoryCacheKey("$url#$attempt") }
            // Bounding the decode is the difference between a 4 MB bitmap and a
            // 160 KB one per card.
            .apply { if (size != null) this.size(with(density) { size.roundToPx() }) }
            .build()
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        if (url.isBlank() || failed) {
            // Tapping a photo that failed tries again: on a weak connection the
            // first attempt is often the one that loses the race.
            Icon(
                Icons.Filled.BrokenImage,
                contentDescription = contentDescription,
                tint = SaeedInkMuted,
                modifier = Modifier
                    .size(32.dp)
                    .clickable(enabled = url.isNotBlank()) {
                        failed = false
                        attempt++
                    },
            )
        } else {
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onState = { state -> if (state is AsyncImagePainter.State.Error) failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

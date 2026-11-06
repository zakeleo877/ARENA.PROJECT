package com.zakeleo877.saeedadmin.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.zakeleo877.saeedadmin.R
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.designsystem.SaeedTextField
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Longest side kept in the uploaded image; enough for a phone screen at 2-3x. */
private const val MAX_IMAGE_EDGE = 1024

/** The bucket refuses anything above this, so the compressor has to stay under it. */
private const val MAX_UPLOAD_BYTES = 1_000_000

/**
 * Picks a photo from the gallery, shrinks it, and uploads it.
 *
 * The photo is chosen through the system photo picker, which needs no storage
 * permission and hands back only the one image the admin selected. The picked
 * bytes are decoded and re-encoded as WebP because the bucket only accepts
 * `image/webp`, `image/jpeg` and `image/png` under one megabyte, while a modern
 * phone camera writes files several times larger.
 *
 * Everything that touches the bitmap runs on [Dispatchers.IO]; decoding a large
 * photo on the main thread would freeze the form for a visible moment.
 */
@Composable
fun ProductImagePicker(
    imageUrl: String,
    uploading: Boolean,
    onUpload: (fileName: String, bytes: ByteArray) -> Unit,
    onLinkChange: (String) -> Unit,
    uploadError: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<Int?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            message = null
            val prepared = withContext(Dispatchers.IO) { prepareImage(context, uri) }
            when (prepared) {
                is PreparedImage.Ready -> {
                    previewUri = uri
                    onUpload(prepared.fileName, prepared.bytes)
                }
                PreparedImage.Unreadable -> message = R.string.admin_image_unreadable
                PreparedImage.TooLarge -> message = R.string.admin_image_too_large
            }
        }
    }

    val shownModel: Any? = previewUri ?: imageUrl.ifBlank { null }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.admin_product_image),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (shownModel != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(shownModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.admin_image_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            } else {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
            }
            if (uploading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaeedButton(
                text = stringResource(
                    if (shownModel == null) R.string.admin_image_add else R.string.admin_image_replace,
                ),
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                enabled = !uploading,
                icon = Icons.Filled.AddAPhoto,
                modifier = Modifier.weight(1f),
            )
        }

        if (uploading) {
            Text(
                text = stringResource(R.string.admin_image_uploading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uploadError) {
            Text(
                text = stringResource(R.string.admin_image_upload_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Start,
            )
        }

        message?.let { text ->
            Text(
                text = stringResource(text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Start,
            )
        }

        Text(
            text = stringResource(R.string.admin_image_from_picker),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // The link stays editable so a product can still point at an image that
        // is already hosted elsewhere, without going through this device.
        Text(
            text = stringResource(R.string.admin_link_alternative_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        SaeedTextField(
            value = imageUrl,
            onValueChange = {
                previewUri = null
                onLinkChange(it)
            },
            label = stringResource(R.string.admin_product_image),
        )
    }
}

/** Result of turning a picked [Uri] into something the bucket will accept. */
private sealed interface PreparedImage {
    data class Ready(val fileName: String, val bytes: ByteArray) : PreparedImage
    data object Unreadable : PreparedImage
    data object TooLarge : PreparedImage
}

/**
 * Decodes the picked image, scales it down, and encodes it as WebP.
 *
 * `inSampleSize` does the cheap first pass while decoding, so a 12-megapixel
 * photo never becomes a full-size bitmap in memory. The second pass then fits
 * the result into [MAX_IMAGE_EDGE] before quality is reduced.
 */
@Suppress("DEPRECATION")
private fun prepareImage(
    context: android.content.Context,
    uri: Uri,
): PreparedImage {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    } ?: return PreparedImage.Unreadable
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return PreparedImage.Unreadable

    var sample = 1
    while (bounds.outWidth / (sample * 2) >= MAX_IMAGE_EDGE ||
        bounds.outHeight / (sample * 2) >= MAX_IMAGE_EDGE
    ) {
        sample *= 2
    }

    val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(
            stream,
            null,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    } ?: return PreparedImage.Unreadable

    val longestEdge = maxOf(decoded.width, decoded.height)
    val scaled = if (longestEdge > MAX_IMAGE_EDGE) {
        val ratio = MAX_IMAGE_EDGE.toFloat() / longestEdge
        Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * ratio).toInt().coerceAtLeast(1),
            (decoded.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        decoded
    }

    // 82 is visually indistinguishable from the original at phone sizes and is
    // well inside the bucket limit for a 1024px WebP. WEBP_LOSSY only exists
    // from API 30, so older devices fall back to the lossy WEBP constant.
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        Bitmap.CompressFormat.WEBP
    }
    val bytes = ByteArrayOutputStream().use { output ->
        scaled.compress(format, 82, output)
        output.toByteArray()
    }
    if (scaled !== decoded) scaled.recycle()
    decoded.recycle()

    if (bytes.isEmpty()) return PreparedImage.Unreadable
    if (bytes.size > MAX_UPLOAD_BYTES) return PreparedImage.TooLarge

    // A random name per upload: the same file name reused twice would overwrite
    // the previous image, and two admins could collide.
    val fileName = "${java.util.UUID.randomUUID()}.webp"
    return PreparedImage.Ready(fileName, bytes)
}

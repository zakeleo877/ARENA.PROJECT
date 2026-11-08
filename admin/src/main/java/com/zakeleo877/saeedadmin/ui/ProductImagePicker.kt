package com.zakeleo877.saeedadmin.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.net.Uri
import androidx.core.content.FileProvider
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.io.File
import java.nio.ByteBuffer
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

    // The camera writes into a file this app owns; the system camera cannot
    // hand back a readable Uri any other way on modern Android.
    //
    // The path is saveable, not merely remembered: the camera runs in another
    // process, and Android may reclaim this one while it is in front. The
    // result would then arrive with no file to read.
    var pendingCapturePath by rememberSaveable { mutableStateOf<String?>(null) }

    fun accept(uri: Uri) {
        scope.launch {
            message = null
            when (val prepared = withContext(Dispatchers.IO) { prepareImage(context, uri) }) {
                is PreparedImage.Ready -> {
                    previewUri = uri
                    onUpload(prepared.fileName, prepared.bytes)
                }
                PreparedImage.Unreadable -> message = R.string.admin_image_unreadable
                PreparedImage.TooLarge -> message = R.string.admin_image_too_large
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) accept(uri) }

    val camera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved ->
        val file = pendingCapturePath?.let(::File)
        pendingCapturePath = null
        if (saved && file != null) {
            accept(Uri.fromFile(file))
        } else {
            // A cancelled shot leaves the temporary file behind; it is removed
            // on the next attempt and by the system when the cache is trimmed.
            file?.delete()
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
                icon = Icons.Filled.PhotoLibrary,
                modifier = Modifier.weight(1f),
            )
            SaeedButton(
                text = stringResource(R.string.admin_image_camera),
                onClick = {
                    // A phone with no camera app at all would otherwise crash the
                    // form. The button simply reports that the shot failed.
                    val started = runCatching {
                        val file = newCaptureFile(context)
                        pendingCapturePath = file.absolutePath
                        val authority = "${context.packageName}.fileprovider"
                        camera.launch(FileProvider.getUriForFile(context, authority, file))
                    }.isSuccess
                    if (!started) {
                        pendingCapturePath = null
                        message = R.string.admin_image_camera_failed
                    }
                },
                enabled = !uploading,
                icon = Icons.Filled.PhotoCamera,
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
 * Opens the picked image, whether it came from the photo picker or the camera.
 *
 * The camera hands back a `file:` Uri because the system camera writes into a
 * file this app owns. `ContentResolver` only understands `content:` Uris: it
 * throws `FileNotFoundException` for `file:` ones, so a photo taken with the
 * camera was always reported as unreadable. Reading the path directly fixes it.
 *
 * It also swallows the failure. `openInputStream` throws rather than returning
 * null when the picker’s grant has expired or the file is gone, and an
 * exception on the IO dispatcher would take the whole form down instead of
 * showing the "unreadable image" line.
 */
private fun openSource(context: android.content.Context, uri: Uri): java.io.InputStream? =
    runCatching {
        if (uri.scheme == "file") {
            uri.path?.let { java.io.FileInputStream(it) }
        } else {
            context.contentResolver.openInputStream(uri)
        }
    }.getOrNull()

/**
 * Reads the picked bytes once, then decodes from memory.
 *
 * The previous version opened the source twice: once for the size and once for
 * the pixels. A picker Uri can be opened once and then refuse, and a camera file
 * the provider has already handed over behaves the same way. Reading the bytes
 * first removes the whole class of "it worked when I tried it" failures, and a
 * phone photo is a few megabytes, which fits in memory for the moment this takes.
 *
 * Decoding goes through ImageDecoder from API 28: it understands HEIC and HEIF,
 * which is what a modern Samsung and most new phones actually store, and which
 * BitmapFactory returns null for. That null was reported as an unreadable image,
 * which is the message the owner saw. Older releases keep BitmapFactory.
 */
private fun prepareImage(
    context: android.content.Context,
    uri: Uri,
): PreparedImage {
    val source = openSource(context, uri)?.use { it.readBytes() }
        ?: return PreparedImage.Unreadable

    val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching {
            val src = ImageDecoder.createSource(ByteBuffer.wrap(source))
            ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_IMAGE_EDGE) {
                    val ratio = MAX_IMAGE_EDGE.toFloat() / longest
                    decoder.setTargetSize(
                        (info.size.width * ratio).toInt().coerceAtLeast(1),
                        (info.size.height * ratio).toInt().coerceAtLeast(1),
                    )
                }
            }
        }.getOrNull()
    } else {
        decodeWithBitmapFactory(source)
    } ?: return PreparedImage.Unreadable

    if (decoded.width <= 0 || decoded.height <= 0) return PreparedImage.Unreadable

    // 82 is visually indistinguishable from the original at phone sizes and is
    // well inside the bucket limit for a 1024px WebP. WEBP_LOSSY only exists
    // from API 30, so older devices fall back to the lossy WEBP constant.
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        Bitmap.CompressFormat.WEBP
    }
    val bytes = ByteArrayOutputStream().use { output ->
        decoded.compress(format, 82, output)
        output.toByteArray()
    }
    decoded.recycle()

    if (bytes.isEmpty()) return PreparedImage.Unreadable
    if (bytes.size > MAX_UPLOAD_BYTES) return PreparedImage.TooLarge

    // A random name per upload: the same file name reused twice would overwrite
    // the previous image, and two admins could collide.
    val fileName = "${java.util.UUID.randomUUID()}.webp"
    return PreparedImage.Ready(fileName, bytes)
}

/** The pre-28 path: sample down while decoding, then fit the long edge. */
@Suppress("DEPRECATION")
private fun decodeWithBitmapFactory(source: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (bounds.outWidth / (sample * 2) >= MAX_IMAGE_EDGE ||
        bounds.outHeight / (sample * 2) >= MAX_IMAGE_EDGE
    ) {
        sample *= 2
    }
    val decoded = BitmapFactory.decodeByteArray(
        source,
        0,
        source.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    ) ?: return null

    val longestEdge = maxOf(decoded.width, decoded.height)
    return if (longestEdge > MAX_IMAGE_EDGE) {
        val ratio = MAX_IMAGE_EDGE.toFloat() / longestEdge
        Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * ratio).toInt().coerceAtLeast(1),
            (decoded.height * ratio).toInt().coerceAtLeast(1),
            true,
        ).also { if (it !== decoded) decoded.recycle() }
    } else {
        decoded
    }
}

/**
 * A fresh file for one camera shot.
 *
 * The camera app writes the picture here, so the name must be unique and the
 * directory must exist. It lives in the cache because the photo is uploaded
 * immediately afterwards and never needs to outlive the form.
 */
private fun newCaptureFile(context: android.content.Context): File {
    val directory = File(context.cacheDir, "product-captures").apply { mkdirs() }
    // Sweep yesterday's abandoned shots so the cache does not grow forever.
    directory.listFiles()?.forEach { stale ->
        if (System.currentTimeMillis() - stale.lastModified() > 24 * 60 * 60 * 1000L) {
            stale.delete()
        }
    }
    return File(directory, "capture-${System.currentTimeMillis()}.jpg")
}

package com.zakeleo877.saeedcore.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Builds CSV files that open correctly in Excel.
 *
 * The UTF-8 byte order mark is written first so Excel detects the encoding and
 * renders the Arabic columns from right to left instead of mojibake.
 */
object CsvExporter {

    /** The UTF-8 byte order mark. */
    private const val BOM = "\uFEFF"

    /** Quotes a single field and doubles any embedded quote. */
    fun field(value: String?): String {
        val text = value.orEmpty()
        return if (text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }

    /** Builds one CSV document from a header row and its data rows. */
    fun build(headers: List<String>, rows: List<List<String>>): String {
        val builder = StringBuilder(BOM)
        builder.append(headers.joinToString(",") { field(it) })
        builder.append("\r\n")
        rows.forEach { row ->
            builder.append(row.joinToString(",") { field(it) })
            builder.append("\r\n")
        }
        return builder.toString()
    }

    /**
     * Writes the CSV and opens the system document picker for the destination.
     *
     * The file is written into the app cache and shared through the
     * FileProvider, so no storage permission is required.
     */
    fun export(context: Context, fileName: String, content: String): Boolean {
        return runCatching {
            val directory = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(directory, fileName)
            file.writeText(content, Charsets.UTF_8)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.filepicker",
                file,
            )
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, fileName)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, fileName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }
}

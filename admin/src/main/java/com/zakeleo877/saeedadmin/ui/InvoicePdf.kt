package com.zakeleo877.saeedadmin.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.zakeleo877.saeedcore.model.Order
import com.zakeleo877.saeedcore.util.Formatters
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a printable invoice. Arabic text is drawn with the platform default
 * typeface so the RTL shaping is handled by the system.
 */
object InvoicePdf {
    private const val PAGE_WIDTH = 595 // A4 at 72 dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun create(context: Context, order: Order, storeName: String, footerNote: String): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val title = Paint().apply {
            isAntiAlias = true
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        val body = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            color = android.graphics.Color.BLACK
        }
        val label = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.BLACK
        }
        val line = Paint().apply {
            strokeWidth = 1f
            color = android.graphics.Color.DKGRAY
        }

        var y = MARGIN + 10f

        canvas.drawText(storeName, PAGE_WIDTH / 2f, y, title)
        y += 26f
        canvas.drawText("فاتورة رقم ${Formatters.orderNumber(order.number)}", PAGE_WIDTH / 2f, y, body)
        y += 20f
        canvas.drawText("التاريخ: ${Formatters.dateTime(order.createdAt)}", PAGE_WIDTH / 2f, y, body)
        y += 24f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, line)
        y += 20f

        canvas.drawText("الزبون: ${order.customer}", MARGIN, y, label)
        y += 18f
        canvas.drawText("الهاتف: ${order.phone}", MARGIN, y, body)
        if (order.notes.isNotBlank()) {
            y += 18f
            canvas.drawText("ملاحظات: ${order.notes}", MARGIN, y, body)
        }
        y += 26f

        canvas.drawText("المنتجات", MARGIN, y, label)
        y += 18f
        order.items.forEach { item ->
            canvas.drawText("${item.name} ×${item.quantity}", MARGIN, y, body)
            val price = Formatters.money(item.price * item.quantity.toBigDecimal(), "د.ج")
            canvas.drawText(price, PAGE_WIDTH - MARGIN, y, body.apply { textAlign = Paint.Align.RIGHT })
            body.textAlign = Paint.Align.LEFT
            y += 18f
        }

        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, line)
        y += 20f
        canvas.drawText("المجموع", MARGIN, y, label)
        canvas.drawText(
            Formatters.money(order.total, "د.ج"),
            PAGE_WIDTH - MARGIN,
            y,
            body.apply { textAlign = Paint.Align.RIGHT; textSize = 14f },
        )
        body.textAlign = Paint.Align.LEFT
        body.textSize = 12f
        y += 30f

        if (footerNote.isNotBlank()) {
            canvas.drawText(footerNote, PAGE_WIDTH / 2f, y, body)
        }

        document.finishPage(page)

        val directory = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(directory, "invoice-${order.number}.pdf")
        FileOutputStream(file).use { stream -> document.writeTo(stream) }
        document.close()
        return file
    }

    /** Shares the invoice through the system chooser. */
    fun share(context: Context, file: File): Intent? {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, null)
    }
}

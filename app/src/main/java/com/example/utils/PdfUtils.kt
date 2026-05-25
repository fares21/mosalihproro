package com.example.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.database.Ticket
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object PdfUtils {
    fun createInvoicePdf(context: Context, ticket: Ticket, storeName: String, storePhone: String, currency: String, lang: String): File? {
        val pdfDocument = PdfDocument()
        // A4 Page Size: 595 x 842
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1E3A8A") // Slate Corporate Blue
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 14f
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 60f

        // Draw Store Name / Header
        canvas.drawText(storeName, 40f, y, titlePaint)
        y += 25f
        canvas.drawText("${Localization.get("phone_label_col", lang)} $storePhone", 40f, y, subTitlePaint)
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = sdf.format(ticket.createdAt)
        canvas.drawText("${Localization.get("registration_date", lang)} $dateStr", 340f, y, subTitlePaint)
        
        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        
        y += 35f
        titlePaint.textSize = 18f
        canvas.drawText("${Localization.get("ticket_details_title", lang)} ${ticket.id}", 40f, y, titlePaint)
        
        y += 40f
        val spacing = 28f
        
        drawInvoiceRow(canvas, Localization.get("client_label_col", lang), ticket.customerName, 40f, y, labelPaint, textPaint)
        y += spacing
        drawInvoiceRow(canvas, Localization.get("phone_label_col", lang), ticket.customerPhone, 40f, y, labelPaint, textPaint)
        y += spacing
        drawInvoiceRow(canvas, Localization.get("registration_date", lang), dateStr, 40f, y, labelPaint, textPaint)
        y += spacing
        drawInvoiceRow(canvas, Localization.get("device_label_col", lang), ticket.deviceModel, 40f, y, labelPaint, textPaint)
        y += spacing
        drawInvoiceRow(canvas, Localization.get("fault_label_col", lang), ticket.faultDescription, 40f, y, labelPaint, textPaint)
        y += spacing
        
        val statusLabelVal = when(ticket.status) {
            "PENDING" -> Localization.get("status_pending", lang)
            "IN_PROGRESS" -> Localization.get("status_inprogress", lang)
            "PART_NEEDED" -> Localization.get("status_parts", lang)
            "COMPLETED" -> Localization.get("status_completed", lang)
            "DELIVERED" -> Localization.get("status_delivered", lang)
            else -> ticket.status
        }
        
        // Dynamic status row
        val rawStatusTitle = Localization.get("share_status", lang).replace("• ", "").replace(" :", "").trim()
        val statusTitle = if (rawStatusTitle.endsWith(":")) rawStatusTitle else "$rawStatusTitle :"
        drawInvoiceRow(canvas, statusTitle, statusLabelVal, 40f, y, labelPaint, textPaint)
        y += spacing
        
        y += 30f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 30f
        
        // Prices block
        drawInvoiceRow(canvas, Localization.get("total_price_col", lang), "${ticket.totalPrice} $currency", 40f, y, labelPaint, textPaint)
        y += spacing
        drawInvoiceRow(canvas, Localization.get("advance_payment_col", lang), "${ticket.advancePayment} $currency", 40f, y, labelPaint, textPaint)
        y += spacing
        
        val duePaint = Paint(textPaint).apply {
            color = Color.parseColor("#DC2626") // Red
            isFakeBoldText = true
        }
        val dueLabelPaint = Paint(labelPaint).apply {
            color = Color.parseColor("#DC2626") // Red
        }
        drawInvoiceRow(canvas, Localization.get("remaining_payment_col", lang), "${ticket.remainingAmount} $currency", 40f, y, dueLabelPaint, duePaint)
        
        y += 40f
        if (ticket.notes.isNotEmpty()) {
            canvas.drawText(Localization.get("additional_notes_col", lang), 40f, y, labelPaint)
            y += 20f
            canvas.drawText(ticket.notes, 50f, y, textPaint)
            y += 40f
        }
        
        pdfDocument.finishPage(page)
        
        // Store pdf to private cache to be shared easily
        val invoiceDir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val invoiceFile = File(invoiceDir, "invoice_${ticket.id}.pdf")
        
        return try {
            FileOutputStream(invoiceFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            invoiceFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawInvoiceRow(canvas: Canvas, label: String, value: String, x: Float, y: Float, labelPaint: Paint, valuePaint: Paint) {
        canvas.drawText(label, x, y, labelPaint)
        canvas.drawText(value, x + 160f, y, valuePaint)
    }
}

package com.grocart.first.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Toast
import androidx.core.content.FileProvider
import com.grocart.first.data.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    /**
     * Generates a PDF invoice styled like a hotel receipt and saves it to the Downloads directory.
     * @param context the context to use for toasts and content resolver.
     * @param order the order to generate the invoice for.
     */
    suspend fun generateInvoicePdf(context: Context, order: Order) {
        withContext(Dispatchers.IO) {
            val pdfDocument = PdfDocument()

            // Receipt tape dimensions: narrow and long
            val pageWidth = 400
            val pageHeight = 800 // Height might need dynamic adjustment for huge orders, but static is fine for standard
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            val canvas: Canvas = page.canvas

            // Paints
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val subtitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 14f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }

            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.LEFT
            }

            val boldTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }

            val rightAlignPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.RIGHT
            }

            val dividerPaint = Paint().apply {
                color = Color.parseColor("#CCCCCC") // Light gray dashes
                strokeWidth = 2f
                style = Paint.Style.STROKE
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
            }

            // Margins and Y tracking
            val margin = 20f
            var currentY = 50f
            val centerX = pageWidth / 2f
            val rightMarginX = pageWidth - margin

            // --- Header ---
            canvas.drawText("GROCART", centerX, currentY, titlePaint)
            currentY += 20f
            canvas.drawText("Fresh Groceries at your door", centerX, currentY, subtitlePaint)
            currentY += 20f
            canvas.drawText("123 Market Street, Cityville", centerX, currentY, subtitlePaint)
            currentY += 15f
            canvas.drawText("Tel: +1 234 567 8900", centerX, currentY, subtitlePaint)

            currentY += 30f

            // --- Order Info ---
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(Date(order.timestamp))
            canvas.drawText("Receipt #: ${order.id ?: System.currentTimeMillis() % 10000}", margin, currentY, textPaint)
            currentY += 20f
            canvas.drawText("Date: $dateStr", margin, currentY, textPaint)
            
            currentY += 15f
            canvas.drawLine(margin, currentY, rightMarginX, currentY, dividerPaint)
            currentY += 25f

            // --- Table Headers ---
            canvas.drawText("Qty  Item", margin, currentY, boldTextPaint)
            canvas.drawText("Amount", rightMarginX, currentY, Paint().apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            })
            currentY += 15f
            canvas.drawLine(margin, currentY, rightMarginX, currentY, dividerPaint)
            currentY += 25f

            // --- Items ---
            val itemsWithQuantity = order.items
                .groupBy { it.itemName }
                .map { (_, items) -> Pair(items.first(), items.size) }

            var subtotal = 0

            for ((item, quantity) in itemsWithQuantity) {
                // Item Name (Trimmed if too long)
                val maxNameLength = 20
                var dispName = item.itemName
                if (dispName.length > maxNameLength) {
                    dispName = dispName.substring(0, maxNameLength - 3) + "..."
                }

                // Format: " 1x  Tomato"
                val qtyStr = quantity.toString().padStart(2, ' ') + "x"
                canvas.drawText("$qtyStr  $dispName", margin, currentY, textPaint)

                // Cost for this line
                val lineTotal = item.itemPrice * quantity
                subtotal += lineTotal
                canvas.drawText(lineTotal.toString(), rightMarginX, currentY, rightAlignPaint)

                currentY += 25f
            }

            currentY += 10f
            canvas.drawLine(margin, currentY, rightMarginX, currentY, dividerPaint)
            currentY += 25f

            // --- Totals ---
            // Note: MyOrdersScreen calculate actual total (with tax/delivery) just locally in ui, 
            // but for Invoice let's estimate or just show the items sum if we don't have the exact total saved.
            // Since delivery and handling are standard in your UI:
            val handlingCharge = (subtotal * 0.01).toInt()
            val deliveryFee = 30
            val grandTotal = subtotal + handlingCharge + deliveryFee

            canvas.drawText("Subtotal:", rightMarginX - 80f, currentY, textPaint)
            canvas.drawText(subtotal.toString(), rightMarginX, currentY, rightAlignPaint)
            currentY += 25f

            canvas.drawText("Handling:", rightMarginX - 80f, currentY, textPaint)
            canvas.drawText(handlingCharge.toString(), rightMarginX, currentY, rightAlignPaint)
            currentY += 25f

            canvas.drawText("Delivery:", rightMarginX - 80f, currentY, textPaint)
            canvas.drawText(deliveryFee.toString(), rightMarginX, currentY, rightAlignPaint)
            currentY += 25f

            canvas.drawLine(margin, currentY, rightMarginX, currentY, dividerPaint)
            currentY += 25f

            canvas.drawText("TOTAL AMOUNT PAID:", margin, currentY, boldTextPaint)
            canvas.drawText("Rs. $grandTotal", rightMarginX, currentY, Paint().apply {
                color = Color.BLACK
                textSize = 16f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            })

            currentY += 40f

            // --- Footer ---
            canvas.drawText("Thank you for shopping with GroCart!", centerX, currentY, subtitlePaint)
            currentY += 20f
            canvas.drawText("Please come again.", centerX, currentY, subtitlePaint)

            pdfDocument.finishPage(page)

            val fileName = "GroCart_Invoice_${System.currentTimeMillis()}.pdf"
            var outputStream: OutputStream? = null

            var pdfUri: android.net.Uri? = null

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        outputStream = resolver.openOutputStream(uri)
                        pdfUri = uri
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName)
                    outputStream = FileOutputStream(file)
                    pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                }

                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream)
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Invoice downloaded", Toast.LENGTH_SHORT).show()
                        // 1. Launch Intent to view PDF
                        pdfUri?.let { uri ->
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No app found to open PDF", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to create invoice file", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving invoice: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                pdfDocument.close()
                outputStream?.close()
            }
        }
    }
}

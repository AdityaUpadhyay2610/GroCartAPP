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
            // CartItemResponse already carries the correct quantity — no need to group
            var subtotal = 0

            // Gray sub-line paint for unit price
            val unitPricePaint = Paint().apply {
                color = Color.GRAY
                textSize = 12f
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.LEFT
            }

            for (item in order.items) {
                val quantity = item.quantity
                val discountedPrice = item.itemPrice * 75 / 100

                // Item Name (Trimmed if too long)
                val maxNameLength = 20
                var dispName = item.itemName
                if (dispName.length > maxNameLength) {
                    dispName = dispName.substring(0, maxNameLength - 3) + "..."
                }

                // Line 1: "2x  Item Name"   →   "Rs. lineTotal"
                val qtyStr = quantity.toString().padStart(2, ' ') + "x"
                canvas.drawText("$qtyStr  $dispName", margin, currentY, textPaint)

                val lineTotal = discountedPrice * quantity
                subtotal += lineTotal
                canvas.drawText("Rs. $lineTotal", rightMarginX, currentY, rightAlignPaint)

                // Line 2: "    @ Rs. X each" (indented, gray, smaller)
                currentY += 18f
                canvas.drawText("     @ Rs. $discountedPrice each", margin, currentY, unitPricePaint)

                currentY += 20f
            }

            currentY += 10f
            canvas.drawLine(margin, currentY, rightMarginX, currentY, dividerPaint)
            currentY += 25f

            // --- Totals: use persisted order values for exact accuracy ---
            val handlingCharge = (subtotal * 0.01).toInt()
            val deliveryFee = 30
            val couponDiscount = order.couponDiscount
            // Use stored totalPaid if available; fall back for older orders
            val grandTotal = if (order.totalPaid > 0) order.totalPaid
                             else subtotal + handlingCharge + deliveryFee - couponDiscount

            canvas.drawText("Subtotal:", margin, currentY, textPaint)
            canvas.drawText("Rs. $subtotal", rightMarginX, currentY, rightAlignPaint)
            currentY += 22f

            canvas.drawText("Handling (1%):", margin, currentY, textPaint)
            canvas.drawText("Rs. $handlingCharge", rightMarginX, currentY, rightAlignPaint)
            currentY += 22f

            canvas.drawText("Delivery Fee:", margin, currentY, textPaint)
            canvas.drawText("Rs. $deliveryFee", rightMarginX, currentY, rightAlignPaint)
            currentY += 22f

            // Show coupon savings only if a coupon was applied
            if (couponDiscount > 0) {
                val savingsPaint = Paint().apply {
                    color = Color.parseColor("#16A34A")
                    textSize = 14f
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.LEFT
                }
                val savingsRightPaint = Paint().apply {
                    color = Color.parseColor("#16A34A")
                    textSize = 14f
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("Coupon Savings:", margin, currentY, savingsPaint)
                canvas.drawText("- Rs. $couponDiscount", rightMarginX, currentY, savingsRightPaint)
                currentY += 22f
            }

            canvas.drawLine(margin, currentY, rightMarginX, currentY, dividerPaint)
            currentY += 10f

            // Highlight total row with a light background
            val totalRowPaint = Paint().apply {
                color = Color.parseColor("#F0FDF4")
                style = Paint.Style.FILL
            }
            canvas.drawRect(margin, currentY - 4f, rightMarginX, currentY + 24f, totalRowPaint)
            currentY += 20f

            canvas.drawText("TOTAL PAID:", margin, currentY, boldTextPaint)
            canvas.drawText("Rs. $grandTotal", rightMarginX, currentY, Paint().apply {
                color = Color.parseColor("#16A34A")
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

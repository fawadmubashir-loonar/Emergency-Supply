package com.emergencysupply.rations.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.ui.DashboardMetrics
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PantryExportManager {
    private const val TAG = "PantryExportManager"
    private const val PAGE_WIDTH = 612   // US-Letter 8.5 x 11 in points (72 DPI)
    private const val PAGE_HEIGHT = 792
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 576f
    private const val USABLE_WIDTH = MARGIN_RIGHT - MARGIN_LEFT
    private const val BOTTOM_LIMIT = 740f

    fun exportPantryPdf(
        context: Context,
        items: List<InventoryItem>,
        metrics: DashboardMetrics
    ): Uri? {
        val document = PdfDocument()
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, "emergency_pantry_inventory.pdf")

        try {
            val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date())

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 9.5f
            }
            val kpiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val kpiValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val colHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 8.5f
            }
            val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 7.5f
            }
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 0.75f
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 8f
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = 40f

            fun drawFooter(c: Canvas, pageNum: Int) {
                linePaint.color = Color.parseColor("#CBD5E1")
                c.drawLine(MARGIN_LEFT, 755f, MARGIN_RIGHT, 755f, linePaint)
                val footerText = "Emergency Supply Rations • Generated $dateStr"
                c.drawText(footerText, MARGIN_LEFT, 768f, footerPaint)
                val pageStr = "Page $pageNum"
                val pageStrWidth = footerPaint.measureText(pageStr)
                c.drawText(pageStr, MARGIN_RIGHT - pageStrWidth, 768f, footerPaint)
            }

            fun advancePage(): Canvas {
                drawFooter(canvas, pageNumber)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                currentY = 42f
                return page.canvas
            }

            // --- PAGE 1 HEADER ---
            canvas.drawText("EMERGENCY PANTRY INVENTORY", MARGIN_LEFT, currentY, titlePaint)
            currentY += 13f
            canvas.drawText("Official Family Preparedness & Ration Stock Status Report • $dateStr", MARGIN_LEFT, currentY, subtitlePaint)
            currentY += 16f

            // --- KPI CARDS (6 metrics in 2 rows of 3) ---
            val kpiCards = listOf(
                Pair("TOTAL UNIQUE ITEMS", "${metrics.totalUniqueItems} SKUs"),
                Pair("IN STOCK QUANTITY", String.format(Locale.US, "%.1f Units", metrics.totalUnitsInStock)),
                Pair("TOTAL SERVINGS", String.format(Locale.US, "%.0f Servings", metrics.totalServings)),
                Pair("EST. SUPPLY DURATION", "${metrics.estDaysOfSupply} Days"),
                Pair("EXPIRING SOON", "${metrics.expiringSoonCount} Items"),
                Pair("RESTOCK NEEDED", "${metrics.restockNeededCount} Items")
            )

            val cardSpacing = 6f
            val cardCols = 3
            val cardWidth = (USABLE_WIDTH - (cardSpacing * (cardCols - 1))) / cardCols
            val cardHeight = 32f

            for (i in kpiCards.indices) {
                val col = i % cardCols
                val row = i / cardCols
                val x = MARGIN_LEFT + col * (cardWidth + cardSpacing)
                val y = currentY + row * (cardHeight + 4f)

                rectPaint.color = Color.parseColor("#F8FAFC")
                canvas.drawRoundRect(RectF(x, y, x + cardWidth, y + cardHeight), 4f, 4f, rectPaint)
                linePaint.color = Color.parseColor("#E2E8F0")
                linePaint.style = Paint.Style.STROKE
                canvas.drawRoundRect(RectF(x, y, x + cardWidth, y + cardHeight), 4f, 4f, linePaint)
                linePaint.style = Paint.Style.FILL

                canvas.drawText(kpiCards[i].first, x + 8f, y + 12f, kpiLabelPaint)
                canvas.drawText(kpiCards[i].second, x + 8f, y + 26f, kpiValuePaint)
            }
            currentY += (cardHeight * 2) + 16f

            // Group items by Category & Storage Spot
            val groupedByCategory = items.groupBy { it.category.ifBlank { "General Category" } }
                .toSortedMap()

            fun drawTableHeader(c: Canvas, y: Float) {
                rectPaint.color = Color.parseColor("#F1F5F9")
                c.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + 16f), 3f, 3f, rectPaint)
                c.drawText("ITEM / DESCRIPTION", MARGIN_LEFT + 6f, y + 11.5f, colHeaderPaint)
                c.drawText("STORAGE SPOT", MARGIN_LEFT + 175f, y + 11.5f, colHeaderPaint)
                c.drawText("QTY / PAR", MARGIN_LEFT + 285f, y + 11.5f, colHeaderPaint)
                c.drawText("EARLIEST EXPIRY", MARGIN_LEFT + 375f, y + 11.5f, colHeaderPaint)
                c.drawText("STATUS", MARGIN_LEFT + 475f, y + 11.5f, colHeaderPaint)
            }

            for ((category, categoryItems) in groupedByCategory) {
                // Check space for Category Header + Table Header + at least one item
                if (currentY + 54f > BOTTOM_LIMIT) {
                    canvas = advancePage()
                }

                // Category Header Banner
                rectPaint.color = Color.parseColor("#E2E8F0")
                canvas.drawRoundRect(RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 18f), 3f, 3f, rectPaint)
                canvas.drawText(
                    "$category (${categoryItems.size} items)",
                    MARGIN_LEFT + 8f,
                    currentY + 13f,
                    sectionHeaderPaint
                )
                currentY += 22f

                drawTableHeader(canvas, currentY)
                currentY += 20f

                val groupedByLocation = categoryItems.groupBy { it.location.ifBlank { "Main Pantry" } }
                    .toSortedMap()

                for ((_, locationItems) in groupedByLocation) {
                    for (item in locationItems) {
                        if (currentY + 22f > BOTTOM_LIMIT) {
                            canvas = advancePage()
                            drawTableHeader(canvas, currentY)
                            currentY += 20f
                        }

                        // Row background (alternating or clean separator)
                        linePaint.color = Color.parseColor("#F1F5F9")
                        canvas.drawLine(MARGIN_LEFT, currentY + 16f, MARGIN_RIGHT, currentY + 16f, linePaint)

                        // Item Name & Unit
                        val displayName = if (item.name.length > 28) item.name.take(26) + "…" else item.name
                        canvas.drawText(displayName, MARGIN_LEFT + 6f, currentY + 10f, bodyBoldPaint)

                        // Location
                        val locName = if (item.location.length > 20) item.location.take(18) + "…" else item.location.ifBlank { "—" }
                        canvas.drawText(locName, MARGIN_LEFT + 175f, currentY + 10f, secondaryTextPaint)

                        // Qty / Par
                        val qtyStr = String.format(Locale.US, "%.1f / %.1f %s", item.currentQty, item.parLevel, item.unit)
                        canvas.drawText(qtyStr, MARGIN_LEFT + 285f, currentY + 10f, bodyPaint)

                        // Earliest Expiry & Days
                        val expiryText = when {
                            item.earliestExpiry != null && item.daysUntilExpiry != null -> {
                                "${item.earliestExpiry} (${item.daysUntilExpiry}d)"
                            }
                            item.earliestExpiry != null -> item.earliestExpiry
                            else -> "No Expiry"
                        }
                        canvas.drawText(expiryText, MARGIN_LEFT + 375f, currentY + 10f, bodyPaint)

                        // Status Badge
                        val statusLabel = when {
                            item.isExpired -> "EXPIRED"
                            item.isExpiringSoon -> "EXPIRING"
                            item.isLowStock -> "LOW STOCK"
                            else -> "HEALTHY"
                        }
                        val (badgeBg, badgeFg) = when (statusLabel) {
                            "EXPIRED" -> Pair("#FEE2E2", "#DC2626")
                            "EXPIRING" -> Pair("#FEF3C7", "#D97706")
                            "LOW STOCK" -> Pair("#FFEDD5", "#EA580C")
                            else -> Pair("#DCFCE7", "#16A34A")
                        }

                        rectPaint.color = Color.parseColor(badgeBg)
                        badgePaint.color = Color.parseColor(badgeFg)
                        val badgeWidth = badgePaint.measureText(statusLabel) + 10f
                        canvas.drawRoundRect(
                            RectF(MARGIN_LEFT + 475f, currentY + 1f, MARGIN_LEFT + 475f + badgeWidth, currentY + 14f),
                            3f, 3f, rectPaint
                        )
                        canvas.drawText(statusLabel, MARGIN_LEFT + 480f, currentY + 10.5f, badgePaint)

                        currentY += 18f
                    }
                }
                currentY += 10f
            }

            drawFooter(canvas, pageNumber)
            document.finishPage(page)

            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            document.close()

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export PDF", e)
            try {
                document.close()
            } catch (_: Exception) {}
            return null
        }
    }

    fun exportPantryCsv(context: Context, items: List<InventoryItem>): Uri? {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, "emergency_pantry_inventory.csv")

        return try {
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // BOM for Excel compatibility
                    writer.write("\uFEFF")

                    // CSV Headers
                    val headers = listOf(
                        "Item ID",
                        "Item Name",
                        "Category",
                        "Storage Spot",
                        "Current Qty",
                        "Unit",
                        "Par Level",
                        "Restock Needed",
                        "Earliest Expiry",
                        "Days Until Expiry",
                        "Stock Status",
                        "Expiration Status",
                        "Total Servings",
                        "Notes",
                        "Last Updated"
                    )
                    writer.write(headers.joinToString(",") { escapeCsv(it) } + "\n")

                    // Rows
                    for (item in items) {
                        val row = listOf(
                            item.id,
                            item.name,
                            item.category,
                            item.location,
                            String.format(Locale.US, "%.2f", item.currentQty),
                            item.unit,
                            String.format(Locale.US, "%.2f", item.parLevel),
                            String.format(Locale.US, "%.2f", item.restockNeeded),
                            item.earliestExpiry ?: "",
                            item.daysUntilExpiry?.toString() ?: "",
                            item.stockStatus,
                            item.expirationStatus,
                            String.format(Locale.US, "%.1f", item.totalServings),
                            item.notes,
                            item.lastUpdated
                        )
                        writer.write(row.joinToString(",") { escapeCsv(it) } + "\n")
                    }
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export CSV", e)
            null
        }
    }

    private fun escapeCsv(value: Any?): String {
        val string = value?.toString() ?: ""
        return if (string.contains(",") || string.contains("\"") || string.contains("\n") || string.contains("\r")) {
            "\"" + string.replace("\"", "\"\"") + "\""
        } else {
            string
        }
    }
}

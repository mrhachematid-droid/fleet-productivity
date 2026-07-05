package com.example.ui.pdf

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)

    fun exportAndSharePdf(
        context: Context,
        cycles: List<TruckCycle>,
        drivers: List<Driver>,
        trucks: List<Truck>,
        machines: List<Machine>,
        shifts: List<Shift>
    ) {
        val pdfFile = generateFleetPdf(context, cycles, drivers, trucks, machines, shifts)
        if (pdfFile != null && pdfFile.exists()) {
            sharePdf(context, pdfFile)
        } else {
            Toast.makeText(context, "Failed to generate PDF Report", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateFleetPdf(
        context: Context,
        cycles: List<TruckCycle>,
        drivers: List<Driver>,
        trucks: List<Truck>,
        machines: List<Machine>,
        shifts: List<Shift>
    ): File? {
        val document = PdfDocument()

        // Page dimensions (A4 size: 595 x 842 pt)
        val pageWidth = 595
        val pageHeight = 842

        // Create Page 1: Dashboard, KPIs, and Charts
        val page1Info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = document.startPage(page1Info)
        val canvas1 = page1.canvas

        drawPage1(canvas1, cycles, drivers, trucks, machines, shifts)
        document.finishPage(page1)

        // Create Page 2: Chronological Timeline and Signatures
        val page2Info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        val page2 = document.startPage(page2Info)
        val canvas2 = page2.canvas

        drawPage2(canvas2, cycles, drivers, trucks, machines, shifts)
        document.finishPage(page2)

        // Save PDF file in the cache directory
        return try {
            val cacheDir = context.cacheDir
            val pdfFile = File(cacheDir, "Fleet_Productivity_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    private fun drawPage1(
        canvas: Canvas,
        cycles: List<TruckCycle>,
        drivers: List<Driver>,
        trucks: List<Truck>,
        machines: List<Machine>,
        shifts: List<Shift>
    ) {
        // Paints
        val paintText = Paint().apply { isAntiAlias = true }
        val paintRect = Paint().apply { isAntiAlias = true }

        // 1. HEADER BANNER (Slate background)
        paintRect.color = Color.parseColor("#0F172A") // Slate 900
        canvas.drawRect(0f, 0f, 595f, 100f, paintRect)

        // 2. VECTOR LOGO EMBLEM (Shield + Gear)
        paintRect.color = Color.parseColor("#F59E0B") // Amber
        val logoPath = Path().apply {
            moveTo(25f, 25f)
            lineTo(45f, 15f)
            lineTo(65f, 25f)
            lineTo(65f, 55f)
            quadTo(45f, 75f, 25f, 55f)
            close()
        }
        canvas.drawPath(logoPath, paintRect)

        paintRect.color = Color.parseColor("#3B82F6") // Blue Inner
        val innerLogo = Path().apply {
            moveTo(32f, 30f)
            lineTo(45f, 23f)
            lineTo(58f, 30f)
            lineTo(58f, 50f)
            quadTo(45f, 65f, 32f, 50f)
            close()
        }
        canvas.drawPath(innerLogo, paintRect)

        // Header Title
        paintText.apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("APEX MINING & LOGISTICS", 80f, 42f, paintText)

        paintText.apply {
            color = Color.parseColor("#94A3B8") // Slate 400
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("FLEET PRODUCTIVITY ANALYSIS & AUDIT REPORT", 80f, 60f, paintText)
        canvas.drawText("SYSTEM DATE: ${dateFormatter.format(Date())} | SHIFT COMBINED", 80f, 75f, paintText)

        // 3. STATISTICAL KPI GRID
        val totalLoads = cycles.size
        val avgCycle = if (cycles.isNotEmpty()) cycles.map { it.cycleTimeMinutes }.average() else 0.0
        
        var totalQueueLoad = 0.0
        var totalLoadTime = 0.0
        var totalTravel = 0.0
        var totalQueueUnload = 0.0
        cycles.forEach {
            totalQueueLoad += (it.loadingStartMillis - it.queueStartMillis) / 60000.0
            totalLoadTime += (it.loadingEndMillis - it.loadingStartMillis) / 60000.0
            totalTravel += (it.unloadingQueueStartMillis - it.loadingEndMillis) / 60000.0
            totalQueueUnload += (it.unloadMillis - it.unloadingQueueStartMillis) / 60000.0
        }

        val totalWaitMin = totalQueueLoad + totalQueueUnload
        val totalCycleSum = cycles.sumOf { it.cycleTimeMinutes }
        val waitPercent = if (totalCycleSum > 0) (totalWaitMin / totalCycleSum) * 100.0 else 0.0
        val prodScore = if (cycles.isNotEmpty()) 100.0 - waitPercent else 0.0

        // Draw 4 Boxes
        val boxWidth = 115f
        val boxHeight = 55f
        val boxY = 120f
        val boxPadding = 16f

        val kpiData = listOf(
            Triple("TOTAL LOADS", "$totalLoads", "#3B82F6"),
            Triple("AVG CYCLE", String.format(Locale.US, "%.1f m", avgCycle), "#10B981"),
            Triple("WAIT RATIO", String.format(Locale.US, "%.1f %%", waitPercent), "#F59E0B"),
            Triple("PROD SCORE", String.format(Locale.US, "%.1f %%", prodScore), "#10B981")
        )

        for (i in kpiData.indices) {
            val left = boxPadding + i * (boxWidth + boxPadding)
            val top = boxY
            val right = left + boxWidth
            val bottom = top + boxHeight

            // Box shadow/border
            paintRect.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(RectF(left, top, right, bottom), 6f, 6f, paintRect)
            
            // Draw colorful indicator tab
            paintRect.color = Color.parseColor(kpiData[i].third)
            canvas.drawRoundRect(RectF(left, top, left + 4f, bottom), 6f, 6f, paintRect)

            paintText.apply {
                color = Color.parseColor("#475569")
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(kpiData[i].first, left + 10f, top + 18f, paintText)

            paintText.apply {
                color = Color.parseColor("#0F172A")
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(kpiData[i].second, left + 10f, top + 42f, paintText)
        }

        // 4. CHART: CYCLE STAGE BREAKDOWN
        paintText.apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("STAGE ANALYSIS (MINUTES)", boxPadding, 210f, paintText)

        // Draw an elegant horizontal stacked bar chart
        val avgQload = if (totalLoads > 0) totalQueueLoad / totalLoads else 0.0
        val avgLoad = if (totalLoads > 0) totalLoadTime / totalLoads else 0.0
        val avgTravel = if (totalLoads > 0) totalTravel / totalLoads else 0.0
        val avgQunload = if (totalLoads > 0) totalQueueUnload / totalLoads else 0.0

        val totalAvgTime = avgQload + avgLoad + avgTravel + avgQunload
        val chartY = 230f
        val chartHeight = 25f
        val maxChartWidth = 563f // 595 - 32

        if (totalAvgTime > 0) {
            val wQload = (avgQload / totalAvgTime * maxChartWidth).toFloat()
            val wLoad = (avgLoad / totalAvgTime * maxChartWidth).toFloat()
            val wTravel = (avgTravel / totalAvgTime * maxChartWidth).toFloat()
            val wQunload = (avgQunload / totalAvgTime * maxChartWidth).toFloat()

            var currentLeft = boxPadding
            
            // Loading Queue (Amber)
            paintRect.color = Color.parseColor("#F59E0B")
            canvas.drawRect(currentLeft, chartY, currentLeft + wQload, chartY + chartHeight, paintRect)
            currentLeft += wQload

            // Loading (Blue)
            paintRect.color = Color.parseColor("#3B82F6")
            canvas.drawRect(currentLeft, chartY, currentLeft + wLoad, chartY + chartHeight, paintRect)
            currentLeft += wLoad

            // Travel (Teal/Emerald)
            paintRect.color = Color.parseColor("#10B981")
            canvas.drawRect(currentLeft, chartY, currentLeft + wTravel, chartY + chartHeight, paintRect)
            currentLeft += wTravel

            // Unloading Queue (Red/Orange)
            paintRect.color = Color.parseColor("#EF4444")
            canvas.drawRect(currentLeft, chartY, currentLeft + wQunload, chartY + chartHeight, paintRect)
        }

        // Legend
        val legendY = 275f
        val legendItems = listOf(
            Pair("Load Queue", "#F59E0B"),
            Pair("At Machine", "#3B82F6"),
            Pair("Travel To Dump", "#10B981"),
            Pair("Unload Queue", "#EF4444")
        )

        for (i in legendItems.indices) {
            val itemX = boxPadding + i * 140f
            paintRect.color = Color.parseColor(legendItems[i].second)
            canvas.drawRect(itemX, legendY - 6f, itemX + 10f, legendY + 4f, paintRect)

            paintText.apply {
                color = Color.parseColor("#334155")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText(legendItems[i].first, itemX + 15f, legendY, paintText)
        }

        // 5. DRIVER RANKING TABLE
        canvas.drawText("DRIVER PERFORMANCE RANKINGS", boxPadding, 315f, paintText.apply { textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

        val driverStats = cycles.groupBy { it.driverId }.map { (driverId, dCycles) ->
            val dName = drivers.find { it.id == driverId }?.name ?: driverId
            val dLoads = dCycles.size
            val dAvgC = dCycles.map { it.cycleTimeMinutes }.average()
            val dWait = dCycles.sumOf { 
                ((it.loadingStartMillis - it.queueStartMillis) + (it.unloadMillis - it.unloadingQueueStartMillis)) / 60000.0
            }
            val dTotalC = dCycles.sumOf { it.cycleTimeMinutes }
            val dWaitP = if (dTotalC > 0) (dWait / dTotalC) * 100 else 0.0
            val dScore = 100.0 - dWaitP
            DriverRow(dName, dLoads, dAvgC, dScore)
        }.sortedByDescending { it.score }

        // Table headers
        val headerY = 335f
        paintRect.color = Color.parseColor("#1E293B")
        canvas.drawRect(boxPadding, headerY - 14f, 595f - boxPadding, headerY + 6f, paintRect)

        paintText.apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("RANK / DRIVER", boxPadding + 10f, headerY, paintText)
        canvas.drawText("LOADS", boxPadding + 220f, headerY, paintText)
        canvas.drawText("AVG CYCLE", boxPadding + 320f, headerY, paintText)
        canvas.drawText("PRODUCTIVITY", boxPadding + 440f, headerY, paintText)

        var rowY = headerY + 22f
        for (idx in driverStats.indices) {
            if (idx >= 6) break // Limit to top 6 in PDF page
            val row = driverStats[idx]

            // Alternate row background
            if (idx % 2 == 1) {
                paintRect.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(boxPadding, rowY - 14f, 595f - boxPadding, rowY + 6f, paintRect)
            }

            paintText.apply { color = Color.parseColor("#0F172A"); typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
            canvas.drawText("#${idx + 1} - ${row.name}", boxPadding + 10f, rowY, paintText)
            canvas.drawText("${row.loads}", boxPadding + 220f, rowY, paintText)
            canvas.drawText(String.format(Locale.US, "%.1f min", row.avgCycle), boxPadding + 320f, rowY, paintText)
            
            // Score colored badge
            paintText.color = if (row.score >= 80) Color.parseColor("#10B981") else Color.parseColor("#F59E0B")
            canvas.drawText(String.format(Locale.US, "%.1f %%", row.score), boxPadding + 440f, rowY, paintText)

            rowY += 20f
        }

        // 6. TRUCK PERFORMANCE RANKINGS
        canvas.drawText("TRUCK UTILIZATION RANKINGS", boxPadding, 495f, paintText.apply { color = Color.parseColor("#0F172A"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

        val truckStats = cycles.groupBy { it.truckId }.map { (truckId, tCycles) ->
            val tModel = trucks.find { it.id == truckId }?.model ?: "HD785"
            val tLoads = tCycles.size
            val tAvgC = tCycles.map { it.cycleTimeMinutes }.average()
            val tWait = tCycles.sumOf { 
                ((it.loadingStartMillis - it.queueStartMillis) + (it.unloadMillis - it.unloadingQueueStartMillis)) / 60000.0
            }
            val tTotalC = tCycles.sumOf { it.cycleTimeMinutes }
            val tWaitP = if (tTotalC > 0) (tWait / tTotalC) * 100 else 0.0
            val tScore = 100.0 - tWaitP
            TruckRow(truckId, tModel, tLoads, tAvgC, tScore)
        }.sortedByDescending { it.score }

        val tHeaderY = 515f
        paintRect.color = Color.parseColor("#1E293B")
        canvas.drawRect(boxPadding, tHeaderY - 14f, 595f - boxPadding, tHeaderY + 6f, paintRect)

        paintText.apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("TRUCK ID / MODEL", boxPadding + 10f, tHeaderY, paintText)
        canvas.drawText("LOADS", boxPadding + 220f, tHeaderY, paintText)
        canvas.drawText("AVG CYCLE", boxPadding + 320f, tHeaderY, paintText)
        canvas.drawText("EFFICIENCY", boxPadding + 440f, tHeaderY, paintText)

        var trowY = tHeaderY + 22f
        for (idx in truckStats.indices) {
            if (idx >= 6) break
            val row = truckStats[idx]

            if (idx % 2 == 1) {
                paintRect.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(boxPadding, trowY - 14f, 595f - boxPadding, trowY + 6f, paintRect)
            }

            paintText.apply { color = Color.parseColor("#0F172A"); typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
            canvas.drawText("${row.id} (${row.model})", boxPadding + 10f, trowY, paintText)
            canvas.drawText("${row.loads}", boxPadding + 220f, trowY, paintText)
            canvas.drawText(String.format(Locale.US, "%.1f min", row.avgCycle), boxPadding + 320f, trowY, paintText)
            
            paintText.color = if (row.score >= 80) Color.parseColor("#10B981") else Color.parseColor("#F59E0B")
            canvas.drawText(String.format(Locale.US, "%.1f %%", row.score), boxPadding + 440f, trowY, paintText)

            trowY += 20f
        }

        // Page footer
        paintText.apply { color = Color.parseColor("#94A3B8"); textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        canvas.drawText("CONFIDENTIAL - APEX OPERATIONS SYSTEM | Page 1 of 2", boxPadding, 810f, paintText)
    }

    private fun drawPage2(
        canvas: Canvas,
        cycles: List<TruckCycle>,
        drivers: List<Driver>,
        trucks: List<Truck>,
        machines: List<Machine>,
        shifts: List<Shift>
    ) {
        val paintText = Paint().apply { isAntiAlias = true }
        val paintRect = Paint().apply { isAntiAlias = true }
        val boxPadding = 16f

        // Header Title
        paintRect.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, 595f, 50f, paintRect)

        paintText.apply { color = Color.WHITE; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("APEX MINING & LOGISTICS - CHRONOLOGICAL LOAD SHEET", boxPadding, 30f, paintText)

        // Machine Analysis Section
        canvas.drawText("MACHINE (LOADER) PRODUCTION ANALYSIS", boxPadding, 85f, paintText.apply { color = Color.parseColor("#0F172A"); textSize = 11f })

        val machineStats = cycles.groupBy { it.machineId }.map { (machineId, mCycles) ->
            val mName = machines.find { it.id == machineId }?.name ?: machineId
            val mTrucksServed = mCycles.map { it.truckId }.distinct().size
            val mAvgLoadTime = mCycles.map { (it.loadingEndMillis - it.loadingStartMillis) / 60000.0 }.average()
            val mAvgQueue = mCycles.map { (it.loadingStartMillis - it.queueStartMillis) / 60000.0 }.average()
            val mProduction = mCycles.size
            MachineRow(mName, mTrucksServed, mAvgLoadTime, mAvgQueue, mProduction)
        }

        val mHeaderY = 105f
        paintRect.color = Color.parseColor("#1E293B")
        canvas.drawRect(boxPadding, mHeaderY - 14f, 595f - boxPadding, mHeaderY + 6f, paintRect)

        paintText.apply { color = Color.WHITE; textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("MACHINE NAME / CODE", boxPadding + 10f, mHeaderY, paintText)
        canvas.drawText("TRUCKS SERVED", boxPadding + 220f, mHeaderY, paintText)
        canvas.drawText("AVG LOAD TIME", boxPadding + 320f, mHeaderY, paintText)
        canvas.drawText("AVG QUEUE", boxPadding + 420f, mHeaderY, paintText)
        canvas.drawText("TOTAL LOADS", boxPadding + 500f, mHeaderY, paintText)

        var mrowY = mHeaderY + 20f
        for (idx in machineStats.indices) {
            val row = machineStats[idx]
            if (idx % 2 == 1) {
                paintRect.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(boxPadding, mrowY - 14f, 595f - boxPadding, mrowY + 6f, paintRect)
            }
            paintText.apply { color = Color.parseColor("#0F172A"); typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
            canvas.drawText(row.name, boxPadding + 10f, mrowY, paintText)
            canvas.drawText("${row.trucksServed}", boxPadding + 220f, mrowY, paintText)
            canvas.drawText(String.format(Locale.US, "%.1f min", row.avgLoadTime), boxPadding + 320f, mrowY, paintText)
            canvas.drawText(String.format(Locale.US, "%.1f min", row.avgQueue), boxPadding + 420f, mrowY, paintText)
            canvas.drawText("${row.production}", boxPadding + 500f, mrowY, paintText)

            mrowY += 18f
        }

        // Timeline Section
        canvas.drawText("DETAILED CHRONOLOGICAL LOAD TIMELINE", boxPadding, mrowY + 25f, paintText.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 11f })

        val timelineHeaderY = mrowY + 45f
        paintRect.color = Color.parseColor("#1E293B")
        canvas.drawRect(boxPadding, timelineHeaderY - 14f, 595f - boxPadding, timelineHeaderY + 6f, paintRect)

        paintText.apply { color = Color.WHITE; textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("LOAD ID", boxPadding + 8f, timelineHeaderY, paintText)
        canvas.drawText("TRUCK (DRIVER)", boxPadding + 65f, timelineHeaderY, paintText)
        canvas.drawText("MACHINE", boxPadding + 185f, timelineHeaderY, paintText)
        canvas.drawText("Q-START", boxPadding + 275f, timelineHeaderY, paintText)
        canvas.drawText("L-START", boxPadding + 325f, timelineHeaderY, paintText)
        canvas.drawText("L-FINISH", boxPadding + 375f, timelineHeaderY, paintText)
        canvas.drawText("UNLOAD", boxPadding + 430f, timelineHeaderY, paintText)
        canvas.drawText("CYCLE", boxPadding + 495f, timelineHeaderY, paintText)

        var tRowY = timelineHeaderY + 20f
        // Show up to 18 rows in chronology
        val sortedCycles = cycles.sortedBy { it.unloadMillis }
        for (idx in sortedCycles.indices) {
            if (idx >= 15) break // Ensure no page overflow!
            val cycle = sortedCycles[idx]
            val driverName = drivers.find { it.id == cycle.driverId }?.name?.split(" ")?.firstOrNull() ?: cycle.driverId
            val mCode = machines.find { it.id == cycle.machineId }?.name?.split(" ")?.firstOrNull() ?: cycle.machineId

            if (idx % 2 == 1) {
                paintRect.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(boxPadding, tRowY - 14f, 595f - boxPadding, tRowY + 6f, paintRect)
            }

            paintText.apply { color = Color.parseColor("#0F172A"); typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
            canvas.drawText(String.format(Locale.US, "#%02d", idx + 1), boxPadding + 8f, tRowY, paintText)
            canvas.drawText("${cycle.truckId} ($driverName)", boxPadding + 65f, tRowY, paintText)
            canvas.drawText(mCode, boxPadding + 185f, tRowY, paintText)

            paintText.color = Color.parseColor("#475569")
            canvas.drawText(timeFormatter.format(Date(cycle.queueStartMillis)), boxPadding + 275f, tRowY, paintText)
            canvas.drawText(timeFormatter.format(Date(cycle.loadingStartMillis)), boxPadding + 325f, tRowY, paintText)
            canvas.drawText(timeFormatter.format(Date(cycle.loadingEndMillis)), boxPadding + 375f, tRowY, paintText)
            canvas.drawText(timeFormatter.format(Date(cycle.unloadMillis)), boxPadding + 430f, tRowY, paintText)

            paintText.apply { color = Color.parseColor("#0F172A"); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            canvas.drawText(String.format(Locale.US, "%.1fm", cycle.cycleTimeMinutes), boxPadding + 495f, tRowY, paintText)

            tRowY += 18f
        }

        // Supervisor sign off area
        val signY = 720f
        paintText.apply { color = Color.parseColor("#475569"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        canvas.drawText("REPORT CERTIFICATION", boxPadding, signY - 20f, paintText.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.parseColor("#0F172A") })
        canvas.drawLine(boxPadding, signY - 10f, 595f - boxPadding, signY - 10f, Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 1f })

        canvas.drawText("The data provided on this chronological and analytical dashboard represents verified automated and manual fleet tracking records.", boxPadding, signY + 10f, paintText.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) })

        val col1X = boxPadding + 20f
        val col2X = 350f
        val lineY = signY + 65f

        canvas.drawLine(col1X, lineY, col1X + 180f, lineY, Paint().apply { color = Color.parseColor("#475569"); strokeWidth = 1f })
        canvas.drawText("AUTHORIZED SUPERVISOR SIGNATURE", col1X, lineY + 14f, paintText.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) })

        canvas.drawLine(col2X, lineY, col2X + 180f, lineY, Paint().apply { color = Color.parseColor("#475569"); strokeWidth = 1f })
        canvas.drawText("DATE OF SIGN-OFF / APPROVAL", col2X, lineY + 14f, paintText)

        // Page footer
        canvas.drawText("CONFIDENTIAL - APEX OPERATIONS SYSTEM | Page 2 of 2", boxPadding, 810f, paintText)
    }

    private fun sharePdf(context: Context, pdfFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val pdfUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "Apex Fleet Productivity Report - ${dateFormatter.format(Date())}")
            putExtra(Intent.EXTRA_TEXT, "Attached please find the comprehensive APEX Fleet Productivity and Load Audit Report.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Professional Fleet PDF Report"))
    }

    // Data structures for helper sorting inside drawing
    private data class DriverRow(val name: String, val loads: Int, val avgCycle: Double, val score: Double)
    private data class TruckRow(val id: String, val model: String, val loads: Int, val avgCycle: Double, val score: Double)
    private data class MachineRow(val name: String, val trucksServed: Int, val avgLoadTime: Double, val avgQueue: Double, val production: Int)
}

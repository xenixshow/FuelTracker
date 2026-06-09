package com.fuelapp.ui

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import com.fuelapp.data.FuelRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ShareUtils {

    /**
     * 生成统计分享图片并触发分享 Intent
     */
    fun shareStats(context: Context, records: List<FuelRecord>) {
        val sorted = records.sortedBy { it.date }
        val totalMileage = if (sorted.size >= 2) sorted.last().mileage - sorted.first().mileage else 0.0
        val totalVolume = records.sumOf { it.fuelVolume }
        val totalCost = records.sumOf { it.totalCost }
        val avgConsumption = if (totalMileage > 0 && totalVolume > 0) (totalVolume / totalMileage) * 100 else 0.0
        val avgCostPerKm = if (totalMileage > 0) totalCost / totalMileage else 0.0
        val totalRecords = records.size

        val width = 1080
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val density = context.resources.displayMetrics.density

        // === 背景 ===
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1C1B1F") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // === 标题 ===
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4CAF50")
            textSize = 48f * density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⛽ 油耗数据报告", width / 2f, 120f * density, titlePaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 24f * density
            textAlign = Paint.Align.CENTER
        }
        val today = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(Date())
        canvas.drawText("生成于 $today", width / 2f, 170f * density, datePaint)

        // === 分隔线 ===
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#333333")
            strokeWidth = 2f
        }
        canvas.drawLine(80f * density, 200f * density, (width - 80) * density, 200f * density, linePaint)

        // === 核心指标 - 平均油耗 & 总里程 ===
        val bigValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 80f * density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 28f * density
            textAlign = Paint.Align.CENTER
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#888888")
            textSize = 26f * density
            textAlign = Paint.Align.CENTER
        }

        // 平均油耗
        canvas.drawText(
            if (avgConsumption > 0) String.format("%.1f", avgConsumption) else "-",
            width * 0.3f, 310f * density, bigValuePaint
        )
        canvas.drawText("L/100km", width * 0.3f, 360f * density, unitPaint)
        canvas.drawText("平均油耗", width * 0.3f, 400f * density, labelPaint)

        // 总里程
        canvas.drawText(
            if (totalMileage > 0) String.format("%.0f", totalMileage) else "-",
            width * 0.7f, 310f * density, bigValuePaint
        )
        canvas.drawText("km", width * 0.7f, 360f * density, unitPaint)
        canvas.drawText("总行驶里程", width * 0.7f, 400f * density, labelPaint)

        // === 第二行分隔 ===
        canvas.drawLine(80f * density, 440f * density, (width - 80) * density, 440f * density, linePaint)

        // === 次要指标 ===
        val midValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f * density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val midUnitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 22f * density
            textAlign = Paint.Align.CENTER
        }

        // 三列：总加油量 / 总费用 / 每公里成本
        val cols = listOf(
            Triple(String.format("%.1f", totalVolume), "L", "总加油量"),
            Triple(String.format("%.0f", totalCost), "元", "总花费"),
            Triple(if (avgCostPerKm > 0) String.format("%.2f", avgCostPerKm) else "-", "元/km", "每公里成本")
        )
        cols.forEachIndexed { i, (value, unit, label) ->
            val x = width * (0.17f + i * 0.33f)
            canvas.drawText(value, x, 530f * density, midValuePaint)
            canvas.drawText(unit, x, 570f * density, midUnitPaint)
            canvas.drawText(label, x, 610f * density, labelPaint)
        }

        // === 油价信息 ===
        canvas.drawLine(80f * density, 650f * density, (width - 80) * density, 650f * density, linePaint)

        val prices = records.map { it.fuelPrice }
        val avgPrice = if (prices.isNotEmpty()) prices.average() else 0.0
        val latestPrice = prices.lastOrNull() ?: 0.0

        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCCCCC")
            textSize = 28f * density
            textAlign = Paint.Align.LEFT
        }

        canvas.drawText("记录总数: $totalRecords 次", 80f * density, 720f * density, infoPaint)
        canvas.drawText("当前油价: ${String.format("%.2f", latestPrice)} 元/L", 80f * density, 770f * density, infoPaint)
        canvas.drawText("平均油价: ${String.format("%.2f", avgPrice)} 元/L", 80f * density, 820f * density, infoPaint)

        val firstDate = sorted.firstOrNull()?.date ?: "-"
        val lastDateStr = sorted.lastOrNull()?.date ?: "-"
        canvas.drawText("统计区间: $firstDate ~ $lastDateStr", 80f * density, 870f * density, infoPaint)

        // === 底部说明 ===
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#555555")
            textSize = 22f * density
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("由「油耗记录」App 生成", width / 2f, height - 80f * density, footerPaint)

        // === 保存并分享 ===
        try {
            val dir = File(context.cacheDir, "share")
            dir.mkdirs()
            val file = File(dir, "fuel_stats_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "分享油耗报告"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

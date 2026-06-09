package com.fuelapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fuelapp.data.FuelRecord
import com.fuelapp.data.MonthlyStat
import com.fuelapp.ui.theme.*
import com.fuelapp.ui.viewmodel.FuelViewModel
import kotlin.math.roundToInt

// ==================== 自定义折线图 ====================

@Composable
fun LineChart(
    data: List<Double>,
    labels: List<String>,
    lineColor: Color,
    fillColor: Color,
    formatValue: (Double) -> String
) {
    if (data.isEmpty()) return

    val minVal = (data.minOrNull() ?: 0.0) * 0.85
    val maxVal = (data.maxOrNull() ?: 1.0) * 1.15
    val range = if (maxVal - minVal < 0.01) 1.0 else maxVal - minVal

    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 28f
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val chartLeft = 50f
        val chartBottom = size.height - 30f
        val chartTop = 10f
        val chartRight = size.width - 10f
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        val stepX = chartWidth / (data.size - 1).coerceAtLeast(1)

        for (i in 0..4) {
            val y = chartBottom - (chartHeight * i / 4)
            drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(chartLeft, y), Offset(chartRight, y), 1f)
            val labelVal = minVal + (range * i / 4)
            drawContext.canvas.nativeCanvas.drawText(String.format("%.1f", labelVal), 2f, y + 5f, textPaint)
        }

        if (data.size >= 2) {
            val path = Path()
            data.forEachIndexed { index, value ->
                val x = chartLeft + stepX * index
                val y = chartBottom - ((value - minVal) / range * chartHeight).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.lineTo(chartLeft + stepX * (data.size - 1), chartBottom)
            path.lineTo(chartLeft, chartBottom)
            path.close()
            drawPath(path, fillColor)
        }

        data.forEachIndexed { index, value ->
            val x = chartLeft + stepX * index
            val y = chartBottom - ((value - minVal) / range * chartHeight).toFloat()
            if (index > 0) {
                val prevX = chartLeft + stepX * (index - 1)
                val prevY = chartBottom - ((data[index - 1] - minVal) / range * chartHeight).toFloat()
                drawLine(lineColor, Offset(prevX, prevY), Offset(x, y), 3f, cap = StrokeCap.Round)
            }
        }

        data.forEachIndexed { index, value ->
            val x = chartLeft + stepX * index
            val y = chartBottom - ((value - minVal) / range * chartHeight).toFloat()
            drawCircle(Color.White, 6f, Offset(x, y))
            drawCircle(lineColor, 4f, Offset(x, y))
            // 在节点上方显示数值
            val label = formatValue(value)
            drawContext.canvas.nativeCanvas.drawText(label, x - label.length * 7f, y - 10f, textPaint.apply {
                color = android.graphics.Color.DKGRAY
                textSize = 24f
            })
        }
        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 28f

        val labelIndices = when {
            labels.size <= 3 -> labels.indices.toList()
            else -> listOf(0, labels.size / 2, labels.size - 1)
        }
        labelIndices.forEach { i ->
            if (i < labels.size) {
                val x = chartLeft + stepX * i
                drawContext.canvas.nativeCanvas.drawText(labels[i], x - 15f, size.height - 5f, textPaint)
            }
        }
    }
}

// ==================== 自定义柱状图 ====================

@Composable
fun BarChart(
    data: List<Double>,
    labels: List<String>,
    barColor: Color,
    barColorTop: Color,
    formatValue: (Double) -> String
) {
    if (data.isEmpty()) return

    val maxVal = (data.maxOrNull() ?: 1.0) * 1.2
    val minVal = 0.0
    val range = if (maxVal <= 0) 1.0 else maxVal

    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 24f
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val chartLeft = 50f; val chartBottom = size.height - 30f
        val chartTop = 10f; val chartRight = size.width - 10f
        val chartWidth = chartRight - chartLeft; val chartHeight = chartBottom - chartTop

        val barCount = data.size
        val barSpacing = chartWidth / (barCount * 2f)
        val barWidth = chartWidth / (barCount * 2.5f)

        for (i in 0..4) {
            val y = chartBottom - (chartHeight * i / 4)
            drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(chartLeft, y), Offset(chartRight, y), 1f)
            drawContext.canvas.nativeCanvas.drawText(formatLabelV(minVal + range * i / 4), 2f, y + 5f, textPaint)
        }

        data.forEachIndexed { index, value ->
            val barH = ((value / range) * chartHeight).toFloat()
            val x = chartLeft + barSpacing + index * (barWidth + barSpacing)
            val y = chartBottom - barH
            val gradient = Brush.verticalGradient(listOf(barColorTop, barColor), y, chartBottom)
            drawRect(gradient, Offset(x, y), Size(barWidth, barH), style = Fill)
            drawRect(barColor.copy(alpha = 0.5f), Offset(x, y), Size(barWidth, barH), style = Stroke(1f))
            // 柱顶显示数值
            val label = formatValue(value)
            drawContext.canvas.nativeCanvas.drawText(label, x + barWidth / 2 - label.length * 7f, y - 8f, textPaint.apply {
                color = android.graphics.Color.DKGRAY
                textSize = 24f
            })
            if (barCount <= 6 || index % 2 == 0) {
                drawContext.canvas.nativeCanvas.drawText(labels.getOrElse(index) { "" }, x + barWidth / 2 - 12f, size.height - 5f, textPaint)
            }
        }
        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 28f
    }
}

private fun formatLabelV(value: Double): String = when {
    value >= 10000 -> String.format("%.0fk", value / 1000)
    value >= 1000 -> String.format("%.1fk", value / 1000)
    value >= 100 -> String.format("%.0f", value)
    value >= 1 -> String.format("%.1f", value)
    else -> String.format("%.2f", value)
}

// ==================== 油耗图表 ====================

@Composable
fun ConsumptionChart(
    consumptionData: List<FuelViewModel.ConsumptionData>,
    allRecords: List<FuelRecord>
) {
    ChartCard(title = "⛽ 百公里油耗走势", unit = "单位：L/100km") {
        if (consumptionData.isEmpty()) {
            ChartEmpty("需要至少2条记录才能计算油耗")
        } else {
            val displayData = consumptionData.takeLast(10)
            Box(modifier = Modifier.height(200.dp)) {
                LineChart(displayData.map { it.consumption },
                    displayData.map { it.date.substringAfterLast("-") },
                    ChartGreen, ChartGreen.copy(alpha = 0.15f),
                    { String.format("%.1f", it) })
            }
            Spacer(modifier = Modifier.height(8.dp))
            val avg = consumptionData.map { it.consumption }.average()
            Text("平均油耗：${String.format("%.1f", avg)} L/100km",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ==================== 费用图表 ====================

@Composable
fun CostChart(monthlyStats: List<MonthlyStat>, allRecords: List<FuelRecord>) {
    ChartCard(title = "💰 月度加油费用", unit = "单位：元") {
        if (monthlyStats.isEmpty()) ChartEmpty("暂无月度统计数据")
        else {
            val display = monthlyStats.takeLast(12)
            Box(modifier = Modifier.height(200.dp)) {
                BarChart(display.map { it.totalCost },
                    display.map { it.month.substringAfterLast("-") + "月" },
                    ChartOrange, ChartRed.copy(alpha = 0.7f),
                    { String.format("%.0f 元", it) })
            }
        }
    }
}

// ==================== 加油量图表 ====================

@Composable
fun VolumeChart(monthlyStats: List<MonthlyStat>, allRecords: List<FuelRecord>) {
    ChartCard(title = "🛢️ 月度加油量", unit = "单位：升 (L)") {
        if (monthlyStats.isEmpty()) ChartEmpty("暂无月度统计数据")
        else {
            val display = monthlyStats.takeLast(12)
            Box(modifier = Modifier.height(200.dp)) {
                BarChart(display.map { it.totalVolume },
                    display.map { it.month.substringAfterLast("-") + "月" },
                    ChartBlue, ChartBlue.copy(alpha = 0.7f),
                    { String.format("%.1f L", it) })
            }
        }
    }
}

// ==================== 油价趋势图表 ====================

@Composable
fun PriceChart(priceData: List<FuelViewModel.FuelPricePoint>, allRecords: List<FuelRecord>) {
    ChartCard(title = "💲 油价趋势", unit = "单位：元/升") {
        if (priceData.size < 2) ChartEmpty("需要至少2条记录才能显示油价趋势")
        else {
            val displayData = priceData.takeLast(20)
            Box(modifier = Modifier.height(200.dp)) {
                LineChart(displayData.map { it.price },
                    displayData.map { it.date.substringAfterLast("-") },
                    ChartPurple, ChartPurple.copy(alpha = 0.15f),
                    { String.format("%.2f", it) })
            }
            Spacer(modifier = Modifier.height(12.dp))
            val prices = priceData.map { it.price }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PriceStatItem("最高", String.format("%.2f", prices.maxOrNull() ?: 0.0), ChartRed)
                PriceStatItem("最低", String.format("%.2f", prices.minOrNull() ?: 0.0), ChartGreen)
                PriceStatItem("平均", String.format("%.2f", prices.average()), ChartBlue)
                val change = (prices.lastOrNull() ?: 0.0) - (prices.firstOrNull() ?: 0.0)
                PriceStatItem("变动", if (change >= 0) "+${String.format("%.2f", change)}" else String.format("%.2f", change),
                    if (change >= 0) ChartRed else ChartGreen)
            }
            Text("当前油价 ${String.format("%.2f", prices.lastOrNull() ?: 0.0)} 元/L | 记录跨度 ${priceData.size} 次加油",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun PriceStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ==================== 核心指标卡片 ====================

@Composable
fun KeyMetricsCard(records: List<FuelRecord>) {
    val sorted = records.sortedBy { it.date }
    val totalMileage = if (sorted.size >= 2) sorted.last().mileage - sorted.first().mileage else 0.0
    val totalVolume = records.sumOf { it.fuelVolume }
    val totalCost = records.sumOf { it.totalCost }
    val avgConsumption = if (totalMileage > 0 && totalVolume > 0) (totalVolume / totalMileage) * 100 else 0.0
    val avgCostPerKm = if (totalMileage > 0) totalCost / totalMileage else 0.0

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🏆 核心指标", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("共 ${records.size} 条记录", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MetricCardItem(if (avgConsumption > 0) String.format("%.1f", avgConsumption) else "-", "L/100km", "平均油耗", true)
                MetricCardItem(if (totalMileage > 0) String.format("%.0f", totalMileage) else "-", "km", "总行驶里程", true)
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MetricCardItem(String.format("%.1f", totalVolume), "L", "总加油量", false)
                MetricCardItem(String.format("%.0f", totalCost), "元", "总花费", false)
                MetricCardItem(if (avgCostPerKm > 0) String.format("%.2f", avgCostPerKm) else "-", "元/km", "每公里成本", false)
            }
        }
    }
}

@Composable
private fun MetricCardItem(value: String, unit: String, label: String, large: Boolean) {
    val color = MaterialTheme.colorScheme.onPrimaryContainer
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = if (large) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.width(2.dp))
            Text(unit, style = if (large) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = if (large) 8.dp else 2.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.6f))
    }
}

// ==================== 月度统计表格 ====================

@Composable
fun MonthlyStatsTable(monthlyStats: List<MonthlyStat>) {
    if (monthlyStats.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📋 月度明细", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 12.dp)) {
                Text("月份", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("加油量", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("费用", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("均油价", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            monthlyStats.takeLast(12).forEachIndexed { index, stat ->
                val bg = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                Row(Modifier.fillMaxWidth().background(bg).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stat.month, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(String.format("%.1f L", stat.totalVolume), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(String.format("%.0f 元", stat.totalCost), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(String.format("%.2f", stat.avgPrice), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ==================== 总体统计 ====================

@Composable
fun OverallStats(records: List<FuelRecord>) {
    if (records.isEmpty()) return
    val sorted = records.sortedBy { it.date }
    val totalMileage = if (sorted.size >= 2) sorted.last().mileage - sorted.first().mileage else 0.0
    val totalVolume = records.sumOf { it.fuelVolume }
    val totalCost = records.sumOf { it.totalCost }
    val avgConsumption = if (totalMileage > 0 && totalVolume > 0) (totalVolume / totalMileage) * 100 else 0.0
    val avgCostPerKm = if (totalMileage > 0) totalCost / totalMileage else 0.0

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📊 总体统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem2("记录总数", "${records.size} 次")
                StatItem2("总加油量", String.format("%.1f L", totalVolume))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem2("总费用", String.format("%.2f 元", totalCost))
                StatItem2("总里程", String.format("%.0f km", totalMileage))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem2("平均油耗", if (avgConsumption > 0) String.format("%.1f L/100km", avgConsumption) else "-")
                StatItem2("每公里成本", if (avgCostPerKm > 0) String.format("%.2f 元", avgCostPerKm) else "-")
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem2("最早记录", sorted.first().date)
                StatItem2("最近记录", sorted.last().date)
            }
        }
    }
}

@Composable
private fun StatItem2(label: String, value: String) {
    val color = MaterialTheme.colorScheme.onTertiaryContainer
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
    }
}

// ==================== 通用组件 ====================

@Composable
fun ChartCard(title: String, unit: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ChartEmpty(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(vertical = 32.dp))
}

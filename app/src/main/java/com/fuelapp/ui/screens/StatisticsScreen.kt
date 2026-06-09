package com.fuelapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuelapp.ui.ShareUtils
import com.fuelapp.ui.viewmodel.FuelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: FuelViewModel) {
    val allRecords by viewModel.allRecordsAsc.collectAsStateWithLifecycle()
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val consumptionData by viewModel.consumptionData.collectAsStateWithLifecycle()
    val fuelPriceData by viewModel.fuelPriceData.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    var selectedChart by remember { mutableStateOf("油耗") }
    val chartOptions = listOf("油耗", "费用", "加油量", "油价")
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
    ) {
        // 标题 + 分享按钮
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📊 数据总览", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { ShareUtils.shareStats(context, allRecords) }) {
                Icon(Icons.Default.Share, contentDescription = "分享", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (allRecords.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    Text("暂无数据可生成图表", color = MaterialTheme.colorScheme.outline)
                }
            }
            return
        }

        // 核心指标卡片
        KeyMetricsCard(allRecords)
        Spacer(Modifier.height(12.dp))

        // 图表类型切换
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chartOptions.forEach { option ->
                FilterChip(selected = selectedChart == option, onClick = { selectedChart = option }, label = { Text(option) })
            }
        }
        Spacer(Modifier.height(16.dp))

        when (selectedChart) {
            "油耗" -> ConsumptionChart(consumptionData, allRecords)
            "费用" -> CostChart(monthlyStats, allRecords)
            "加油量" -> VolumeChart(monthlyStats, allRecords)
            "油价" -> PriceChart(fuelPriceData, allRecords)
        }

        Spacer(Modifier.height(16.dp))
        MonthlyStatsTable(monthlyStats)
        Spacer(Modifier.height(16.dp))
        OverallStats(allRecords)
        Spacer(Modifier.height(32.dp))
    }
}

package com.fuelapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuelapp.data.FuelRecord
import com.fuelapp.ui.viewmodel.FuelViewModel
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: FuelViewModel) {
    val timeRanges = listOf("全部", "近一周", "近一月", "近三月", "近一年")
    var selectedRange by remember { mutableStateOf("全部") }
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<FuelRecord?>(null) }

    // 获取过滤后的记录
    val records by viewModel.getFilteredRecords(selectedRange)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val missedIds by viewModel.missedRecordIds.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // 时间范围筛选
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FilterList, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("时间：", style = MaterialTheme.typography.bodyMedium)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedRange,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .width(120.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        timeRanges.forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range) },
                                onClick = {
                                    selectedRange = range
                                    viewModel.updateSelectedTimeRange(range)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 导入 + 导出 按钮组
            val csvPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let { viewModel.importFromCsv(it) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // 导入按钮
                FilledTonalButton(
                    onClick = { csvPickerLauncher.launch("text/*") },
                    enabled = !viewModel.isExporting
                ) {
                    Icon(Icons.Default.Download, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导入", style = MaterialTheme.typography.labelMedium)
                }

                // 导出 CSV 按钮
                FilledTonalButton(
                    onClick = { viewModel.exportToCsv() },
                    enabled = !viewModel.isExporting
                ) {
                    if (viewModel.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Upload, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // 导出状态提示
        AnimatedVisibility(visible = viewModel.exportStatus.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.exportStatus.startsWith("✅"))
                        MaterialTheme.colorScheme.primaryContainer
                    else if (viewModel.exportStatus.startsWith("❌"))
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(viewModel.exportStatus,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { viewModel.clearExportStatus() }) {
                        Text("关闭", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (records.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "暂无加油记录",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "在「添加记录」页面添加第一条记录吧！",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // 统计概览卡片
            SummaryCard(records)
            Spacer(modifier = Modifier.height(4.dp))

            // 记录列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    RecordCard(
                        record = record,
                        isMissed = record.id in missedIds,
                        onEdit = { viewModel.editRecord(it) },
                        onDelete = { showDeleteDialog = it }
                    )
                }
            }
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { record ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除 ${if (record.time.isNotBlank()) "${record.date} ${record.time}" else record.date} 的加油记录吗？此操作不可恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecord(record.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SummaryCard(records: List<FuelRecord>) {
    val totalVolume = records.sumOf { it.fuelVolume }
    val totalCost = records.sumOf { it.totalCost }
    val totalMileage = if (records.size >= 2) {
        val sorted = records.sortedBy { it.date }
        sorted.last().mileage - sorted.first().mileage
    } else 0.0
    val avgConsumption = if (totalMileage > 0) (totalVolume / totalMileage) * 100 else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📊 统计概览", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("记录数", "${records.count()} 次", MaterialTheme.colorScheme.onPrimaryContainer)
                StatItem("总加油量", String.format("%.1f L", totalVolume), MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("总费用", String.format("%.2f 元", totalCost), MaterialTheme.colorScheme.onPrimaryContainer)
                StatItem("平均油耗", if (avgConsumption > 0) String.format("%.1f L/100km", avgConsumption) else "-",
                    MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordCard(
    record: FuelRecord,
    isMissed: Boolean = false,
    onEdit: (FuelRecord) -> Unit,
    onDelete: (FuelRecord) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalGasStation, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (record.time.isNotBlank()) "${record.date} ${record.time}"
                        else record.date,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isMissed) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Warning, contentDescription = "可能漏记",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp))
                    }
                }
                Text(String.format("%.2f 元", record.totalCost),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(Icons.Default.Speed, "${record.mileage.toInt()} km")
                InfoChip(Icons.Default.LocalGasStation, "${record.fuelVolume} L")
                InfoChip(Icons.Default.MonetizationOn, "${record.fuelPrice} 元/L")
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                if (record.notes.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notes, contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("备注: ${record.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onEdit(record) }) {
                        Icon(Icons.Default.Edit, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("编辑")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onDelete(record) }) {
                        Icon(Icons.Default.Delete, contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

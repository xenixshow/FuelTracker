package com.fuelapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuelapp.ui.components.VehicleManageDialog
import com.fuelapp.ui.components.VehicleSelector
import com.fuelapp.ui.viewmodel.CalculationMode
import com.fuelapp.ui.viewmodel.FuelViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(viewModel: FuelViewModel) {
    val scrollState = rememberScrollState()

    // 每次进入页面时检查上次记录
    LaunchedEffect(Unit) {
        viewModel.checkLastRecord()
    }

    // 漏记提醒对话框
    if (viewModel.showMissedReminder) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMissedReminder() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null,
                tint = MaterialTheme.colorScheme.error) },
            title = { Text("⚠️ 可能漏记了？") },
            text = {
                val last = viewModel.lastRecordInfo
                if (last != null) {
                    Text(
                        "上次加油（${last.date}）里程为 ${last.mileage.toInt()} km\n" +
                        "本次里程差已达 ${last.kmSince.toInt()} km，\n" +
                        "一箱油一般跑 200-350km，可能中间漏记了加油记录。\n\n" +
                        "建议先查看历史记录确认是否有遗漏。"
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissMissedReminder() }) {
                    Text("继续记录")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissMissedReminder()
                    viewModel.clearForm()
                }) {
                    Text("先不记录")
                }
            }
        )
    }

    // 车辆列表
    val vehicles by viewModel.allVehicles.collectAsStateWithLifecycle()
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    var showVehicleManage by remember { mutableStateOf(false) }

    // 车辆管理对话框
    if (showVehicleManage) {
        VehicleManageDialog(
            viewModel = viewModel,
            vehicles = vehicles,
            onDismiss = { showVehicleManage = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标题
        Text(
            text = if (viewModel.editingRecordId == null) "⛽ 添加加油记录" else "✏️ 编辑加油记录",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // 车辆选择器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VehicleSelector(
                viewModel = viewModel,
                vehicles = vehicles,
                expanded = vehicleDropdownExpanded,
                onExpandedChange = { vehicleDropdownExpanded = it },
                onManageClick = { showVehicleManage = true }
            )
            Spacer(modifier = Modifier)
        }

        // 上次加油提醒卡片
        val lastInfo = viewModel.lastRecordInfo
        if (lastInfo != null && viewModel.editingRecordId == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("上次加油记录",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(lastInfo.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("日期",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${lastInfo.mileage.toInt()} km",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("里程",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${lastInfo.daysSince} 天",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("距今",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        // 日期（点击弹出日期选择器）
        var showDatePicker by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = viewModel.date,
                onValueChange = {},
                readOnly = true,
                label = { Text("日期") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            // 透明覆盖层捕获点击（覆盖在 TextField 上方）
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = true) { showDatePicker = true }
            )
        }
        // DatePickerDialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    sdf.parse(viewModel.date)?.time
                } catch (e: Exception) { System.currentTimeMillis() }
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            viewModel.updateDate(sdf.format(Date(millis)))
                        }
                        showDatePicker = false
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // 时间（手动输入 HH:mm）
        OutlinedTextField(
            value = viewModel.time,
            onValueChange = { viewModel.updateTime(it) },
            label = { Text("时间") },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("HH:mm（如 14:30）") },
            supportingText = { Text("精确到分钟", style = MaterialTheme.typography.labelSmall) }
        )

        // 里程数
        OutlinedTextField(
            value = viewModel.mileage,
            onValueChange = { viewModel.updateMileage(it) },
            label = { Text("里程数 (km)") },
            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text("请输入当前里程表读数") },
            isError = viewModel.mileageError != null,
            supportingText = {
                viewModel.mileageError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        )

        // 加油量
        val isVolumeAuto = viewModel.calcMode == CalculationMode.AUTO_VOLUME
        OutlinedTextField(
            value = viewModel.fuelVolume,
            onValueChange = { viewModel.updateFuelVolume(it) },
            label = { Text("加油量 (升)") },
            leadingIcon = { Icon(Icons.Default.LocalGasStation, contentDescription = null) },
            trailingIcon = {
                Icon(
                    if (isVolumeAuto) Icons.Default.Lock else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isVolumeAuto) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text("输入加油量") },
            supportingText = {
                if (isVolumeAuto) {
                    Text("🔒 自动根据油价÷费用计算", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("✏️ 手动输入或由费用自动计算", style = MaterialTheme.typography.labelSmall)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isVolumeAuto) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = if (isVolumeAuto) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline
            )
        )

        // 油价
        OutlinedTextField(
            value = viewModel.fuelPrice,
            onValueChange = { viewModel.updateFuelPrice(it) },
            label = { Text("油价 (元/升)") },
            leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text("请输入当前油价") }
        )

        // 总费用
        val isCostAuto = viewModel.calcMode == CalculationMode.AUTO_COST
        OutlinedTextField(
            value = viewModel.totalCost,
            onValueChange = { viewModel.updateTotalCost(it) },
            label = { Text("加油费用 (元)") },
            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
            trailingIcon = {
                Icon(
                    if (isCostAuto) Icons.Default.Lock else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isCostAuto) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text("输入总费用") },
            supportingText = {
                when (viewModel.calcMode) {
                    CalculationMode.AUTO_COST -> {
                        Text("🔒 自动根据油价×加油量计算", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    CalculationMode.AUTO_VOLUME -> {
                        Text("由费用反算加油量中…", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                    CalculationMode.MANUAL -> {
                        Text("✏️ 手动输入或由加油量自动计算", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isCostAuto) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = if (isCostAuto) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline
            )
        )

        // 备注
        OutlinedTextField(
            value = viewModel.notes,
            onValueChange = { viewModel.updateNotes(it) },
            label = { Text("备注") },
            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            placeholder = { Text("可选：加油站名称、备注信息等") }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 保存按钮
        Button(
            onClick = { viewModel.saveRecord() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = viewModel.mileage.isNotBlank()
                    && viewModel.fuelVolume.isNotBlank()
                    && viewModel.fuelPrice.isNotBlank()
                    && viewModel.totalCost.isNotBlank()
                    && !viewModel.isSaving
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (viewModel.editingRecordId == null) "保存记录" else "更新记录",
                style = MaterialTheme.typography.titleMedium)
        }

        // 取消编辑
        if (viewModel.editingRecordId != null) {
            OutlinedButton(
                onClick = { viewModel.clearForm() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("取消编辑")
            }
        }
    }
}

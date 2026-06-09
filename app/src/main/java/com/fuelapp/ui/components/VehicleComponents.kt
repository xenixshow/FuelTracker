package com.fuelapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fuelapp.data.Vehicle
import com.fuelapp.ui.viewmodel.FuelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSelector(
    viewModel: FuelViewModel,
    vehicles: List<Vehicle>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onManageClick: () -> Unit
) {
    val current = vehicles.find { it.id == viewModel.selectedVehicleId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = current?.let { "${it.name} (${it.type})" } ?: "选择车辆",
            onValueChange = {},
            readOnly = true,
            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().width(220.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            vehicles.forEach { v ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${v.name}  ", style = MaterialTheme.typography.bodyMedium)
                            Text("(${v.type})", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    },
                    onClick = {
                        viewModel.selectVehicle(v.id)
                        onExpandedChange(false)
                    },
                    trailingIcon = if (v.id == viewModel.selectedVehicleId) {
                        { Icon(Icons.Default.Check, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("+ 管理车辆", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary) },
                onClick = { onExpandedChange(false); onManageClick() },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

@Composable
fun VehicleManageDialog(
    viewModel: FuelViewModel,
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🚗 管理车辆", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (vehicles.isEmpty()) {
                    Text("暂无车辆，请添加", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline)
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(vehicles, key = { it.id }) { v ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (v.id == viewModel.selectedVehicleId)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(v.name, fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge)
                                            if (v.id == viewModel.selectedVehicleId) {
                                                Spacer(Modifier.width(4.dp))
                                                Text("当前", style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Text("${v.type} · ${v.plate.ifBlank { "无牌" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline)
                                    }
                                    if (vehicles.size > 1 && v.id != 1L) {
                                        IconButton(onClick = { vehicleToDelete = v }) {
                                            Icon(Icons.Default.Delete, contentDescription = "删除",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加车辆")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )

    // 添加车辆对话框
    if (showAddDialog) {
        AddVehicleDialog(
            onConfirm = { name, plate, type, mileage, range ->
                viewModel.addVehicle(name, plate, type, mileage, range)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // 删除确认
    vehicleToDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            title = { Text("删除车辆") },
            text = { Text("确定删除「${v.name}」吗？该车辆的所有加油记录也会被删除。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteVehicle(v.id)
                    vehicleToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("删除")
                }
            },
            dismissButton = { TextButton(onClick = { vehicleToDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun AddVehicleDialog(
    onConfirm: (String, String, String, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(Vehicle.TYPES.first()) }
    var initialMileage by remember { mutableStateOf("") }
    var maxRange by remember { mutableStateOf(Vehicle.TYPE_RANGES[Vehicle.TYPES.first()]?.toString() ?: "300") }
    val types = Vehicle.TYPES.toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加车辆") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("车辆名称") }, singleLine = true,
                    placeholder = { Text("如：小牛UQi、丰田卡罗拉") },
                    modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = plate, onValueChange = { plate = it },
                    label = { Text("车牌号（可选）") }, singleLine = true,
                    placeholder = { Text("如：京A88888") },
                    modifier = Modifier.fillMaxWidth())

                // 类型选择（使用 Vehicle.TYPES）
                Text("选择车型：", style = MaterialTheme.typography.labelMedium)
                Column {
                    types.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { t ->
                                FilterChip(
                                    selected = type == t,
                                    onClick = {
                                        type = t
                                        maxRange = Vehicle.TYPE_RANGES[t]?.toString() ?: "300"
                                    },
                                    label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                // 一箱油最大续航（可自定义）
                OutlinedTextField(value = maxRange, onValueChange = { maxRange = it },
                    label = { Text("一箱油续航 (km)") }, singleLine = true,
                    placeholder = { Text("超过此里程判定为漏记") },
                    supportingText = { Text("小踏板~200km · 大摩托~300km · 轿车~700km · SUV~500km · 卡车~400km",
                        style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = initialMileage, onValueChange = { initialMileage = it },
                    label = { Text("初始里程数（可选）") }, singleLine = true,
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, plate, type, initialMileage.toDoubleOrNull() ?: 0.0, maxRange.toDoubleOrNull() ?: 300.0) },
                enabled = name.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

package com.fuelapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object AddRecord : Screen("add_record", "添加记录", Icons.Default.AddCircle)
    object History : Screen("history", "历史记录", Icons.AutoMirrored.Filled.List)
    object Statistics : Screen("statistics", "统计图表", Icons.Default.BarChart)
}

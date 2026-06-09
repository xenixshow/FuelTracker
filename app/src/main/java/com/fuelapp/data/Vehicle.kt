package com.fuelapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val plate: String = "",
    val type: String = "摩托车",
    val initialMileage: Double = 0.0,
    val maxTankRange: Double = 300.0,  // 一箱油最大续航(km)，超过此值判定为漏记
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        // 各车型默认续航（km）
        val TYPE_RANGES = mapOf(
            "小踏板" to 200.0,
            "大摩托" to 300.0,
            "轿车" to 700.0,
            "SUV/越野" to 500.0,
            "卡车" to 400.0,
            "电动车" to 200.0
        )
        val TYPES = TYPE_RANGES.keys.toList()
    }
}

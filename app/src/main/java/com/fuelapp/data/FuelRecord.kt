package com.fuelapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuel_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("date")]
)
data class FuelRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long = 1,
    val date: String,                 // "2024-01-15"
    val time: String = "",             // "14:30" 精确到分钟
    val mileage: Double,
    val fuelVolume: Double,
    val fuelPrice: Double,
    val totalCost: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

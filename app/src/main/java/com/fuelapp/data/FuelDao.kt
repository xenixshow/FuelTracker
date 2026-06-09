package com.fuelapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY date DESC, createdAt DESC")
    fun getAllRecords(vehicleId: Long = 1): Flow<List<FuelRecord>>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY date ASC, createdAt ASC")
    fun getAllRecordsAsc(vehicleId: Long = 1): Flow<List<FuelRecord>>

    @Query("SELECT * FROM fuel_records WHERE id = :id")
    suspend fun getRecordById(id: Long): FuelRecord?

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getRecordsByDateRange(vehicleId: Long, startDate: String, endDate: String): Flow<List<FuelRecord>>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId AND strftime('%Y-%m', date) = :yearMonth ORDER BY date ASC")
    fun getRecordsByMonth(vehicleId: Long, yearMonth: String): Flow<List<FuelRecord>>

    @Query("""
        SELECT strftime('%Y-%m', date) as month, COUNT(*) as count, 
               SUM(fuelVolume) as totalVolume, SUM(totalCost) as totalCost, AVG(fuelPrice) as avgPrice 
        FROM fuel_records WHERE vehicleId = :vehicleId 
        GROUP BY strftime('%Y-%m', date) ORDER BY month ASC
    """)
    fun getMonthlyStats(vehicleId: Long = 1): Flow<List<MonthlyStat>>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestRecord(vehicleId: Long): FuelRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: FuelRecord): Long

    @Update
    suspend fun update(record: FuelRecord)

    @Delete
    suspend fun delete(record: FuelRecord)

    @Query("DELETE FROM fuel_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}

data class MonthlyStat(
    val month: String,
    val count: Int,
    val totalVolume: Double,
    val totalCost: Double,
    val avgPrice: Double
)

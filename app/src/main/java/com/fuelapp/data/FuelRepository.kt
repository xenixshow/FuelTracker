package com.fuelapp.data

import kotlinx.coroutines.flow.Flow

class FuelRepository(
    private val fuelDao: FuelDao,
    private val vehicleDao: VehicleDao
) {
    // ===== 加油记录 =====
    fun getAllRecords(vehicleId: Long = 1): Flow<List<FuelRecord>> = fuelDao.getAllRecords(vehicleId)
    fun getAllRecordsAsc(vehicleId: Long = 1): Flow<List<FuelRecord>> = fuelDao.getAllRecordsAsc(vehicleId)
    suspend fun getRecordById(id: Long): FuelRecord? = fuelDao.getRecordById(id)
    fun getRecordsByDateRange(vehicleId: Long, startDate: String, endDate: String): Flow<List<FuelRecord>> =
        fuelDao.getRecordsByDateRange(vehicleId, startDate, endDate)
    suspend fun insert(record: FuelRecord): Long = fuelDao.insert(record)
    suspend fun update(record: FuelRecord) = fuelDao.update(record)
    suspend fun delete(record: FuelRecord) = fuelDao.delete(record)
    suspend fun deleteById(id: Long) = fuelDao.deleteById(id)
    fun getRecordsByMonth(vehicleId: Long, yearMonth: String): Flow<List<FuelRecord>> =
        fuelDao.getRecordsByMonth(vehicleId, yearMonth)
    fun getMonthlyStats(vehicleId: Long = 1): Flow<List<MonthlyStat>> = fuelDao.getMonthlyStats(vehicleId)
    suspend fun getLatestRecord(vehicleId: Long): FuelRecord? = fuelDao.getLatestRecord(vehicleId)

    // ===== 车辆管理 =====
    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles()
    suspend fun getVehicleById(id: Long): Vehicle? = vehicleDao.getVehicleById(id)
    suspend fun insertVehicle(vehicle: Vehicle): Long = vehicleDao.insert(vehicle)
    suspend fun updateVehicle(vehicle: Vehicle) = vehicleDao.update(vehicle)
    suspend fun deleteVehicle(vehicle: Vehicle) = vehicleDao.delete(vehicle)
    suspend fun deleteVehicleById(id: Long) = vehicleDao.deleteById(id)
    suspend fun getVehicleCount(): Int = vehicleDao.getCount()
}

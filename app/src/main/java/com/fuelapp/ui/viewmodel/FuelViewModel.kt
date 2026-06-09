package com.fuelapp.ui.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fuelapp.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class CalculationMode { AUTO_COST, AUTO_VOLUME, MANUAL }

@OptIn(ExperimentalCoroutinesApi::class)
class FuelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FuelRepository
    private val appContext: Context = application

    // ===== 加油记录 =====
    val allRecords: StateFlow<List<FuelRecord>>
    val allRecordsAsc: StateFlow<List<FuelRecord>>
    val monthlyStats: StateFlow<List<MonthlyStat>>
    val consumptionData: StateFlow<List<ConsumptionData>>
    val fuelPriceData: StateFlow<List<FuelPricePoint>>

    // ===== 漏记检测 =====
    val missedRecordIds: StateFlow<Set<Long>>

    // ===== 车辆管理 =====
    val allVehicles: StateFlow<List<Vehicle>>
    var selectedVehicleId by mutableStateOf(1L)
        private set
    var selectedVehicleName by mutableStateOf("默认车辆")
        private set
    var currentMaxRange by mutableStateOf(300.0)
        private set

    // ===== 表单 =====
    var date by mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        private set
    var time by mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
        private set
    var mileage by mutableStateOf(""); private set
    var fuelVolume by mutableStateOf(""); private set
    var fuelPrice by mutableStateOf(""); private set
    var totalCost by mutableStateOf(""); private set
    var notes by mutableStateOf(""); private set
    var calcMode by mutableStateOf(CalculationMode.MANUAL); private set
    var editingRecordId by mutableStateOf<Long?>(null); private set
    var selectedTimeRange by mutableStateOf("全部"); private set

    // ===== 上次加油提醒 =====
    data class LastRecordInfo(val date: String, val mileage: Double, val daysSince: Long, val kmSince: Double)
    var lastRecordInfo by mutableStateOf<LastRecordInfo?>(null); private set
    var showMissedReminder by mutableStateOf(false); private set

    // ===== 里程校验 =====
    var mileageError by mutableStateOf<String?>(null); private set
    var isSaving by mutableStateOf(false); private set

    // ===== 导出/导入 =====
    var exportStatus by mutableStateOf(""); private set
    var exportFilePath by mutableStateOf<String?>(null); private set
    var isExporting by mutableStateOf(false); private set

    // ===== 油耗计算结果 =====
    data class ConsumptionData(val date: String, val consumption: Double, val costPerKm: Double)
    data class FuelPricePoint(val date: String, val price: Double)

    init {
        val dao = FuelDatabase.getDatabase(application).fuelDao()
        val vehicleDaoVal = FuelDatabase.getDatabase(application).vehicleDao()
        repository = FuelRepository(dao, vehicleDaoVal)

        // 车辆列表
        allVehicles = repository.getAllVehicles()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        // 根据选中车辆加载数据（用 flatMapLatest 响应车辆切换）
        val vehicleIdFlow = MutableStateFlow(1L)

        allRecords = vehicleIdFlow.flatMapLatest { vid ->
            repository.getAllRecords(vid)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        allRecordsAsc = vehicleIdFlow.flatMapLatest { vid ->
            repository.getAllRecordsAsc(vid)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        monthlyStats = vehicleIdFlow.flatMapLatest { vid ->
            repository.getMonthlyStats(vid)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        consumptionData = allRecordsAsc.map { records ->
            if (records.size < 2) emptyList()
            else records.zipWithNext { prev, curr ->
                val k = curr.mileage - prev.mileage
                // 里程差 > 当前车辆续航则跳过（漏记）
                if (k > 0 && k <= currentMaxRange) {
                    ConsumptionData(curr.date, (curr.fuelVolume / k) * 100, curr.totalCost / k)
                } else null
            }.filterNotNull()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        fuelPriceData = allRecordsAsc.map { records ->
            records.map { FuelPricePoint(it.date, it.fuelPrice) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        // 检测漏记的记录 ID（里程差 > 车辆续航）
        missedRecordIds = allRecordsAsc.map { records ->
            if (records.size < 2) emptySet()
            else records.zipWithNext { prev, curr ->
                if (curr.mileage - prev.mileage > currentMaxRange) prev.id else null
            }.filterNotNull().toSet()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

        // 监听车辆切换，更新选中车辆名
        viewModelScope.launch {
            allVehicles.collect { vehicles ->
                vehicles.find { it.id == selectedVehicleId }?.let {
                    selectedVehicleName = it.name
                }
            }
        }
    }

    // ===== 车辆切换 =====
    fun selectVehicle(vehicleId: Long) {
        selectedVehicleId = vehicleId
        // 更新当前车辆的续航阈值
        viewModelScope.launch {
            allVehicles.first().find { it.id == vehicleId }?.let {
                currentMaxRange = it.maxTankRange
            }
        }
        clearForm()
    }

    // ===== 车辆管理 =====
    fun addVehicle(name: String, plate: String, type: String, initialMileage: Double, maxTankRange: Double) {
        viewModelScope.launch {
            val id = repository.insertVehicle(Vehicle(name = name, plate = plate, type = type, initialMileage = initialMileage, maxTankRange = maxTankRange))
            selectVehicle(id)
        }
    }

    fun deleteVehicle(id: Long) {
        viewModelScope.launch {
            repository.deleteVehicleById(id)
            if (id == selectedVehicleId) selectVehicle(1)
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch { repository.updateVehicle(vehicle) }
    }

    // ===== 上次记录检查 =====
    fun checkLastRecord() {
        viewModelScope.launch {
            val latest = repository.getLatestRecord(selectedVehicleId) ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val lastDate = sdf.parse(latest.date) ?: return@launch
            val today = Calendar.getInstance()
            val lastCal = Calendar.getInstance().apply { time = lastDate }
            val daysSince = (today.timeInMillis - lastCal.timeInMillis) / (1000 * 60 * 60 * 24)
            lastRecordInfo = LastRecordInfo(latest.date, latest.mileage, daysSince, 0.0)
        }
    }

    fun checkMileageGap(currentMileage: Double) {
        val last = lastRecordInfo ?: return
        if (currentMileage <= last.mileage) return
        val kmDiff = currentMileage - last.mileage
        if (kmDiff > currentMaxRange) showMissedReminder = true
        lastRecordInfo = last.copy(kmSince = kmDiff)
    }

    fun dismissMissedReminder() { showMissedReminder = false }

    // ===== 表单操作 =====
    fun updateDate(value: String) { date = value }
    fun updateTime(value: String) { time = value }

    fun updateMileage(value: String) {
        mileage = value; mileageError = null
        value.toDoubleOrNull()?.let { v ->
            if (v < 0) { mileageError = "里程数不能为负数"; return }
            val last = lastRecordInfo
            if (last != null && v < last.mileage) { mileageError = "里程数不能低于上次记录的 ${last.mileage.toInt()} km"; return }
            checkMileageGap(v)
        }
    }

    fun updateFuelVolume(value: String) {
        fuelVolume = value
        if (calcMode == CalculationMode.AUTO_VOLUME) { calcMode = CalculationMode.MANUAL; return }
        val p = fuelPrice.toDoubleOrNull()
        if (p != null && p > 0 && value.toDoubleOrNull() != null) { calcMode = CalculationMode.AUTO_COST; recalculate() }
        else calcMode = CalculationMode.MANUAL
    }

    fun updateFuelPrice(value: String) {
        fuelPrice = value
        if (calcMode == CalculationMode.MANUAL) {
            val p = value.toDoubleOrNull()
            if (p != null && p > 0) {
                if (fuelVolume.toDoubleOrNull() != null) { calcMode = CalculationMode.AUTO_COST; recalculate(); return }
                if (totalCost.toDoubleOrNull() != null) { calcMode = CalculationMode.AUTO_VOLUME; recalculate(); return }
            }
        }
        recalculate()
    }

    fun updateTotalCost(value: String) {
        totalCost = value
        if (calcMode == CalculationMode.AUTO_COST) { calcMode = CalculationMode.MANUAL; return }
        val p = fuelPrice.toDoubleOrNull()
        if (p != null && p > 0 && value.toDoubleOrNull() != null) { calcMode = CalculationMode.AUTO_VOLUME; recalculate() }
        else calcMode = CalculationMode.MANUAL
    }

    fun updateNotes(value: String) { notes = value }

    private fun recalculate() {
        val p = fuelPrice.toDoubleOrNull() ?: return
        if (p <= 0) return
        when (calcMode) {
            CalculationMode.AUTO_COST -> { val v = fuelVolume.toDoubleOrNull() ?: return; if (v > 0) totalCost = String.format("%.2f", v * p) }
            CalculationMode.AUTO_VOLUME -> { val c = totalCost.toDoubleOrNull() ?: return; if (c > 0) fuelVolume = String.format("%.2f", c / p) }
            CalculationMode.MANUAL -> {}
        }
    }

    fun saveRecord() {
        if (isSaving) return
        val m = mileage.toDoubleOrNull() ?: return
        val v = fuelVolume.toDoubleOrNull() ?: return
        val p = fuelPrice.toDoubleOrNull() ?: return
        val c = totalCost.toDoubleOrNull() ?: return
        if (date.isBlank() || mileageError != null) return
        isSaving = true
        viewModelScope.launch {
            repository.insert(FuelRecord(
                id = editingRecordId ?: 0,
                vehicleId = selectedVehicleId,
                date = date, time = time, mileage = m,
                fuelVolume = v, fuelPrice = p,
                totalCost = c, notes = notes
            ))
            isSaving = false; clearForm()
        }
    }

    fun deleteRecord(id: Long) { viewModelScope.launch { repository.deleteById(id) } }

    fun editRecord(record: FuelRecord) {
        date = record.date; time = record.time
        mileage = record.mileage.toInt().toString()
        fuelVolume = record.fuelVolume.toString(); fuelPrice = record.fuelPrice.toString()
        totalCost = record.totalCost.toString(); notes = record.notes
        mileageError = null
        val expected = record.fuelVolume * record.fuelPrice
        calcMode = if (kotlin.math.abs(expected - record.totalCost) < 0.01) CalculationMode.AUTO_COST else CalculationMode.MANUAL
        editingRecordId = record.id
    }

    fun clearForm() {
        val now = Date()
        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        mileage = ""; fuelVolume = ""; fuelPrice = ""; totalCost = ""; notes = ""
        calcMode = CalculationMode.MANUAL; editingRecordId = null; mileageError = null
    }

    fun updateSelectedTimeRange(range: String) { selectedTimeRange = range }

    fun getFilteredRecords(range: String): Flow<List<FuelRecord>> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val vid = selectedVehicleId
        return when (range) {
            "近一周" -> { cal.add(Calendar.DAY_OF_YEAR, -7); repository.getRecordsByDateRange(vid, fmt.format(cal.time), fmt.format(Date())) }
            "近一月" -> { cal.add(Calendar.MONTH, -1); repository.getRecordsByDateRange(vid, fmt.format(cal.time), fmt.format(Date())) }
            "近三月" -> { cal.add(Calendar.MONTH, -3); repository.getRecordsByDateRange(vid, fmt.format(cal.time), fmt.format(Date())) }
            "近一年" -> { cal.add(Calendar.YEAR, -1); repository.getRecordsByDateRange(vid, fmt.format(cal.time), fmt.format(Date())) }
            else -> repository.getAllRecords(vid)
        }
    }

    // ===== CSV 导出 =====
    fun exportToCsv() {
        viewModelScope.launch {
            isExporting = true; exportStatus = "正在导出…"
            val records = allRecords.value
            if (records.isEmpty()) { exportStatus = "暂无数据可导出"; isExporting = false; return@launch }
            try {
                val sorted = records.sortedBy { "${it.date}|${it.createdAt}" }
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val fileName = "油耗记录_${sdf.format(Date())}.csv"
                val csvContent = buildString {
                    append("\uFEFF"); append("日期,时间,里程数(km),加油量(L),油价(元/L),总费用(元),油耗(L/100km),每公里费用(元/km),备注\n")
                    var prev: Double? = null
                    for (r in sorted) {
                        val con = if (prev != null && r.mileage > prev && r.mileage - prev <= currentMaxRange) String.format("%.2f", (r.fuelVolume / (r.mileage - prev)) * 100) else "-"
                        val cpk = if (prev != null && r.mileage > prev && r.mileage - prev <= currentMaxRange) String.format("%.4f", r.totalCost / (r.mileage - prev)) else "-"
                        append("${r.date},${r.time},${r.mileage.toInt()},${r.fuelVolume},${r.fuelPrice},${r.totalCost},$con,$cpk,${r.notes.replace(",", "，")}\n")
                        prev = r.mileage
                    }
                }
                val cacheDir = java.io.File(appContext.cacheDir, "export").apply { mkdirs() }
                val cacheFile = java.io.File(cacheDir, fileName)
                cacheFile.writeText(csvContent, Charsets.UTF_8)
                exportFilePath = cacheFile.absolutePath; exportStatus = "✅ 导出成功: $fileName"
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val cv = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        appContext.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)?.let { uri ->
                            appContext.contentResolver.openOutputStream(uri)?.use { it.write(csvContent.toByteArray(Charsets.UTF_8)) }
                        }
                    } else {
                        java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName).writeText(csvContent, Charsets.UTF_8)
                    }
                } catch (_: Exception) {}
                val shareUri = androidx.core.content.FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", cacheFile)
                appContext.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"; putExtra(Intent.EXTRA_STREAM, shareUri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "分享油耗数据").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) { exportStatus = "❌ 导出失败: ${e.localizedMessage ?: "未知错误"}" }
            isExporting = false
        }
    }

    fun clearExportStatus() { exportStatus = ""; exportFilePath = null }

    // ===== CSV 导入 =====
    fun importFromCsv(uri: android.net.Uri) {
        viewModelScope.launch {
            isExporting = true; exportStatus = "正在导入…"
            try {
                val lines = (appContext.contentResolver.openInputStream(uri) ?: run { exportStatus = "❌ 无法读取文件"; isExporting = false; return@launch }).bufferedReader(Charsets.UTF_8).readLines()
                if (lines.size < 2) { exportStatus = "❌ CSV 文件为空或只有表头"; isExporting = false; return@launch }
                if (!lines[0].replace("\uFEFF", "").contains("日期")) { exportStatus = "❌ 文件格式不正确"; isExporting = false; return@launch }
                var ok = 0; var err = 0
                val sf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                for (i in 1 until lines.size) {
                    val line = lines[i].trim(); if (line.isBlank()) continue
                    try {
                        val parts = parseCsvLine(line) ?: continue
                        if (parts.size < 5) { err++; continue }
                        val d = parts[0].trim(); val t = if (parts.size > 1) parts[1].trim() else ""
                        val ml = parts[2].trim().toDoubleOrNull()
                        val vl = parts[3].trim().toDoubleOrNull(); val pr = parts[4].trim().toDoubleOrNull()
                        val co = parts[5].trim().toDoubleOrNull()
                        if (ml == null || vl == null || pr == null || co == null) { err++; continue }
                        if (sf.parse(d) == null) { err++; continue }
                        repository.insert(FuelRecord(vehicleId = selectedVehicleId, date = d, time = t, mileage = ml, fuelVolume = vl, fuelPrice = pr, totalCost = co, notes = if (parts.size > 8) parts[8].trim() else ""))
                        ok++
                    } catch (_: Exception) { err++ }
                }
                exportStatus = "✅ 导入完成: 成功 $ok 条" + if (err > 0) "，跳过 $err 条" else ""
            } catch (e: Exception) { exportStatus = "❌ 导入失败: ${e.localizedMessage ?: "未知错误"}" }
            isExporting = false
        }
    }

    private fun parseCsvLine(line: String): List<String>? {
        if (line.isBlank()) return null
        val r = mutableListOf<String>(); val cur = StringBuilder(); var q = false
        for (ch in line) { when { ch == '"' -> q = !q; ch == ',' && !q -> { r.add(cur.toString()); cur.clear() }; else -> cur.append(ch) } }
        r.add(cur.toString()); return r
    }
}

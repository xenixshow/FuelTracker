package com.fuelapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FuelRecord::class, Vehicle::class], version = 4, exportSchema = false)
abstract class FuelDatabase : RoomDatabase() {
    abstract fun fuelDao(): FuelDao
    abstract fun vehicleDao(): VehicleDao

    companion object {
        @Volatile
        private var INSTANCE: FuelDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `vehicles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL, `plate` TEXT NOT NULL DEFAULT '', `type` TEXT NOT NULL DEFAULT '摩托车', `initialMileage` REAL NOT NULL DEFAULT 0.0, `createdAt` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO vehicles (id, name, plate, type, initialMileage, createdAt) VALUES (1, '默认车辆', '', '摩托车', 0.0, ${System.currentTimeMillis()})")
                db.execSQL("ALTER TABLE `fuel_records` ADD COLUMN `vehicleId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_records_vehicleId` ON `fuel_records` (`vehicleId`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `fuel_records` ADD COLUMN `time` TEXT NOT NULL DEFAULT ''")
            }
        }

        // v3→v4：vehicles 加 maxTankRange 列，根据类型设默认续航
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `maxTankRange` REAL NOT NULL DEFAULT 300.0")
                // 已有车辆根据类型更新默认值
                db.execSQL("UPDATE vehicles SET maxTankRange = 200 WHERE type = '小踏板'")
                db.execSQL("UPDATE vehicles SET maxTankRange = 300 WHERE type = '大摩托' OR type = '摩托车'")
                db.execSQL("UPDATE vehicles SET maxTankRange = 700 WHERE type = '轿车'")
                db.execSQL("UPDATE vehicles SET maxTankRange = 500 WHERE type = 'SUV/越野'")
                db.execSQL("UPDATE vehicles SET maxTankRange = 400 WHERE type = '卡车'")
            }
        }

        fun getDatabase(context: Context): FuelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FuelDatabase::class.java,
                    "fuel_tracker_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

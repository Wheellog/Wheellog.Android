package com.cooper.wheellog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripDataDbEntry::class], version = 2, exportSchema = false)
abstract class TripDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: TripDatabase? = null

        private val migration1To2: Migration = object: Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("BEGIN TRANSACTION;")
                    execSQL("ALTER TABLE `trip_database` ADD COLUMN `distance` INTEGER NOT NULL DEFAULT 0;")
                    execSQL("ALTER TABLE `trip_database` ADD COLUMN `consumptionTotal` REAL NOT NULL DEFAULT 0;")
                    execSQL("ALTER TABLE `trip_database` ADD COLUMN `consumptionByKm` REAL NOT NULL DEFAULT 0;")
                    execSQL("COMMIT;")
                }
            }
        }

        private val migration2To1: Migration = object: Migration(2, 1) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("BEGIN TRANSACTION;")
                    execSQL("ALTER TABLE `trip_database` DROP COLUMN `distance`;")
                    execSQL("ALTER TABLE `trip_database` DROP COLUMN `consumptionTotal`;")
                    execSQL("ALTER TABLE `trip_database` DROP COLUMN `consumptionByKm`;")
                    execSQL("COMMIT;")
                }
            }
        }

        fun getDataBase(context: Context): TripDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripDatabase::class.java,
                    "trip_database"
                )
                    .addMigrations(migration1To2, migration2To1)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
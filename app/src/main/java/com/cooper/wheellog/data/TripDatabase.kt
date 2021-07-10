package com.cooper.wheellog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TripData::class], version = 1, exportSchema = false)
abstract class TripDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: TripDatabase? = null

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
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
package com.cooper.wheellog.data

import androidx.room.*

@Dao
interface TripDao {
    @Query("SELECT * FROM trip_database ORDER BY id ASC")
    suspend fun getAll(): List<TripData>

    @Query("SELECT * FROM trip_database WHERE id IN (:tripIds) ORDER BY id ASC")
    suspend fun loadAllByIds(tripIds: IntArray): List<TripData>

    @Query("SELECT * FROM trip_database WHERE fileName LIKE :fileName LIMIT 1")
    suspend fun getTripByFileName(fileName: String): TripData?

    @Query("SELECT * FROM trip_database WHERE ecId LIKE :ecId LIMIT 1")
    suspend fun getTripByElectroClubId(ecId: Int): TripData?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg tripData: TripData)

    @Update
    suspend fun update(vararg tripData: TripData)

    @Delete
    suspend fun delete(vararg tripData: TripData)
}
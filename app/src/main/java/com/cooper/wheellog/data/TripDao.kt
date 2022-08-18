package com.cooper.wheellog.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TripDao {
    @Query("SELECT * FROM trip_database ORDER BY id ASC")
    fun getAll(): LiveData<List<TripData>>

    @Query("SELECT * FROM trip_database WHERE id IN (:tripIds) ORDER BY id ASC")
    fun loadAllByIds(tripIds: IntArray): LiveData<List<TripData>>

    @Query("SELECT * FROM trip_database WHERE fileName LIKE :fileName LIMIT 1")
    fun getTripByFileName(fileName: String): LiveData<TripData?>

    @Query("SELECT * FROM trip_database WHERE ecId LIKE :ecId LIMIT 1")
    fun getTripByElectroClubId(ecId: Int): LiveData<TripData?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg tripData: TripData)

    @Update
    fun update(vararg tripData: TripData)

    @Delete
    fun delete(vararg tripData: TripData)
}
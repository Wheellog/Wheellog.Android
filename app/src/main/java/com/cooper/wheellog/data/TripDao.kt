package com.cooper.wheellog.data

import androidx.room.*

@Dao
interface TripDao {
    @Query("SELECT * FROM trip_database ORDER BY id ASC")
    fun getAll(): List<TripDataDbEntry>

    @Query("SELECT * FROM trip_database WHERE id IN (:tripIds) ORDER BY id ASC")
    fun loadAllByIds(tripIds: IntArray): List<TripDataDbEntry>

    @Query("SELECT * FROM trip_database WHERE fileName LIKE :fileName LIMIT 1")
    fun getTripByFileName(fileName: String): TripDataDbEntry?

    @Query("SELECT * FROM trip_database WHERE ecId LIKE :ecId LIMIT 1")
    fun getTripByElectroClubId(ecId: Int): TripDataDbEntry?

    @Insert(entity = TripDataDbEntry::class, onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg tripDatumTuples: TripDataDbEntry)

    @Update(entity = TripDataDbEntry::class, onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg tripDatumTuples: TripDataDbEntry)

    @Delete
    fun delete(vararg tripDatumTuples: TripDataDbEntry)

    @Query("DELETE FROM trip_database WHERE id = :inputId")
    fun deleteDataById(inputId: Long)
}
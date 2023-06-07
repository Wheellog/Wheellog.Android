package com.cooper.wheellog.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TripRepository (private val tripDao: TripDao) {

    suspend fun insertNewData(statisticDbEntity: TripDataDbEntry) {
        withContext(Dispatchers.IO) {
            tripDao.insert(statisticDbEntity)
        }
    }

    suspend fun getAllData(): List<TripDataDbEntry> {
        return withContext(Dispatchers.IO) {
            return@withContext tripDao.getAll()
        }
    }

    suspend fun removeDataById(id: Long) {
        withContext(Dispatchers.IO) {
            tripDao.deleteDataById(id)
        }
    }
}
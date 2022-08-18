package com.cooper.wheellog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONObject
import java.sql.*

@Entity(tableName = "trip_database", indices = [Index(value = ["fileName"], unique = true)])
data class TripData(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val fileName: String,
        var mac: String = "",
        var profileName: String = "",
        var start: Int = 0,
        var duration: Int = 0,

        var ecId: Int = 0,
        var ecStartTime: Int = 0,
        var ecDuration: Int = 0,
        var ecUrl: String = "",
        var ecUrlImage: String = "",
        var ecTransportId: Int = 0,

        var maxSpeed: Float = 0f,
        var maxSpeedGps: Float = 0f,
        var avgSpeed: Float = 0f,
        var maxPwm: Float = 0f,
        var maxCurrent: Float = 0f,
        var maxPower: Float = 0f,
        
        var additionalJson: String? = null
)
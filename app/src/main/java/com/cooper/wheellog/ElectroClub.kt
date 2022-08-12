package com.cooper.wheellog

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.cooper.wheellog.data.TripDao
import com.cooper.wheellog.data.TripData
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlin.coroutines.*


class ElectroClub {

    companion object {
        @JvmStatic val instance: ElectroClub = ElectroClub()

        const val LOGIN_METHOD = "login"
        const val UPLOAD_METHOD = "uploadTrack"
        const val GET_GARAGE_METHOD = "getUserGarage"
        const val GET_GARAGE_METHOD_FILTRED = "garage"
    }

    var url = "https://electro.club/api/v1"
    private val accessToken = BuildConfig.ec_accessToken
    private val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    var lastError: String? = null
    var errorListener: ((String?, String?)->Unit)? = null
    var successListener: ((String?, Any?)->Unit)? = null
    var dao: TripDao? = null

    fun login(email: String, password: String, success: (Boolean) -> Unit) {
        val httpUrl = url.toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("method", LOGIN_METHOD)
                .addQueryParameter("access_token", accessToken)
                .addQueryParameter("email", email)
                .addQueryParameter("password", password)
                .build()

        val request = Request.Builder()
                .url(httpUrl)
                .method("GET", null)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                WheelLog.AppConfig.ecUserId = null
                WheelLog.AppConfig.ecToken = null
                lastError = "[unexpected] " + e.message
                errorListener?.invoke(LOGIN_METHOD, lastError)
                e.printStackTrace()
                success(false)
            }

            override fun onResponse(call: Call, response: Response) {
                var userToken: String? = null
                var userId: String? = null
                var nickname = ""
                response.use {
                    val json = getSafeJson(LOGIN_METHOD, response)
                    if (json == null) {
                        success(false)
                        return
                    }
                    if (!response.isSuccessful) {
                        parseError(json)
                    } else {
                        val userObj = json.optJSONObject("data")?.optJSONObject("user")
                        if (userObj != null) {
                            userToken = userObj.getString("user_token")
                            userId = userObj.getString("user_id")
                            nickname = userObj.getString("nickname")
                        }
                    }

                    WheelLog.AppConfig.ecUserId = userId
                    WheelLog.AppConfig.ecToken = userToken
                    if (userId == null || userToken == null) {
                        errorListener?.invoke(LOGIN_METHOD, lastError)
                        success(false)
                    } else {
                        successListener?.invoke(LOGIN_METHOD, nickname)
                        success(true)
                    }
                }
            }
        })
    }

    fun logout() {
        WheelLog.AppConfig.apply {
            ecToken = null
            ecUserId = null
            ecGarage = null
            autoUploadEc = false
        }
    }

    suspend fun uploadTrackAsync(data: ByteArray, fileName: String, verified: Boolean): Boolean {
        if (WheelLog.AppConfig.ecToken == null) {
            lastError = "Missing parameters"
            errorListener?.invoke(UPLOAD_METHOD, lastError)
            return false
        }
        val prepare = GlobalScope.launch {
            insertEmptyEntryInDb(fileName, WheelLog.AppConfig.profileName)
        }
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault())
        val currentLocalTime = calendar.time
        val date = SimpleDateFormat("Z", Locale.getDefault())
        var localTime = date.format(currentLocalTime)
        localTime = StringBuilder(localTime).insert(localTime.length - 2, ":").toString()
        val mediaType = "text/csv".toMediaTypeOrNull()
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("method", UPLOAD_METHOD)
                .addFormDataPart("access_token", accessToken)
                .addFormDataPart("user_token", WheelLog.AppConfig.ecToken!!)
                .addFormDataPart("file", fileName, data.toRequestBody(mediaType))
                .addFormDataPart("time_zone", localTime)
        if (WheelLog.AppConfig.ecGarage != null && WheelLog.AppConfig.ecGarage != "0") {
            bodyBuilder.addFormDataPart("garage_id", WheelLog.AppConfig.ecGarage!!)
        }
        if (verified) {
            bodyBuilder.addFormDataPart("verified", "1")
        }

        val request = Request.Builder()
                .url(url)
                .method("POST", bodyBuilder.build())
                .build()

        return suspendCoroutine { continuation ->
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    lastError = "[unexpected] " + e.message
                    errorListener?.invoke(UPLOAD_METHOD, lastError)
                    e.printStackTrace()
                    continuation.resume(false)
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            val json = getSafeJson(UPLOAD_METHOD, response)
                            if (json == null) {
                                continuation.resume(false)
                                return
                            }
                            if (!response.isSuccessful) {
                                parseError(json)
                                errorListener?.invoke(UPLOAD_METHOD, lastError)
                                continuation.resume(false)
                            } else {
                                val track = json.optJSONObject("data")?.optJSONObject("track")
                                val id = track?.optInt("id")
                                if (id == null) {
                                    lastError = "electro club id is wrong"
                                    errorListener?.invoke(UPLOAD_METHOD, lastError)
                                    continuation.resume(false)
                                } else {
                                    GlobalScope.launch {
                                        prepare.join()
                                        val tripInserted = updateEntryInDb(track, fileName)
                                        successListener?.invoke(UPLOAD_METHOD, "trackId = ${tripInserted.ecId}")
                                        continuation.resume(true)
                                    }
                                }
                            }
                        } catch (ex: Exception) {
                            lastError = ex.message
                            errorListener?.invoke(UPLOAD_METHOD, lastError)
                            continuation.resume(false)
                        }
                    }
                }
            })
        }
    }

    fun uploadTrack(data: ByteArray, fileName: String, verified: Boolean, success: (Boolean) -> Unit) {
        GlobalScope.launch {
            success(uploadTrackAsync(data, fileName, verified))
        }
    }

    private fun insertEmptyEntryInDb(fileName: String, profileName: String) {
        dao?.getTripByFileName(fileName)
            ?: dao?.insert(TripData(fileName = fileName, profileName = profileName))
    }

    fun updateEntryInDb(track: JSONObject, fileName: String): TripData {
        // gets or create trip with fileName field
        val trip = dao?.getTripByFileName(fileName)?.value
                ?: TripData(fileName = fileName)
        trip.apply {
            ecId = track.getInt("id")
            ecStartTime = track.optInt("start_time")
            ecTransportId = track.optInt("garage_id")
            ecUrlImage = track.optString("image")
            ecDuration = track.optInt("duration")
        }

        dao?.update(trip)
        return trip
    }

    fun getAndSelectGarageByMacOrShowChooseDialog(mac: String, activity: Activity, success: (String?) -> Unit) {
        if (!WheelData.getInstance().isConnected || WheelLog.AppConfig.ecGarage != null)
            return // not connected or already selected

        getGarage { transportList ->
            val transport = transportList.find { it.mac == mac }
            if (transport != null) {
                WheelLog.AppConfig.ecGarage = transport.id
                success(transport.id)
                successListener?.invoke(GET_GARAGE_METHOD_FILTRED, transport.name)
                return@getGarage
            }

            // UI with list select garage if mac isn't found
            activity.runOnUiThread {
                var selectedTransport: Transport? = null
                AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.ec_choose_transport))
                        .setSingleChoiceItems(transportList.map { it.name }.toTypedArray(), -1) { _, which ->
                            if (which != -1) {
                                selectedTransport = transportList[which]
                            }
                        }
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            if (selectedTransport != null) {
                                WheelLog.AppConfig.ecGarage = selectedTransport!!.id
                                success(selectedTransport!!.id)
                                successListener?.invoke(GET_GARAGE_METHOD_FILTRED, selectedTransport!!.name)
                            } else {
                                errorListener?.invoke(GET_GARAGE_METHOD_FILTRED, "selected item not valid")
                            }
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            WheelLog.AppConfig.ecGarage = "0"
                            successListener?.invoke(GET_GARAGE_METHOD_FILTRED, "nothing")
                        }
                        .setCancelable(false)
                        .create()
                        .show()
            }
        }
    }

    private fun getGarage(success: (Array<Transport>) -> Unit) {
        if (WheelLog.AppConfig.ecToken == null || WheelLog.AppConfig.ecUserId == null)
        {
            lastError = "Missing parameters"
            errorListener?.invoke(GET_GARAGE_METHOD, lastError)
            return
        }

        val httpUrl = url.toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("method", GET_GARAGE_METHOD)
                .addQueryParameter("access_token", accessToken)
                .addQueryParameter("user_token", WheelLog.AppConfig.ecToken)
                .addQueryParameter("user_id", WheelLog.AppConfig.ecUserId)
                .build()

        val request = Request.Builder()
                .url(httpUrl)
                .method("GET", null)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                lastError = "[unexpected] " + e.message
                errorListener?.invoke("login", lastError)
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val json = getSafeJson(GET_GARAGE_METHOD, response) ?: return
                    if (!response.isSuccessful) {
                        parseError(json)
                        errorListener?.invoke(GET_GARAGE_METHOD, lastError)
                    } else {
                        try {
                            val data = json.optJSONObject("data")
                            if (data == null || !data.has("transport_list")) {
                                lastError = "no transport"
                                errorListener?.invoke(GET_GARAGE_METHOD, lastError)
                                return
                            }
                            val transportListJson = data.getJSONArray("transport_list")
                            val transportList = Array(transportListJson.length()) { Transport() }
                            val len = transportListJson.length() - 1
                            for (i in 0..len) {
                                val t = transportListJson.optJSONObject(i) ?: continue
                                transportList[i].apply {
                                    id = t.getString("id")
                                    name = t.getString("name")
                                    mac = t.optString("MAC")
                                }
                            }
                            success(transportList)
                        } catch (e: Exception) {
                            lastError = "json parsing error: " + e.message
                            errorListener?.invoke(GET_GARAGE_METHOD, lastError)
                        }
                    }
                    successListener?.invoke(GET_GARAGE_METHOD, WheelLog.AppConfig.ecToken)
                }
            }
        })
    }

    private fun parseError(jsonObject: JSONObject?) {
        lastError = jsonObject
                ?.optJSONObject("data")
                ?.optString("error")
                ?: "Unknown error"
    }

    private fun getSafeJson(method: String, response: Response): JSONObject? {
        if (response.code in 500..599) {
            lastError = "500 exception"
            errorListener?.invoke(method, lastError)
            return null
        }
        try {
            return JSONObject(response.body!!.string())
        } catch (e: Exception) {
            lastError = "json parsing error: " + e.message
            errorListener?.invoke(method, lastError)
        }
        return null
    }

    private class Transport {
        var id: String = ""
        var name: String = ""
        var mac: String? = null
    }
}
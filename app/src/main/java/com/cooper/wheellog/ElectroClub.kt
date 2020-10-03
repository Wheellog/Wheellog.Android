package com.cooper.wheellog

import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ElectroClub {

    companion object {
        @JvmStatic lateinit var instance: ElectroClub
    }

    private val url = "https://electro.club/api/v1"
    private val accessToken = BuildConfig.ec_accessToken
    private val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    var userToken: String? = null
    var lastError: String? = null
    var errorListener: ((String?, String?)->Unit)? = null
    var successListener: ((String?, Any?)->Unit)? = null

    fun login(email: String, password: String, success: (Boolean) -> Unit) {
        userToken = null
        val urlWithParams = Uri.parse(url)
                .buildUpon()
                .appendQueryParameter("method", "login")
                .appendQueryParameter("access_token", accessToken)
                .appendQueryParameter("email", email)
                .appendQueryParameter("password", password)
                .build()
                .toString()

        val request = Request.Builder()
                .url(urlWithParams)
                .method("GET", null)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                userToken = null
                lastError = "[unexpected] " + e.message
                errorListener?.invoke("login", lastError)
                e.printStackTrace()
                success(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val json = JSONObject(response.body!!.string())
                    if (!response.isSuccessful) {
                        userToken = null
                        parseError(json)
                        errorListener?.invoke("login", lastError)
                    } else {
                        userToken = json
                                .getObjectSafe("data")
                                ?.getObjectSafe("user")
                                ?.getString("user_token")
                    }
                    successListener?.invoke("login", userToken)
                    success(true)
                }
            }
        })
    }

    fun uploadTrack(data: ByteArray, fileName: String) {
        userToken ?: return
        val mediaType = "text/csv".toMediaTypeOrNull()
        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("method", "uploadTrack")
                .addFormDataPart("access_token", accessToken)
                .addFormDataPart("user_token", userToken!!)
                .addFormDataPart("file", fileName, data.toRequestBody(mediaType))
                .build()

        val request = Request.Builder()
                .url(url)
                .method("POST", body)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                lastError = "[unexpected] " + e.message
                errorListener?.invoke("uploadTrack", lastError)
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val json = JSONObject(response.body!!.string())
                    if (!response.isSuccessful) {
                        parseError(json)
                        errorListener?.invoke("uploadTrack", lastError)
                    } else {
                        successListener?.invoke("uploadTrack", json)
                    }
                }
            }
        })
    }

    private fun parseError(jsonObject: JSONObject?) {
        lastError = jsonObject
                ?.getObjectSafe("data")
                ?.getStringSafe("error")
                ?: "Unknown error"
    }

    private fun JSONObject.getObjectSafe(name: String): JSONObject? {
        if (this.has(name)) {
            return this.getJSONObject(name)
        }
        return null
    }

    private fun JSONObject.getStringSafe(name: String): String? {
        if (this.has(name)) {
            return this.getString(name)
        }
        return null
    }
}
package com.cooper.wheellog

import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit


class ElectroClub {

    companion object {
        @JvmStatic val instance: ElectroClub = ElectroClub()

        const val LOGIN_METHOD = "login"
        const val UPLOAD_METHOD = "uploadTrack"
        const val GET_GARAGE_METHOD = "getUserGarage"
    }

    private val url = "https://electro.club/api/v1"
    private val accessToken = BuildConfig.ec_accessToken
    private val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    var userToken: String? = null
    var userId: String? = null
    var lastError: String? = null
    var selectedGarage: String = "0"
    var errorListener: ((String?, String?)->Unit)? = null
    var successListener: ((String?, Any?)->Unit)? = null

    fun login(email: String, password: String, success: (Boolean) -> Unit) {
        userToken = null
        userId = null
        var nickname = ""
        val urlWithParams = Uri.parse(url)
                .buildUpon()
                .appendQueryParameter("method", LOGIN_METHOD)
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
                userId = null
                lastError = "[unexpected] " + e.message
                errorListener?.invoke(LOGIN_METHOD, lastError)
                e.printStackTrace()
                success(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val json = JSONObject(response.body!!.string())
                    if (!response.isSuccessful) {
                        userToken = null
                        userId = null
                        parseError(json)
                        errorListener?.invoke(LOGIN_METHOD, lastError)
                        return
                    } else {
                        val userObj = json.getObjectSafe("data")?.getObjectSafe("user")
                        if (userObj != null) {
                            userToken = userObj.getString("user_token")
                            userId = userObj.getString("user_id")
                            nickname = userObj.getString("nickname")
                        } else {
                            userToken = null
                            userId = null
                            return
                        }
                    }
                    successListener?.invoke(LOGIN_METHOD, nickname)
                    success(true)
                }
            }
        })
    }

    fun uploadTrack(data: ByteArray, fileName: String) {
        if (userToken == null)
        {
            lastError = "Missing parameters"
            errorListener?.invoke(UPLOAD_METHOD, lastError)
            return
        }
        val mediaType = "text/csv".toMediaTypeOrNull()
        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("method", UPLOAD_METHOD)
                .addFormDataPart("access_token", accessToken)
                .addFormDataPart("user_token", userToken!!)
                .addFormDataPart("file", fileName, data.toRequestBody(mediaType))
                .addFormDataPart("garage_id", selectedGarage)
                .build()

        val request = Request.Builder()
                .url(url)
                .method("POST", body)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                lastError = "[unexpected] " + e.message
                errorListener?.invoke(UPLOAD_METHOD, lastError)
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val json = JSONObject(response.body!!.string())
                    if (!response.isSuccessful) {
                        parseError(json)
                        errorListener?.invoke(UPLOAD_METHOD, lastError)
                    } else {
                        successListener?.invoke(UPLOAD_METHOD, json)
                    }
                }
            }
        })
    }

    fun getAndSelectGarageByMacOrPrimary(mac: String, success: (String?) -> Unit)
    {
        getGarage {
            val len = it.length() - 1
            var primaryId: String? = null
            for (i in 0..len) {
                val g = it.getJSONObject(i)
                val m = g.getStringSafe("MAC")
                val isPrimary = g.getStringSafe("primary") == "1"
                if (m != null && m == mac) {
                    selectedGarage = it.getJSONObject(i).getString("id")
                    success(selectedGarage)
                    break
                }
                if (isPrimary) {
                    primaryId = it.getJSONObject(i).getString("id")
                }
            }

            // TODO todo todo need UI
            /*if (primaryId != null)
            {
                selectedGarage = primaryId
                success(selectedGarage)
            }*/
        }
    }

    fun getGarage(success: (JSONArray) -> Unit) {
        if (userToken == null || userId == null)
        {
            lastError = "Missing parameters"
            errorListener?.invoke(GET_GARAGE_METHOD, lastError)
            return
        }
        val urlWithParams = Uri.parse(url)
                .buildUpon()
                .appendQueryParameter("method", GET_GARAGE_METHOD)
                .appendQueryParameter("access_token", accessToken)
                .appendQueryParameter("user_token", userToken)
                .appendQueryParameter("user_id", userId)
                .build()
                .toString()

        val request = Request.Builder()
                .url(urlWithParams)
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
                    val json = JSONObject(response.body!!.string())
                    if (!response.isSuccessful) {
                        parseError(json)
                        errorListener?.invoke(GET_GARAGE_METHOD, lastError)
                    } else {
                        val data = json.getObjectSafe("data")
                        if (data == null || !data.has("transport_list")) {
                            lastError = "no transport"
                            errorListener?.invoke(GET_GARAGE_METHOD, lastError)
                            return
                        }
                        val transportList = data.getJSONArray("transport_list")
                        success(transportList)
                    }
                    successListener?.invoke(GET_GARAGE_METHOD, userToken)
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
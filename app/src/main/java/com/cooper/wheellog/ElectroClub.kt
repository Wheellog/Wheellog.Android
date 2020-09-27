package com.cooper.wheellog

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.cooper.wheellog.utils.FileDataPart
import com.cooper.wheellog.utils.VolleyFileUploadRequest
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class ElectroClub(private val context: Context) {
    private val url = "https://electro.club/api"
    private val accessToken = BuildConfig.ec_accessToken

    var userToken: String? = null
        get() = field
        private set(value) {
            field = value
        }
    var lastError: String? = null
        get() = field
        private set(value) {
            field = value
        }

    private fun send (method: Int, parameters: MutableMap<String, String>,
                      success: (jsonObject: JSONObject) -> Unit, err: (message: String) -> Unit) {
        // TODO: GET method params
        val req = object : StringRequest(
                method,
                url,
                {
                    val jsonObject = JSONObject(it)
                    lastError = parseError(jsonObject)
                    success(jsonObject)
                },
                {
                    lastError = "Response was not successful. $it"
                    err(it.message!!)
                }) {
            override fun getParams(): MutableMap<String, String> {
                return parameters
            }
        }
        Volley.newRequestQueue(context).add(req)
    }

    fun login(email: String, password: String, success: (token: String?) -> Unit) {
        userToken = null
        send(Request.Method.POST, mutableMapOf(
                "method" to "login",
                "access_token" to accessToken,
                "email" to email,
                "password" to password), {
            userToken = it.getObjectSafe("loginSuccess")?.getString("user_token") ?: ""
            success(userToken)
        }, {
            println("error is: $it")
        })
    }

    fun uploadTrack(file: File, success: (json: JSONObject?) -> Unit) {
        userToken ?: return
        val parameters = mutableMapOf(
                "method" to "uploadTrack",
                "access_token" to accessToken,
                "user_token" to userToken!!)

        val request = object : VolleyFileUploadRequest(
                Method.POST,
                url,
                {
                    println("response is: $it")
                    try {
                        val jsonObject = JSONObject(String(it.data))
                        lastError = parseError(jsonObject)
                        if (lastError == null) {
                            success(jsonObject
                                    .getObjectSafe("response")
                                    ?.getObjectSafe("track"))
                        }
                    } catch (e: JSONException) {
                        // API is not stable
                        println("JSONException is: $e")
                    }
                },
                {
                    println("error is: $it")
                }
        ) {
            override fun getByteData(): MutableMap<String, FileDataPart> {
                var params = HashMap<String, FileDataPart>()
                val fileData = byteArrayOf(14, 15, 16, 17, 0, 0) // file.readBytes()
                params["file"] = FileDataPart("file", fileData, "csv")
                return params
            }

            override fun getParams(): MutableMap<String, String> {
                return parameters
            }
        }
        Volley.newRequestQueue(context).add(request)
    }

    private fun JSONObject.getObjectSafe(name: String): JSONObject? {
        if (this.has(name)) {
            return this.getJSONObject(name)
        }
        return null
    }

    private fun parseError(jsonObject: JSONObject): String? {
        return jsonObject.getObjectSafe("error")?.getString("description")
    }
}
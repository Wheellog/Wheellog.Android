package com.cooper.wheellog

import android.content.Context
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.cooper.wheellog.utils.VolleyMultipartRequest
import org.json.JSONObject
import java.net.HttpURLConnection

class ElectroClub(context: Context) {

    companion object {
        @JvmStatic lateinit var instance: ElectroClub
    }

    private val url = "https://electro.club/api/v1"
    private val accessToken = BuildConfig.ec_accessToken

    var userToken: String? = null
    var lastError: String? = null
    var errorListener: ((String)->Unit)? = null
    var requestQueue = Volley.newRequestQueue(context)

    fun login(email: String, password: String, done: (String?) -> Unit) {
        userToken = null
        send(Request.Method.POST, mutableMapOf(
                "method" to "login",
                "access_token" to accessToken,
                "email" to email,
                "password" to password), {
            userToken = it
                    .getObjectSafe("data")
                    ?.getObjectSafe("user")
                    ?.getString("user_token")
            done(userToken)
        }, {
            println("error is: $it")
            userToken = null
            done(null)
        })
    }

    fun uploadTrack(data: ByteArray, success: (JSONObject?) -> Unit) {
        userToken ?: return
        val parameters = mutableMapOf(
                "method" to "uploadTrack",
                "access_token" to accessToken,
                "user_token" to userToken!!)

        val request = object : VolleyMultipartRequest(
                Method.POST,
                url,
                {
                    val jsonObject = JSONObject(String(it.data))
                    success(jsonObject.getObjectSafe("data")?.getObjectSafe("track"))
                },
                {
                    parseRequestError(it)
                }
        ) {
            override fun getByteData(): MutableMap<String, DataPart>? {
                val params: MutableMap<String, DataPart> = HashMap()
                params["file"] = DataPart("file.csv", data, "text/csv")
                return params
            }

            override fun getParams(): MutableMap<String, String> {
                return parameters
            }
        }
        requestQueue.add(request)
    }

    private fun send(method: Int, parameters: MutableMap<String, String>,
                     success: (JSONObject) -> Unit, err: (String?) -> Unit) {
        // TODO: GET method params
        val req = object : StringRequest(
                method,
                url,
                {
                    val jsonObject = JSONObject(it)
                    success(jsonObject)
                },
                {
                    parseRequestError(it)
                    err(lastError)
                }) {
            override fun getParams(): MutableMap<String, String> {
                return parameters
            }
        }
        requestQueue.add(req)
    }

    private fun parseRequestError(it: VolleyError) {
        it.networkResponse.let { r ->
            lastError = if (r.statusCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                JSONObject(String(r.data))
                        .getObjectSafe("data")
                        ?.getStringSafe("error")
                        ?: "Unknown error"
            } else {
                "Response was not successful. code:${r.statusCode}"
            }
        }
        errorListener?.let { it(lastError ?: "Unknown error") }
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
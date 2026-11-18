package com.gramcall.ai.network

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CloudResponse(val code: Int, val body: JSONObject?)

/** Chhota HTTPS client (backend ke /api/auth aur /api/backup ke liye). Secrets APK mein nahi. */
object CloudClient {
    fun call(method: String, url: String, token: String?, body: JSONObject?): CloudResponse {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("Accept", "application/json")
            if (!token.isNullOrEmpty()) conn.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = try { if (text.isBlank()) null else JSONObject(text) } catch (e: Exception) { null }
            return CloudResponse(code, json)
        } finally {
            conn.disconnect()
        }
    }
}

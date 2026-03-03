package com.fastt.app.net

import com.fastt.app.model.ApiResponse
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class TiktokApi(
    private val apiKeyProvider: () -> String
) {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun fetch(url: String): Result<ApiResponse> {
        return try {
            val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            val req = Request.Builder()
                .url("https://api.ananta.qzz.io/api/v2/tiktok?url=$encoded")
                .header("x-api-key", apiKeyProvider())
                .get()
                .build()

            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) return Result.failure(RuntimeException("HTTP ${res.code}: $body"))
                val parsed = gson.fromJson(body, ApiResponse::class.java)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.kaushalvasava.apps.documentscanner.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

private const val BASE_URL = "https://dashboard.miracleai.in"
private const val TAG = "TemplatesApi"

object TemplatesApi {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    /**
     * C/H: Fetch the user's templates for the template picker.
     * Mirrors web: GET /users/{userId}/templates
     */
    suspend fun getUserTemplates(
        userId: Int,
        accessToken: String
    ): ApiResult<List<Template>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/users/$userId/templates")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val adapter = moshi.adapter(UserTemplatesResponse::class.java)
                val parsed = adapter.fromJson(body)
                ApiResult.Success(parsed?.templates ?: emptyList())
            } else {
                Log.e(TAG, "getUserTemplates failed [${response.code}]: $body")
                ApiResult.Error("Failed to load templates: ${response.code}", response.code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserTemplates exception", e)
            ApiResult.Error("Network error loading templates")
        }
    }
}

package com.kaushalvasava.apps.documentscanner.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor

private const val BASE_URL = "https://dashboard.miracleai.in"
private const val TAG = "CreditsApi"

object CreditsApi {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    /**
     * D: Check remaining credits for the user.
     * Mirrors web hook useDocumentUpload → creditsApi.checkCredits(userId, 1)
     * Endpoint: POST /credits/user/{userId}/check  { amount: 1 }
     */
    suspend fun checkCredits(
        userId: Int,
        accessToken: String
    ): ApiResult<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = """{"amount":1}"""
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/api/credits/user/$userId/check")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(jsonBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val adapter = moshi.adapter(CreditCheckResponse::class.java)
                val parsed = adapter.fromJson(body)
                val balance = parsed?.userInfo?.currentBalance ?: 0
                ApiResult.Success(balance)
            } else {
                Log.e(TAG, "checkCredits failed [${response.code}]: $body")
                ApiResult.Error("Credits unavailable", response.code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkCredits exception", e)
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

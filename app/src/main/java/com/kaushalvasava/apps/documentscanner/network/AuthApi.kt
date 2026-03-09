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
private const val TAG = "AuthApi"

object AuthApi {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // CookieJar to store the HttpOnly refresh_token cookie
    private val cookieJar = InMemoryCookieJar()

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(loggingInterceptor)
        .build()

    suspend fun login(email: String, password: String): ApiResult<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val loginRequestAdapter = moshi.adapter(LoginRequest::class.java)
                val body = loginRequestAdapter.toJson(LoginRequest(email, password))
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/api/users/login")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val adapter = moshi.adapter(LoginResponse::class.java)
                    val loginResponse = adapter.fromJson(responseBody)
                    if (loginResponse != null) {
                        ApiResult.Success(loginResponse)
                    } else {
                        ApiResult.Error("Invalid response from server")
                    }
                } else {
                    ApiResult.Error("Login failed: ${response.message}", response.code)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun refreshToken(): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/users/auth/refresh")
                .post("".toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val adapter = moshi.adapter(RefreshResponse::class.java)
                val refreshResponse = adapter.fromJson(responseBody)
                if (refreshResponse != null) {
                    ApiResult.Success(refreshResponse.accessToken)
                } else {
                    ApiResult.Error("Failed to parse refresh response")
                }
            } else {
                ApiResult.Error("Refresh failed: ${response.message}", response.code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh error", e)
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    fun getRefreshToken(): String? = cookieJar.getRefreshToken()

}

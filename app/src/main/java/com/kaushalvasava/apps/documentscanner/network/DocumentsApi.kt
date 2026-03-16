package com.kaushalvasava.apps.documentscanner.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull

private const val BASE_URL = "https://dashboard.miracleai.in"
private const val TAG = "DocumentsApi"

object DocumentsApi {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    /**
     * A: Fetch recent documents for the user.
     * Mirrors web: GET /users/{userId}/documents
     */
    suspend fun getUserDocuments(
        userId: Int,
        accessToken: String
    ): ApiResult<List<UploadedDocument>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/users/$userId/documents")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val adapter = moshi.adapter(UserDocumentsResponse::class.java)
                val parsed = adapter.fromJson(body)
                ApiResult.Success(parsed?.documents ?: emptyList())
            } else {
                Log.e(TAG, "getUserDocuments failed [${response.code}]: $body")
                ApiResult.Error("Failed to load documents (${response.code})", response.code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserDocuments exception", e)
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    /**
     * E: Poll document processing status.
     * Mirrors web: GET /documents/{docId}/status
     */
    suspend fun getDocumentStatus(
        docId: Int,
        accessToken: String
    ): ApiResult<DocumentStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/documents/$docId/status")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val adapter = moshi.adapter(DocumentStatusResponse::class.java)
                val parsed = adapter.fromJson(body)
                    ?: return@withContext ApiResult.Error("Empty response")
                ApiResult.Success(parsed)
            } else {
                ApiResult.Error("Status check failed (${response.code})", response.code)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    /**
     * I: Reprocess a document with a given template.
     * Mirrors web: POST /documents/{docId}/reprocess
     */
    suspend fun reprocessDocument(
        docId: Int,
        templateId: Int,
        accessToken: String
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = """{"template_id":$templateId}"""
                .toByteArray()
                .let { okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), it) }

            val request = Request.Builder()
                .url("$BASE_URL/api/documents/$docId/reprocess")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.Error("Reprocess failed (${response.code})", response.code)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

private fun String.toMediaTypeOrNull() = this.toMediaTypeOrNull()

package com.kaushalvasava.apps.documentscanner.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File

private const val BASE_URL = "https://dashboard.miracleai.in"
private const val TAG = "UploadApi"

sealed class UploadResult {
    object Success : UploadResult()
    data class Error(val message: String) : UploadResult()
}

object UploadApi {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * Uploads a single scanned file to the backend.
     *
     * @param file The JPEG (single page) or PDF (multiple pages) to upload.
     * @param accessToken The JWT bearer token.
     * @param userId The authenticated user's ID.
     * @param templateId Optional extraction template ID.
     * @param autoProcess If true, backend triggers OCR immediately after upload.
     */
    suspend fun uploadDocument(
        file: File,
        accessToken: String,
        userId: Int,
        templateId: String?,
        autoProcess: Boolean
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val mimeType = if (file.name.endsWith(".pdf")) "application/pdf" else "image/jpeg"
            val fileBody = file.asRequestBody(mimeType.toMediaType())

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, fileBody)
                .addFormDataPart("user_id", userId.toString())

            if (!templateId.isNullOrBlank()) {
                multipartBuilder.addFormDataPart("template_id", templateId)
                // only include auto_process when template is set
                multipartBuilder.addFormDataPart("auto_process", autoProcess.toString())
            }

            val request = Request.Builder()
                .url("$BASE_URL/api/documents/")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(multipartBuilder.build())
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                UploadResult.Success
            } else {
                val body = response.body?.string() ?: ""
                Log.e(TAG, "Upload failed [${response.code}]: $body")
                UploadResult.Error("Upload failed (${response.code}): ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            UploadResult.Error(e.message ?: "Network error during upload")
        }
    }
}

package com.kaushalvasava.apps.documentscanner.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
    data class Success(val document: UploadedDocument) : UploadResult()
    data class Error(val message: String, val code: Int = -1) : UploadResult()
}

object UploadApi {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    /**
     * Uploads a single scanned file to the backend and parses the returned document.
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
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                // Parse the returned UserDocument — server always returns the full object
                val adapter = moshi.adapter(UploadedDocument::class.java)
                val doc = try {
                    adapter.fromJson(body)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse upload response, using fallback: $e")
                    null
                }
                // Fallback: create a minimal doc if JSON parse fails
                val uploadedDoc = doc ?: UploadedDocument(
                    docId = 0,
                    originalFilename = file.name,
                    status = "pending"
                )
                UploadResult.Success(uploadedDoc)
            } else {
                Log.e(TAG, "Upload failed [${response.code}]: $body")
                UploadResult.Error("Upload failed (${response.code}): ${response.message}", response.code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            UploadResult.Error(e.message ?: "Network error during upload")
        }
    }
}

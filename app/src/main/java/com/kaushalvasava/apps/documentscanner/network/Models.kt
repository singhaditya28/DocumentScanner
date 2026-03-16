package com.kaushalvasava.apps.documentscanner.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─── Auth models ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val user: UserInfo,
    val auth: AuthInfo
)

@JsonClass(generateAdapter = true)
data class UserInfo(
    @Json(name = "user_id") val userId: Int,
    val name: String,
    val email: String,
    @Json(name = "is_approved") val isApproved: Boolean = false,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthInfo(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "token_type") val tokenType: String
)

@JsonClass(generateAdapter = true)
data class RefreshResponse(
    @Json(name = "access_token") val accessToken: String
)

// ─── Upload response ─────────────────────────────────────────────────────────

/** Mirrors the UserDocument returned by POST /api/documents/ */
@JsonClass(generateAdapter = true)
data class UploadedDocument(
    @Json(name = "doc_id") val docId: Int,
    @Json(name = "original_filename") val originalFilename: String,
    val status: String = "pending",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "template_id") val templateId: Int? = null,
    @Json(name = "template_name") val templateName: String? = null,
    @Json(name = "is_duplicate") val isDuplicate: Boolean = false,
    @Json(name = "duplicate_of_doc_id") val duplicateOfDocId: Int? = null
)

// ─── Document list ────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class UserDocumentsResponse(
    val documents: List<UploadedDocument>
)

// ─── Document status polling ──────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class DocumentStatusResponse(
    @Json(name = "document_id") val documentId: Int,
    val status: String,
    @Json(name = "original_filename") val originalFilename: String,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "processed_at") val processedAt: String? = null,
    @Json(name = "pet_name") val petName: String? = null,
    @Json(name = "has_ocr_data") val hasOcrData: Boolean = false
)

// ─── Templates ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class Template(
    @Json(name = "temp_id") val templateId: Int,
    val name: String,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class UserTemplatesResponse(
    val templates: List<Template>
)

// ─── Credits ─────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CreditCheckResponse(
    val success: Boolean,
    @Json(name = "has_sufficient_credits") val hasSufficientCredits: Boolean,
    val message: String,
    @Json(name = "user_info") val userInfo: CreditUserInfo? = null
)

@JsonClass(generateAdapter = true)
data class CreditUserInfo(
    @Json(name = "user_id") val userId: Int,
    @Json(name = "current_balance") val currentBalance: Int,
    @Json(name = "plan_type") val planType: String? = null
)

// ─── Generic result type ──────────────────────────────────────────────────────

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = -1) : ApiResult<Nothing>()
}

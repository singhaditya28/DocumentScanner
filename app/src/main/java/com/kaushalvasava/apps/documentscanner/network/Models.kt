package com.kaushalvasava.apps.documentscanner.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = -1) : ApiResult<Nothing>()
}

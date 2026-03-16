package com.kaushalvasava.apps.documentscanner.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaushalvasava.apps.documentscanner.data.SettingsDataStore
import com.kaushalvasava.apps.documentscanner.network.ApiResult
import com.kaushalvasava.apps.documentscanner.network.AuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(private val dataStore: SettingsDataStore) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    /** Flow of whether the user is currently logged in. */
    val isLoggedIn = dataStore.accessToken

    /** First name of the logged-in user for the greeting. */
    val userName = dataStore.userName

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password cannot be empty")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = AuthApi.login(email.trim(), password)) {
                is ApiResult.Success -> {
                    val refreshToken = AuthApi.getRefreshToken()
                    dataStore.saveSession(
                        accessToken = result.data.auth.accessToken,
                        refreshToken = refreshToken,
                        userId = result.data.user.userId,
                        userName = result.data.user.name,
                        userEmail = result.data.user.email
                    )
                    _uiState.value = AuthUiState.Success
                }
                is ApiResult.Error -> {
                    val msg = when (result.code) {
                        401, 400 -> "Incorrect email or password"
                        403 -> "Your account is not approved yet"
                        else -> result.message
                    }
                    _uiState.value = AuthUiState.Error(msg)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStore.clearSession()
            // The InMemoryCookieJar lives for the process lifetime;
            // clearing session DataStore is enough to prevent further authed calls.
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(SettingsDataStore(context)) as T
        }
    }
}

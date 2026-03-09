package com.kaushalvasava.apps.documentscanner.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaushalvasava.apps.documentscanner.data.SettingsDataStore
import com.kaushalvasava.apps.documentscanner.network.UploadApi
import com.kaushalvasava.apps.documentscanner.network.UploadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class UploadUiState {
    object Idle : UploadUiState()
    object Uploading : UploadUiState()
    data class Success(val message: String) : UploadUiState()
    data class Error(val message: String) : UploadUiState()
    object NotConfigured : UploadUiState()
}

class ScannerViewModel(private val dataStore: SettingsDataStore) : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uploadState: StateFlow<UploadUiState> = _uploadState

    val templateId = dataStore.templateId
    val autoProcess = dataStore.autoProcess

    fun upload(jpegUri: Uri?, pdfUri: Uri?, pageCount: Int) {
        _uploadState.value = UploadUiState.Uploading
        viewModelScope.launch {
            val accessToken = dataStore.accessToken.first()
            val userId = dataStore.userId.first()

            if (accessToken.isNullOrBlank() || userId == null) {
                _uploadState.value = UploadUiState.NotConfigured
                return@launch
            }

            val templateIdVal = dataStore.templateId.first()
            val autoProcessVal = dataStore.autoProcess.first()

            // Single page → upload JPEG; multiple pages → upload PDF
            val fileUri: Uri? = if (pageCount == 1) jpegUri ?: pdfUri else pdfUri ?: jpegUri

            if (fileUri == null) {
                _uploadState.value = UploadUiState.Error("No scanned file found")
                return@launch
            }

            val file = try {
                fileUri.toFile()
            } catch (e: Exception) {
                _uploadState.value = UploadUiState.Error("Could not access scanned file")
                return@launch
            }

            val result = UploadApi.uploadDocument(
                file = file,
                accessToken = accessToken,
                userId = userId,
                templateId = templateIdVal,
                autoProcess = autoProcessVal
            )

            _uploadState.value = when (result) {
                is UploadResult.Success -> UploadUiState.Success("Document uploaded successfully!")
                is UploadResult.Error -> UploadUiState.Error(result.message)
            }
        }
    }

    fun saveSettings(templateId: String?, autoProcess: Boolean) {
        viewModelScope.launch {
            dataStore.saveTemplateSettings(templateId, autoProcess)
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadUiState.Idle
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerViewModel(SettingsDataStore(context)) as T
        }
    }
}

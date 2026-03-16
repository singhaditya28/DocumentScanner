package com.kaushalvasava.apps.documentscanner.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaushalvasava.apps.documentscanner.data.SettingsDataStore
import com.kaushalvasava.apps.documentscanner.network.ApiResult
import com.kaushalvasava.apps.documentscanner.network.CreditsApi
import com.kaushalvasava.apps.documentscanner.network.DocumentsApi
import com.kaushalvasava.apps.documentscanner.network.Template
import com.kaushalvasava.apps.documentscanner.network.TemplatesApi
import com.kaushalvasava.apps.documentscanner.network.UploadApi
import com.kaushalvasava.apps.documentscanner.network.UploadedDocument
import com.kaushalvasava.apps.documentscanner.network.UploadResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ─── UI States ────────────────────────────────────────────────────────────────

sealed class UploadUiState {
    object Idle : UploadUiState()
    data class Uploading(val pageCount: Int) : UploadUiState()
    data class Success(
        val message: String,
        val docId: Int = 0,
        val isDuplicate: Boolean = false,
        val duplicateOfDocId: Int? = null
    ) : UploadUiState()
    data class Error(val message: String, val isSessionExpired: Boolean = false) : UploadUiState()
    object NotConfigured : UploadUiState()
}

data class LastUploadInfo(
    val pageCount: Int,
    val timestamp: Long,
    val type: String,       // "image" or "pdf"
    val docId: Int = 0,
    val docStatus: String = "pending"
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ScannerViewModel(
    private val dataStore: SettingsDataStore,
    private val appContext: Context
) : ViewModel() {

    // Upload state (A, E, F, G)
    private val _uploadState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uploadState: StateFlow<UploadUiState> = _uploadState

    // Recent documents list (A)
    private val _recentDocuments = MutableStateFlow<List<UploadedDocument>>(emptyList())
    val recentDocuments: StateFlow<List<UploadedDocument>> = _recentDocuments

    // Last upload (shown as card on HomePage)
    private val _lastUpload = MutableStateFlow<LastUploadInfo?>(null)
    val lastUpload: StateFlow<LastUploadInfo?> = _lastUpload

    // Available templates for the picker (C/H)
    private val _templates = MutableStateFlow<List<Template>>(emptyList())
    val templates: StateFlow<List<Template>> = _templates

    // Persisted settings
    val templateId = dataStore.templateId
    val autoProcess = dataStore.autoProcess

    // D: Credits remaining
    private val _creditsRemaining = MutableStateFlow<Int?>(null)
    val creditsRemaining: StateFlow<Int?> = _creditsRemaining

    // Dashboard Refresh State
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    // Status polling job (E)
    private var pollingJob: Job? = null

    // ─── Init ─────────────────────────────────────────────────────────────────

    init {
        // Load templates, documents, and credits as soon as ViewModel is created
        viewModelScope.launch {
            val token = dataStore.accessToken.first()
            val userId = dataStore.userId.first()
            if (!token.isNullOrBlank() && userId != null) {
                loadAll(userId, token)
            }
        }
    }

    private fun loadAll(userId: Int, token: String) {
        loadRecentDocuments(userId, token)
        loadTemplates(userId, token)
        loadCredits(userId, token)
    }

    // ─── A: Recent documents ──────────────────────────────────────────────────

    fun loadRecentDocuments(userId: Int? = null, token: String? = null) {
        viewModelScope.launch {
            val tok = token ?: dataStore.accessToken.first() ?: return@launch
            val uid = userId ?: dataStore.userId.first() ?: return@launch
            when (val result = DocumentsApi.getUserDocuments(uid, tok)) {
                is ApiResult.Success -> {
                    _recentDocuments.value = result.data.take(10)
                }
                is ApiResult.Error -> { /* silently ignore — list is non-critical */ }
            }
            _isRefreshing.value = false
        }
    }

    fun refresh(userId: Int? = null, token: String? = null) {
        _isRefreshing.value = true
        loadRecentDocuments(userId, token)
        loadCredits(userId, token)
        loadTemplates(userId, token)
    }

    // ─── C/H: Load templates ──────────────────────────────────────────────────

    fun loadTemplates(userId: Int? = null, token: String? = null) {
        viewModelScope.launch {
            val tok = token ?: dataStore.accessToken.first() ?: return@launch
            val uid = userId ?: dataStore.userId.first() ?: return@launch
            when (val result = TemplatesApi.getUserTemplates(uid, tok)) {
                is ApiResult.Success -> _templates.value = result.data
                is ApiResult.Error -> { /* graceful fallback — already handled in TemplatesApi */ }
            }
        }
    }

    // ─── D: Credits ───────────────────────────────────────────────────────────

    fun loadCredits(userId: Int? = null, token: String? = null) {
        viewModelScope.launch {
            val tok = token ?: dataStore.accessToken.first() ?: return@launch
            val uid = userId ?: dataStore.userId.first() ?: return@launch
            when (val result = CreditsApi.checkCredits(uid, tok)) {
                is ApiResult.Success -> _creditsRemaining.value = result.data
                is ApiResult.Error -> { /* silently ignore */ }
            }
        }
    }

    // ─── Upload (F, G, E trigger) ─────────────────────────────────────────────

    fun upload(
        jpegUri: Uri?,
        pdfUri: Uri?,
        pageCount: Int,
        selectedTemplateId: String? = null
    ) {
        _uploadState.value = UploadUiState.Uploading(pageCount)
        viewModelScope.launch {
            val accessToken = dataStore.accessToken.first()
            val userId = dataStore.userId.first()

            if (accessToken.isNullOrBlank() || userId == null) {
                _uploadState.value = UploadUiState.NotConfigured
                return@launch
            }

            // Use caller-supplied template or fall back to DataStore default
            val templateToUse = selectedTemplateId ?: dataStore.templateId.first()
            val autoProcessVal = dataStore.autoProcess.first()

            val isImage = pageCount == 1
            val fileUri: Uri? = if (isImage) jpegUri ?: pdfUri else pdfUri ?: jpegUri

            if (fileUri == null) {
                _uploadState.value = UploadUiState.Error("No scanned file found")
                return@launch
            }

            val file = try {
                fileUri.toFile()
            } catch (e: Exception) {
                _uploadState.value = UploadUiState.Error("Could not access the scanned file. Please try again.")
                return@launch
            }

            val result = UploadApi.uploadDocument(
                file = file,
                accessToken = accessToken,
                userId = userId,
                templateId = templateToUse,
                autoProcess = autoProcessVal
            )

            when (result) {
                is UploadResult.Success -> {
                    val doc = result.document
                    val pageLabel = if (pageCount == 1) "1 page" else "$pageCount pages"

                    // F: Show doc_id in success message
                    val docLabel = if (doc.docId > 0) " (Doc #${doc.docId})" else ""

                    _lastUpload.value = LastUploadInfo(
                        pageCount = pageCount,
                        timestamp = System.currentTimeMillis(),
                        type = if (isImage) "image" else "pdf",
                        docId = doc.docId,
                        docStatus = doc.status
                    )

                    // G: Duplicate detection alert
                    val isDuplicate = doc.isDuplicate
                    val message = when {
                        isDuplicate -> "⚠️ Duplicate detected — this looks like a previously uploaded document$docLabel."
                        else -> "Uploaded $pageLabel successfully$docLabel"
                    }

                    vibrateSuccess()
                    _uploadState.value = UploadUiState.Success(
                        message = message,
                        docId = doc.docId,
                        isDuplicate = isDuplicate,
                        duplicateOfDocId = doc.duplicateOfDocId
                    )

                    // E: Start status polling if doc is processing
                    if (doc.docId > 0 && doc.status in listOf("pending", "queued", "processing")) {
                        startStatusPolling(doc.docId, accessToken)
                    }

                    // Refresh document list and credits after upload
                    loadRecentDocuments(userId, accessToken)
                    loadCredits(userId, accessToken)
                }
                is UploadResult.Error -> {
                    val isExpired = result.code == 401 ||
                        result.message.contains("Unauthorized", ignoreCase = true)
                    val humanMessage = humanizeError(result.message, isExpired)
                    _uploadState.value = UploadUiState.Error(humanMessage, isExpired)
                }
            }
        }
    }

    // ─── E: Status polling ────────────────────────────────────────────────────

    private fun startStatusPolling(docId: Int, accessToken: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var attempts = 0
            while (isActive && attempts < 20) {
                delay(5_000L) // poll every 5 seconds (same cadence as web)
                attempts++
                when (val result = DocumentsApi.getDocumentStatus(docId, accessToken)) {
                    is ApiResult.Success -> {
                        val status = result.data.status
                        // Update the last upload card with current status
                        _lastUpload.value = _lastUpload.value?.copy(docStatus = status)
                        // Update matching doc in recent list
                        _recentDocuments.value = _recentDocuments.value.map { doc ->
                            if (doc.docId == docId) doc.copy(status = status) else doc
                        }
                        // Stop polling for terminal states
                        if (status in listOf("processed", "ready_for_export", "exported", "failed", "rejected")) {
                            break
                        }
                    }
                    is ApiResult.Error -> break // stop on error
                }
            }
        }
    }

    // ─── I: Reprocess ─────────────────────────────────────────────────────────

    fun reprocessDocument(docId: Int, templateId: Int) {
        viewModelScope.launch {
            val token = dataStore.accessToken.first() ?: return@launch
            DocumentsApi.reprocessDocument(docId, templateId, token)
            // After reprocess, reload documents list and start polling
            val uid = dataStore.userId.first() ?: return@launch
            loadRecentDocuments(uid, token)
            startStatusPolling(docId, token)
        }
    }

    // ─── Settings ─────────────────────────────────────────────────────────────

    fun saveSettings(templateId: String?, autoProcess: Boolean) {
        viewModelScope.launch {
            dataStore.saveTemplateSettings(templateId, autoProcess)
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun humanizeError(raw: String, isExpired: Boolean): String {
        return when {
            isExpired -> "Your session has expired. Please sign in again."
            raw.contains("timeout", ignoreCase = true) ||
                raw.contains("timed out", ignoreCase = true) ->
                "Upload timed out. Check your internet connection and try again."
            raw.contains("Unable to resolve host", ignoreCase = true) ||
                raw.contains("Failed to connect", ignoreCase = true) ||
                raw.contains("Network", ignoreCase = true) ->
                "Couldn't reach the server. Check your connection and try again."
            raw.contains("500") || raw.contains("Server Error", ignoreCase = true) ->
                "Something went wrong on the server. Please try again shortly."
            raw.contains("413") || raw.contains("too large", ignoreCase = true) ->
                "The file is too large to upload. Try scanning fewer pages."
            else -> "Upload failed. Please try again."
        }
    }

    private fun vibrateSuccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
            }
        } catch (_: Exception) { }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerViewModel(SettingsDataStore(context), context.applicationContext) as T
        }
    }
}

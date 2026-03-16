package com.kaushalvasava.apps.documentscanner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scanner_prefs")

class SettingsDataStore(private val context: Context) {

    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = intPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val TEMPLATE_ID = stringPreferencesKey("template_id")
        val AUTO_PROCESS = booleanPreferencesKey("auto_process")
        // Stores the web-compatible theme key, e.g. "light", "dark", "theme-miracle-orange"
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val userId: Flow<Int?> = context.dataStore.data.map { it[USER_ID] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL] }
    val templateId: Flow<String?> = context.dataStore.data.map { it[TEMPLATE_ID] }
    val autoProcess: Flow<Boolean> = context.dataStore.data.map { it[AUTO_PROCESS] ?: false }
    val appTheme: Flow<String?> = context.dataStore.data.map { it[APP_THEME] }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: Int,
        userName: String,
        userEmail: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[USER_ID] = userId
            prefs[USER_NAME] = userName
            prefs[USER_EMAIL] = userEmail
            refreshToken?.let { prefs[REFRESH_TOKEN] = it }
        }
    }

    suspend fun saveTemplateSettings(templateId: String?, autoProcess: Boolean) {
        context.dataStore.edit { prefs ->
            if (templateId.isNullOrBlank()) {
                prefs.remove(TEMPLATE_ID)
            } else {
                prefs[TEMPLATE_ID] = templateId
            }
            prefs[AUTO_PROCESS] = autoProcess
        }
    }

    suspend fun saveTheme(themeWebKey: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_THEME] = themeWebKey
        }
    }

    suspend fun saveRefreshedToken(newAccessToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = newAccessToken
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}

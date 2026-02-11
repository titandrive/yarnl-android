package com.yarnl.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yarnl_preferences")

class PreferencesRepository(private val context: Context) {

    val serverUrlFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SERVER_URL]
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: false
    }

    val fcmTokenRegisteredFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FCM_TOKEN_REGISTERED] ?: false
    }

    suspend fun getServerUrl(): String? {
        return context.dataStore.data.first()[PreferencesKeys.SERVER_URL]
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SERVER_URL] = url.trimEnd('/')
        }
    }

    suspend fun getFcmToken(): String? {
        return context.dataStore.data.first()[PreferencesKeys.FCM_TOKEN]
    }

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FCM_TOKEN] = token
        }
    }

    suspend fun setFcmTokenRegistered(registered: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FCM_TOKEN_REGISTERED] = registered
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

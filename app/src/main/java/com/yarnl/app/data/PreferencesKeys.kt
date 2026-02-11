package com.yarnl.app.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    val SERVER_URL = stringPreferencesKey("server_url")
    val FCM_TOKEN = stringPreferencesKey("fcm_token")
    val FCM_TOKEN_REGISTERED = booleanPreferencesKey("fcm_token_registered")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
}

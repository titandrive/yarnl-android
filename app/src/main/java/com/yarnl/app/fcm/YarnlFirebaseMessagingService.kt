package com.yarnl.app.fcm

import android.webkit.CookieManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yarnl.app.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class YarnlFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = PreferencesRepository(applicationContext)
            prefs.saveFcmToken(token)
            prefs.setFcmTokenRegistered(false)
            registerTokenWithServer(prefs, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.data["title"] ?: message.notification?.title ?: "Yarnl"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val type = message.data["type"]
        val url = message.data["url"]

        NotificationHelper(this).showNotification(title, body, type, url)
    }

    companion object {
        fun registerTokenWithServer(prefs: PreferencesRepository, token: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val serverUrl = prefs.getServerUrl() ?: return@launch
                    val cookie = CookieManager.getInstance().getCookie(serverUrl)
                        ?: return@launch

                    val url = URL("$serverUrl/api/fcm/register")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Cookie", cookie)
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000
                    connection.doOutput = true

                    val body = """{"token":"$token","platform":"android"}"""
                    connection.outputStream.use { it.write(body.toByteArray()) }

                    val responseCode = connection.responseCode
                    connection.disconnect()

                    if (responseCode in 200..299) {
                        prefs.setFcmTokenRegistered(true)
                    }
                } catch (_: Exception) {
                    // Registration failed, will retry on next app launch
                }
            }
        }
    }
}

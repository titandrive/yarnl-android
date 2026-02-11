package com.yarnl.app.util

import android.webkit.URLUtil
import java.net.HttpURLConnection
import java.net.URL

object UrlValidator {

    fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false
        return URLUtil.isValidUrl(trimmed)
    }

    fun normalizeUrl(url: String): String {
        return url.trim().trimEnd('/')
    }

    /**
     * Performs a HEAD request to check if the server is reachable.
     * Must be called from a background thread.
     */
    fun testConnection(url: String): ConnectionResult {
        return try {
            val connection = URL(normalizeUrl(url)).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            val responseCode = connection.responseCode
            connection.disconnect()
            if (responseCode in 200..399) {
                ConnectionResult.Success
            } else {
                ConnectionResult.Error("Server returned HTTP $responseCode")
            }
        } catch (e: javax.net.ssl.SSLException) {
            ConnectionResult.Error("SSL error: ${e.localizedMessage ?: "Certificate issue"}")
        } catch (e: java.net.ConnectException) {
            ConnectionResult.Error("Could not connect to server")
        } catch (e: java.net.SocketTimeoutException) {
            ConnectionResult.Error("Connection timed out")
        } catch (e: Exception) {
            ConnectionResult.Error(e.localizedMessage ?: "Connection failed")
        }
    }

    sealed class ConnectionResult {
        data object Success : ConnectionResult()
        data class Error(val message: String) : ConnectionResult()
    }
}

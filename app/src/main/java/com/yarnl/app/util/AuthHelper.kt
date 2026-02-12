package com.yarnl.app.util

import android.webkit.CookieManager
import java.io.OutputStreamWriter
import java.net.CookieHandler
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pre-authenticates with the Yarnl server before the WebView loads.
 * Uses cookies from CookieManager (shared with WebView) and syncs
 * any new session cookies back.
 *
 * Must be called from a background thread.
 */
object AuthHelper {

    /**
     * Checks if the current session is valid, and if not, attempts
     * auto-login for single-user mode.
     * Returns true if a fresh login was performed successfully.
     * Returns false if session was already valid or login failed.
     */
    fun ensureAuthenticated(serverUrl: String): Boolean {
        // Disable Java's default cookie handler so it doesn't interfere
        CookieHandler.setDefault(null)

        // First, check if we already have a valid session
        if (checkSession(serverUrl)) {
            return false // already authenticated, no login performed
        }

        // Session expired or missing — check auth mode
        val mode = getAuthMode(serverUrl) ?: return false

        // Only auto-login for single-user mode
        if (mode.mode != "single") return false

        val username = mode.adminUsername ?: return false

        // Auto-login
        return login(serverUrl, username)
    }

    private fun checkSession(serverUrl: String): Boolean {
        return try {
            val connection = URL("$serverUrl/api/auth/me").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            attachCookies(connection, serverUrl)

            val code = connection.responseCode
            syncCookies(connection, serverUrl)
            connection.disconnect()
            code == 200
        } catch (_: Exception) {
            false
        }
    }

    private data class AuthMode(val mode: String, val adminUsername: String?)

    private fun getAuthMode(serverUrl: String): AuthMode? {
        return try {
            val connection = URL("$serverUrl/api/auth/mode").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000

            val code = connection.responseCode
            if (code != 200) {
                connection.disconnect()
                return null
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val mode = Regex("\"mode\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            val username = Regex("\"adminUsername\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)

            if (mode != null) AuthMode(mode, username) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun login(serverUrl: String, username: String): Boolean {
        return try {
            val connection = URL("$serverUrl/api/auth/login").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("Content-Type", "application/json")

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write("{\"username\":\"$username\"}")
            writer.flush()
            writer.close()

            val code = connection.responseCode
            syncCookies(connection, serverUrl)
            connection.disconnect()
            code == 200
        } catch (_: Exception) {
            false
        }
    }

    private fun attachCookies(connection: HttpURLConnection, serverUrl: String) {
        val cookies = CookieManager.getInstance().getCookie(serverUrl)
        if (cookies != null) {
            connection.setRequestProperty("Cookie", cookies)
        }
    }

    private fun syncCookies(connection: HttpURLConnection, serverUrl: String) {
        val cookieManager = CookieManager.getInstance()
        val headers = connection.headerFields ?: return
        for ((key, values) in headers) {
            if (key != null && key.equals("Set-Cookie", ignoreCase = true)) {
                for (cookie in values) {
                    cookieManager.setCookie(serverUrl, cookie)
                }
            }
        }
        cookieManager.flush()
    }
}

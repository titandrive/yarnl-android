package com.yarnl.app.util

import android.webkit.CookieManager

object CookieHelper {

    fun setupCookieManager() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
    }

    fun flushCookies() {
        CookieManager.getInstance().flush()
    }

    fun getCookiesForUrl(url: String): String? {
        return CookieManager.getInstance().getCookie(url)
    }

    fun clearCookiesForUrl(url: String) {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(url) ?: return
        cookies.split(";").forEach { cookie ->
            val name = cookie.split("=").firstOrNull()?.trim() ?: return@forEach
            cookieManager.setCookie(url, "$name=; Max-Age=0")
        }
        cookieManager.flush()
    }
}

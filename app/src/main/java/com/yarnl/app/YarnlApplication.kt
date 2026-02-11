package com.yarnl.app

import android.app.Application
import com.yarnl.app.util.CookieHelper

class YarnlApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CookieHelper.setupCookieManager()
    }
}

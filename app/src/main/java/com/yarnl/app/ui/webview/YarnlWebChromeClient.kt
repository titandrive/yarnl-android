package com.yarnl.app.ui.webview

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher

class YarnlWebChromeClient(
    private val fileChooserLauncher: ActivityResultLauncher<Intent>,
    private val onProgressChanged: (Int) -> Unit,
) : WebChromeClient() {

    var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged.invoke(newProgress)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        // Cancel any pending callback to prevent the WebView file input from getting stuck
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        val intent = fileChooserParams?.createIntent() ?: return false

        if (fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        try {
            fileChooserLauncher.launch(intent)
        } catch (e: Exception) {
            this.filePathCallback?.onReceiveValue(null)
            this.filePathCallback = null
            return false
        }

        return true
    }

    fun onFileChooserResult(uris: Array<Uri>?) {
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }
}

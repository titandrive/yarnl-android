package com.yarnl.app.ui.webview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yarnl.app.navigation.ShortcutAction
import com.yarnl.app.ui.theme.YarnlDarker
import com.yarnl.app.ui.theme.YarnlOrange
import com.yarnl.app.ui.theme.YarnlTextDim
import com.yarnl.app.util.AuthHelper
import com.yarnl.app.util.CookieHelper
import com.yarnl.app.util.ShortcutHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    serverUrl: String,
    onOpenSettings: () -> Unit,
    fileChooserLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    chromeClient: YarnlWebChromeClient,
    shortcutAction: ShortcutAction? = null,
    onShortcutActionConsumed: () -> Unit = {},
    onContentReady: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Could not connect to server") }
    var pendingShortcutAction by remember { mutableStateOf(shortcutAction) }
    var isPageReady by remember { mutableStateOf(false) }

    // Compute initial URL based on shortcut action
    val initialUrl = remember(serverUrl, shortcutAction) {
        when (shortcutAction) {
            is ShortcutAction.Library -> "$serverUrl#library"
            is ShortcutAction.Current -> "$serverUrl#current"
            is ShortcutAction.Pattern -> "$serverUrl#pattern/${shortcutAction.patternId}"
            is ShortcutAction.Upload -> serverUrl // File picker opens after page loads
            null -> serverUrl
        }
    }

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#13111C"))

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                cacheMode = WebSettings.LOAD_DEFAULT
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                userAgentString = "$userAgentString YarnlAndroid/1.0"
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            // JavaScript interface for native callbacks
            addJavascriptInterface(object {
                @JavascriptInterface
                fun updateRecentPattern(id: String, name: String) {
                    ShortcutHelper.updateRecentPatternShortcut(context, id, name)
                }

                @JavascriptInterface
                fun onShortcutHandled() {
                    pendingShortcutAction = null
                    onShortcutActionConsumed()
                }

                @JavascriptInterface
                fun onPageReady() {
                    isPageReady = true
                    onContentReady()
                }

            }, "YarnlApp")

            // Start invisible — only show once page content is ready
            visibility = android.view.View.INVISIBLE

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    // Pre-set auth flag so web app skips login render
                    view?.evaluateJavascript(
                        "try{localStorage.setItem('authenticated','true')}catch(e){}",
                        null,
                    )
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isLoading = false
                    hasError = false

                    // Force-hide login container, then reveal WebView + dismiss splash
                    view?.evaluateJavascript("""
                        (function() {
                            var lc = document.getElementById('login-container');
                            if (lc) lc.style.setProperty('display', 'none', 'important');
                            setTimeout(function() {
                                YarnlApp.onPageReady();
                            }, 300);
                        })()
                    """.trimIndent(), null)
                    view?.postDelayed({ view.visibility = android.view.View.VISIBLE }, 400)

                    // Keep localStorage.authenticated synced
                    view?.evaluateJavascript(
                        "fetch('/api/auth/me').then(function(r){if(r.ok)localStorage.setItem('authenticated','true')}).catch(function(){})",
                        null,
                    )

                    // Handle upload shortcut — delay to let web app finish tab restoration
                    if (pendingShortcutAction is ShortcutAction.Upload) {
                        view?.evaluateJavascript("""
                            setTimeout(function() {
                                if (typeof showUploadPanel === 'function') {
                                    showUploadPanel();
                                    if (typeof YarnlApp !== 'undefined') YarnlApp.onShortcutHandled();
                                }
                            }, 1500)
                        """.trimIndent(), null)
                    } else if (pendingShortcutAction != null) {
                        pendingShortcutAction = null
                        onShortcutActionConsumed()
                    }

                    // Update dynamic shortcut with most recent pattern
                    view?.evaluateJavascript("""
                        (function() {
                            fetch('/api/patterns/recent')
                                .then(function(r) { return r.json(); })
                                .then(function(data) {
                                    if (data && data.id) {
                                        return fetch('/api/patterns/' + data.id)
                                            .then(function(r) { return r.json(); })
                                            .then(function(p) {
                                                YarnlApp.updateRecentPattern(String(p.id), p.name || 'Recent Pattern');
                                            });
                                    }
                                })
                                .catch(function() {});
                        })()
                    """.trimIndent(), null)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    val url = request?.url ?: return false
                    val serverHost = Uri.parse(serverUrl).host

                    // Keep same-host navigation in WebView
                    if (url.host == serverHost) return false

                    // Handle special schemes
                    if (url.scheme == "intent" || url.scheme == "market") {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, url))
                        } catch (_: Exception) { }
                        return true
                    }

                    // Open external links in system browser
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, url))
                    } catch (_: Exception) { }
                    return true
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    super.onReceivedError(view, request, error)
                    // Only handle main frame errors
                    if (request?.isForMainFrame == true) {
                        hasError = true
                        isLoading = false
                        errorMessage = error?.description?.toString() ?: "Could not connect to server"
                    }
                }
            }

            webChromeClient = chromeClient

            // Download listener
            setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val cookie = CookieManager.getInstance().getCookie(url)

                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    addRequestHeader("Cookie", cookie)
                    addRequestHeader("User-Agent", userAgent)
                    setTitle(filename)
                    setDescription("Downloading from Yarnl")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                }

                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)
                Toast.makeText(context, "Downloading $filename", Toast.LENGTH_SHORT).show()
            }

            loadUrl(initialUrl)
        }
    }

    // Background pre-auth: only reload if we had to perform a fresh login
    LaunchedEffect(Unit) {
        val hadCookie = CookieManager.getInstance().getCookie(serverUrl)
            ?.contains("session_id") == true
        if (!hadCookie) {
            val loggedIn = withContext(Dispatchers.IO) {
                try {
                    AuthHelper.ensureAuthenticated(serverUrl)
                } catch (_: Exception) { false }
            }
            if (loggedIn) {
                webView.reload()
            }
        }
    }

    // Handle shortcut action changes when app is already running (onNewIntent)
    if (shortcutAction != null && shortcutAction != pendingShortcutAction) {
        pendingShortcutAction = shortcutAction
        when (shortcutAction) {
            is ShortcutAction.Library -> webView.loadUrl("$serverUrl#library")
            is ShortcutAction.Current -> webView.loadUrl("$serverUrl#current")
            is ShortcutAction.Pattern -> webView.loadUrl("$serverUrl#pattern/${shortcutAction.patternId}")
            is ShortcutAction.Upload -> {
                pendingShortcutAction = null
                onShortcutActionConsumed()
                webView.evaluateJavascript(
                    "if (typeof showUploadPanel === 'function') showUploadPanel();",
                    null,
                )
            }
        }
    }

    // Update chrome client progress callback
    remember(chromeClient) {
        chromeClient
    }

    // Lifecycle observer to flush cookies
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> CookieHelper.flushCookies()
                Lifecycle.Event.ON_STOP -> CookieHelper.flushCookies()
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Back button handling
    BackHandler(enabled = true) {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasError) {
            // Error state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(
                    text = "Connection Error",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = YarnlTextDim,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        hasError = false
                        isLoading = true
                        webView.reload()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YarnlOrange,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Retry")
                }
            }
        } else {
            // WebView — starts INVISIBLE, shown after login container is hidden
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Loading progress bar
        AnimatedVisibility(
            visible = isLoading && !hasError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = systemBarsPadding.calculateTopPadding()),
        ) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = YarnlOrange,
                trackColor = YarnlDarker,
            )
        }

        // Settings FAB
        FilledIconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = 16.dp + navBarPadding.calculateBottomPadding(),
                )
                .size(40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                contentColor = YarnlTextDim,
            ),
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

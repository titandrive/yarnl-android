package com.yarnl.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.yarnl.app.data.PreferencesRepository
import com.yarnl.app.fcm.YarnlFirebaseMessagingService
import com.yarnl.app.navigation.ShortcutAction
import com.yarnl.app.navigation.YarnlNavGraph
import com.yarnl.app.ui.theme.YarnlTheme
import com.yarnl.app.ui.webview.YarnlWebChromeClient
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var chromeClient: YarnlWebChromeClient

    companion object {
        const val EXTRA_SHORTCUT_ACTION = "shortcut_action"
        const val EXTRA_PATTERN_ID = "pattern_id"
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val uris: Array<Uri>? = when {
                data?.clipData != null -> {
                    Array(data.clipData!!.itemCount) { i ->
                        data.clipData!!.getItemAt(i).uri
                    }
                }
                data?.data != null -> arrayOf(data.data!!)
                else -> null
            }
            chromeClient.onFileChooserResult(uris)
        } else {
            // User cancelled - must still call with null to avoid breaking the file input
            chromeClient.onFileChooserResult(null)
        }
    }

    val isContentReady = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isContentReady.value }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesRepository = PreferencesRepository(applicationContext)

        chromeClient = YarnlWebChromeClient(
            fileChooserLauncher = fileChooserLauncher,
            onProgressChanged = { /* handled by compose state */ },
        )

        // Extract shortcut action from launch intent
        extractShortcutAction(intent)?.let { shortcutAction.value = it }

        // Attempt to register FCM token on launch
        registerFcmToken()

        setContent {
            YarnlTheme {
                val serverUrl by preferencesRepository.serverUrlFlow.collectAsState(initial = null)
                val hasServerUrl = serverUrl != null
                val navController = rememberNavController()

                // Recreate chrome client with progress callback wired to compose
                val composeChromeClient = remember(chromeClient) { chromeClient }

                YarnlNavGraph(
                    navController = navController,
                    hasServerUrl = hasServerUrl,
                    serverUrl = serverUrl,
                    preferencesRepository = preferencesRepository,
                    fileChooserLauncher = fileChooserLauncher,
                    chromeClient = composeChromeClient,
                    shortcutAction = shortcutAction.value,
                    onShortcutActionConsumed = { shortcutAction.value = null },
                    onContentReady = { isContentReady.value = true },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractShortcutAction(intent)?.let { shortcutAction.value = it }
    }

    private val shortcutAction = mutableStateOf<ShortcutAction?>(null)

    private fun extractShortcutAction(intent: Intent): ShortcutAction? {
        val action = intent.getStringExtra(EXTRA_SHORTCUT_ACTION) ?: return null
        return when (action) {
            "library" -> ShortcutAction.Library
            "current" -> ShortcutAction.Current
            "upload" -> ShortcutAction.Upload
            "pattern" -> {
                val patternId = intent.getStringExtra(EXTRA_PATTERN_ID) ?: return null
                ShortcutAction.Pattern(patternId)
            }
            else -> null
        }
    }

    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CoroutineScope(Dispatchers.IO).launch {
                preferencesRepository.saveFcmToken(token)
                if (preferencesRepository.getServerUrl() != null) {
                    YarnlFirebaseMessagingService.registerTokenWithServer(
                        preferencesRepository, token
                    )
                }
            }
        }
    }
}

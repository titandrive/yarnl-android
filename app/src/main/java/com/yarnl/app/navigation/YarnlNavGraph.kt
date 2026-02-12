package com.yarnl.app.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yarnl.app.data.PreferencesRepository
import com.yarnl.app.ui.settings.SettingsScreen
import com.yarnl.app.ui.setup.SetupScreen
import com.yarnl.app.ui.webview.WebViewScreen
import com.yarnl.app.ui.webview.YarnlWebChromeClient

sealed class ShortcutAction {
    data object Library : ShortcutAction()
    data object Current : ShortcutAction()
    data object Upload : ShortcutAction()
    data class Pattern(val patternId: String) : ShortcutAction()
}

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object WebView : Screen("webview")
    data object Settings : Screen("settings")
}

@Composable
fun YarnlNavGraph(
    navController: NavHostController,
    hasServerUrl: Boolean,
    serverUrl: String?,
    preferencesRepository: PreferencesRepository,
    fileChooserLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    chromeClient: YarnlWebChromeClient,
    shortcutAction: ShortcutAction? = null,
    onShortcutActionConsumed: () -> Unit = {},
    onContentReady: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = if (hasServerUrl) Screen.WebView.route else Screen.Setup.route,
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(
                preferencesRepository = preferencesRepository,
                onServerConfigured = {
                    navController.navigate(Screen.WebView.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.WebView.route) {
            WebViewScreen(
                serverUrl = serverUrl ?: "",
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                fileChooserLauncher = fileChooserLauncher,
                chromeClient = chromeClient,
                shortcutAction = shortcutAction,
                onShortcutActionConsumed = onShortcutActionConsumed,
                onContentReady = onContentReady,
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                preferencesRepository = preferencesRepository,
                onBack = { navController.popBackStack() },
                onServerUrlChanged = {
                    navController.navigate(Screen.WebView.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}

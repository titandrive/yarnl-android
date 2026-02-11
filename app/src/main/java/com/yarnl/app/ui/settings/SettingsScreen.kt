package com.yarnl.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yarnl.app.BuildConfig
import com.yarnl.app.data.PreferencesRepository
import com.yarnl.app.ui.theme.YarnlOrange
import com.yarnl.app.ui.theme.YarnlTextDim
import com.yarnl.app.util.UrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesRepository: PreferencesRepository,
    onBack: () -> Unit,
    onServerUrlChanged: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val serverUrl by preferencesRepository.serverUrlFlow.collectAsState(initial = "")
    val notificationsEnabled by preferencesRepository.notificationsEnabledFlow.collectAsState(initial = false)
    val fcmRegistered by preferencesRepository.fcmTokenRegisteredFlow.collectAsState(initial = false)

    var showUrlDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            preferencesRepository.setNotificationsEnabled(granted)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Server section
            Text(
                text = "SERVER",
                style = MaterialTheme.typography.labelLarge,
                color = YarnlOrange,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Current server",
                style = MaterialTheme.typography.bodyMedium,
                color = YarnlTextDim,
            )
            Text(
                text = serverUrl ?: "Not configured",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showUrlDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Change Server URL")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(24.dp))

            // Notifications section
            Text(
                text = "NOTIFICATIONS",
                style = MaterialTheme.typography.labelLarge,
                color = YarnlOrange,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Push notifications",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = if (fcmRegistered) "Registered with server" else "Not registered",
                        style = MaterialTheme.typography.bodyMedium,
                        color = YarnlTextDim,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val permission = Manifest.permission.POST_NOTIFICATIONS
                            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                                notificationPermissionLauncher.launch(permission)
                                return@Switch
                            }
                        }
                        scope.launch {
                            preferencesRepository.setNotificationsEnabled(enabled)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = YarnlOrange,
                        checkedTrackColor = YarnlOrange.copy(alpha = 0.3f),
                    ),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(24.dp))

            // About section
            Text(
                text = "ABOUT",
                style = MaterialTheme.typography.labelLarge,
                color = YarnlOrange,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Yarnl",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = YarnlTextDim,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Change URL dialog
    if (showUrlDialog) {
        ChangeUrlDialog(
            currentUrl = serverUrl ?: "",
            onDismiss = { showUrlDialog = false },
            onConfirm = { newUrl ->
                showUrlDialog = false
                scope.launch {
                    preferencesRepository.saveServerUrl(newUrl)
                    onServerUrlChanged()
                }
            },
        )
    }
}

@Composable
private fun ChangeUrlDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var url by remember { mutableStateOf(currentUrl) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Change Server URL") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        errorMessage = null
                    },
                    label = { Text("Server URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { msg ->
                        { Text(text = msg, color = MaterialTheme.colorScheme.error) }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YarnlOrange,
                        focusedLabelColor = YarnlOrange,
                        cursorColor = YarnlOrange,
                    ),
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = YarnlOrange,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!UrlValidator.isValidUrl(url)) {
                        errorMessage = "Please enter a valid URL"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            UrlValidator.testConnection(url)
                        }
                        isLoading = false
                        when (result) {
                            is UrlValidator.ConnectionResult.Success -> {
                                onConfirm(UrlValidator.normalizeUrl(url))
                            }
                            is UrlValidator.ConnectionResult.Error -> {
                                errorMessage = result.message
                            }
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = YarnlOrange),
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = YarnlTextDim)
            }
        },
    )
}

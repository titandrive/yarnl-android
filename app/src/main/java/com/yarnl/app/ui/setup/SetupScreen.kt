package com.yarnl.app.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yarnl.app.data.PreferencesRepository
import com.yarnl.app.ui.theme.YarnlOrange
import com.yarnl.app.ui.theme.YarnlTextDim
import com.yarnl.app.util.UrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupScreen(
    preferencesRepository: PreferencesRepository,
    onServerConfigured: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun connect() {
        if (isLoading) return
        errorMessage = null

        if (!UrlValidator.isValidUrl(url)) {
            errorMessage = "Please enter a valid URL (e.g. https://yarnl.example.com)"
            return
        }

        isLoading = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                UrlValidator.testConnection(url)
            }
            isLoading = false
            when (result) {
                is UrlValidator.ConnectionResult.Success -> {
                    preferencesRepository.saveServerUrl(UrlValidator.normalizeUrl(url))
                    onServerConfigured()
                }
                is UrlValidator.ConnectionResult.Error -> {
                    errorMessage = result.message
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // App title
            Text(
                text = "Yarnl",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = YarnlOrange,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect to your Yarnl server",
                style = MaterialTheme.typography.bodyLarge,
                color = YarnlTextDim,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // URL input
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    errorMessage = null
                },
                label = { Text("Server URL") },
                placeholder = { Text("https://yarnl.example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { connect() },
                ),
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

            Spacer(modifier = Modifier.height(24.dp))

            // Connect button
            Button(
                onClick = { connect() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading && url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YarnlOrange,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Connect",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

package net.chasmine.oneline.ui

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.chasmine.oneline.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var repoUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is SettingsViewModel.UiState.Loaded) {
            val loaded = uiState as SettingsViewModel.UiState.Loaded
            repoUrl = loaded.repoUrl
            username = loaded.username
            token = loaded.token
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("GitHub設定", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = repoUrl,
            onValueChange = { repoUrl = it },
            label = { Text("リポジトリURL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("ユーザー名") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("アクセストークン") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.saveSettings(repoUrl, username, token) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存")
        }
        when (uiState) {
            is SettingsViewModel.UiState.Saving -> Text("保存中...", color = MaterialTheme.colorScheme.primary)
            is SettingsViewModel.UiState.SaveSuccess -> Text("保存しました！", color = MaterialTheme.colorScheme.primary)
            is SettingsViewModel.UiState.Error -> Text((uiState as SettingsViewModel.UiState.Error).message, color = MaterialTheme.colorScheme.error)
            else -> {}
        }
    }
}

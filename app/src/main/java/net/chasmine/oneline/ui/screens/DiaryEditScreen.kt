package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chasmine.oneline.ui.components.DiaryForm
import net.chasmine.oneline.ui.viewmodels.DiaryEditViewModel
import kotlinx.coroutines.launch

@Composable
fun DiaryEditScreen(
    date: String,
    onNavigateBack: () -> Unit,
    viewModel: DiaryEditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val scope = rememberCoroutineScope()
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(key1 = date) {
        viewModel.loadEntry(date)
    }

    LaunchedEffect(saveStatus) {
        when (saveStatus) {
            is DiaryEditViewModel.SaveStatus.Success -> {
                onNavigateBack()
            }
            is DiaryEditViewModel.SaveStatus.Error -> {
                errorMessage = (saveStatus as DiaryEditViewModel.SaveStatus.Error).message
                showErrorDialog = true
            }
            else -> {}
        }
    }

    when (uiState) {
        is DiaryEditViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is DiaryEditViewModel.UiState.Editing -> {
            val editState = uiState as DiaryEditViewModel.UiState.Editing

            DiaryForm(
                entry = editState.entry,
                isNew = editState.isNew,
                onContentChange = { viewModel.updateContent(it) },
                onSave = { viewModel.saveEntry() },
                onDelete = { showConfirmDeleteDialog = true },
                onNavigateBack = onNavigateBack
            )

            if (showConfirmDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDeleteDialog = false },
                    title = { Text("日記の削除") },
                    text = { Text("この日記を削除しますか？この操作は元に戻せません。") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmDeleteDialog = false
                                scope.launch {
                                    viewModel.deleteEntry()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("削除")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDeleteDialog = false }) {
                            Text("キャンセル")
                        }
                    }
                )
            }
        }
        is DiaryEditViewModel.UiState.Error -> {
            val errorState = uiState as DiaryEditViewModel.UiState.Error

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("エラーが発生しました: ${errorState.message}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) {
                        Text("戻る")
                    }
                }
            }
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("エラー") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (saveStatus is DiaryEditViewModel.SaveStatus.Saving) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 半透明の背景オーバーレイ
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {}
            
            // ローディングカード
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "保存中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
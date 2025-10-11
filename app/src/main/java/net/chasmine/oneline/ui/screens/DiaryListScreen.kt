package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chasmine.oneline.ui.components.DiaryCard
import net.chasmine.oneline.ui.components.TodayEntryForm
import net.chasmine.oneline.ui.components.TodayEntryCard
import net.chasmine.oneline.ui.viewmodels.DiaryListViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToNewEntry: () -> Unit,
    viewModel: DiaryListViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val todayEntry by viewModel.todayEntry.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val scope = rememberCoroutineScope()
    var showSyncStatusMessage by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadEntries()
    }

    LaunchedEffect(syncStatus) {
        when (syncStatus) {
            is DiaryListViewModel.SyncStatus.Success -> {
                syncStatusMessage = "同期が完了しました"
                showSyncStatusMessage = true
            }
            is DiaryListViewModel.SyncStatus.Error -> {
                syncStatusMessage = "同期に失敗しました: ${(syncStatus as DiaryListViewModel.SyncStatus.Error).message}"
                showSyncStatusMessage = true
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OneLine",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.syncRepository()
                            }
                        },
                        enabled = syncStatus !is DiaryListViewModel.SyncStatus.Syncing
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = "同期",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "設定",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            val isSyncing = syncStatus is DiaryListViewModel.SyncStatus.Syncing
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // 同期中の場合はメッセージを表示
                if (isSyncing) {
                    Card(
                        modifier = Modifier.padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = "同期中...",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                FloatingActionButton(
                    onClick = {
                        if (!isSyncing) {
                            onNavigateToNewEntry()
                        }
                    },
                    containerColor = if (isSyncing) 
                        MaterialTheme.colorScheme.surfaceVariant 
                    else 
                        MaterialTheme.colorScheme.primary
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新規作成",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (entries.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "まだ日記がありません",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "右下の＋ボタンから\n最初の日記を書いてみましょう",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 今日の日記の処理
                    if (todayEntry == null) {
                        // 今日の日記が存在しない場合は入力フォームを表示
                        item {
                            TodayEntryForm(
                                onSave = { content ->
                                    viewModel.saveTodayEntry(content)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        // 今日の日記が存在する場合は特別なカードを表示
                        item {
                            todayEntry?.let { entry ->
                                TodayEntryCard(
                                    entry = entry,
                                    onClick = {
                                        onNavigateToEdit(entry.date.toString())
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                    
                    // 今日以外の日記エントリー
                    items(entries.filter { it.date != LocalDate.now() }) { entry ->
                        DiaryCard(
                            entry = entry,
                            onClick = {
                                onNavigateToEdit(entry.date.toString())
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // 同期中の表示
            if (syncStatus is DiaryListViewModel.SyncStatus.Syncing) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp) // FABの上に表示
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "同期中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // 同期結果の表示
            if (showSyncStatusMessage) {
                LaunchedEffect(key1 = showSyncStatusMessage) {
                    // 3秒後に自動的に非表示にする
                    kotlinx.coroutines.delay(3000)
                    showSyncStatusMessage = false
                }

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (syncStatus is DiaryListViewModel.SyncStatus.Success)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = syncStatusMessage,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chasmine.oneline.ui.components.DiaryCard
import net.chasmine.oneline.ui.viewmodels.DiaryListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: DiaryListViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMoreData by viewModel.hasMoreData.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
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

    // 無限スクロールのトリガー
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= entries.size - 3 &&
                    hasMoreData &&
                    !isLoadingMore) {
                    viewModel.loadMoreEntries()
                }
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
                windowInsets = WindowInsets(0, 0, 0, 0),
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
                        text = "ボトムバーの＋ボタンから\n最初の日記を書いてみましょう",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // 全ての日記エントリー
                    items(
                        count = entries.size,
                        key = { index -> entries[index].date.toString() }
                    ) { index ->
                        val entry = entries[index]
                        DiaryCard(
                            entry = entry,
                            onClick = {
                                onNavigateToEdit(entry.date.toString())
                            },
                            showTopLine = index > 0,
                            showBottomLine = index < entries.size - 1
                        )
                    }

                    // ローディングインジケータ
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }
                }
            }

            // 同期中の表示
            if (syncStatus is DiaryListViewModel.SyncStatus.Syncing) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
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
                                containerColor = MaterialTheme.colorScheme.primaryContainer
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

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (syncStatus is DiaryListViewModel.SyncStatus.Success)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = syncStatusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (syncStatus is DiaryListViewModel.SyncStatus.Success)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
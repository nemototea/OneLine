package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chasmine.oneline.ui.components.DiaryCard
import net.chasmine.oneline.ui.components.LottieLoadingIndicator
import net.chasmine.oneline.ui.viewmodels.DiaryListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreenImpl(
    onNavigateToSettings: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: DiaryListViewModel
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMoreData by viewModel.hasMoreData.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val listState = rememberLazyListState()
    var showSyncStatusMessage by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadEntries()
    }

    // 同期の成功は一覧への反映で伝わるため、メッセージは失敗時のみ表示する
    LaunchedEffect(syncStatus) {
        when (syncStatus) {
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
                        style = MaterialTheme.typography.displayLarge
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = syncStatus !is DiaryListViewModel.SyncStatus.Syncing
                    ) {
                        if (syncStatus is DiaryListViewModel.SyncStatus.Syncing) {
                            // 同期中はアイコンを小さなスピナーに置き換えて控えめに伝える
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = "同期",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LottieLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    size = 150.dp
                )
            } else if (entries.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // 空状態でもpull-to-refreshが使えるようスクロール可能にする
                        .verticalScroll(rememberScrollState())
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
                                LottieLoadingIndicator(
                                    size = 60.dp
                                )
                            }
                        }
                    }
                }
            }

            // 同期失敗の表示（成功時は一覧への反映のみで通知しない）
            if (showSyncStatusMessage) {
                LaunchedEffect(key1 = showSyncStatusMessage) {
                    // 3秒後に自動的に非表示にする
                    kotlinx.coroutines.delay(3000)
                    showSyncStatusMessage = false
                }

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
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
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

package net.chasmine.oneline.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.chasmine.oneline.data.repository.RepositoryFactory
import net.chasmine.oneline.data.repository.RepositoryMode
import net.chasmine.oneline.data.model.DiaryEntry
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 日記リスト画面のViewModel（共通コード）
 *
 * 同期戦略（ローカルファースト）:
 * - 画面表示時はまず手元のデータを即座に表示する
 * - 自動同期は前回成功から一定時間(AUTO_SYNC_INTERVAL_MILLIS)経過時のみ実行
 * - pull-to-refresh・同期ボタンからの手動同期は常に実行
 *
 * @param repositoryFactory リポジトリファクトリー
 */
class DiaryListViewModel(
    private val repositoryFactory: RepositoryFactory
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _entries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries: StateFlow<List<DiaryEntry>> = _entries

    private val _todayEntry = MutableStateFlow<DiaryEntry?>(null)
    val todayEntry: StateFlow<DiaryEntry?> = _todayEntry

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _hasMoreData = MutableStateFlow(true)
    val hasMoreData: StateFlow<Boolean> = _hasMoreData

    private var allEntries: List<DiaryEntry> = emptyList()
    private var currentPage = 0
    private val pageSize = 20

    init {
        viewModelScope.launch {
            repositoryFactory.initialize()

            // まず手元のデータを即表示し、同期はバックグラウンドで行う
            loadEntries()
            autoSyncIfNeeded()
        }
    }

    fun loadEntries() {
        viewModelScope.launch {
            repositoryFactory.getAllEntries().collect { loaded ->
                // 内容が空のエントリーは表示しない（過去バージョンで空保存できた名残への防御）
                val diaryEntries = loaded.filter { it.content.isNotBlank() }
                allEntries = diaryEntries
                currentPage = 0
                _hasMoreData.value = allEntries.size > pageSize

                // 最初のページを読み込み
                _entries.value = allEntries.take(pageSize)

                // 今日の日記をチェック
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                _todayEntry.value = diaryEntries.find { it.date == today }
            }
        }
    }

    fun loadMoreEntries() {
        if (_isLoadingMore.value || !_hasMoreData.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true

            try {
                // 次のページを計算
                val nextPage = currentPage + 1
                val startIndex = nextPage * pageSize
                val endIndex = minOf(startIndex + pageSize, allEntries.size)

                if (startIndex < allEntries.size) {
                    // 新しいエントリーを追加
                    val newEntries = allEntries.subList(startIndex, endIndex)
                    _entries.value = _entries.value + newEntries
                    currentPage = nextPage

                    // まだデータがあるかチェック
                    _hasMoreData.value = endIndex < allEntries.size
                } else {
                    _hasMoreData.value = false
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    /**
     * 今日の日記を保存
     */
    fun saveTodayEntry(content: String) {
        viewModelScope.launch {
            try {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val entry = DiaryEntry(date = today, content = content)
                val success = repositoryFactory.saveEntry(entry)

                if (success) {
                    // エントリーを再読み込み
                    loadEntries()
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "日記の保存に失敗しました")
            }
        }
    }

    /**
     * 前回の同期から一定時間経過している場合のみ自動同期する
     * （画面遷移のたびに通信が走るのを防ぐ）
     */
    private suspend fun autoSyncIfNeeded() {
        if (repositoryFactory.getCurrentMode() != RepositoryMode.Git) return

        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastSuccessfulSyncMillis < AUTO_SYNC_INTERVAL_MILLIS) return

        doSync()
    }

    /**
     * 手動同期（pull-to-refresh・同期ボタン）
     * スロットルせず常に実行する
     */
    fun refresh() {
        viewModelScope.launch {
            if (repositoryFactory.getCurrentMode() != RepositoryMode.Git) {
                // ローカルモードでは再読み込みのみ
                loadEntries()
                return@launch
            }

            _isRefreshing.value = true
            try {
                doSync()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * 手動同期（互換用エイリアス）
     */
    fun syncRepository() = refresh()

    private suspend fun doSync() {
        _syncStatus.value = SyncStatus.Syncing

        try {
            val success = repositoryFactory.syncRepository()
            if (success) {
                lastSuccessfulSyncMillis = Clock.System.now().toEpochMilliseconds()
                _syncStatus.value = SyncStatus.Success
                loadEntries() // 同期成功後にエントリを再ロード
            } else {
                _syncStatus.value = SyncStatus.Error("同期に失敗しました")
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "同期に失敗しました")
        }
    }

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        object Success : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }

    companion object {
        // ViewModelは画面遷移のたびに再生成されるため、同期時刻はプロセス内で共有する
        private var lastSuccessfulSyncMillis: Long = 0L
        private const val AUTO_SYNC_INTERVAL_MILLIS = 5 * 60 * 1000L
    }
}

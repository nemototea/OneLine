package net.chasmine.oneline.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.chasmine.oneline.data.repository.RepositoryFactory
import net.chasmine.oneline.data.model.DiaryEntry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 日記リスト画面のViewModel（共通コード）
 *
 * @param repositoryFactory リポジトリファクトリー
 */
class DiaryListViewModel(
    private val repositoryFactory: RepositoryFactory
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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
            // リポジトリを初期化
            repositoryFactory.initialize()

            val hasSettings = repositoryFactory.hasValidSettings()
            if (hasSettings) syncRepository()
        }
    }

    fun loadEntries() {
        viewModelScope.launch {
            repositoryFactory.getAllEntries().collect { diaryEntries ->
                allEntries = diaryEntries
                currentPage = 0
                _hasMoreData.value = allEntries.size > pageSize

                // 最初のページを読み込み
                _entries.value = allEntries.take(pageSize)

                // 今日の日記をチェック
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val todayEntryFound = diaryEntries.find { it.date == today }
                _todayEntry.value = todayEntryFound

                println("DiaryListViewModel: Loaded entries: ${diaryEntries.size}") // デバッグ用ログ
                println("DiaryListViewModel: Today's entry: ${todayEntryFound?.content ?: "None"}")
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

                    println("DiaryListViewModel: Loaded more entries: ${newEntries.size}")
                } else {
                    _hasMoreData.value = false
                }
            } catch (e: Exception) {
                println("DiaryListViewModel: Failed to load more entries: ${e.message}")
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
                    println("DiaryListViewModel: Today's entry saved: $content")
                } else {
                    println("DiaryListViewModel: Failed to save today's entry")
                }
            } catch (e: Exception) {
                println("DiaryListViewModel: Failed to save today's entry: ${e.message}")
            }
        }
    }

    private suspend fun initializeRepository() {
        _isLoading.value = true
        try {
            val success = repositoryFactory.initialize()
            if (!success) {
                _syncStatus.value = SyncStatus.Error("リポジトリの初期化に失敗しました")
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "リポジトリの初期化に失敗しました")
        } finally {
            _isLoading.value = false
        }
    }

    fun syncRepository() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing

            try {
                val success = repositoryFactory.syncRepository()
                if (success) {
                    _syncStatus.value = SyncStatus.Success
                    loadEntries() // 同期成功後にエントリを再ロード
                } else {
                    _syncStatus.value = SyncStatus.Error("同期に失敗しました")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "同期に失敗しました")
            }
        }
    }

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        object Success : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }
}

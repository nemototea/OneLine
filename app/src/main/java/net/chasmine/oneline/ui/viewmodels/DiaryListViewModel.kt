package net.chasmine.oneline.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.repository.RepositoryManager
import net.chasmine.oneline.data.model.DiaryEntry
import net.chasmine.oneline.data.preferences.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class DiaryListViewModel(application: Application) : AndroidViewModel(application) {

    private val repositoryManager = RepositoryManager.getInstance(application)
    private val settingsManager = SettingsManager.getInstance(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _entries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries: StateFlow<List<DiaryEntry>> = _entries

    private val _todayEntry = MutableStateFlow<DiaryEntry?>(null)
    val todayEntry: StateFlow<DiaryEntry?> = _todayEntry

    init {
        viewModelScope.launch {
            // リポジトリを初期化
            repositoryManager.initialize()
            
            val hasSettings = repositoryManager.hasValidSettings()
            if (hasSettings) syncRepository()
        }
    }

    fun loadEntries() {
        viewModelScope.launch {
            repositoryManager.getAllEntries().collect { diaryEntries ->
                _entries.value = diaryEntries
                
                // 今日の日記をチェック
                val today = LocalDate.now()
                val todayEntryFound = diaryEntries.find { it.date == today }
                _todayEntry.value = todayEntryFound
                
                Log.d("DiaryListViewModel", "Loaded entries: ${diaryEntries.size}") // デバッグ用ログ
                Log.d("DiaryListViewModel", "Today's entry: ${todayEntryFound?.content ?: "None"}")
            }
        }
    }
    
    /**
     * 今日の日記を保存
     */
    fun saveTodayEntry(content: String) {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val entry = DiaryEntry(date = today, content = content)
                val success = repositoryManager.saveEntry(entry)
                
                if (success) {
                    // エントリーを再読み込み
                    loadEntries()
                    Log.d("DiaryListViewModel", "Today's entry saved: $content")
                } else {
                    Log.e("DiaryListViewModel", "Failed to save today's entry")
                }
            } catch (e: Exception) {
                Log.e("DiaryListViewModel", "Failed to save today's entry", e)
            }
        }
    }

    private suspend fun initializeRepository() {
        _isLoading.value = true
        try {
            val success = repositoryManager.initialize()
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
                val success = repositoryManager.syncRepository()
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
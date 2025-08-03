package net.chasmine.oneline.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.model.DiaryEntry
import net.chasmine.oneline.data.preferences.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class DiaryListViewModel(application: Application) : AndroidViewModel(application) {

    private val gitRepository = GitRepository.getInstance(application)
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
            val hasSettings = settingsManager.hasValidSettings.first()
            if (hasSettings) syncRepository()
        }
    }

    fun loadEntries() {
        viewModelScope.launch {
            gitRepository.getAllEntries().collect { diaryEntries ->
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
                gitRepository.saveEntry(entry)
                
                // エントリーを再読み込み
                loadEntries()
                
                Log.d("DiaryListViewModel", "Today's entry saved: $content")
            } catch (e: Exception) {
                Log.e("DiaryListViewModel", "Failed to save today's entry", e)
            }
        }
    }

    private suspend fun initializeRepository() {
        _isLoading.value = true
        try {
            val repoUrl = settingsManager.gitRepoUrl.first()
            val username = settingsManager.gitUsername.first()
            val token = settingsManager.gitToken.first()

            if (repoUrl.isNotBlank() && username.isNotBlank() && token.isNotBlank()) {
                gitRepository.initRepository(repoUrl, username, token)
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "Repository initialization failed")
        } finally {
            _isLoading.value = false
        }
    }

    fun syncRepository() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing

            try {
                if (!gitRepository.isConfigValid()) {
                    initializeRepository()
                }

                val result = gitRepository.syncRepository()

                result.fold(
                    onSuccess = {
                        _syncStatus.value = SyncStatus.Success
                        loadEntries() // 同期成功後にエントリを再ロード
                    },
                    onFailure = { e ->
                        _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
                    }
                )
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
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
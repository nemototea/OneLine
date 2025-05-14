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

    init {
        viewModelScope.launch {
            val hasSettings = settingsManager.hasValidSettings.first()
            if (hasSettings) {
                initializeRepository()
                loadEntries() // リポジトリ初期化後にエントリをロード
            }
        }
    }

    fun loadEntries() {
        viewModelScope.launch {
            gitRepository.getAllEntries().collect { diaryEntries ->
                _entries.value = diaryEntries
                Log.d("DiaryListViewModel", "Loaded entries: ${diaryEntries.size}") // デバッグ用ログ
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
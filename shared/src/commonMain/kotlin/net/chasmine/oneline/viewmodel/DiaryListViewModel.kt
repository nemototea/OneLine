package net.chasmine.oneline.viewmodel

import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.model.DiaryEntry
import net.chasmine.oneline.data.preferences.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DiaryListViewModel(
    private val gitRepository: GitRepository,
    private val settingsRepository: SettingsRepository,
    private val viewModelScope: CoroutineScope
) {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _entries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries: StateFlow<List<DiaryEntry>> = _entries

    init {
        viewModelScope.launch {
            val hasSettings = settingsRepository.hasValidSettings().first()
            if (hasSettings) syncRepository()
        }
    }

    fun loadEntries() {
        viewModelScope.launch {
            gitRepository.getAllEntries().collect { diaryEntries ->
                _entries.value = diaryEntries
            }
        }
    }

    private suspend fun initializeRepository() {
        _isLoading.value = true
        try {
            val repoUrl = settingsRepository.gitRepoUrl().first()
            val username = settingsRepository.gitUsername().first()
            val token = settingsRepository.gitToken().first()
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
                        loadEntries()
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

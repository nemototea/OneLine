package net.chasmine.oneline.viewmodel

import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.preferences.SettingsRepository
import net.chasmine.oneline.data.preferences.GitSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val gitRepository: GitRepository,
    private val viewModelScope: CoroutineScope
) {
    val gitRepoUrl = settingsRepository.gitRepoUrl()
    val gitUsername = settingsRepository.gitUsername()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val repoUrl = settingsRepository.gitRepoUrl().first()
                val username = settingsRepository.gitUsername().first()
                val token = settingsRepository.gitToken().first()
                _uiState.value = UiState.Loaded(
                    repoUrl = repoUrl,
                    username = username,
                    token = token
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load settings")
            }
        }
    }

    fun saveSettings(repoUrl: String, username: String, token: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Saving
            try {
                settingsRepository.saveGitSettings(GitSettings(repoUrl, username, token))
                val result = gitRepository.initRepository(repoUrl, username, token)
                result.fold(
                    onSuccess = {
                        _uiState.value = UiState.SaveSuccess
                    },
                    onFailure = { e ->
                        _uiState.value = UiState.Error(e.message ?: "Repository initialization failed")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to save settings")
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Loaded(val repoUrl: String, val username: String, val token: String) : UiState()
        object Saving : UiState()
        object SaveSuccess : UiState()
        data class Error(val message: String) : UiState()
    }
}

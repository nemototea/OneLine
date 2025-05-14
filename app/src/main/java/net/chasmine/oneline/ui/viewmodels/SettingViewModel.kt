package net.chasmine.oneline.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.preferences.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager.getInstance(application)
    private val gitRepository = GitRepository.getInstance(application)

    val gitRepoUrl = settingsManager.gitRepoUrl
    val gitUsername = settingsManager.gitUsername

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                val repoUrl = settingsManager.gitRepoUrl.first()
                val username = settingsManager.gitUsername.first()
                val token = settingsManager.gitToken.first()

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
                // 設定を保存
                settingsManager.saveGitSettings(repoUrl, username, token)

                // リポジトリを初期化
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
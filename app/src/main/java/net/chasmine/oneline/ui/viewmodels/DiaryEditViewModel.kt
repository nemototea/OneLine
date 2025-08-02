package net.chasmine.oneline.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.model.DiaryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DiaryEditViewModel(application: Application) : AndroidViewModel(application) {

    private val gitRepository = GitRepository.getInstance(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus

    fun loadEntry(dateString: String) {
        viewModelScope.launch {
            try {
                // "new"の場合は今日の日付で既存エントリをチェック
                if (dateString == "new") {
                    val today = LocalDate.now()
                    val todayString = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    
                    // 今日の日記が既に存在するかチェック
                    val existingEntry = gitRepository.getEntry(todayString)
                    
                    if (existingEntry != null) {
                        // 既存の今日の日記がある場合は、それを編集モードで開く
                        Log.d("DiaryEditViewModel", "Found existing entry for today: $todayString")
                        _uiState.value = UiState.Editing(existingEntry, isNew = false)
                    } else {
                        // 今日の日記がない場合は新規作成
                        Log.d("DiaryEditViewModel", "No existing entry for today, creating new: $todayString")
                        _uiState.value = UiState.Editing(
                            DiaryEntry(
                                date = today,
                                content = ""
                            ),
                            isNew = true
                        )
                    }
                } else {
                    // 特定の日付が指定された場合の既存処理
                    val entry = gitRepository.getEntry(dateString)

                    if (entry != null) {
                        _uiState.value = UiState.Editing(entry, isNew = false)
                    } else {
                        // 指定日付のエントリがない場合は新規作成
                        val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                        _uiState.value = UiState.Editing(
                            DiaryEntry(
                                date = date,
                                content = ""
                            ),
                            isNew = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DiaryEditViewModel", "Failed to load entry", e)
                _uiState.value = UiState.Error(e.message ?: "Failed to load entry")
            }
        }
    }

    fun updateContent(content: String) {
        val currentState = _uiState.value
        if (currentState is UiState.Editing) {
            _uiState.value = currentState.copy(
                entry = currentState.entry.copy(content = content)
            )
        }
    }

    fun saveEntry() {
        val currentState = _uiState.value
        if (currentState is UiState.Editing) {
            viewModelScope.launch {
                _saveStatus.value = SaveStatus.Saving

                try {
                    val saveResult = gitRepository.saveEntry(currentState.entry)

                    saveResult.fold(
                        onSuccess = {
                            // 投稿成功後にリモートリポジトリと同期
                            val syncResult = gitRepository.syncRepository()
                            syncResult.fold(
                                onSuccess = {
                                    _saveStatus.value = SaveStatus.Success
                                },
                                onFailure = { e ->
                                    Log.e("DiaryEditViewModel", "Failed to sync after save", e)
                                    val userFriendlyMessage = when {
                                        e.message?.contains("authentication") == true -> 
                                            "日記は保存されましたが、GitHubとの同期に失敗しました。認証情報を確認してください。"
                                        e.message?.contains("network") == true || e.message?.contains("connection") == true -> 
                                            "日記は保存されましたが、ネットワーク接続の問題で同期に失敗しました。"
                                        else -> 
                                            "日記は保存されましたが、同期に失敗しました: ${e.message}"
                                    }
                                    _saveStatus.value = SaveStatus.Error(userFriendlyMessage)
                                }
                            )
                        },
                        onFailure = { e ->
                            Log.e("DiaryEditViewModel", "Failed to save entry", e)
                            
                            // エラーメッセージを分かりやすく変換
                            val userFriendlyMessage = when {
                                e.message?.contains("Repository not initialized") == true -> 
                                    "Git設定が完了していません。設定画面でGitHubリポジトリの情報を入力してください。"
                                e.message?.contains("authentication") == true -> 
                                    "GitHubへの認証に失敗しました。ユーザー名とアクセストークンを確認してください。"
                                e.message?.contains("network") == true || e.message?.contains("connection") == true -> 
                                    "ネットワーク接続を確認してください。"
                                else -> 
                                    "日記の保存に失敗しました: ${e.message}"
                            }
                            
                            _saveStatus.value = SaveStatus.Error(userFriendlyMessage)
                        }
                    )
                } catch (e: Exception) {
                    _saveStatus.value = SaveStatus.Error(e.message ?: "Save failed")
                }
            }
        }
    }

    fun deleteEntry() {
        val currentState = _uiState.value
        if (currentState is UiState.Editing && !currentState.isNew) {
            viewModelScope.launch {
                _saveStatus.value = SaveStatus.Saving

                try {
                    val deleteResult = gitRepository.deleteEntry(currentState.entry)

                    deleteResult.fold(
                        onSuccess = {
                            // 削除成功後にリモートリポジトリと同期
                            val syncResult = gitRepository.syncRepository()
                            syncResult.fold(
                                onSuccess = {
                                    _saveStatus.value = SaveStatus.Success
                                },
                                onFailure = { e ->
                                    _saveStatus.value = SaveStatus.Error(e.message ?: "Sync failed")
                                }
                            )
                        },
                        onFailure = { e ->
                            _saveStatus.value = SaveStatus.Error(e.message ?: "Delete failed")
                        }
                    )
                } catch (e: Exception) {
                    _saveStatus.value = SaveStatus.Error(e.message ?: "Delete failed")
                }
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Editing(val entry: DiaryEntry, val isNew: Boolean) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class SaveStatus {
        object Idle : SaveStatus()
        object Saving : SaveStatus()
        object Success : SaveStatus()
        data class Error(val message: String) : SaveStatus()
    }
}
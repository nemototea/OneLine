package net.chasmine.oneline.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.repository.RepositoryManager
import net.chasmine.oneline.data.model.DiaryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DiaryEditViewModel(application: Application) : AndroidViewModel(application) {

    private val repositoryManager = RepositoryManager.getInstance(application)

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
                    val existingEntry = repositoryManager.getEntry(todayString)
                    
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
                    val entry = repositoryManager.getEntry(dateString)

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
                _uiState.value = UiState.Error(e.message ?: "エントリの読み込みに失敗しました")
            }
        }
    }

    fun saveEntry() {
        val currentState = _uiState.value
        if (currentState is UiState.Editing) {
            viewModelScope.launch {
                _saveStatus.value = SaveStatus.Saving

                try {
                    val success = repositoryManager.saveEntry(currentState.entry)
                    
                    if (success) {
                        // 保存成功後に同期を試行
                        val syncSuccess = repositoryManager.syncRepository()
                        if (syncSuccess) {
                            _saveStatus.value = SaveStatus.Success
                        } else {
                            // 同期に失敗しても保存は成功
                            Log.w("DiaryEditViewModel", "Save succeeded but sync failed")
                            _saveStatus.value = SaveStatus.Success
                        }
                    } else {
                        _saveStatus.value = SaveStatus.Error("日記の保存に失敗しました")
                    }
                } catch (e: Exception) {
                    Log.e("DiaryEditViewModel", "Failed to save entry", e)
                    _saveStatus.value = SaveStatus.Error(e.message ?: "保存中にエラーが発生しました")
                }
            }
        }
    }

    fun updateContent(newContent: String) {
        val currentState = _uiState.value
        if (currentState is UiState.Editing) {
            _uiState.value = currentState.copy(
                entry = currentState.entry.copy(content = newContent)
            )
        }
    }

    fun deleteEntry() {
        val currentState = _uiState.value
        if (currentState is UiState.Editing && !currentState.isNew) {
            viewModelScope.launch {
                _saveStatus.value = SaveStatus.Saving

                try {
                    val dateString = currentState.entry.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val success = repositoryManager.deleteEntry(dateString)
                    
                    if (success) {
                        // 削除成功後に同期を試行
                        val syncSuccess = repositoryManager.syncRepository()
                        if (syncSuccess) {
                            _saveStatus.value = SaveStatus.Success
                        } else {
                            // 同期に失敗しても削除は成功
                            Log.w("DiaryEditViewModel", "Delete succeeded but sync failed")
                            _saveStatus.value = SaveStatus.Success
                        }
                    } else {
                        _saveStatus.value = SaveStatus.Error("日記の削除に失敗しました")
                    }
                } catch (e: Exception) {
                    Log.e("DiaryEditViewModel", "Failed to delete entry", e)
                    _saveStatus.value = SaveStatus.Error(e.message ?: "削除中にエラーが発生しました")
                }
            }
        }
    }

    fun resetSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
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

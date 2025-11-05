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
 * 日記編集画面のViewModel（共通コード）
 *
 * @param repositoryFactory リポジトリファクトリー
 */
class DiaryEditViewModel(
    private val repositoryFactory: RepositoryFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus

    fun loadEntry(dateString: String) {
        viewModelScope.launch {
            try {
                // "new"の場合は今日の日付で既存エントリをチェック
                if (dateString == "new") {
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    val todayString = today.toString()

                    // 今日の日記が既に存在するかチェック
                    val existingEntry = repositoryFactory.getEntry(todayString)

                    if (existingEntry != null) {
                        // 既存の今日の日記がある場合は、それを編集モードで開く
                        _uiState.value = UiState.Editing(existingEntry, isNew = false)
                    } else {
                        // 今日の日記がない場合は新規作成
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
                    val entry = repositoryFactory.getEntry(dateString)

                    if (entry != null) {
                        _uiState.value = UiState.Editing(entry, isNew = false)
                    } else {
                        // 指定日付のエントリがない場合は新規作成
                        val date = LocalDate.parse(dateString)
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
                    val success = repositoryFactory.saveEntry(currentState.entry)

                    if (success) {
                        // 保存成功後に同期を試行
                        val syncSuccess = repositoryFactory.syncRepository()
                        if (syncSuccess) {
                            _saveStatus.value = SaveStatus.Success
                        } else {
                            // 同期に失敗しても保存は成功
                            _saveStatus.value = SaveStatus.Success
                        }
                    } else {
                        _saveStatus.value = SaveStatus.Error("日記の保存に失敗しました")
                    }
                } catch (e: Exception) {
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
                    val dateString = currentState.entry.date.toString()
                    val success = repositoryFactory.deleteEntry(dateString)

                    if (success) {
                        // 削除成功後に同期を試行
                        val syncSuccess = repositoryFactory.syncRepository()
                        if (syncSuccess) {
                            _saveStatus.value = SaveStatus.Success
                        } else {
                            // 同期に失敗しても削除は成功
                            _saveStatus.value = SaveStatus.Success
                        }
                    } else {
                        _saveStatus.value = SaveStatus.Error("日記の削除に失敗しました")
                    }
                } catch (e: Exception) {
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

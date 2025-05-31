package net.chasmine.oneline.viewmodel

import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.model.DiaryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class DiaryEditViewModel(
    private val gitRepository: GitRepository,
    private val viewModelScope: CoroutineScope
) {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus

    fun loadEntry(dateString: String) {
        viewModelScope.launch {
            try {
                if (dateString == "new") {
                    val today = LocalDate.now()
                    _uiState.value = UiState.Editing(
                        DiaryEntry(
                            date = today,
                            content = ""
                        ),
                        isNew = true
                    )
                } else {
                    val entry = gitRepository.getEntry(dateString)
                    if (entry != null) {
                        _uiState.value = UiState.Editing(entry, isNew = false)
                    } else {
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
                            _saveStatus.value = SaveStatus.Error(e.message ?: "Save failed")
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

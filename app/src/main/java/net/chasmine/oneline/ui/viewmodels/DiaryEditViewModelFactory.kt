package net.chasmine.oneline.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DiaryEditViewModelFactory(
    private val diaryListViewModel: DiaryListViewModel
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiaryEditViewModel::class.java)) {
            return DiaryEditViewModel(diaryListViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

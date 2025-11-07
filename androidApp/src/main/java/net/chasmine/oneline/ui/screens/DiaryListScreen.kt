package net.chasmine.oneline.ui.screens

import androidx.compose.runtime.Composable
import net.chasmine.oneline.ui.viewmodels.DiaryListViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * DiaryListScreen wrapper for Android
 *
 * This file delegates to the shared implementation with Koin-provided ViewModel
 */
@Composable
fun DiaryListScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val viewModel: DiaryListViewModel = koinViewModel()

    // Delegate to shared implementation
    DiaryListScreenImpl(
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToEdit = onNavigateToEdit,
        viewModel = viewModel
    )
}
package net.chasmine.oneline.ui.screens

import androidx.compose.runtime.Composable
import net.chasmine.oneline.ui.viewmodels.DiaryEditViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * DiaryEditScreen wrapper for Android
 *
 * This file delegates to the shared implementation with Koin-provided ViewModel
 */
@Composable
fun DiaryEditScreen(
    date: String,
    onNavigateBack: () -> Unit
) {
    val viewModel: DiaryEditViewModel = koinViewModel()

    // Delegate to shared implementation
    net.chasmine.oneline.ui.screens.DiaryEditScreen(
        date = date,
        onNavigateBack = onNavigateBack,
        viewModel = viewModel
    )
}

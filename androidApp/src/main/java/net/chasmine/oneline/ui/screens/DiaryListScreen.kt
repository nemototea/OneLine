package net.chasmine.oneline.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.chasmine.oneline.data.repository.RepositoryFactory
import net.chasmine.oneline.ui.viewmodels.DiaryListViewModel

/**
 * DiaryListScreen wrapper for Android
 *
 * This file delegates to the shared implementation and provides
 * platform-specific ViewModel creation.
 */
@Composable
fun DiaryListScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: DiaryListViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repositoryFactory = RepositoryFactory.create(context)
                return DiaryListViewModel(repositoryFactory) as T
            }
        }
    )

    // Delegate to shared implementation
    net.chasmine.oneline.ui.screens.DiaryListScreen(
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToEdit = onNavigateToEdit,
        viewModel = viewModel
    )
}
package net.chasmine.oneline.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chasmine.oneline.data.git.GitRepositoryServiceImpl
import net.chasmine.oneline.data.preferences.SettingsManagerFactory
import net.chasmine.oneline.data.repository.RepositoryFactory
import net.chasmine.oneline.ui.viewmodels.SettingsViewModel

/**
 * Android wrapper for GitSettingsScreen
 *
 * Delegates to shared GitSettingsScreen implementation with Android-specific dependencies
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitSettingsScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: (() -> Unit)? = null,
    isInitialSetup: Boolean = false
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManagerFactory.getInstance(context) }
    val repositoryFactory = remember { RepositoryFactory.create(context) }

    val viewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val gitRepositoryService = GitRepositoryServiceImpl.getInstance(context)
                return SettingsViewModel(settingsManager, gitRepositoryService) as T
            }
        }
    )

    net.chasmine.oneline.ui.screens.GitSettingsScreen(
        onNavigateBack = onNavigateBack,
        onSetupComplete = onSetupComplete,
        isInitialSetup = isInitialSetup,
        viewModel = viewModel,
        settingsManager = settingsManager,
        repositoryFactory = repositoryFactory
    )
}

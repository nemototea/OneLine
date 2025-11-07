package net.chasmine.oneline.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.data.repository.RepositoryFactory
import net.chasmine.oneline.ui.viewmodels.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Android wrapper for GitSettingsScreen
 *
 * Delegates to shared GitSettingsScreen implementation with Koin-provided dependencies
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitSettingsScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: (() -> Unit)? = null,
    isInitialSetup: Boolean = false
) {
    val settingsManager: SettingsManager = koinInject()
    val repositoryFactory: RepositoryFactory = koinInject()
    val viewModel: SettingsViewModel = koinViewModel()

    GitSettingsScreenImpl(
        onNavigateBack = onNavigateBack,
        onSetupComplete = onSetupComplete,
        isInitialSetup = isInitialSetup,
        viewModel = viewModel,
        settingsManager = settingsManager,
        repositoryFactory = repositoryFactory
    )
}

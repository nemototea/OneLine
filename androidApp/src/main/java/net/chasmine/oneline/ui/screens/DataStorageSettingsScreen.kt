package net.chasmine.oneline.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.data.repository.RepositoryManager
import net.chasmine.oneline.data.repository.MigrationResult as SharedMigrationResult
import org.koin.compose.koinInject

/**
 * Android wrapper for DataStorageSettingsScreen
 *
 * Delegates to shared DataStorageSettingsScreen implementation with Koin-provided dependencies
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStorageSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGitSettings: () -> Unit
) {
    val settingsManager: SettingsManager = koinInject()
    val repositoryManager: RepositoryManager = koinInject()

    net.chasmine.oneline.ui.screens.DataStorageSettingsScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToGitSettings = onNavigateToGitSettings,
        settingsManager = settingsManager,
        onMigrateToLocalMode = { clearGitData ->
            // RepositoryManager.MigrationResultをshared.MigrationResultに変換
            val result = repositoryManager.migrateToLocalMode(clearGitData)
            when (result) {
                is RepositoryManager.MigrationResult.Success -> SharedMigrationResult.Success
                is RepositoryManager.MigrationResult.GitInitializationFailed -> SharedMigrationResult.GitInitializationFailed
                is RepositoryManager.MigrationResult.LocalInitializationFailed -> SharedMigrationResult.LocalInitializationFailed
                is RepositoryManager.MigrationResult.DataMigrationFailed -> SharedMigrationResult.DataMigrationFailed
                is RepositoryManager.MigrationResult.GitSettingsNotConfigured -> SharedMigrationResult.GitSettingsNotConfigured
                is RepositoryManager.MigrationResult.UnknownError -> SharedMigrationResult.UnknownError(result.message)
            }
        }
    )
}

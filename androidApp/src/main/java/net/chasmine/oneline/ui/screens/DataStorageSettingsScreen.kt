package net.chasmine.oneline.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import net.chasmine.oneline.data.preferences.SettingsManagerFactory
import net.chasmine.oneline.data.repository.RepositoryManager
import net.chasmine.oneline.data.repository.MigrationResult as SharedMigrationResult

/**
 * Android wrapper for DataStorageSettingsScreen
 *
 * Delegates to shared DataStorageSettingsScreen implementation with Android-specific dependencies
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStorageSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGitSettings: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManagerFactory.getInstance(context) }
    val repositoryManager = remember { RepositoryManager.getInstance(context) }

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

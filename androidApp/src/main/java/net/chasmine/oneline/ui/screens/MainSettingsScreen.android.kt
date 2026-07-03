package net.chasmine.oneline.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import net.chasmine.oneline.data.preferences.SettingsManagerFactory

/**
 * MainSettingsScreen wrapper for Android
 *
 * This file delegates to the shared implementation and provides
 * platform-specific SettingsManager creation.
 */
@Composable
fun MainSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDataStorage: () -> Unit,
    onNavigateToGitSettings: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToKmpVerification: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManagerFactory.getInstance(context) }

    // Delegate to shared implementation
    MainSettingsScreenImpl(
        onNavigateBack = onNavigateBack,
        onNavigateToDataStorage = onNavigateToDataStorage,
        onNavigateToGitSettings = onNavigateToGitSettings,
        onNavigateToNotificationSettings = onNavigateToNotificationSettings,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateToKmpVerification = onNavigateToKmpVerification,
        settingsManager = settingsManager
    )
}

package net.chasmine.oneline.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import net.chasmine.oneline.data.preferences.SettingsManagerFactory

/**
 * WelcomeScreen wrapper for Android
 *
 * This file delegates to the shared implementation and provides
 * platform-specific SettingsManager creation.
 */
@Composable
fun WelcomeScreen(
    onLocalModeSelected: () -> Unit,
    onGitModeSelected: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManagerFactory.getInstance(context) }

    // Delegate to shared implementation
    WelcomeScreenImpl(
        onLocalModeSelected = onLocalModeSelected,
        onGitModeSelected = onGitModeSelected,
        settingsManager = settingsManager
    )
}

package net.chasmine.oneline.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import net.chasmine.oneline.data.repository.RepositoryFactory

/**
 * CalendarScreen wrapper for Android
 *
 * This file delegates to the shared implementation and provides
 * platform-specific RepositoryFactory creation.
 */
@Composable
fun CalendarScreen(
    onNavigateToEdit: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val repositoryFactory = remember { RepositoryFactory.create(context) }

    // Delegate to shared implementation
    CalendarScreenImpl(
        onNavigateToEdit = onNavigateToEdit,
        onNavigateToSettings = onNavigateToSettings,
        repositoryFactory = repositoryFactory
    )
}

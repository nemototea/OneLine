package net.chasmine.oneline.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/**
 * Android wrapper for SettingsViewModel
 *
 * Note: This class is kept for backwards compatibility.
 * New code should use the shared SettingsViewModel directly with proper DI.
 */
@Deprecated(
    message = "Use shared SettingsViewModel with proper dependency injection instead",
    replaceWith = ReplaceWith(
        "SettingsViewModel(settingsManager, gitRepositoryService)",
        "net.chasmine.oneline.ui.viewmodels.SettingsViewModel"
    )
)
class SettingViewModel(application: Application) : AndroidViewModel(application) {
    // This class is deprecated and should not be used
    // Use the shared SettingsViewModel instead
}
package net.chasmine.oneline.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {
    private val gitRepoUrlKey = stringPreferencesKey("git_repo_url")
    private val gitUsernameKey = stringPreferencesKey("git_username")
    private val gitTokenKey = stringPreferencesKey("git_token")

    override suspend fun saveGitSettings(settings: GitSettings) {
        context.dataStore.edit { preferences ->
            preferences[gitRepoUrlKey] = settings.repoUrl
            preferences[gitUsernameKey] = settings.username
            preferences[gitTokenKey] = settings.token
        }
    }

    override fun gitRepoUrl(): Flow<String> = context.dataStore.data.map { it[gitRepoUrlKey] ?: "" }
    override fun gitUsername(): Flow<String> = context.dataStore.data.map { it[gitUsernameKey] ?: "" }
    override fun gitToken(): Flow<String> = context.dataStore.data.map { it[gitTokenKey] ?: "" }
    override fun hasValidSettings(): Flow<Boolean> = context.dataStore.data.map {
        val repoUrl = it[gitRepoUrlKey] ?: ""
        val username = it[gitUsernameKey] ?: ""
        val token = it[gitTokenKey] ?: ""
        repoUrl.isNotBlank() && username.isNotBlank() && token.isNotBlank()
    }
}

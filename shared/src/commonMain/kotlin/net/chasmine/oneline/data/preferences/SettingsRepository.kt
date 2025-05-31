package net.chasmine.oneline.data.preferences

import kotlinx.coroutines.flow.Flow

// KMP共通インターフェース
data class GitSettings(val repoUrl: String, val username: String, val token: String)

interface SettingsRepository {
    suspend fun saveGitSettings(settings: GitSettings)
    fun gitRepoUrl(): Flow<String>
    fun gitUsername(): Flow<String>
    fun gitToken(): Flow<String>
    fun hasValidSettings(): Flow<Boolean>
}

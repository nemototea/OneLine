package net.chasmine.oneline.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// iOS用の仮実装（永続化は未実装。必要に応じてNSUserDefaultsやKMPファイルAPIを利用）
class SettingsRepositoryImpl : SettingsRepository {
    private val _gitRepoUrl = MutableStateFlow("")
    private val _gitUsername = MutableStateFlow("")
    private val _gitToken = MutableStateFlow("")

    override suspend fun saveGitSettings(settings: GitSettings) {
        _gitRepoUrl.value = settings.repoUrl
        _gitUsername.value = settings.username
        _gitToken.value = settings.token
    }
    override fun gitRepoUrl(): Flow<String> = _gitRepoUrl.asStateFlow()
    override fun gitUsername(): Flow<String> = _gitUsername.asStateFlow()
    override fun gitToken(): Flow<String> = _gitToken.asStateFlow()
    override fun hasValidSettings(): Flow<Boolean> = kotlinx.coroutines.flow.combine(_gitRepoUrl, _gitUsername, _gitToken) { repoUrl, username, token ->
        repoUrl.isNotBlank() && username.isNotBlank() && token.isNotBlank()
    }
}

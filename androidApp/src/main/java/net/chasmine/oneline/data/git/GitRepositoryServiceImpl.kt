package net.chasmine.oneline.data.git

import android.content.Context
import net.chasmine.oneline.data.git.ValidationResult as SharedValidationResult
import net.chasmine.oneline.data.git.MigrationOption as SharedMigrationOption

/**
 * Android用のGitRepositoryService実装
 * 既存のGitRepositoryクラスに委譲
 */
class GitRepositoryServiceImpl private constructor(private val context: Context) : GitRepositoryService {

    private val gitRepository = GitRepository.getInstance(context)

    override suspend fun initRepository(
        repoUrl: String,
        username: String,
        token: String
    ): Result<Boolean> {
        return gitRepository.initRepository(repoUrl, username, token)
    }

    override suspend fun validateRepositorySafely(
        repoUrl: String,
        username: String,
        token: String
    ): SharedValidationResult {
        val result = gitRepository.validateRepositorySafely(repoUrl, username, token)

        // GitRepository.ValidationResultをsharedのValidationResultに変換
        return when (result) {
            GitRepository.ValidationResult.DIARY_REPOSITORY -> SharedValidationResult.DIARY_REPOSITORY
            GitRepository.ValidationResult.LIKELY_DIARY_REPOSITORY -> SharedValidationResult.LIKELY_DIARY_REPOSITORY
            GitRepository.ValidationResult.EMPTY_REPOSITORY -> SharedValidationResult.EMPTY_REPOSITORY
            GitRepository.ValidationResult.UNKNOWN_REPOSITORY -> SharedValidationResult.UNKNOWN_REPOSITORY
            GitRepository.ValidationResult.SUSPICIOUS_REPOSITORY -> SharedValidationResult.SUSPICIOUS_REPOSITORY
            GitRepository.ValidationResult.DANGEROUS_REPOSITORY -> SharedValidationResult.DANGEROUS_REPOSITORY
            GitRepository.ValidationResult.OWNERSHIP_VERIFICATION_FAILED -> SharedValidationResult.OWNERSHIP_VERIFICATION_FAILED
            GitRepository.ValidationResult.AUTHENTICATION_FAILED -> SharedValidationResult.AUTHENTICATION_FAILED
            GitRepository.ValidationResult.REPOSITORY_NOT_FOUND -> SharedValidationResult.REPOSITORY_NOT_FOUND
            GitRepository.ValidationResult.CONNECTION_FAILED -> SharedValidationResult.CONNECTION_FAILED
            GitRepository.ValidationResult.VALIDATION_FAILED -> SharedValidationResult.VALIDATION_FAILED
        }
    }

    override suspend fun migrateToNewRepository(
        repoUrl: String,
        username: String,
        token: String,
        option: SharedMigrationOption
    ): Result<Boolean> {
        // SharedMigrationOptionをGitRepository.MigrationOptionに変換
        val gitRepoOption = when (option) {
            SharedMigrationOption.MIGRATE_DATA -> GitRepository.MigrationOption.MIGRATE_DATA
            SharedMigrationOption.DISCARD_AND_SWITCH -> GitRepository.MigrationOption.DISCARD_AND_SWITCH
        }

        return gitRepository.migrateToNewRepository(repoUrl, username, token, gitRepoOption)
    }

    companion object {
        @Volatile
        private var INSTANCE: GitRepositoryServiceImpl? = null

        fun getInstance(context: Context): GitRepositoryService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GitRepositoryServiceImpl(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

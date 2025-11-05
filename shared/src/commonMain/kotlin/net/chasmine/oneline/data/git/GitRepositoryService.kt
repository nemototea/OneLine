package net.chasmine.oneline.data.git

/**
 * Gitリポジトリサービスのインターフェース
 * プラットフォーム固有の実装で提供
 */
interface GitRepositoryService {
    /**
     * リポジトリを初期化
     */
    suspend fun initRepository(
        repoUrl: String,
        username: String,
        token: String
    ): Result<Boolean>

    /**
     * リポジトリの安全性を検証（クローンなし）
     */
    suspend fun validateRepositorySafely(
        repoUrl: String,
        username: String,
        token: String
    ): ValidationResult

    /**
     * 新しいリポジトリに移行
     */
    suspend fun migrateToNewRepository(
        repoUrl: String,
        username: String,
        token: String,
        option: MigrationOption
    ): Result<Boolean>
}

/**
 * 検証結果
 */
enum class ValidationResult {
    DIARY_REPOSITORY,
    LIKELY_DIARY_REPOSITORY,
    EMPTY_REPOSITORY,
    UNKNOWN_REPOSITORY,
    SUSPICIOUS_REPOSITORY,
    DANGEROUS_REPOSITORY,
    OWNERSHIP_VERIFICATION_FAILED,
    AUTHENTICATION_FAILED,
    REPOSITORY_NOT_FOUND,
    CONNECTION_FAILED,
    VALIDATION_FAILED
}

/**
 * 移行オプション
 */
enum class MigrationOption {
    MIGRATE_DATA,
    DISCARD_AND_SWITCH
}

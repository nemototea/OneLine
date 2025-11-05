package net.chasmine.oneline.data.repository

/**
 * リポジトリモード
 */
enum class RepositoryMode {
    LocalOnly,
    Git
}

/**
 * 移行結果
 */
sealed class MigrationResult {
    object Success : MigrationResult()
    object GitInitializationFailed : MigrationResult()
    object LocalInitializationFailed : MigrationResult()
    object DataMigrationFailed : MigrationResult()
    object GitSettingsNotConfigured : MigrationResult()
    data class UnknownError(val message: String) : MigrationResult()

    fun getErrorMessage(): String = when (this) {
        is Success -> "移行が完了しました"
        is GitInitializationFailed -> "Git リポジトリの初期化に失敗しました。設定を確認してください。"
        is LocalInitializationFailed -> "ローカルストレージの初期化に失敗しました。"
        is DataMigrationFailed -> "データの移行に失敗しました。一部のデータが移行されていない可能性があります。"
        is GitSettingsNotConfigured -> "Git設定が完了していません。リポジトリURL、ユーザー名、トークンを設定してください。"
        is UnknownError -> "予期しないエラーが発生しました: $message"
    }
}

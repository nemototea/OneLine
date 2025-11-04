package net.chasmine.oneline.data.git

/**
 * Git操作の共通インターフェース
 *
 * プラットフォーム固有の実装:
 * - Android: JGitを使用
 * - iOS: libgit2またはREST API（実装予定）
 */
expect class GitOperations {
    /**
     * リポジトリを初期化（クローンまたはオープン）
     *
     * @param repoUrl リモートリポジトリのURL
     * @param localPath ローカルリポジトリのパス
     * @param auth 認証情報
     * @return 成功時true、失敗時は例外をスロー
     */
    suspend fun initRepository(
        repoUrl: String,
        localPath: String,
        auth: GitAuth
    ): Result<Boolean>

    /**
     * ファイルを保存してコミット
     *
     * @param fileName ファイル名
     * @param content ファイル内容
     * @param commitMessage コミットメッセージ
     * @param authorName コミット作成者名（省略可）
     * @param authorEmail コミット作成者メール（省略可）
     * @return 成功時true、失敗時は例外をスロー
     */
    suspend fun saveAndCommit(
        fileName: String,
        content: String,
        commitMessage: String,
        authorName: String = "",
        authorEmail: String = ""
    ): Result<Boolean>

    /**
     * ファイルを削除してコミット
     *
     * @param fileName ファイル名
     * @param commitMessage コミットメッセージ
     * @param authorName コミット作成者名（省略可）
     * @param authorEmail コミット作成者メール（省略可）
     * @return 成功時true、失敗時は例外をスロー
     */
    suspend fun deleteAndCommit(
        fileName: String,
        commitMessage: String,
        authorName: String = "",
        authorEmail: String = ""
    ): Result<Boolean>

    /**
     * リモートにプッシュ
     *
     * @return 成功時true、失敗時は例外をスロー
     */
    suspend fun push(): Result<Boolean>

    /**
     * リモートからプル
     *
     * @param useOursStrategy true: ローカルを優先（OURS戦略）、false: 通常のマージ
     * @return 成功時true、失敗時は例外をスロー
     */
    suspend fun pull(useOursStrategy: Boolean = true): Result<Boolean>

    /**
     * ファイルをステージングエリアに追加
     *
     * @param filePath ファイルパス
     * @return 成功時true、失敗時は例外をスロー
     */
    suspend fun addFile(filePath: String): Result<Boolean>

    /**
     * リポジトリの状態を確認
     *
     * @return 未コミットの変更があればtrue
     */
    suspend fun hasUncommittedChanges(): Result<Boolean>

    /**
     * リポジトリが初期化されているか確認
     *
     * @return 初期化済みならtrue
     */
    fun isInitialized(): Boolean

    /**
     * ローカルリポジトリのパスを取得
     *
     * @return ローカルパス、未初期化の場合はnull
     */
    fun getLocalPath(): String?

    /**
     * リモートリポジトリのURLを取得
     *
     * @return リモートURL、未初期化の場合はnull
     */
    suspend fun getRemoteUrl(): Result<String?>
}

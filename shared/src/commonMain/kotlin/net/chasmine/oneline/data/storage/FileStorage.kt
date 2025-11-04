package net.chasmine.oneline.data.storage

/**
 * ファイルストレージの共通インターフェース
 *
 * プラットフォーム固有の実装:
 * - Android: Context.filesDir を使用
 * - iOS: NSFileManager を使用
 */
expect class FileStorage {
    /**
     * ファイルに内容を書き込む
     *
     * @param path ファイルパス（相対パスまたは絶対パス）
     * @param content 書き込む内容
     * @return 成功時true、失敗時は例外をスロー
     */
    suspend fun writeFile(path: String, content: String): Result<Unit>

    /**
     * ファイルの内容を読み込む
     *
     * @param path ファイルパス（相対パスまたは絶対パス）
     * @return ファイルの内容、失敗時は例外をスロー
     */
    suspend fun readFile(path: String): Result<String>

    /**
     * ファイルを削除する
     *
     * @param path ファイルパス（相対パスまたは絶対パス）
     * @return 成功時true、失敗時は例外をスロー
     */
    suspend fun deleteFile(path: String): Result<Unit>

    /**
     * ディレクトリ内のファイル一覧を取得
     *
     * @param directory ディレクトリパス（相対パスまたは絶対パス）
     * @return ファイル名のリスト、失敗時は例外をスロー
     */
    suspend fun listFiles(directory: String): Result<List<String>>

    /**
     * ファイルが存在するか確認
     *
     * @param path ファイルパス（相対パスまたは絶対パス）
     * @return 存在する場合true
     */
    suspend fun exists(path: String): Result<Boolean>

    /**
     * ディレクトリを作成
     *
     * @param directory ディレクトリパス（相対パスまたは絶対パス）
     * @return 成功時true、失敗時は例外をスロー
     */
    suspend fun createDirectory(directory: String): Result<Unit>

    /**
     * アプリ専用のデータディレクトリのパスを取得
     *
     * @return データディレクトリの絶対パス
     */
    fun getAppDirectory(): String
}

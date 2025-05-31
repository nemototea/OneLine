package net.chasmine.oneline.data.git

import net.chasmine.oneline.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

// iOS用の仮実装（ファイル永続化やGit連携は未実装。必要に応じてSwift/ObjC連携やKMPファイルAPIを利用）
actual class GitRepository actual constructor() {
    actual suspend fun initRepository(remoteUrl: String, username: String, password: String): Result<Boolean> = Result.success(true)
    actual fun getAllEntries(): Flow<List<DiaryEntry>> = kotlinx.coroutines.flow.flowOf(emptyList())
    actual suspend fun getEntry(dateStr: String): DiaryEntry? = null
    actual suspend fun saveEntry(entry: DiaryEntry): Result<Boolean> = Result.success(true)
    actual suspend fun deleteEntry(entry: DiaryEntry): Result<Boolean> = Result.success(true)
    actual suspend fun syncRepository(): Result<Boolean> = Result.success(true)
    actual fun isConfigValid(): Boolean = true
}

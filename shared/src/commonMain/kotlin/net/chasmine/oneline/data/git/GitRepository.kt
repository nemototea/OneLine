package net.chasmine.oneline.data.git

import net.chasmine.oneline.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

// KMP用 expectクラス定義
expect class GitRepository {
    suspend fun initRepository(remoteUrl: String, username: String, password: String): Result<Boolean>
    fun getAllEntries(): Flow<List<DiaryEntry>>
    suspend fun getEntry(dateStr: String): DiaryEntry?
    suspend fun saveEntry(entry: DiaryEntry): Result<Boolean>
    suspend fun deleteEntry(entry: DiaryEntry): Result<Boolean>
    suspend fun syncRepository(): Result<Boolean>
    fun isConfigValid(): Boolean
}

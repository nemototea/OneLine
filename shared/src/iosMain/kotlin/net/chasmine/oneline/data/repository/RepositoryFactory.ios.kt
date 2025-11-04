package net.chasmine.oneline.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.chasmine.oneline.data.model.DiaryEntry

/**
 * iOS用のリポジトリファクトリー実装（骨組み）
 *
 * Phase 3以降で実装予定。
 * 現在は空の実装のみを提供し、ビルドエラーを回避します。
 */
actual class RepositoryFactory private constructor() {

    actual suspend fun initialize(): Boolean {
        TODO("iOS implementation will be added in Phase 3")
    }

    actual suspend fun saveEntry(entry: DiaryEntry): Boolean {
        TODO("iOS implementation will be added in Phase 3")
    }

    actual suspend fun getEntry(date: String): DiaryEntry? {
        TODO("iOS implementation will be added in Phase 3")
    }

    actual fun getAllEntries(): Flow<List<DiaryEntry>> {
        return flow { emit(emptyList()) }
    }

    actual suspend fun deleteEntry(date: String): Boolean {
        TODO("iOS implementation will be added in Phase 3")
    }

    actual suspend fun syncRepository(): Boolean {
        TODO("iOS implementation will be added in Phase 3")
    }

    actual suspend fun getCurrentMode(): RepositoryMode {
        return RepositoryMode.LocalOnly
    }

    actual suspend fun hasValidSettings(): Boolean {
        return false
    }

    actual enum class RepositoryMode {
        LocalOnly,
        Git
    }

    actual companion object {
        actual fun create(): RepositoryFactory {
            return RepositoryFactory()
        }
    }
}

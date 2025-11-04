package net.chasmine.oneline.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import net.chasmine.oneline.data.model.DiaryEntry

/**
 * Android用のリポジトリファクトリー実装
 *
 * 注意: このファイルはexpect/actualの骨組みです。
 * 実際の実装は androidApp/src/main/java の RepositoryManager を使用します。
 * Phase 3以降で完全な実装に移行します。
 */
actual class RepositoryFactory private constructor(private val context: Context) {

    actual suspend fun initialize(): Boolean {
        // TODO: Phase 3で実装
        // 現時点では androidApp の RepositoryManager を使用
        return true
    }

    actual suspend fun saveEntry(entry: DiaryEntry): Boolean {
        // TODO: Phase 3で実装
        return true
    }

    actual suspend fun getEntry(date: String): DiaryEntry? {
        // TODO: Phase 3で実装
        return null
    }

    actual fun getAllEntries(): Flow<List<DiaryEntry>> {
        // TODO: Phase 3で実装
        return kotlinx.coroutines.flow.flow { emit(emptyList()) }
    }

    actual suspend fun deleteEntry(date: String): Boolean {
        // TODO: Phase 3で実装
        return true
    }

    actual suspend fun syncRepository(): Boolean {
        // TODO: Phase 3で実装
        return true
    }

    actual suspend fun getCurrentMode(): RepositoryMode {
        // TODO: Phase 3で実装
        return RepositoryMode.LocalOnly
    }

    actual suspend fun hasValidSettings(): Boolean {
        // TODO: Phase 3で実装
        return false
    }

    actual enum class RepositoryMode {
        LocalOnly,
        Git
    }

    actual companion object {
        actual fun create(): RepositoryFactory {
            throw UnsupportedOperationException(
                "RepositoryFactory.create() requires Android Context. " +
                "This will be implemented in Phase 3 when we have proper platform abstractions."
            )
        }

        fun create(context: Context): RepositoryFactory {
            return RepositoryFactory(context.applicationContext)
        }
    }
}

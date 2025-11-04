package net.chasmine.oneline.data.preferences

import android.content.Context
import net.chasmine.oneline.data.preferences.SettingsStorage as SettingsStorageImpl
import net.chasmine.oneline.data.preferences.SettingsManager as SharedSettingsManager

/**
 * SettingsManagerのファクトリークラス
 *
 * Sharedモジュールの SettingsManager を使用するためのヘルパークラスです。
 * 既存のコードとの互換性を保ちながら、共通化されたSettingManagerを使用できます。
 */
object SettingsManagerFactory {
    @Volatile
    private var storageInstance: SettingsStorageImpl? = null

    @Volatile
    private var managerInstance: SharedSettingsManager? = null

    /**
     * SettingsManagerのシングルトンインスタンスを取得
     *
     * @param context Androidコンテキスト
     * @return SettingsManagerのインスタンス
     */
    fun getInstance(context: Context): SharedSettingsManager {
        return managerInstance ?: synchronized(this) {
            managerInstance ?: run {
                // SettingsStorageのインスタンスを作成
                val storage = storageInstance ?: synchronized(this) {
                    storageInstance ?: SettingsStorageImpl(context.applicationContext).also {
                        storageInstance = it
                    }
                }

                // SettingsManagerのインスタンスを作成
                SharedSettingsManager.getInstance(storage)
            }.also {
                managerInstance = it
            }
        }
    }

    /**
     * インスタンスをクリア（テスト用）
     */
    fun destroyInstance() {
        synchronized(this) {
            storageInstance = null
            managerInstance = null
            SharedSettingsManager.destroyInstance()
        }
    }
}

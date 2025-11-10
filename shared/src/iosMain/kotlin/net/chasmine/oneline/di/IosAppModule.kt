package net.chasmine.oneline.di

import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.data.preferences.SettingsStorage
import net.chasmine.oneline.data.repository.RepositoryFactory
import org.koin.dsl.module

/**
 * iOS固有のKoinモジュール定義
 */
val iosAppModule = module {
    // SettingsStorage - iOS 実装（UserDefaults）
    single<SettingsStorage> { SettingsStorage() }

    // SettingsManager - shared モジュールの共通実装
    single<SettingsManager> { SettingsManager.getInstance(get()) }

    // RepositoryFactory - shared モジュールの iOS 実装
    single<RepositoryFactory> { RepositoryFactory.create() }
}

package net.chasmine.oneline.di

import net.chasmine.oneline.data.git.GitRepositoryService
import net.chasmine.oneline.data.git.GitRepositoryServiceImpl
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.data.preferences.SettingsManagerFactory
import net.chasmine.oneline.data.repository.RepositoryFactory
import net.chasmine.oneline.data.repository.RepositoryManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * AndroidApp固有のKoinモジュール定義
 */
val androidAppModule = module {
    // SettingsManager - shared モジュールの共通実装
    single<SettingsManager> { SettingsManagerFactory.getInstance(androidContext()) }

    // RepositoryFactory - shared モジュールの Android 実装
    single<RepositoryFactory> { RepositoryFactory.create(androidContext()) }

    // GitRepositoryService - androidApp 固有の実装
    single<GitRepositoryService> { GitRepositoryServiceImpl.getInstance(androidContext()) }

    // RepositoryManager - androidApp 固有の実装
    single { RepositoryManager.getInstance(androidContext()) }
}

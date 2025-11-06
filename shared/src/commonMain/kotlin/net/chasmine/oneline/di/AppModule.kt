package net.chasmine.oneline.di

import net.chasmine.oneline.ui.viewmodels.DiaryListViewModel
import net.chasmine.oneline.ui.viewmodels.DiaryEditViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 共通のKoinモジュール定義
 *
 * Note: SettingsViewModelはGitRepositoryService（Android固有）に依存しているため、
 * androidAppModuleに定義されています。
 */
val viewModelModule = module {
    viewModel { DiaryListViewModel(get()) }
    viewModel { DiaryEditViewModel(get()) }
}

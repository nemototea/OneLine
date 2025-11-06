package net.chasmine.oneline.di

import net.chasmine.oneline.ui.viewmodels.DiaryListViewModel
import net.chasmine.oneline.ui.viewmodels.DiaryEditViewModel
import net.chasmine.oneline.ui.viewmodels.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 共通のKoinモジュール定義
 */
val viewModelModule = module {
    viewModel { DiaryListViewModel(get()) }
    viewModel { DiaryEditViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
}

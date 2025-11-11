package net.chasmine.oneline

import net.chasmine.oneline.di.iosAppModule
import net.chasmine.oneline.di.viewModelModule
import org.koin.core.context.startKoin

/**
 * iOS用のKoin初期化関数
 *
 * iOSApp.swiftのinit()から呼び出されます
 */
fun initKoin() {
    startKoin {
        // モジュールをロード
        modules(
            iosAppModule,       // iOS固有のモジュール（依存性を先に提供）
            viewModelModule     // 共通ViewModelモジュール
        )
    }
}

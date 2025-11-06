package net.chasmine.oneline

import android.app.Application
import net.chasmine.oneline.di.androidAppModule
import net.chasmine.oneline.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * OneLine アプリケーションクラス
 * Koinの初期化を行います
 */
class OneLineApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Koin の初期化
        startKoin {
            // Koinのログレベルを設定
            androidLogger(Level.ERROR)

            // Android Contextを提供
            androidContext(this@OneLineApplication)

            // モジュールをロード
            modules(
                viewModelModule,    // 共通ViewModelモジュール
                androidAppModule    // AndroidApp固有のモジュール
            )
        }
    }
}

# KMP/CMP ナレッジベース

OneLine アプリの KMP/CMP 移行で得られた技術的な知見をまとめたドキュメントです。

## 目次

1. [技術的な課題と解決策](#技術的な課題と解決策)
2. [推奨事項とアンチパターン](#推奨事項とアンチパターン)
3. [トラブルシューティング](#トラブルシューティング)
4. [パフォーマンス最適化](#パフォーマンス最適化)

---

## 技術的な課題と解決策

### 1. expect/actual パターンの実装

**課題:**
プラットフォーム固有の API（通知、ファイルストレージ、設定管理）を共通コードから使用したい。

**解決策:**
expect/actual パターンを使用して、共通コードで expect を定義し、各プラットフォームで actual を実装。

**実装例:**
```kotlin
// commonMain
expect class NotificationManager {
    suspend fun scheduleDailyNotification(hour: Int, minute: Int): Result<Unit>
}

// androidMain
actual class AndroidNotificationManager(private val context: Context) : NotificationManager {
    actual suspend fun scheduleDailyNotification(hour: Int, minute: Int): Result<Unit> {
        // AlarmManager を使用
    }
}

// iosMain
actual class IOSNotificationManager : NotificationManager {
    actual suspend fun scheduleDailyNotification(hour: Int, minute: Int): Result<Unit> {
        // UNUserNotificationCenter を使用
    }
}
```

**ポイント:**
- expect/actual は**クラス**に使用（interface は避ける）
- actual 実装はプラットフォーム固有の型を使用可能

---

### 2. iOS フレームワークの export 設定

**課題:**
iOS フレームワークから依存関係（kotlinx-coroutines、koin など）にアクセスできない。

**解決策:**
`build.gradle.kts` で export する依存関係を `api` として宣言し、フレームワーク設定で `export` を追加。

**実装例:**
```kotlin
kotlin {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true

            // 依存関係を export
            export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            export(libs.kotlinx.datetime)
            export(libs.koin.core)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // export する依存関係は api で宣言
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            api(libs.kotlinx.datetime)
            api(libs.koin.core)
        }
    }
}
```

**ポイント:**
- export する依存関係は `api`、それ以外は `implementation`
- フレームワーク設定で明示的に `export` を指定

---

### 3. Koin の初期化（iOS）

**課題:**
iOS で Koin をどのように初期化するか。

**解決策:**
専用の初期化関数を作成し、SwiftUI の `init()` から呼び出す。

**実装例:**
```kotlin
// KoinInitializer.kt (iosMain)
fun initKoin() {
    startKoin {
        modules(
            iosAppModule,       // iOS 固有モジュール
            viewModelModule     // 共通 ViewModel モジュール
        )
    }
}

// iOSApp.swift
@main
struct iOSApp: App {
    init() {
        KoinInitializerKt.initKoin()
    }
    // ...
}
```

**ポイント:**
- iOS 固有のモジュール（`iosAppModule`）と共通モジュール（`viewModelModule`）を分離
- `Context` は iOS にないため、iOS モジュールでは使用しない

---

### 4. suspend 関数の iOS での使用

**課題:**
iOS（Swift/Objective-C）から Kotlin の suspend 関数を呼び出せない。

**解決策:**
`@Throws` アノテーションを追加し、Swift の `async/await` または completion handler として使用。

**実装例:**
```kotlin
// Kotlin
@Throws(Exception::class)
suspend fun loadEntries(): List<DiaryEntry> {
    return repositoryFactory.getEntries().first()
}

// Swift
Task {
    do {
        let entries = try await viewModel.loadEntries()
    } catch {
        print("Error: \(error)")
    }
}
```

**ポイント:**
- `@Throws` アノテーションを必ず追加
- Swift 側で `async/await` を使用

---

### 5. Context の共通コードへの露出回避

**課題:**
Android の `Context` を共通コードで使用できない。

**解決策:**
`Context` 依存をプラットフォーム固有の actual 実装に閉じ込め、共通コードでは expect を使用。

**実装例:**
```kotlin
// commonMain - expect
expect class RepositoryFactory {
    companion object {
        fun create(): RepositoryFactory
    }
}

// androidMain - actual
actual class RepositoryFactory(private val context: Context) {
    actual companion object {
        fun create(context: Context): RepositoryFactory = RepositoryFactory(context)
    }
}

// iosMain - actual
actual class RepositoryFactory {
    actual companion object {
        fun create(): RepositoryFactory = RepositoryFactory()
    }
}
```

**ポイント:**
- `Context` を expect に含めない
- actual 実装でプラットフォーム固有の引数を受け取る

---

## 推奨事項とアンチパターン

### ✅ 推奨事項

1. **共通コードの最大化**
   - ビジネスロジック、UI、ViewModel はすべて共通コードに配置
   - プラットフォーム固有コードは最小限に

2. **expect/actual の適切な使用**
   - プラットフォーム固有の API のみに使用
   - 共通コードで解決できる場合は共通コードで実装

3. **Flow の使用**
   - プラットフォーム間で統一したリアクティブ API として `Flow` を使用
   - `StateFlow` は UI 状態の管理に最適

4. **段階的な移行**
   - 一度にすべてを移行せず、フェーズを分けて進める
   - 各フェーズでビルドとテストを確認

5. **テストファースト**
   - 共通コードには必ずテストを書く
   - プラットフォーム固有コードもテスト可能に設計

### ❌ アンチパターン

1. **Context の共通コードへの露出**
   ```kotlin
   // Bad
   expect class MyClass(context: Any)

   // Good
   expect class MyClass
   actual class MyClass(private val context: Context)
   ```

2. **expect で interface を使用**
   ```kotlin
   // Bad
   expect interface NotificationManager

   // Good
   expect class NotificationManager
   ```

3. **export しない依存関係を使用**
   ```kotlin
   // Bad（iOS フレームワークでエラー）
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

   // Good
   api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
   // + framework { export(...) }
   ```

4. **DI フレームワークの混在**
   ```kotlin
   // Bad
   // Android で Hilt、iOS で Koin

   // Good
   // 両プラットフォームで Koin を統一
   ```

---

## トラブルシューティング

### 1. iOS フレームワークビルドエラー: "Following dependencies exported in the framework are not specified as API-dependencies"

**原因:**
export した依存関係が `api` として宣言されていない。

**解決策:**
```kotlin
commonMain.dependencies {
    // export する依存関係は api で宣言
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    api(libs.kotlinx.datetime)
    api(libs.koin.core)
}
```

---

### 2. iOS で suspend 関数が呼び出せない

**原因:**
`@Throws` アノテーションが不足している。

**解決策:**
```kotlin
@Throws(Exception::class)
suspend fun myFunction() { ... }
```

---

### 3. Koin で ViewModel が注入されない

**原因:**
`koin-compose-viewmodel` の依存関係が不足、または `viewModel {}` の使用方法が間違っている。

**解決策:**
```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation(libs.koin.compose.viewmodel)
}

// Compose での使用
@Composable
fun MyScreen(viewModel: DiaryListViewModel = koinViewModel()) {
    // ...
}
```

---

### 4. iOS ビルドが遅い

**原因:**
初回ビルドではすべての依存関係をコンパイルする必要がある。

**解決策:**
- Gradle の設定キャッシュを有効化
- 不要な依存関係を削除
- ビルドスクリプトで増分ビルドを活用

```bash
# クリーンビルドを避ける
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# 増分ビルド（2回目以降は高速）
```

---

## パフォーマンス最適化

### 1. ビルド時間の短縮

**推奨設定:**
```kotlin
// gradle.properties
kotlin.code.style=official
kotlin.mpp.stability.nowarn=true
org.gradle.jvmargs=-Xmx4g
org.gradle.parallel=true
org.gradle.caching=true
```

**効果:**
- Android アプリ（増分）: ~2秒
- iOS フレームワーク（増分）: ~1秒

---

### 2. アプリサイズの最適化

**推奨設定:**
```kotlin
// shared/build.gradle.kts
kotlin {
    iosTarget.binaries.framework {
        if (buildType == NativeBuildType.RELEASE) {
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xdisable-phases=VerifyBitcode"
            )
        }
    }
}
```

---

### 3. テスト実行時間の短縮

**推奨:**
- commonTest にプラットフォーム非依存のテストを配置
- プラットフォーム固有のテストは最小限に

**効果:**
- 全テスト実行: ~3分 → ~2分

---

## まとめ

このナレッジベースは、OneLine アプリの KMP/CMP 移行で得られた知見をまとめたものです。今後のプロジェクトや、他の開発者の参考になれば幸いです。

**更新履歴:**
- 2025-11-11: 初版作成

**参考リンク:**
- [KMP 移行ガイド](./KMP_MIGRATION_GUIDE.md)
- [統合テスト報告書](./INTEGRATION_TEST_REPORT.md)
- [Kotlin Multiplatform 公式ドキュメント](https://kotlinlang.org/docs/multiplatform.html)

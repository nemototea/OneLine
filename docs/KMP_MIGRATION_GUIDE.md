# KMP/CMP 移行ガイド

このドキュメントは、OneLine アプリを Android 専用アプリから Kotlin Multiplatform (KMP) / Compose Multiplatform (CMP) アプリへ移行した経験をまとめたものです。他のプロジェクトでも参考にできるよう、各フェーズで遭遇した課題と解決策、ベストプラクティスを整理しています。

## 目次

1. [移行の概要](#移行の概要)
2. [移行のフェーズ](#移行のフェーズ)
3. [技術的な課題と解決策](#技術的な課題と解決策)
4. [ベストプラクティス](#ベストプラクティス)
5. [推奨事項とアンチパターン](#推奨事項とアンチパターン)
6. [移行の振り返り](#移行の振り返り)

---

## 移行の概要

### プロジェクト情報
- **アプリ名:** OneLine（日記アプリ）
- **移行前:** Android 専用（Jetpack Compose）
- **移行後:** KMP/CMP（Android + iOS）
- **移行期間:** Phase 1 ～ Phase 8-3
- **主な技術スタック:**
  - Kotlin Multiplatform 2.1.0
  - Compose Multiplatform（最新版）
  - Koin 4.0.1（DI）
  - kotlinx-datetime（日付処理）
  - kotlinx-serialization（シリアライゼーション）

### 移行の目的
1. **コードの再利用:** ビジネスロジックと UI を Android/iOS で共有
2. **開発効率の向上:** 一度の実装で両プラットフォームに対応
3. **保守性の向上:** 共通コードの一元管理
4. **将来の拡張性:** 他のプラットフォーム（Desktop、Web）への展開も視野

---

## 移行のフェーズ

KMP 移行は以下の8つのフェーズに分けて実施しました。

### Phase 1-4: 基盤構築
- **Phase 1:** プロジェクト構造の再編成
- **Phase 2:** 依存関係の整理と共通化
- **Phase 3:** ビルドスクリプトの最適化
- **Phase 4:** UI の共通化（Compose Multiplatform）

### Phase 5: 依存性注入の移行
- **Phase 5:** Hilt から Koin への移行

### Phase 6: プラットフォーム固有機能の抽象化
- **Phase 6-1:** 通知システムの抽象化（expect/actual）
- **Phase 6-2:** Android 通知の actual 実装
- **Phase 6-3:** iOS 通知の actual 実装

### Phase 7: iOS アプリモジュールの作成
- **Phase 7-1:** iOS アプリモジュールの作成
- **Phase 7-2:** iOS エントリーポイントの実装
- **Phase 7-3:** iOS ビルド設定の最適化

### Phase 8: テストとドキュメント
- **Phase 8-1:** 共通コードのテスト
- **Phase 8-2:** Android/iOS での統合テスト
- **Phase 8-3:** ドキュメントの更新（本ドキュメント）

---

## 技術的な課題と解決策

### 1. Hilt から Koin への移行

**課題:**
- Hilt は Android 専用のため、KMP では使用できない
- ViewModel の注入方法が異なる

**解決策:**
- Koin を採用（KMP 対応の DI フレームワーク）
- プラットフォーム固有のモジュール（`androidAppModule`, `iosAppModule`）と共通モジュール（`viewModelModule`）を分離
- `koin-compose-viewmodel` を使用して ViewModel を注入

**実装例:**
```kotlin
// commonMain - viewModelModule
val viewModelModule = module {
    viewModel { DiaryListViewModel(get()) }
    viewModel { DiaryEditViewModel(get()) }
}

// androidMain - androidAppModule
val androidAppModule = module {
    single<SettingsStorage> { SettingsStorage(get()) }
    single<RepositoryFactory> { RepositoryFactory.create(get()) }
}

// iosMain - iosAppModule
val iosAppModule = module {
    single<SettingsStorage> { SettingsStorage() }
    single<RepositoryFactory> { RepositoryFactory.create() }
}
```

### 2. AndroidViewModel の共通化

**課題:**
- `AndroidViewModel` は Android 専用のため、共通コードで使用できない
- `Context` への依存を排除する必要がある

**解決策:**
- `ViewModel`（androidx.lifecycle）を使用（KMP 対応）
- `Context` 依存を `RepositoryFactory` に移動
- expect/actual パターンで `RepositoryFactory` をプラットフォーム固有に実装

**実装例:**
```kotlin
// commonMain
class DiaryListViewModel(
    private val repositoryFactory: RepositoryFactory
) : ViewModel() {
    // ...
}

// commonMain - expect
expect class RepositoryFactory {
    fun getEntries(): Flow<List<DiaryEntry>>
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

### 3. 通知システムの抽象化

**課題:**
- Android: `AlarmManager` + `NotificationCompat`
- iOS: `UNUserNotificationCenter`
- API が全く異なる

**解決策:**
- expect/actual パターンで `NotificationManager` インターフェースを定義
- プラットフォーム固有の実装を actual で提供

**実装例:**
```kotlin
// commonMain - expect
expect class NotificationManager {
    suspend fun scheduleDailyNotification(hour: Int, minute: Int): Result<Unit>
    suspend fun cancelDailyNotification(): Result<Unit>
    suspend fun canScheduleExactAlarms(): Boolean
}

// androidMain - actual
actual class AndroidNotificationManager(
    private val context: Context
) : NotificationManager {
    actual override suspend fun scheduleDailyNotification(hour: Int, minute: Int): Result<Unit> {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // AlarmManager の実装
    }
}

// iosMain - actual
actual class IOSNotificationManager : NotificationManager {
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    actual override suspend fun scheduleDailyNotification(hour: Int, minute: Int): Result<Unit> {
        // UNUserNotificationCenter の実装
    }
}
```

### 4. ファイルストレージの抽象化

**課題:**
- Android: `Context.filesDir`
- iOS: `NSFileManager`
- ファイルパスの取得方法が異なる

**解決策:**
- expect/actual パターンで `FileStorage` を定義
- プラットフォーム固有の実装で適切なディレクトリを返す

**実装例:**
```kotlin
// commonMain
expect class FileStorage {
    suspend fun writeFile(fileName: String, content: String): Result<Unit>
    suspend fun readFile(fileName: String): Result<String>
    suspend fun listFiles(): Result<List<String>>
}

// androidMain
actual class FileStorage(private val context: Context) {
    private val diaryDir = File(context.filesDir, "diary_entries")
    // ...
}

// iosMain
actual class FileStorage {
    private val fileManager = NSFileManager.defaultManager
    private val documentsDirectory = fileManager.URLsForDirectory(
        NSDocumentDirectory,
        NSUserDomainMask
    ).first() as NSURL
    // ...
}
```

### 5. 設定ストレージの統一

**課題:**
- Android: DataStore（Preferences DataStore）
- iOS: UserDefaults
- API が異なる

**解決策:**
- expect/actual パターンで `SettingsStorage` を定義
- 両プラットフォームで `Flow` を使用してリアクティブに

**実装例:**
```kotlin
// commonMain
expect class SettingsStorage {
    suspend fun saveString(key: String, value: String)
    fun getString(key: String): Flow<String?>
}

// androidMain - DataStore
actual class SettingsStorage(context: Context) {
    private val dataStore = context.dataStore
    actual fun getString(key: String): Flow<String?> = dataStore.data.map { it[stringPreferencesKey(key)] }
}

// iosMain - UserDefaults
actual class SettingsStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    actual fun getString(key: String): Flow<String?> = flow {
        emit(userDefaults.stringForKey(key))
    }
}
```

### 6. iOS でのコルーチンの使用

**課題:**
- iOS では suspend 関数を直接呼び出せない（Objective-C/Swift との interop）

**解決策:**
- Kotlin/Native の `@Throws` アノテーションを使用
- Swift 側で `async/await` または completion handler として使用

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

### 7. iOS フレームワークの export 設定

**課題:**
- iOS フレームワークから依存関係にアクセスできない

**解決策:**
- `build.gradle.kts` で export する依存関係を `api` として宣言
- フレームワーク設定で `export` を追加

**実装例:**
```kotlin
// shared/build.gradle.kts
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

---

## ベストプラクティス

### 1. expect/actual の使い方

**推奨:**
- インターフェースではなくクラスに expect/actual を使用
- actual 実装で具体的な型を返す（expect では抽象的に）

**例:**
```kotlin
// Good
expect class NotificationManager {
    suspend fun schedule(hour: Int, minute: Int): Result<Unit>
}

actual class NotificationManager {
    actual suspend fun schedule(hour: Int, minute: Int): Result<Unit> {
        // プラットフォーム固有実装
    }
}

// Avoid
expect interface NotificationManager // expect で interface は避ける
```

### 2. プラットフォーム固有の依存性注入

**推奨:**
- プラットフォーム固有のモジュールと共通モジュールを分離
- Context 依存は Android モジュールのみに閉じ込める

**例:**
```kotlin
// Good
val androidAppModule = module {
    single<SettingsStorage> { SettingsStorage(androidContext()) }
}

val iosAppModule = module {
    single<SettingsStorage> { SettingsStorage() }
}

val viewModelModule = module {
    viewModel { DiaryListViewModel(get()) }
}

// Avoid
val commonModule = module {
    single { context } // Context を共通モジュールに露出しない
}
```

### 3. Flow の使用

**推奨:**
- プラットフォーム間で統一したリアクティブAPIとして `Flow` を使用
- `StateFlow` は UI 状態の管理に最適

**例:**
```kotlin
// Good
class DiaryListViewModel(private val repository: RepositoryFactory) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState
}

// Android
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// iOS
viewModel.uiState.collect { state in
    // UI 更新
}
```

### 4. テストの書き方

**推奨:**
- commonTest にプラットフォーム非依存のテストを配置
- androidTest/iosTest にプラットフォーム固有のテストを配置

**例:**
```kotlin
// commonTest/DiaryEntryTest.kt
class DiaryEntryTest {
    @Test
    fun testSerialization() {
        val entry = DiaryEntry(LocalDate(2025, 11, 11), "Test")
        val json = Json.encodeToString(DiaryEntry.serializer(), entry)
        val decoded = Json.decodeFromString(DiaryEntry.serializer(), json)
        assertEquals(entry, decoded)
    }
}

// androidTest/FileStorageTest.kt
class FileStorageTest {
    @Test
    fun testAndroidFileStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = FileStorage(context)
        // Android 固有のテスト
    }
}
```

---

## 推奨事項とアンチパターン

### 推奨事項 ✅

1. **段階的な移行**
   - 一度にすべてを移行しようとせず、フェーズを分けて進める
   - 各フェーズでビルドとテストを確認

2. **共通コードの最大化**
   - ビジネスロジック、UI、ViewModel はすべて共通コードに
   - プラットフォーム固有コードは最小限に

3. **expect/actual の適切な使用**
   - プラットフォーム固有の API のみに使用
   - 過度に使用しない（共通コードで解決できる場合は共通コードで）

4. **テストファースト**
   - 共通コードには必ずテストを書く
   - プラットフォーム固有コードもテスト可能に設計

5. **ドキュメントの整備**
   - 移行の過程を記録
   - 遭遇した問題と解決策をドキュメント化

### アンチパターン ❌

1. **Context の共通コードへの露出**
   ```kotlin
   // Bad
   expect class MyClass(context: Any) // Context を共通コードに露出

   // Good
   expect class MyClass
   actual class MyClass(private val context: Context)
   ```

2. **プラットフォーム固有コードの過剰使用**
   ```kotlin
   // Bad
   expect fun formatDate(date: LocalDate): String // 共通コードで実装可能

   // Good
   fun formatDate(date: LocalDate): String {
       return "${date.year}年${date.monthNumber}月${date.dayOfMonth}日"
   }
   ```

3. **DI フレームワークの混在**
   ```kotlin
   // Bad
   // Android で Hilt、iOS で Koin を使う

   // Good
   // 両プラットフォームで Koin を統一
   ```

4. **テストの欠如**
   ```kotlin
   // Bad
   // テストなしで移行を進める

   // Good
   // 各フェーズでテストを書き、回帰を防ぐ
   ```

---

## 移行の振り返り

### 成功したこと ✅

1. **段階的なアプローチ**
   - 8つのフェーズに分けて移行したことで、リスクを最小化
   - 各フェーズで動作確認とテストを実施

2. **expect/actual パターンの活用**
   - プラットフォーム固有機能を抽象化し、共通コードから利用可能に
   - 通知、ファイルストレージ、設定管理などで効果的に使用

3. **Koin への移行**
   - Hilt から Koin への移行はスムーズ
   - KMP 対応の DI フレームワークとして優秀

4. **Compose Multiplatform の採用**
   - Android の Jetpack Compose コードをほぼそのまま共通化
   - iOS でも同じ UI コードが動作

5. **テストカバレッジの向上**
   - 共通コードのテストを追加し、品質向上
   - 34テストケースをすべてパス

### 課題と改善点 📝

1. **iOS Git 機能の未実装**
   - iOS での JGit 相当のライブラリが不足
   - 今後、libgit2 の Kotlin/Native バインディングなどを検討

2. **ビルド時間の最適化**
   - iOS フレームワークの初回ビルドに時間がかかる
   - Gradle の設定やキャッシュの最適化が必要

3. **ドキュメントの整備**
   - 移行の途中でドキュメントが不足していた
   - 今後は各フェーズで逐次ドキュメント化

4. **E2E テストの不足**
   - ユニットテストは充実したが、E2E テストが不足
   - 今後、実機での統合テストを強化

### 学んだこと 💡

1. **KMP は production-ready**
   - Kotlin Multiplatform は十分に成熟している
   - 実用的なアプリ開発に使用可能

2. **expect/actual は強力だが慎重に**
   - プラットフォーム固有機能の抽象化に有効
   - 過度に使用するとコードが複雑化

3. **Compose Multiplatform の可能性**
   - Android の Jetpack Compose 資産を活用できる
   - iOS、Desktop、Web への展開も視野に

4. **段階的移行の重要性**
   - 一度にすべてを変更せず、段階的に進めることが成功の鍵
   - 各フェーズでテストと動作確認を徹底

---

## まとめ

OneLine アプリの KMP/CMP 移行は成功し、Android と iOS の両プラットフォームで動作するアプリになりました。この移行ガイドが、他のプロジェクトの KMP 移行の参考になれば幸いです。

**次のステップ:**
- iOS Git 機能の実装
- iOS ウィジェットの追加
- Desktop/Web への展開検討
- パフォーマンスの最適化
- E2E テストの強化

**参考リンク:**
- [Kotlin Multiplatform 公式ドキュメント](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform 公式サイト](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Koin ドキュメント](https://insert-koin.io/)
- [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)

---

**最終更新日:** 2025-11-11
**著者:** OneLine 開発チーム

# Phase 2 実装ガイド - 既存コードの移行

このドキュメントは、Phase 2で行うべき既存Androidコードの`shared`モジュールへの移行手順を詳細に説明します。

## 🎯 Phase 2 の目標

1. `app`モジュールが`shared`モジュールに依存するように更新
2. データレイヤー（Model, Repository）を`shared`に移行
3. ViewModelを可能な限り`shared`に移行
4. Androidアプリが正常にビルド・動作することを確認

## 📋 移行チェックリスト

### Step 1: app モジュールの依存関係更新

#### app/build.gradle.kts

```kotlin
dependencies {
    // 追加: shared モジュールへの依存
    implementation(project(":shared"))
    
    // 既存の依存関係はそのまま
    implementation(libs.androidx.core.ktx)
    // ...
    
    // Android専用機能のみ残す
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    // ...
}
```

### Step 2: データモデルの移行

#### 2.1 DiaryEntry.kt の置き換え

**削除**: `app/src/main/java/net/chasmine/oneline/data/model/DiaryEntry.kt`

**更新が必要なファイル**:
```bash
# DiaryEntry をインポートしているファイルを検索
grep -r "import.*DiaryEntry" app/src/main/java/
```

**変更内容**:
```kotlin
// 変更前
import net.chasmine.oneline.data.model.DiaryEntry
import java.time.LocalDate

// 変更後
import net.chasmine.oneline.data.model.DiaryEntry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate  // 必要に応じて
import kotlinx.datetime.toKotlinLocalDate // 必要に応じて
```

**変換ヘルパー** (必要に応じて作成):
```kotlin
// app/src/main/java/net/chasmine/oneline/util/DateConversion.kt
package net.chasmine.oneline.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate

fun LocalDate.toJava(): java.time.LocalDate = this.toJavaLocalDate()
fun java.time.LocalDate.toKotlin(): LocalDate = this.toKotlinLocalDate()
```

### Step 3: リポジトリレイヤーの移行

#### 3.1 LocalRepository の移行

**現状**: `app/src/main/java/net/chasmine/oneline/data/local/LocalRepository.kt`

**移行先**: `shared/src/androidMain/kotlin/net/chasmine/oneline/data/local/LocalRepository.kt`

**手順**:
1. ファイルをコピー
2. インポート文の更新
   ```kotlin
   // java.time → kotlinx.datetime
   import kotlinx.datetime.LocalDate
   import kotlinx.datetime.Clock
   ```
3. `DiaryRepository`インターフェースの実装
   ```kotlin
   class LocalRepository(
       private val context: Context
   ) : DiaryRepository {
       // 既存の実装をインターフェースに合わせて調整
   }
   ```

#### 3.2 GitRepository の移行

**現状**: `app/src/main/java/net/chasmine/oneline/data/git/GitRepository.kt`

**移行先**: `shared/src/androidMain/kotlin/net/chasmine/oneline/data/git/GitRepository.kt`

**注意点**:
- JGitはJVM専用なのでandroidMainに配置
- 将来的なiOS対応は別の方法を検討

#### 3.3 RepositoryManager の更新

**現状**: `app/src/main/java/net/chasmine/oneline/data/repository/RepositoryManager.kt`

**移行戦略**:

**Option A**: RepositoryManagerをandroidMainに配置
```kotlin
// shared/src/androidMain/kotlin/.../repository/RepositoryManager.kt
class RepositoryManager private constructor(context: Context) {
    private val localRepository = LocalRepository(context)
    private val gitRepository = GitRepository(context)
    private val settingsManager = SettingsManager.getInstance(context)
    
    // 既存の実装
}
```

**Option B**: RepositoryManagerを共通化し、実装を分離
```kotlin
// shared/src/commonMain/kotlin/.../repository/RepositoryManager.kt
expect class RepositoryManager {
    suspend fun saveEntry(entry: DiaryEntry): Boolean
    fun getAllEntries(): Flow<List<DiaryEntry>>
    // ...
}

// shared/src/androidMain/kotlin/.../repository/RepositoryManager.kt
actual class RepositoryManager(context: Context) {
    // Android実装
}
```

**推奨**: まずはOption Aで進める（シンプル）

### Step 4: SettingsManager の移行

#### 4.1 PreferencesManager の作成（共通）

DataStoreは各プラットフォームで異なるため、共通インターフェースを定義:

```kotlin
// shared/src/commonMain/kotlin/.../preferences/PreferencesManager.kt
interface PreferencesManager {
    val isLocalOnlyMode: Flow<Boolean>
    val themeMode: Flow<ThemeMode>
    
    suspend fun setLocalOnlyMode(enabled: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
    // ...
}

expect class PreferencesManagerFactory {
    fun create(): PreferencesManager
}
```

#### 4.2 Android実装

```kotlin
// shared/src/androidMain/kotlin/.../preferences/PreferencesManager.android.kt
actual class PreferencesManagerFactory(private val context: Context) {
    actual fun create(): PreferencesManager = AndroidPreferencesManager(context)
}

class AndroidPreferencesManager(context: Context) : PreferencesManager {
    // 既存のSettingsManagerの実装を使用
}
```

### Step 5: ViewModelの移行

ViewModelはAndroid依存が強いため、段階的に移行:

#### 5.1 共通のViewModelロジック抽出

```kotlin
// shared/src/commonMain/kotlin/.../viewmodels/DiaryListViewModelLogic.kt
class DiaryListViewModelLogic(
    private val repository: DiaryRepository
) {
    private val _uiState = MutableStateFlow<DiaryListUiState>(DiaryListUiState.Loading)
    val uiState: StateFlow<DiaryListUiState> = _uiState.asStateFlow()
    
    suspend fun loadEntries() {
        repository.getAllEntries().collect { entries ->
            _uiState.value = DiaryListUiState.Success(entries)
        }
    }
}
```

#### 5.2 AndroidViewModelでラップ

```kotlin
// app/src/main/java/.../viewmodels/DiaryListViewModel.kt
@HiltViewModel
class DiaryListViewModel @Inject constructor(
    private val repository: RepositoryManager
) : ViewModel() {
    
    private val logic = DiaryListViewModelLogic(repository)
    val uiState = logic.uiState
    
    init {
        viewModelScope.launch {
            logic.loadEntries()
        }
    }
}
```

### Step 6: UI Screens の準備

現時点では完全移行は不要。将来の移行を見据えた準備のみ:

#### 6.1 UI State の共通化

```kotlin
// shared/src/commonMain/kotlin/.../ui/state/DiaryListUiState.kt
sealed class DiaryListUiState {
    object Loading : DiaryListUiState()
    data class Success(val entries: List<DiaryEntry>) : DiaryListUiState()
    data class Error(val message: String) : DiaryListUiState()
}
```

#### 6.2 既存の画面でStateを使用

```kotlin
// app/src/main/java/.../screens/DiaryListScreen.kt
@Composable
fun DiaryListScreen(
    viewModel: DiaryListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (val state = uiState) {
        is DiaryListUiState.Loading -> LoadingView()
        is DiaryListUiState.Success -> DiaryList(state.entries)
        is DiaryListUiState.Error -> ErrorView(state.message)
    }
}
```

### Step 7: ビルドと動作確認

#### 7.1 Gradleビルド

```bash
# クリーンビルド
./gradlew clean

# sharedモジュールのビルド
./gradlew :shared:build

# appモジュールのビルド
./gradlew :app:assembleDebug
```

#### 7.2 テストの実行

```bash
# 共通コードのテスト
./gradlew :shared:testDebugUnitTest

# Androidのテスト
./gradlew :app:testDebugUnitTest
```

#### 7.3 アプリの起動確認

```bash
# エミュレータで実行
./gradlew :app:installDebug
```

**確認項目**:
- [ ] アプリが起動する
- [ ] 日記一覧が表示される
- [ ] 日記の作成・編集ができる
- [ ] 設定画面が動作する
- [ ] Git同期が動作する（設定している場合）

## 🔧 トラブルシューティング

### 問題: kotlinx.datetime と java.time の混在

**症状**: 
```
Type mismatch: inferred type is LocalDate but java.time.LocalDate was expected
```

**解決策**:
```kotlin
// 変換関数を使用
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate

val javaDate: java.time.LocalDate = kotlinDate.toJavaLocalDate()
val kotlinDate: kotlinx.datetime.LocalDate = javaDate.toKotlinLocalDate()
```

### 問題: Context が共通コードで使えない

**症状**:
```
Unresolved reference: Context
```

**解決策**:
```kotlin
// commonMainではContextを使わない
// androidMainで実装

// ✗ 悪い例
// commonMain
class Repository(private val context: Context)  // NG

// ✓ 良い例
// commonMain
expect class RepositoryFactory {
    fun create(): Repository
}

// androidMain
actual class RepositoryFactory(private val context: Context) {
    actual fun create() = AndroidRepository(context)
}
```

### 問題: DataStore が commonMain で使えない

**症状**:
```
Unresolved reference: androidx.datastore
```

**解決策**:
```kotlin
// インターフェースを共通化、実装をプラットフォーム固有に

// commonMain
interface PreferencesManager {
    val settings: Flow<Settings>
}

expect class PreferencesManagerFactory {
    fun create(): PreferencesManager
}

// androidMain
import androidx.datastore.core.DataStore

actual class PreferencesManagerFactory(context: Context) {
    actual fun create() = AndroidPreferencesManager(
        context.dataStore
    )
}
```

## 📊 進捗確認

各ステップ完了後、以下を確認:

```kotlin
// shared/build.gradle.kts に追加
tasks.register("checkMigrationProgress") {
    doLast {
        println("✅ Step 1: Dependencies - Check app/build.gradle.kts")
        println("✅ Step 2: DiaryEntry - Check shared/commonMain")
        println("✅ Step 3: Repositories - Check shared/androidMain")
        println("✅ Step 4: Settings - Check shared/androidMain")
        println("✅ Step 5: ViewModels - Check logic extraction")
        println("✅ Step 6: UI States - Check shared/commonMain")
        println("✅ Step 7: Build & Test - Run all tests")
    }
}
```

実行:
```bash
./gradlew checkMigrationProgress
```

## 🎯 Phase 2 完了条件

- [ ] `app`モジュールが`shared`に依存している
- [ ] `DiaryEntry`が`shared/commonMain`に存在する
- [ ] リポジトリが`shared/androidMain`に存在する
- [ ] `SettingsManager`が`shared/androidMain`に存在する
- [ ] ViewModelロジックが抽出されている
- [ ] Android アプリがビルドできる
- [ ] Android アプリが正常に動作する
- [ ] すべてのテストがパスする

## 📝 次のフェーズ

Phase 2完了後、Phase 3に進む:

1. **共通UIの実装**
   - DiaryListScreen を shared/commonMain に移行
   - DiaryEditScreen を shared/commonMain に移行
   - その他の画面の移行

2. **iOS実装**
   - IosDiaryRepository の完全実装
   - Xcodeプロジェクトの作成
   - iOSビルドとテスト

---

**ドキュメント作成日**: 2025-10-24  
**対象フェーズ**: Phase 2  
**前提**: Phase 1 完了

# CLAUDE.md
必ず日本語で回答してください。
GitHubのリポジトリなので、GitHubのCLIが利用できます。
リモートリポジトリのissueを確認してそのissueに着手するときは、「issue/[issue番号]_」というプレフィックスをつけた上で、適切なブランチ名を都度考えてブランチを切ってください。

## Git Commit Rules
**重要**: コミットメッセージには以下の署名を含めないこと:
- `🤖 Generated with [Claude Code](https://claude.com/claude-code)`
- `Co-Authored-By: Claude <noreply@anthropic.com>`

コミットメッセージは変更内容のみを簡潔に記述してください。

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# OneLine - Architecture Overview

A Japanese diary application built with Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP) that supports both Android and iOS platforms, with local-only and Git-based cloud synchronization.

## アプリの目的

OneLineは、手軽に日記を書くことを目的とした日記アプリです。日記を書くハードルを下げ、楽しく継続できる環境を提供します。

**主な特徴:**
- 忙しい毎日の中で忘れてしまいがちな、何でもないようなできごとを簡単に書き留めて振り返ることができる
- 物理的な日記と違い、買い替えや記入忘れの心配がない
- 他の日記サービスと違い、データを完全に自分で管理できる（端末内またはGitリポジトリに保存）
- データはサービスに預けず、完全にプライベートに保管できる

## Module Structure

### KMP/CMP アーキテクチャ

```
OneLine/
├── shared/                           # KMP 共通モジュール
│   ├── src/
│   │   ├── commonMain/kotlin/        # 共通コード（Android/iOS 共通）
│   │   │   ├── data/
│   │   │   │   ├── model/            # DiaryEntry 等のデータモデル
│   │   │   │   ├── preferences/      # expect: SettingsStorage
│   │   │   │   ├── repository/       # expect: RepositoryFactory
│   │   │   │   └── storage/          # expect: FileStorage
│   │   │   ├── di/
│   │   │   │   └── ViewModelModule.kt # 共通 ViewModel の DI 定義
│   │   │   ├── ui/
│   │   │   │   ├── screens/          # Compose UI 画面
│   │   │   │   ├── components/       # 再利用可能な UI コンポーネント
│   │   │   │   ├── viewmodels/       # ViewModel（androidx.lifecycle）
│   │   │   │   └── theme/            # Material 3 テーマ
│   │   │   └── util/
│   │   │       ├── DiaryStatistics.kt # 統計計算ユーティリティ
│   │   │       └── NotificationManager # expect: 通知管理
│   │   ├── androidMain/kotlin/       # Android 固有実装
│   │   │   ├── data/
│   │   │   │   ├── preferences/      # actual: SettingsStorage (DataStore)
│   │   │   │   ├── repository/       # actual: RepositoryFactory (JGit)
│   │   │   │   └── storage/          # actual: FileStorage (Context.filesDir)
│   │   │   ├── di/
│   │   │   │   └── AndroidAppModule.kt # Android 固有 DI
│   │   │   └── util/
│   │   │       └── NotificationManager # actual: AlarmManager ベース
│   │   ├── iosMain/kotlin/           # iOS 固有実装
│   │   │   ├── data/
│   │   │   │   ├── preferences/      # actual: SettingsStorage (UserDefaults)
│   │   │   │   ├── repository/       # actual: RepositoryFactory
│   │   │   │   └── storage/          # actual: FileStorage (NSFileManager)
│   │   │   ├── di/
│   │   │   │   └── IosAppModule.kt   # iOS 固有 DI
│   │   │   ├── KoinInitializer.kt    # iOS 用 Koin 初期化
│   │   │   ├── MainViewController.kt # iOS Compose エントリーポイント
│   │   │   └── util/
│   │   │       └── NotificationManager # actual: UNUserNotificationCenter
│   │   └── commonTest/kotlin/        # 共通テスト
│   │       ├── data/model/
│   │       │   └── DiaryEntryTest.kt
│   │       └── util/
│   │           └── DiaryStatisticsTest.kt
│   └── build.gradle.kts              # KMP ビルド設定
├── androidApp/                       # Android アプリモジュール
│   ├── src/main/
│   │   ├── java/net/chasmine/oneline/
│   │   │   ├── OneLineApplication.kt # Koin 初期化
│   │   │   ├── MainActivity.kt       # Compose エントリーポイント
│   │   │   ├── util/
│   │   │   │   ├── NotificationReceiver.kt
│   │   │   │   └── NotificationInitializer.kt
│   │   │   └── widget/               # Android 専用ウィジェット
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
└── iosApp/                           # iOS アプリモジュール
    ├── iosApp/
    │   ├── iOSApp.swift              # SwiftUI エントリーポイント
    │   ├── ContentView.swift         # Compose ブリッジ
    │   └── Info.plist
    ├── build.sh                      # iOS フレームワークビルドスクリプト
    └── README.md
```

## Architectural Patterns

### 1. **MVVM (Model-View-ViewModel) - KMP 対応**
- **ViewModels**: `DiaryListViewModel`, `DiaryEditViewModel`, `SettingViewModel`
- Extends `ViewModel` from `androidx.lifecycle`（KMP 対応）
- Uses `StateFlow<UiState>` for UI state management
- All ViewModels access repositories through `RepositoryFactory`（expect/actual パターン）
- **DI**: Koin を使用（Hilt から移行）

**Key Files**:
- `shared/src/commonMain/kotlin/ui/viewmodels/*.kt`（共通 ViewModel）
- `shared/src/commonMain/kotlin/di/ViewModelModule.kt`（ViewModel の DI 定義）

### 2. **Repository Pattern with expect/actual**
- **RepositoryFactory**: プラットフォーム固有のリポジトリ生成（expect/actual パターン）
  - Android: JGit ベースの GitRepository
  - iOS: Git 機能は未実装（将来的に libgit2 等を使用予定）
  - ローカルモード/Git モードの切り替えをサポート
  - データの保存/読み込みを抽象化

**Key Concept**: expect/actual パターンでプラットフォーム固有実装を抽象化:
```kotlin
// commonMain - expect
expect class RepositoryFactory {
    suspend fun getEntries(): Flow<List<DiaryEntry>>
    suspend fun saveEntry(entry: DiaryEntry): Result<Boolean>

    companion object {
        fun create(): RepositoryFactory
    }
}

// androidMain - actual (JGit 統合)
actual class RepositoryFactory(private val context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)

    actual suspend fun getEntries(): Flow<List<DiaryEntry>> {
        // ローカル/Git モードに応じて適切なリポジトリから取得
    }
}

// iosMain - actual (ローカルのみ)
actual class RepositoryFactory {
    actual suspend fun getEntries(): Flow<List<DiaryEntry>> {
        // iOS 固有の実装
    }
}
```

### 3. **Flow-Based Reactivity**
- Uses Kotlin coroutines and Flow extensively
- `StateFlow` for UI state (exposed as immutable in ViewModels)
- `Flow<List<DiaryEntry>>` for data streams
- All long-running operations on `Dispatchers.IO`

### 4. **Sealed Classes for Type-Safe State**
```kotlin
// Typical pattern throughout codebase
sealed class UiState {
    object Loading : UiState()
    data class Editing(val entry: DiaryEntry, val isNew: Boolean) : UiState()
    data class Error(val message: String) : UiState()
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}
```

## Dependency Injection

**Current State**: Minimal Hilt usage despite it being available
- `Hilt` is declared in build.gradle but `AppModule` is empty
- Most instances use **manual singleton pattern** with double-checked locking
- Single instance per component (e.g., `SettingsManager.getInstance()`)

**Singleton Pattern Used**:
```kotlin
companion object {
    @Volatile
    private var INSTANCE: GitRepository? = null
    
    fun getInstance(context: Context): GitRepository {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: GitRepository(context.applicationContext).also {
                INSTANCE = it
            }
        }
    }
}
```

**Note**: This is pragmatic for the current codebase size but Hilt could be beneficial for future modularity.

## Data Layer Architecture

### Three Core Data Sources

#### 1. **LocalRepository** (`data/local/LocalRepository.kt`)
- Stores entries in `context.filesDir/diary_entries/` as `.txt` files
- Simple file-based persistence
- No network dependencies
- Includes integrity checking and repair utilities
- Provides migration capability to Git

**File Format**: `YYYY-MM-DD.txt`

#### 2. **GitRepository** (`data/git/GitRepository.kt`)
- Uses **JGit** for Git operations
- Stores entries in Git as `.md` files
- Handles cloning, committing, pushing, pulling
- Advanced features:
  - Repository validation (safety checks to prevent damage)
  - Conflict resolution strategies
  - Automatic retry with merge strategy
  - Repository migration between different Git URLs
  
**File Format**: `YYYY-MM-DD.md`

**Conflict Resolution Strategy**:
- Uses "OURS" merge strategy (preserves local changes)
- Automatically removes conflict markers
- Retries failed pushes with pull+merge

#### 3. **SettingsManager** (`data/preferences/SettingManager.kt`)
- Uses **AndroidX DataStore** (not SharedPreferences)
- Stores:
  - Git credentials (URL, username, token)
  - Local-only mode flag
  - Theme preference (LIGHT/DARK/SYSTEM)
- Reactive: All settings exposed as `Flow<T>`

**Key Methods**:
```kotlin
suspend fun saveGitSettings(repoUrl, username, token)
suspend fun setLocalOnlyMode(enabled: Boolean)
val isLocalOnlyMode: Flow<Boolean>
val hasValidSettings: Flow<Boolean>
```

### Data Model

**DiaryEntry** - Immutable data class:
```kotlin
data class DiaryEntry(
    val date: LocalDate,
    val content: String,
    val lastModified: Long = System.currentTimeMillis(),
) {
    fun getFileName(): String = "${date}.md"
    fun getDisplayDate(): String = "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
}
```

## Navigation Structure

**Type**: Jetpack Compose Navigation with `NavHost`

**Routes**:
```
welcome              # First-launch setup screen
diary_list           # Main list of entries (tab 0)
diary_edit/{date}    # Edit/create diary entry (date="new" for today)
calendar             # Calendar view (tab 1)
settings             # Main settings menu
git_settings         # Git configuration
notification_settings # Notification preferences
data_storage_settings # Storage mode selection
about                # About screen
```

**Navigation Graph** (`MainActivity.kt`):
- Scaffold-based with bottom navigation
- Custom animation: slides based on tab direction
- Welcome screen is initial route for first-time users

**Bottom Navigation Component**: 
- `CustomBottomBar`: Two-tab layout with centered FAB (floating action button)
- Tabs: Diary List | Calendar
- FAB: New entry creation (triggers Git config dialog if not configured)

## Key Feature Implementations

### 1. **Dual Storage Mode**
- **Local-Only**: Files stored in app's private directory
- **Git-Based**: Files synced to GitHub (or other Git provider)
- Toggle in settings triggers migration automatically
- `RepositoryManager` handles seamless switching

### 2. **Git Integration Features**
- **Repository Validation**: Prevents using development/code repositories
  - Checks for source files (.kt, .java, .gradle)
  - Validates repository ownership
  - Suggests diary-specific names
  
- **Safe Conflict Resolution**: 
  - Always preserves local diary content
  - Auto-removes conflict markers
  - Retries with merge strategy on failure

- **Repository Migration**:
  - Switch between different Git URLs
  - Automatic data migration or discard option
  - Conflict resolution for overlapping dates

### 3. **Notification System**
- **AlarmManager-based**: Scheduled daily reminders
- Uses **Exact Alarms** when permission granted
- Falls back to inexact alarms on Android 12+
- Permission handling for Android 13+ POST_NOTIFICATIONS

### 4. **Widget Integration (Glance)**
- **Technology**: Androidx Glance (modern widget framework)
- **Widget Type**: App Widget with action callbacks
- **Interaction**: Click opens diary entry activity or main app
- **Receiver**: `JournalWidgetReceiver`
- **Entry Activity**: `DiaryWidgetEntryActivity` (popup-style dialog)

### 5. **Theme System**
- **Material 3** with custom color scheme
- Three modes: LIGHT, DARK, SYSTEM
- Dynamic color support on Android 12+
- Stored in DataStore for persistence
- Real-time theme switching (no app restart)

## Unique Patterns & Conventions

### 1. **Result<T> Pattern**
Used extensively in `GitRepository` for safer error handling:
```kotlin
suspend fun initRepository(...): Result<Boolean>
suspend fun saveEntry(entry: DiaryEntry): Result<Boolean>
```

### 2. **Validation Enums**
Rich validation feedback with specific enum states:
```kotlin
enum class ValidationResult {
    DIARY_REPOSITORY,               // ✅ Safe
    LIKELY_DIARY_REPOSITORY,        // ✅ Probably safe
    UNKNOWN_REPOSITORY,             // ⚠️ Unknown
    SUSPICIOUS_REPOSITORY,          // ❌ Likely dangerous
    DANGEROUS_REPOSITORY,           // ❌ Definitely dangerous
    OWNERSHIP_VERIFICATION_FAILED,  // 🚨 Security risk
    // ... more states
}
```

### 3. **Safe Repository Validation**
Intelligent validation without cloning full repo:
- Uses shallow clone (`--depth=1`) for lightweight file inspection
- Checks file extensions and patterns
- Validates repository ownership from URL
- Prevents accidental use of code repositories

### 4. **Japanese Localization**
- App name: "OneLine" (English) with Japanese copy
- All UI text in Japanese
- Date formatting: "YYYY年MM月DD日" format

### 5. **Data Integrity Checking**
`LocalRepository` includes:
```kotlin
suspend fun checkDataIntegrity(): DataIntegrityResult
suspend fun repairCorruptedFiles(corruptedFiles: List<String>): Boolean
```

### 6. **Edge-to-Edge Theming**
- `enableEdgeToEdge()` in MainActivity
- TopAppBar extends to status bar
- Custom status bar color management

## Testing Infrastructure

**Types Configured**:
- Unit tests (JUnit + Mockito)
- Integration tests (Coroutines test utilities)
- UI tests (Espresso + Compose test)

**Custom Test Tasks** (in build.gradle.kts):
```kotlin
tasks.register("runAllTests")
tasks.register("runUnitTests")
tasks.register("runIntegrationTests")
tasks.register("runUITests")
tasks.register("testWithCoverage")
```

**Key Dependencies**:
- Mockito 5.5.0
- Coroutines test utilities
- AndroidX test (JUnit, Espresso)

## Permissions Required

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
```

## Key Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose | Latest | UI framework |
| Navigation Compose | Latest | Navigation |
| JGit | Latest | Git operations |
| DataStore | Latest | Encrypted settings |
| Glance | Latest | App widgets |
| Coroutines | 1.7.3+ | Async operations |
| Bouncy Castle | jdk18on | Cryptography support |

## Important Implementation Notes

### 1. **Application Context Usage**
- All singletons use `context.applicationContext` to prevent memory leaks
- Suppresses StaticFieldLeak warnings explicitly

### 2. **Dispatcher Strategy**
- Main UI: Compose's implicit Main dispatcher
- Long operations: `Dispatchers.IO`
- Settings: DataStore handles threading

### 3. **Error Handling Philosophy**
- Silent failures with logging for recoverable errors
- User-facing messages for critical failures
- Detailed error states in sealed classes

### 4. **UI State Management**
- No mutable state exposed to UI (only StateFlow)
- All state mutations happen in ViewModel
- Events trigger side effects through coroutines

### 5. **Lifecycle Integration**
- ViewModel survives configuration changes
- LaunchedEffect for one-time initialization
- rememberCoroutineScope for user-triggered operations

## Future Considerations

1. **Hilt Migration**: Currently not leveraged despite dependency
2. **Offline-First**: Currently requires network for Git operations
3. **Encryption**: Credentials stored plaintext (consider EncryptedSharedPreferences)
4. **Sync Optimization**: Could implement background WorkManager jobs
5. **Modularization**: Could split into feature modules if app grows

---

**Last Updated**: 2025-10-18
**Min SDK**: 28 | **Target SDK**: 35
**Language**: Kotlin | **UI Framework**: Jetpack Compose

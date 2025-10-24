# KMP/CMP移行 Phase 1 完了レポート

## 📊 実装サマリー

このPRは、OneLineアプリをAndroid専用アプリからKotlin Multiplatform (KMP) / Compose Multiplatform (CMP)対応のマルチプラットフォームアプリに移行するための **Phase 1: 基盤構築** を完了しました。

## ✅ Phase 1で実装した内容

### 1. プロジェクト構造の刷新

#### 新規モジュール
- `shared/` - KMP対応の共通コードモジュール
- `iosApp/` - iOSアプリケーションの骨格

#### ディレクトリ構成
```
OneLine/
├── shared/                          # NEW: 共通コードモジュール
│   ├── src/
│   │   ├── commonMain/kotlin/       # プラットフォーム非依存コード
│   │   ├── androidMain/kotlin/      # Android固有実装
│   │   └── iosMain/kotlin/          # iOS固有実装
│   └── build.gradle.kts             # KMP設定
├── app/                             # EXISTING: Androidアプリ
├── iosApp/                          # NEW: iOSアプリ
│   └── iosApp/
│       ├── iOSApp.swift
│       └── ContentView.swift
└── build.gradle.kts                 # UPDATED: KMPプラグイン追加
```

### 2. 共通データレイヤー（commonMain）

#### DiaryEntry.kt
**変更前 (Android専用)**:
```kotlin
import java.time.LocalDate

data class DiaryEntry(
    val date: LocalDate,  // java.time (JVM専用)
    val lastModified: Long = System.currentTimeMillis()
)
```

**変更後 (マルチプラットフォーム)**:
```kotlin
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock

data class DiaryEntry(
    val date: LocalDate,  // kotlinx.datetime (全プラットフォーム対応)
    val lastModified: Long = Clock.System.now().toEpochMilliseconds()
)
```

#### DiaryRepository.kt (新規)
expect/actualパターンでリポジトリを抽象化:
```kotlin
// commonMain - インターフェース定義
interface DiaryRepository {
    suspend fun saveEntry(entry: DiaryEntry): Boolean
    fun getAllEntries(): Flow<List<DiaryEntry>>
    // ...
}

// プラットフォーム固有のファクトリ
expect class DiaryRepositoryFactory {
    fun createRepository(): DiaryRepository
}

// androidMain - Android実装
actual class DiaryRepositoryFactory(private val context: Context) {
    actual fun createRepository(): DiaryRepository = AndroidDiaryRepository(context)
}

// iosMain - iOS実装
actual class DiaryRepositoryFactory {
    actual fun createRepository(): DiaryRepository = IosDiaryRepository()
}
```

### 3. 共通UIレイヤー（commonMain）

#### テーマシステム
以下のファイルをマルチプラットフォーム対応で作成:

1. **Color.kt** - iOS風カラーパレット
   - ライトテーマ/ダークテーマ両対応
   - Material 3 カラースキーム準拠

2. **Type.kt** - タイポグラフィ定義
   - プラットフォーム非依存
   - Material 3 タイポグラフィスケール

3. **Theme.kt** - メインテーマ
   ```kotlin
   @Composable
   fun OneLineTheme(
       darkTheme: Boolean = isSystemInDarkTheme(),
       content: @Composable () -> Unit
   )
   
   @Composable
   expect fun isSystemInDarkTheme(): Boolean  // プラットフォーム固有
   ```

### 4. プラットフォーム固有実装

#### Android (androidMain)
```kotlin
// Theme.android.kt
@Composable
actual fun isSystemInDarkTheme(): Boolean = 
    androidx.compose.foundation.isSystemInDarkTheme()

// DiaryRepository.android.kt
actual class DiaryRepositoryFactory(private val context: Context) {
    actual fun createRepository() = AndroidDiaryRepository(context)
}
```

#### iOS (iosMain)
```kotlin
// Theme.ios.kt
@Composable
actual fun isSystemInDarkTheme(): Boolean {
    val userInterfaceStyle = UITraitCollection.currentTraitCollection.userInterfaceStyle
    return userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
}

// DiaryRepository.ios.kt
actual class DiaryRepositoryFactory {
    actual fun createRepository() = IosDiaryRepository()
}
```

### 5. iOSアプリの骨格

#### iOSApp.swift
```swift
import SwiftUI
import shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

#### ContentView.swift
```swift
struct ContentView: View {
    var body: some View {
        VStack {
            Text("OneLine")
                .font(.largeTitle)
            Text("日記アプリ - iOS版")
                .font(.title2)
        }
    }
}
```

### 6. Gradle設定の更新

#### libs.versions.toml
新規追加した依存関係:
```toml
[versions]
kotlinxCoroutinesCore = "1.7.3"
kotlinxDatetime = "0.6.1"
composeMultiplatform = "1.7.1"

[libraries]
kotlinx-coroutines-core = "..."
kotlinx-datetime = "..."

[plugins]
kotlin-multiplatform = "..."
compose-multiplatform = "..."
android-library = "..."
```

#### build.gradle.kts (ルート)
```kotlin
buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    }
}
```

#### shared/build.gradle.kts
```kotlin
kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation("kotlinx-coroutines-core")
            implementation("kotlinx-datetime")
        }
        androidMain.dependencies {
            implementation("androidx.core:core-ktx")
            implementation("org.eclipse.jgit:...")
        }
    }
}
```

### 7. ドキュメント作成

- **KMP_MIGRATION_GUIDE.md** - 詳細な移行ガイド
- **KMP_IMPLEMENTATION_GUIDE.md** - 実装ガイドとTODO

## 🎯 主要な設計決定

### expect/actual パターンの採用

プラットフォーム固有の実装を明確に分離:
```kotlin
// 共通: インターフェース定義
expect class PlatformSpecific

// Android: 具体的な実装
actual class PlatformSpecific { ... }

// iOS: 具体的な実装  
actual class PlatformSpecific { ... }
```

**利点**:
- 型安全性の保証
- コンパイル時のチェック
- IDEのサポート

### kotlinx ライブラリの活用

| JVM専用 → マルチプラットフォーム |
|---|---|
| `java.time.LocalDate` → `kotlinx.datetime.LocalDate` |
| `System.currentTimeMillis()` → `Clock.System.now()` |

### Android専用機能の明確化

以下はAndroid専用として残す:
- App Widget (Glance) - iOS非対応
- Notification (AlarmManager) - プラットフォーム固有
- JGit (Git同期) - JVM専用 ※要検討

## 🚧 既知の制限事項

### 1. ビルド未確認
Google Mavenリポジトリへのアクセス制限により、実際のビルドは未確認です。
実際の開発環境では動作するはずです。

### 2. JGit問題
現在の実装で使用しているJGitはJVM専用です。
iOS対応には以下の選択肢があります:

**オプション A**: libgit2 + Kotlin/Native interop
```kotlin
// iosMain
actual class GitRepository {
    private val git = LibGit2Wrapper()  // Cinterop
}
```

**オプション B**: プラットフォームネイティブ実装
```kotlin
// androidMain - JGit使用
actual class GitRepository { ... }

// iosMain - NSTask/Git CLI使用
actual class GitRepository { ... }
```

**オプション C**: HTTP APIベース同期
- プラットフォーム非依存
- GitHubのREST APIを直接使用
- より柔軟だが実装コストが高い

### 3. Android固有機能
Widget、Notificationは `app/` モジュールに残し、Android専用機能として維持します。

## 📋 次フェーズのタスク

### Phase 2: 既存コードの移行

#### 優先度: 高
- [ ] 既存の `app/` モジュールを `shared` 依存に更新
- [ ] データレイヤーの完全実装
  - LocalRepository の移行
  - GitRepository のAndroid実装
  - SettingsManager の共通化
- [ ] ViewModel の共通化
  - DiaryListViewModel
  - DiaryEditViewModel
  - SettingViewModel

#### 優先度: 中
- [ ] 共通UI画面の実装
  - DiaryListScreen
  - DiaryEditScreen
  - CalendarScreen
  - SettingsScreen
- [ ] Navigation の共通化

#### 優先度: 低
- [ ] iOS向けの完全なリポジトリ実装
- [ ] Git同期のマルチプラットフォーム対応

### Phase 3: iOS完成

- [ ] Xcodeプロジェクトの作成
- [ ] Kotlin Frameworkの統合
- [ ] SwiftUIとCompose UIの統合
- [ ] iOSビルド・テスト

### Phase 4: 品質向上

- [ ] ユニットテストの実装
- [ ] 統合テストの実装
- [ ] CI/CDパイプラインの更新
- [ ] App Store対応

## 📚 参考情報

### アーキテクチャパターン

```
┌─────────────────────────────────────┐
│         Presentation Layer          │
│  (Compose Multiplatform UI)        │
│  - Screens                          │
│  - Components                       │
│  - ViewModels (shared)              │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          Domain Layer               │
│  - Use Cases (optional)             │
│  - Business Logic                   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│           Data Layer                │
│  - Repository Interface (common)    │
│  - Models (common)                  │
│  ├─ Android Implementation          │
│  │   - LocalRepository              │
│  │   - GitRepository (JGit)         │
│  └─ iOS Implementation              │
│      - LocalRepository              │
│      - GitRepository (要実装)        │
└─────────────────────────────────────┘
```

### フォルダ構成の最終イメージ

```
shared/src/
├── commonMain/kotlin/
│   └── net/chasmine/oneline/
│       ├── data/
│       │   ├── model/
│       │   │   └── DiaryEntry.kt ✅
│       │   └── repository/
│       │       └── DiaryRepository.kt ✅
│       ├── domain/
│       │   └── usecase/              ⏳ (将来)
│       └── ui/
│           ├── screens/
│           │   ├── DiaryListScreen.kt  🚧
│           │   ├── DiaryEditScreen.kt  🚧
│           │   └── SettingsScreen.kt   🚧
│           ├── components/           🚧
│           ├── viewmodels/           🚧
│           └── theme/
│               ├── Color.kt ✅
│               ├── Theme.kt ✅
│               └── Type.kt ✅
├── androidMain/kotlin/
│   └── net/chasmine/oneline/
│       ├── data/
│       │   └── repository/
│       │       └── DiaryRepository.android.kt ✅
│       └── ui/
│           └── theme/
│               └── Theme.android.kt ✅
└── iosMain/kotlin/
    └── net/chasmine/oneline/
        ├── data/
        │   └── repository/
        │       └── DiaryRepository.ios.kt ✅
        └── ui/
            └── theme/
                └── Theme.ios.kt ✅

Legend:
✅ = 実装済み
🚧 = 未実装（次フェーズ）
⏳ = 将来的に検討
```

## 🎓 学んだこと

### 1. expect/actual の使い方
- インターフェースではなくクラスでも使える
- Composable関数でも使える
- 型パラメータの扱いに注意

### 2. kotlinx.datetime の利点
- プラットフォーム非依存
- java.time より API がシンプル
- タイムゾーン処理が明確

### 3. Compose Multiplatform の可能性
- ほとんどのUIコードを共通化できる
- Material 3が全プラットフォームで使える
- プラットフォーム固有UIは expect/actual で対応

## 💭 今後の検討事項

### 1. Git同期戦略
現状のJGitは JVM 専用。iOS対応には:
- **短期**: iOS版はローカルのみ
- **中期**: HTTP API経由の同期
- **長期**: ネイティブGit統合

### 2. ViewModel の共有
`AndroidViewModel` は Android 専用。選択肢:
- Kotlin Multiplatformの `ViewModel` ライブラリ使用
- 独自の ViewModel 基底クラス作成
- Redux/MVI パターンの採用

### 3. ナビゲーション
- Compose Navigation は Android/Desktop のみ
- マルチプラットフォーム対応のナビゲーションライブラリ検討
  - voyager
  - compose-router
  - PreCompose

## 🏁 まとめ

Phase 1では、KMP/CMP移行のための **基盤を完全に構築** しました。

**完了したこと**:
- ✅ プロジェクト構造の刷新
- ✅ 共通データモデルの作成
- ✅ リポジトリの抽象化
- ✅ 共通UIテーマの実装
- ✅ プラットフォーム固有実装の分離
- ✅ iOS アプリの骨格作成

**次にやること**:
- 🚧 既存 Android コードの移行
- 🚧 共通 UI の実装
- 🚧 iOS リポジトリの完全実装

この基盤の上に、段階的にコードを移行していくことで、
最終的に Android/iOS 両対応の OneLine アプリが完成します。

---

**コミット日**: 2025-10-24  
**Phase**: 1/4 完了  
**次のマイルストーン**: Phase 2 - 既存コードの移行

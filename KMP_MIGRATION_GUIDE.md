# KMP/CMP Migration Guide - OneLine iOS対応

このドキュメントは、OneLineアプリをKotlin Multiplatform (KMP) と Compose Multiplatform (CMP) に移行し、iOS対応を実現するための手順を記録しています。

## 📋 移行の概要

### 目標
- Android専用アプリをKMP/CMP構成に移行
- iOSアプリの開発と動作確認
- ウィジェット機能は対象外（Android専用として残す）

### 対象プラットフォーム
- Android (既存)
- iOS (新規)

## 🏗️ プロジェクト構造

```
OneLine/
├── shared/                      # 共通コードモジュール（KMP）
│   ├── src/
│   │   ├── commonMain/          # プラットフォーム非依存コード
│   │   │   └── kotlin/
│   │   │       └── net/chasmine/oneline/
│   │   │           ├── data/
│   │   │           │   ├── model/        # DiaryEntry等のデータモデル
│   │   │           │   └── repository/   # リポジトリインターフェース
│   │   │           └── ui/
│   │   │               ├── theme/        # カラー、タイポグラフィ
│   │   │               ├── screens/      # 共通画面
│   │   │               └── components/   # 共通コンポーネント
│   │   ├── androidMain/         # Android固有実装
│   │   │   └── kotlin/
│   │   │       └── net/chasmine/oneline/
│   │   │           ├── data/repository/  # Android固有リポジトリ
│   │   │           └── ui/theme/         # Android固有テーマ
│   │   └── iosMain/             # iOS固有実装
│   │       └── kotlin/
│   │           └── net/chasmine/oneline/
│   │               ├── data/repository/  # iOS固有リポジトリ
│   │               └── ui/theme/         # iOS固有テーマ
│   └── build.gradle.kts         # KMP設定
├── app/                         # Androidアプリモジュール
│   └── src/
│       └── main/
│           └── java/
│               └── net/chasmine/oneline/
│                   ├── widget/           # Android専用Widget
│                   ├── util/             # Android専用ユーティリティ
│                   └── MainActivity.kt   # Androidエントリーポイント
├── iosApp/                      # iOSアプリ
│   └── iosApp/
│       ├── ContentView.swift    # iOSエントリーポイント
│       └── Info.plist
├── build.gradle.kts             # ルートビルド設定
└── settings.gradle.kts          # プロジェクト設定
```

## 🔄 移行ステップ

### Step 1: プロジェクト構成の変更 ✅

1. **sharedモジュールの作成**
   ```kotlin
   // settings.gradle.kts
   include(":app")
   include(":shared")  // 追加
   ```

2. **Gradle設定の更新**
   - KMP/CMPプラグインの追加
   - 依存関係の整理
   - ターゲットプラットフォームの設定

### Step 2: 共通コードの作成 ✅

1. **データモデルの共通化**
   - `java.time.LocalDate` → `kotlinx.datetime.LocalDate`
   - `System.currentTimeMillis()` → `Clock.System.now().toEpochMilliseconds()`

2. **リポジトリインターフェースの定義**
   - `DiaryRepository` インターフェース作成
   - `expect/actual` パターンでプラットフォーム固有実装を分離

3. **UI テーマの共通化**
   - カラー定義（Color.kt）
   - タイポグラフィ（Type.kt）
   - テーマ適用（Theme.kt）

### Step 3: プラットフォーム固有実装

#### Android実装 ✅
- `Theme.android.kt`: ダークモード検出
- `DiaryRepository.android.kt`: 既存のRepositoryManagerをラップ

#### iOS実装 ✅
- `Theme.ios.kt`: UIKitを使用したダークモード検出
- `DiaryRepository.ios.kt`: NSFileManagerを使用したファイル操作

### Step 4: Android アプリの移行 🚧

1. **依存関係の更新**
   ```kotlin
   // app/build.gradle.kts
   implementation(project(":shared"))
   ```

2. **既存コードの移行**
   - 共通コードへの移行可能な部分を特定
   - プラットフォーム固有機能（Widget, Notification）を分離

### Step 5: iOS アプリの作成 🚧

1. **Xcodeプロジェクトの作成**
   - iOSAppディレクトリの作成
   - SwiftUIとの統合

2. **フレームワークの統合**
   ```swift
   import shared
   
   struct ContentView: View {
       var body: some View {
           ComposeView()
       }
   }
   ```

### Step 6: ビルドとテスト 🚧

1. **Androidビルド**
   ```bash
   ./gradlew :app:assembleDebug
   ```

2. **iOSビルド**
   ```bash
   ./gradlew :shared:linkDebugFrameworkIosX64
   ```

3. **Xcodeでのビルド**
   - Xcodeプロジェクトを開く
   - Simulatorで実行

## 🔑 重要なポイント

### expect/actual パターン

KMPでは、プラットフォーム固有の実装を`expect/actual`で宣言します。

```kotlin
// commonMain
expect class DiaryRepositoryFactory {
    fun createRepository(): DiaryRepository
}

// androidMain
actual class DiaryRepositoryFactory(private val context: Context) {
    actual fun createRepository(): DiaryRepository = AndroidDiaryRepository(context)
}

// iosMain
actual class DiaryRepositoryFactory {
    actual fun createRepository(): DiaryRepository = IosDiaryRepository()
}
```

### kotlinx ライブラリの使用

プラットフォーム固有のAPIを避け、kotlinxライブラリを使用:
- `kotlinx.datetime` (日時処理)
- `kotlinx.coroutines` (非同期処理)
- `kotlinx.serialization` (シリアライゼーション)

### Compose Multiplatform

UI層はCompose Multiplatformで共通化:
- Material 3コンポーネント
- プラットフォーム固有のダイアログやピッカーは`expect/actual`で実装

## 📦 主要な依存関係

```kotlin
// shared/build.gradle.kts
kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime")
        }
    }
}
```

## 🚧 現在の状態

- [x] プロジェクト構造の作成
- [x] 共通データモデルの作成
- [x] 共通UIテーマの作成
- [x] プラットフォーム固有実装のスタブ作成
- [ ] 既存Androidコードの移行
- [ ] iOSアプリの完全な実装
- [ ] ビルドと動作確認

## 📝 次のステップ

1. 既存のAndroidコードをsharedモジュールに移行
2. Androidアプリをsharedモジュールを使用するように更新
3. iOSアプリの完全な実装
4. 両プラットフォームでのテスト
5. CI/CDパイプラインの更新

## 🔗 参考リンク

- [Kotlin Multiplatform 公式ドキュメント](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [kotlinx.datetime](https://github.com/Kotlin/kotlinx-datetime)

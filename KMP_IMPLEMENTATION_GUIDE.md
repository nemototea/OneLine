# Kotlin Multiplatform & Compose Multiplatform 移行 - 実装ガイド

## 🎯 このPRの目的

Android専用アプリだったOneLineを、Kotlin Multiplatform (KMP) と Compose Multiplatform (CMP) を使用してiOSでも動作するように移行する基盤を構築します。

## ✅ 完了した作業

### 1. プロジェクト構造の構築

#### sharedモジュールの作成
- `shared/` ディレクトリの作成
- KMP対応の `build.gradle.kts` 設定
- Android、iOS両プラットフォームのターゲット設定

#### ディレクトリ構成
```
shared/
├── src/
│   ├── commonMain/kotlin/     # プラットフォーム共通コード
│   ├── androidMain/kotlin/    # Android固有実装
│   └── iosMain/kotlin/        # iOS固有実装
```

### 2. 共通データレイヤーの作成

#### DiaryEntry.kt (commonMain)
- `java.time.LocalDate` → `kotlinx.datetime.LocalDate` に変更
- `System.currentTimeMillis()` → `Clock.System.now().toEpochMilliseconds()` に変更
- プラットフォーム非依存の実装に

#### DiaryRepository インターフェース (commonMain)
- リポジトリの共通インターフェース定義
- `expect/actual` パターンでプラットフォーム固有実装を分離
- Flow ベースのリアクティブAPI

### 3. 共通UIレイヤーの作成

#### テーマシステム (commonMain)
- `Color.kt`: iOS風のカラーパレット（Light/Dark両対応）
- `Type.kt`: マルチプラットフォーム対応のタイポグラフィ
- `Theme.kt`: Material 3ベースのテーマ実装

### 4. プラットフォーム固有実装

#### Android実装 (androidMain)
- `Theme.android.kt`: `isSystemInDarkTheme()` のAndroid実装
- `DiaryRepository.android.kt`: 既存RepositoryManagerのラッパー（スタブ）

#### iOS実装 (iosMain)
- `Theme.ios.kt`: UIKitベースのダークモード検出
- `DiaryRepository.ios.kt`: NSFileManagerを使用したファイル操作（スタブ）

### 5. iOS アプリの骨格作成

- `iosApp/` ディレクトリの作成
- SwiftUIベースの基本構造
  - `iOSApp.swift`: アプリのエントリーポイント
  - `ContentView.swift`: メインビュー

### 6. ドキュメント作成

- `KMP_MIGRATION_GUIDE.md`: 詳細な移行ガイド
- 各ステップの説明と実装のポイント

## 🔧 Gradle設定の変更

### build.gradle.kts (ルート)
```kotlin
buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    }
}
```

### libs.versions.toml
新規追加:
- `kotlin-multiplatform` プラグイン
- `compose-multiplatform` プラグイン
- `kotlinx-coroutines-core`
- `kotlinx-datetime`

### settings.gradle.kts
```kotlin
include(":app")
include(":shared")  // 追加
```

## 🚧 次のステップ（未実装）

### 短期的なタスク

1. **ビルド環境の修正**
   - Google Mavenリポジトリへのアクセス問題の解決
   - AGP・Kotlinバージョンの最終調整

2. **既存Androidコードの移行**
   - `app/` モジュールから `shared/` への段階的移行
   - データレイヤーの完全な実装
   - ViewModelの共通化

3. **Androidアプリの更新**
   - `shared` モジュールへの依存追加
   - プラットフォーム固有機能の分離
     - Widget (Android専用として残す)
     - Notification (Android専用として残す)
     - Git操作 (JGitはJVM専用なので要検討)

### 中期的なタスク

4. **iOSリポジトリ実装**
   - ファイルベースのローカルストレージ実装
   - iCloud同期の検討

5. **共通UI実装**
   - Compose Multiplatformでの画面実装
   - DiaryListScreen
   - DiaryEditScreen
   - SettingsScreen

6. **iOSアプリの完成**
   - Xcodeプロジェクトの作成
   - Frameworkの統合
   - App Store対応

### 長期的なタスク

7. **Git同期のマルチプラットフォーム対応**
   - JGitの代替手段検討
   - プラットフォームネイティブのGit実装
   - または共通のHTTP APIベース同期

8. **テストの実装**
   - 共通ビジネスロジックのユニットテスト
   - UIテスト
   - 統合テスト

9. **CI/CDパイプライン**
   - Android・iOS両方のビルドパイプライン
   - 自動テスト実行
   - リリースプロセスの自動化

## ⚠️ 注意点と制約

### JGit (Git同期機能)
- JGitはJVM専用ライブラリ
- iOS では使用不可
- 代替案:
  1. libgit2 + Kotlin/Native interop
  2. プラットフォームネイティブのGit実装
  3. HTTP APIベースの同期

### Android専用機能
以下の機能はAndroid専用として残す:
- App Widget (Glance)
- Notification (AlarmManager)
- Android固有のUI (Material Youダイナミックカラー等)

### kotlinx-datetime
- `java.time` の代替
- すべてのプラットフォームで動作
- APIが若干異なるため既存コードの修正が必要

## 📚 参考にした設計パターン

### expect/actual パターン
プラットフォーム固有の実装を分離:
```kotlin
// commonMain
expect fun getPlatformName(): String

// androidMain
actual fun getPlatformName(): String = "Android"

// iosMain
actual fun getPlatformName(): String = "iOS"
```

### Repository パターン
データアクセスを抽象化:
- Interface: commonMain
- Implementation: androidMain/iosMain

### MVVM
- ViewModel: commonMain（将来的に）
- View: Compose Multiplatform

## 🎓 学習リソース

- [Kotlin Multiplatform 公式ドキュメント](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [KMP Best Practices](https://kotlinlang.org/docs/multiplatform-mobile-icerock.html)

## 💡 実装のヒント

### ビルドエラーへの対処
現在、Google Mavenリポジトリへのアクセス制限によりビルドが失敗しています。
実際の開発環境では以下を確認:
- ネットワーク接続
- プロキシ設定
- Gradleキャッシュ

### iOS開発環境
- Xcodeのインストール (macOS必須)
- CocoaPodsまたはSPMの設定
- iOS Simulatorでのテスト

### デバッグ
- Android: Android Studio
- iOS: Xcode
- 共通コード: IntelliJ IDEA / Fleet

---

**作成日**: 2025-10-24  
**対応Issue**: #[issue番号]  
**移行フェーズ**: Phase 1 - 基盤構築 ✅

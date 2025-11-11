# OneLine

手軽に日記を書くことを目的とした、Kotlin Multiplatform (KMP) / Compose Multiplatform (CMP) で構築された日記アプリです。Android と iOS の両プラットフォームに対応しています。

## 主な特徴

- 📝 **シンプルな日記**: 何でもない日常を簡単に書き留められる
- 🔒 **プライベート**: データは端末内または自分の Git リポジトリに保存
- 📊 **統計機能**: ストリーク、月間投稿数などの統計を可視化
- 🔔 **通知機能**: 毎日のリマインダー通知
- ☁️ **Git 同期**: GitHub 等の Git リポジトリと同期（Android のみ）
- 📱 **マルチプラットフォーム**: Android と iOS で同じコードベース

## 技術スタック

- **Kotlin Multiplatform** 2.1.0
- **Compose Multiplatform** (UI)
- **Koin** 4.0.1 (依存性注入)
- **kotlinx-datetime** (日付処理)
- **kotlinx-serialization** (シリアライゼーション)
- **JGit** (Git 操作 - Android のみ)

## プロジェクト構造

```
OneLine/
├── shared/                 # KMP 共通モジュール
│   ├── commonMain/         # 共通コード（Android/iOS）
│   ├── androidMain/        # Android 固有実装
│   └── iosMain/            # iOS 固有実装
├── androidApp/             # Android アプリ
└── iosApp/                 # iOS アプリ
```

## ビルド方法

### Android

```bash
# デバッグビルド
./gradlew :androidApp:assembleDebug

# リリースビルド
./gradlew :androidApp:assembleRelease
```

### iOS

```bash
# iOS フレームワークのビルド（シミュレータ）
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# iOS フレームワークのビルド（実機）
./gradlew :shared:linkReleaseFrameworkIosArm64

# ビルドスクリプトを使用（推奨）
cd iosApp
./build.sh
```

Xcode でプロジェクトを開いてビルドすることも可能です。詳細は [iosApp/README.md](iosApp/README.md) を参照してください。

## テスト

```bash
# 全テスト実行
./gradlew :shared:allTests

# 共通テストのみ
./gradlew :shared:commonTest

# Android テスト
./gradlew :shared:testDebugUnitTest

# iOS テスト
./gradlew :shared:iosSimulatorArm64Test
```

## 開発環境

### 必須

- **Kotlin** 2.1.0+
- **Java JDK** 17+
- **Android Studio** Ladybug | 2024.2.1+
- **Xcode** 15.0+ (iOS 開発の場合)

### 推奨

- **Gradle** 8.5+
- **Git** 2.0+

## ドキュメント

- [KMP 移行ガイド](docs/KMP_MIGRATION_GUIDE.md) - KMP/CMP 移行の全体プロセス
- [ナレッジベース](docs/KMP_KNOWLEDGE_BASE.md) - 技術的な課題と解決策
- [統合テスト報告書](docs/INTEGRATION_TEST_REPORT.md) - テスト結果
- [CLAUDE.md](CLAUDE.md) - アーキテクチャ概要（Claude Code 向け）

## 主な機能

### 日記の作成と編集
- Markdown 形式で日記を記録
- 日付ごとに自動的にファイルを作成
- リアルタイムプレビュー

### 統計とグラフ
- 現在のストリーク（連続投稿日数）
- 最長ストリーク
- 月間投稿数
- GitHub スタイルのコントリビューショングラフ

### Git 同期（Android のみ）
- GitHub 等の Git リポジトリと同期
- 自動コミット・プッシュ
- コンフリクト解決

### 通知
- 毎日のリマインダー通知
- 通知時刻のカスタマイズ
- Android: AlarmManager
- iOS: UNUserNotificationCenter

## トラブルシューティング

### iOS フレームワークが見つからない

```bash
# フレームワークを再ビルド
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### ビルドエラー: "No such module 'Shared'"

1. フレームワークがビルドされていることを確認
2. Xcode の `Framework Search Paths` を確認
3. Xcode でプロジェクトをクリーン: `Product` > `Clean Build Folder`

### Git 同期エラー

- リポジトリ URL、ユーザー名、トークンを確認
- ネットワーク接続を確認
- リポジトリの権限を確認

詳細は [ナレッジベース](docs/KMP_KNOWLEDGE_BASE.md) を参照してください。

## ライセンス

このプロジェクトは個人利用を目的としています。

## 貢献

このプロジェクトは現在、個人開発プロジェクトです。

## 作者

- **nemototea** - [GitHub](https://github.com/nemototea)

---

**Last Updated:** 2025-11-11
**Version:** KMP/CMP 対応版

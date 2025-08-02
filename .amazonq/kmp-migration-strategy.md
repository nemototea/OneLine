# KMP/CMP 移行戦略

## 移行の背景
OneLineアプリは当初Androidネイティブアプリとして開発されましたが、将来的にiOSにも対応するため、Kotlin Multiplatform (KMP) と Compose Multiplatform (CMP) への移行を計画しています。

## 移行フェーズ

### Phase 1: Android基盤完成 (現在)
- Androidネイティブアプリとして全機能を実装
- 安定したアーキテクチャの確立
- ユーザーフィードバックの収集

### Phase 2: KMP/CMP移行準備
- プロジェクト構造の再編成
- 共通モジュールの分離
- プラットフォーム固有コードの特定

### Phase 3: iOS対応実装
- iOSターゲットの追加
- iOS固有UIの実装
- クロスプラットフォームテスト

## 共通化対象コンポーネント

### 完全共通化
- **データモデル**: `DiaryEntry`など
- **ビジネスロジック**: ViewModelのロジック部分
- **Git操作**: JGitを使用したリポジトリ管理
- **ユーティリティ**: 日付処理など

### UI共通化 (CMP)
- **基本画面**: DiaryListScreen, DiaryEditScreen, SettingsScreen
- **コンポーネント**: DiaryForm, DiaryCard
- **テーマ**: Color, Typography (Material3)

### プラットフォーム固有
- **Androidウィジェット**: Glanceを使用 (Android専用)
- **iOSウィジェット**: WidgetKit使用 (将来検討)
- **プラットフォーム固有設定**: 通知、ファイルアクセスなど

## 技術的考慮事項

### 依存関係の整理
- **JGit**: KMP対応確認済み
- **Hilt**: Android専用 → KMP対応DIライブラリへ移行検討
- **DataStore**: KMP対応版への移行
- **Navigation**: Compose Multiplatform Navigation使用

### アーキテクチャ調整
```
共通モジュール (commonMain)
├── data/
│   ├── model/
│   ├── repository/
│   └── git/
├── domain/
│   └── usecase/
└── presentation/
    └── viewmodel/

プラットフォーム固有 (androidMain/iosMain)
├── ui/
│   └── platform-specific/
├── widget/
└── platform/
```

### 現在の実装で注意すべき点
- **Context依存**: Android Contextに依存するコードの抽象化
- **ファイルシステム**: プラットフォーム固有のファイルアクセス
- **暗号化**: Bouncy Castleの代替検討

## 移行時の互換性維持
- 既存のAndroidユーザーデータの完全互換性
- Gitリポジトリ形式の維持
- 設定データの移行サポート

## 開発時の配慮事項
現在のAndroid開発においても、将来のKMP/CMP移行を考慮して：
- プラットフォーム固有APIの使用を最小限に
- ビジネスロジックとUI層の明確な分離
- 依存性注入の抽象化
- テスタブルなコード設計

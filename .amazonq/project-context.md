# OneLine プロジェクトコンテキスト

## プロダクト概要
**OneLine**は、毎日一行の日記を記録するシンプルなモバイルアプリです。日記データはGitリポジトリで管理され、Markdownファイルとして保存されます。

### 開発ロードマップ
1. **Phase 1 (現在)**: Androidネイティブアプリとして開発
2. **Phase 2 (将来)**: KMP (Kotlin Multiplatform) + CMP (Compose Multiplatform) でiOS対応
3. **最終目標**: 完全にKotlinでAndroid・iOS両対応のクロスプラットフォームアプリ

## 主要機能
- **一行日記の作成・編集・削除**：日付ごとに一行の日記を記録
- **Gitベースのデータ管理**：日記データをGitリポジトリで同期・バックアップ
- **Androidウィジェット**：ホーム画面から直接日記を投稿可能
- **ダークモード対応**：システム設定に応じた自動切り替え

## 技術スタック

### 現在 (Android Phase)
- **言語**: Kotlin
- **UIフレームワーク**: Jetpack Compose + Material3
- **アーキテクチャ**: MVVM + Repository Pattern
- **DI**: Hilt
- **ナビゲーション**: Navigation Compose
- **データ永続化**: DataStore (設定), Git (日記データ)
- **Git操作**: JGit
- **暗号化**: Bouncy Castle
- **ウィジェット**: Glance (Android専用)

### 将来 (KMP/CMP Phase)
- **マルチプラットフォーム**: Kotlin Multiplatform (KMP)
- **UI**: Compose Multiplatform (CMP)
- **対象プラットフォーム**: Android + iOS
- **共通化対象**: 
  - ビジネスロジック (ViewModelなど)
  - データレイヤー (Repository, Git操作)
  - UIコンポーネント (Compose)
- **プラットフォーム固有**: 
  - Androidウィジェット (現状維持)
  - iOSウィジェット (将来的に検討)

## プロジェクト構造
```
net.chasmine.oneline/
├── data/
│   ├── git/GitRepository.kt          # Git操作の中核
│   ├── model/DiaryEntry.kt           # 日記エントリのデータクラス
│   └── preferences/SettingsManager.kt # アプリ設定管理
├── ui/
│   ├── screens/                      # 各画面のComposable
│   ├── components/                   # 再利用可能なUIコンポーネント
│   ├── viewmodels/                   # ViewModelクラス
│   └── theme/                        # テーマ・カラー定義
├── widget/                           # Androidウィジェット関連
└── util/                            # ユーティリティクラス
```

## データモデル
```kotlin
data class DiaryEntry(
    val date: LocalDate,      // 日記の日付
    val content: String,      // 一行の内容
    val lastModified: Long    // 最終更新時刻
)
```

## カラーテーマ
- **プライマリカラー**: `#FFD700` (ゴールド) - ライト・ダークモード共通
- **背景色**: 
  - ライト: `#F5F5F5`
  - ダーク: `#1A1A2E`
- **サーフェス色**:
  - ライト: `#FFFFFF`
  - ダーク: `#2C2C44`
- **テキスト色**:
  - ライト: `#1A1A2E`
  - ダーク: `#E0E0E0`

## 業務知識
- **日記ファイル形式**: `YYYY-MM-DD.md` (例: `2025-08-02.md`)
- **Git運用**: 各日記の保存時に自動コミット・プッシュ
- **ウィジェット**: ホーム画面から素早い日記投稿が可能
- **データ同期**: リモートGitリポジトリとの同期でデータバックアップ

## 開発方針
- **Material3デザインシステム**準拠
- **アクセシビリティ**重視
- **シンプルさ**を保持（一行日記のコンセプト）
- **データの永続性**確保（Git管理）
- **将来のKMP/CMP移行を考慮**した設計
  - プラットフォーム固有コードの最小化
  - ビジネスロジックの共通化を意識
  - Compose Multiplatformで利用可能なAPIの優先使用

## 重要なファイル
- `app/src/main/java/net/chasmine/oneline/ui/theme/Color.kt` - カラー定義
- `app/src/main/java/net/chasmine/oneline/ui/theme/Theme.kt` - テーマ設定
- `app/src/main/java/net/chasmine/oneline/ui/components/DiaryForm.kt` - 日記入力フォーム
- `app/src/main/java/net/chasmine/oneline/ui/screens/DiaryListScreen.kt` - 日記一覧画面

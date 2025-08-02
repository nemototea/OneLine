# OneLine理想的なUX改善提案

## 現在の問題点
1. **リポジトリ用途の曖昧性**: 何のリポジトリを設定すべきか不明確
2. **設定変更の危険性**: 間違った設定による重大な影響
3. **検証機能の不足**: 設定したリポジトリが適切かどうかの確認不足
4. **ユーザーガイダンス不足**: 正しい設定方法の説明不足

## 理想的なUX設計

### 1. 初回セットアップウィザード

#### A. リポジトリ作成オプション
```
┌─────────────────────────────────────┐
│ 日記リポジトリのセットアップ          │
├─────────────────────────────────────┤
│ ○ 新しいリポジトリを作成する          │
│   → "oneline-diary-YYYY" を自動作成   │
│                                     │
│ ○ 既存のリポジトリを使用する          │
│   → 手動でURLを入力                  │
│                                     │
│ ○ 後で設定する                      │
│   → ローカルのみで使用               │
└─────────────────────────────────────┘
```

#### B. リポジトリ検証ステップ
```
┌─────────────────────────────────────┐
│ リポジトリ検証中...                  │
├─────────────────────────────────────┤
│ ✓ 接続テスト完了                    │
│ ✓ 権限確認完了                      │
│ ⚠ 既存ファイル検出: 3個のmdファイル   │
│                                     │
│ このリポジトリは日記用として         │
│ 適切に見えます。続行しますか？       │
│                                     │
│ [詳細を確認] [続行] [キャンセル]      │
└─────────────────────────────────────┘
```

### 2. 設定変更時の安全機能

#### A. 変更前確認ダイアログ
```
┌─────────────────────────────────────┐
│ ⚠ リポジトリ変更の確認               │
├─────────────────────────────────────┤
│ 現在: github.com/user/obsidian-vault │
│ 新規: github.com/user/new-diary      │
│                                     │
│ 変更により以下が発生します:          │
│ • ローカルデータの削除               │
│ • 新しいリポジトリからの同期         │
│                                     │
│ 現在のデータをバックアップしますか？  │
│                                     │
│ [バックアップして変更] [キャンセル]   │
└─────────────────────────────────────┘
```

#### B. リポジトリ適合性チェック
```kotlin
data class RepositoryValidation(
    val isAccessible: Boolean,
    val hasWritePermission: Boolean,
    val containsDiaryFiles: Boolean,
    val hasConflictingContent: Boolean,
    val recommendedAction: RecommendedAction
)

enum class RecommendedAction {
    SAFE_TO_USE,           // 安全に使用可能
    CREATE_NEW_BRANCH,     // 新しいブランチを作成推奨
    BACKUP_REQUIRED,       // バックアップ必須
    NOT_RECOMMENDED        // 使用非推奨
}
```

### 3. 直感的な設定UI

#### A. ビジュアルガイド付き設定画面
```
┌─────────────────────────────────────┐
│ 📚 日記リポジトリ設定                │
├─────────────────────────────────────┤
│                                     │
│ 🔗 リポジトリURL                    │
│ ┌─────────────────────────────────┐ │
│ │ https://github.com/user/diary   │ │
│ └─────────────────────────────────┘ │
│                                     │
│ 💡 ヒント:                          │
│ • 日記専用のリポジトリを推奨         │
│ • プライベートリポジトリが安全       │
│                                     │
│ [リポジトリを検証] [テスト投稿]      │
└─────────────────────────────────────┘
```

#### B. リアルタイム検証
- URL入力時にリアルタイムで接続テスト
- リポジトリの内容をプレビュー表示
- 推奨設定の自動提案

### 4. エラー防止機能

#### A. 危険操作の防止
```kotlin
class SafeRepositoryManager {
    fun validateRepositoryChange(
        currentUrl: String,
        newUrl: String
    ): ValidationResult {
        // 1. URLの類似性チェック
        if (isSimilarRepository(currentUrl, newUrl)) {
            return ValidationResult.Warning("類似したリポジトリです")
        }
        
        // 2. 既知の危険パターンチェック
        if (isDevelopmentRepository(newUrl)) {
            return ValidationResult.Error("開発用リポジトリは使用できません")
        }
        
        // 3. 内容の互換性チェック
        return checkContentCompatibility(newUrl)
    }
}
```

#### B. 自動バックアップ機能
```kotlin
class AutoBackupManager {
    suspend fun createBackupBeforeChange(
        currentRepo: String
    ): BackupResult {
        val timestamp = System.currentTimeMillis()
        val backupBranch = "backup-$timestamp"
        
        return try {
            git.branchCreate()
                .setName(backupBranch)
                .call()
            
            BackupResult.Success(backupBranch)
        } catch (e: Exception) {
            BackupResult.Failed(e)
        }
    }
}
```

### 5. ユーザーガイダンス強化

#### A. コンテキストヘルプ
```
設定画面の各項目に「？」アイコン
→ タップで詳細説明を表示

例:
「リポジトリURL」の「？」をタップ
┌─────────────────────────────────────┐
│ 📖 リポジトリURLについて             │
├─────────────────────────────────────┤
│ 日記データを保存するGitHubリポジトリ  │
│ のURLを入力してください。            │
│                                     │
│ ✅ 良い例:                          │
│ github.com/user/my-diary            │
│                                     │
│ ❌ 避けるべき例:                    │
│ github.com/user/app-source-code     │
│                                     │
│ [新しいリポジトリを作成する方法]     │
└─────────────────────────────────────┘
```

#### B. セットアップガイド
- 初回起動時のチュートリアル
- GitHubリポジトリ作成手順の案内
- アクセストークン取得方法の説明

### 6. 高度な安全機能

#### A. リポジトリフィンガープリント
```kotlin
data class RepositoryFingerprint(
    val repoName: String,
    val primaryLanguage: String,
    val fileTypes: Set<String>,
    val typicalFileNames: Set<String>
) {
    fun isDiaryRepository(): Boolean {
        return fileTypes.contains("md") && 
               typicalFileNames.any { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.md")) }
    }
}
```

#### B. 操作履歴とロールバック
```kotlin
class OperationHistory {
    fun recordRepositoryChange(
        from: String,
        to: String,
        timestamp: Long,
        backupRef: String?
    )
    
    fun rollbackToLastKnownGood(): RollbackResult
}
```

## 実装優先度

### Phase 1 (緊急)
- [x] リモートURL変更時の安全チェック
- [x] Force push無効化
- [ ] 設定変更確認ダイアログ

### Phase 2 (重要)
- [ ] リポジトリ検証機能
- [ ] 自動バックアップ機能
- [ ] コンテキストヘルプ

### Phase 3 (改善)
- [ ] 初回セットアップウィザード
- [ ] リポジトリ自動作成機能
- [ ] 操作履歴とロールバック

## KMP/CMP対応考慮事項
- 全ての新機能はCompose Multiplatformで実装
- プラットフォーム固有のGit操作は共通インターフェースで抽象化
- iOS版でも同様のUXを提供

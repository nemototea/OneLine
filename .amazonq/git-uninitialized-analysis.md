# Git未設定時の日記投稿問題分析

## 現在の動作

### シナリオ: Git設定未完了で日記投稿を試行

1. **アプリ初回起動**
   - Git設定が未完了
   - `isInitialized = false`

2. **プラスボタンをタップ**
   - 日記編集画面が開く
   - 入力フォームが表示される（問題なし）

3. **日記を入力して保存ボタンをタップ**
   - `DiaryEditViewModel.saveEntry()` が呼ばれる
   - `gitRepository.saveEntry(entry)` が実行される

4. **GitRepository.saveEntry()での処理**
   ```kotlin
   if (!isInitialized || git == null || repoDirectory == null) {
       Log.e(TAG, "Repository not properly initialized")
       return Result.failure(Exception("Repository not initialized"))
   }
   ```

5. **結果**
   - `Result.failure` が返される
   - エラーメッセージ: "Repository not initialized"

## 問題点

### 1. ユーザーエクスペリエンスの問題
- ユーザーは日記を書いた後でエラーに遭遇
- 入力した内容が失われる可能性
- エラーメッセージが技術的すぎる

### 2. 予防的チェックの不足
- 日記編集画面に入る前にGit設定をチェックしていない
- プラスボタンが常に有効になっている

### 3. 設定への誘導不足
- エラーが発生しても設定画面への誘導がない
- ユーザーが次に何をすべきか分からない

## 期待される改善

### 1. 事前チェック
- プラスボタンタップ時にGit設定をチェック
- 未設定の場合は設定画面に誘導

### 2. 分かりやすいエラーメッセージ
- 技術的なメッセージではなく、ユーザー向けの説明
- 解決方法の提示

### 3. データ保護
- 入力中の日記内容を一時保存
- 設定完了後に復元可能

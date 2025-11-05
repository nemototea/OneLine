# Branch Protection and Workflow Settings

このドキュメントは、GitHub リポジトリでブランチ保護ルールを設定する手順を説明します。

## ブランチ保護ルールの設定

### main ブランチの保護

1. **GitHub リポジトリページへアクセス**
   - リポジトリの Settings タブを開く

2. **Branches 設定**
   - 左メニューから "Branches" を選択
   - "Branch protection rules" セクションで "Add rule" をクリック

3. **ルールの設定**
   
   **Branch name pattern**: `main`
   
   以下のオプションを有効化:
   
   ✅ **Require a pull request before merging**
   - Require approvals: 1 (推奨)
   - Dismiss stale pull request approvals when new commits are pushed
   
   ✅ **Require status checks to pass before merging**
   - Require branches to be up to date before merging
   - Status checks that are required:
     - `build-and-test` (PR Check workflow)
   
   ✅ **Require conversation resolution before merging**
   
   ✅ **Require linear history** (オプション - クリーンな履歴を保持)
   
   ✅ **Do not allow bypassing the above settings**
   
   ✅ **Restrict who can push to matching branches**
   - GitHub Actions (ワークフロー用)
   - 管理者のみ (緊急時用)

4. **Create** をクリックして保存

### release ブランチの保護 (オプション)

同様の手順で `release` ブランチも保護可能:

**Branch name pattern**: `release`
- 同じルールを適用
- または `release/*` でリリースブランチ全体を保護

## 必要なシークレットの設定

Google Play へのデプロイには以下のシークレットが必要です:

### リポジトリシークレットの追加

1. Settings > Secrets and variables > Actions
2. "New repository secret" をクリック
3. 以下のシークレットを追加:

#### Keystoreの設定
```bash
# Keystoreをbase64エンコード
base64 -i your-keystore.jks | pbcopy  # macOS
base64 -w 0 your-keystore.jks         # Linux
```

**シークレット名**: `KEYSTORE_BASE64`
**値**: エンコードされたkeystore文字列

#### Keystore関連
- **KEYSTORE_PASSWORD**: keystoreのパスワード
- **KEY_ALIAS**: キーのエイリアス
- **KEY_PASSWORD**: キーのパスワード

#### Google Play Console
- **GOOGLE_PLAY_SERVICE_ACCOUNT_JSON**: サービスアカウントのJSON (後述)

## Google Play Console サービスアカウントの設定

### 1. Google Cloud Console でサービスアカウントを作成

1. [Google Cloud Console](https://console.cloud.google.com/) にアクセス
2. プロジェクトを選択または作成
3. "IAM & Admin" > "Service Accounts" へ移動
4. "Create Service Account" をクリック
5. 名前: `github-actions-deploy` (任意)
6. 役割: なし (Play Console で設定)
7. "Create and Continue" → "Done"

### 2. JSON キーを作成

1. 作成したサービスアカウントをクリック
2. "Keys" タブ > "Add Key" > "Create new key"
3. "JSON" を選択してダウンロード
4. このJSONファイルの内容をGitHubシークレット `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` に設定

### 3. Google Play Console で権限を付与

1. [Google Play Console](https://play.google.com/console) にアクセス
2. "Users and permissions" (ユーザーとアクセス権限)
3. "Invite new users" (新しいユーザーを招待)
4. サービスアカウントのメールアドレスを入力
5. 以下の権限を付与:
   - **App access**: アプリへのアクセス (OneLineアプリを選択)
   - **Permissions**:
     - ✅ リリースの管理
     - ✅ ストアの掲載情報の管理
     - ✅ アプリのコンテンツの編集
6. "Invite user" で招待を送信

## ワークフローの概要

### PR Check Workflow (pr-check.yml)
- **トリガー**: PRが作成・更新されたとき
- **動作**:
  1. コードをチェックアウト
  2. JDK 17 をセットアップ
  3. Gradleビルド実行
  4. ユニットテスト実行
  5. テスト結果をPRにコメント

### Release Workflow (release.yml)
- **トリガー**: main ブランチへのプッシュ
- **動作**:
  1. リリースAPK/AABをビルド
  2. Google Play Console (Internal Track) へアップロード
  3. GitHub Release を作成
  4. APK/AABをアーティファクトとして保存

## ブランチ戦略

```
main (保護済み)
 ↑
 │ PR + レビュー
 │
release
 ↑
 │ PR
 │
feature/xxx, issue/xx_xxx
```

### ワークフロー

1. **開発**: `issue/xx_description` ブランチで作業
2. **統合**: `release` ブランチへPR
3. **本番**: `release` → `main` へPR (自動デプロイ)

## トラブルシューティング

### ビルドが失敗する
- Gradle キャッシュをクリア: Settings > Actions > Caches
- JDK バージョンを確認

### Google Play アップロードが失敗する
- サービスアカウントの権限を確認
- パッケージ名が正しいか確認: `net.chasmine.oneline`

### Keystore エラー
- Base64エンコードが正しいか確認
- シークレット名のタイポがないか確認

## 次のステップ

1. ✅ メタデータファイルの準備 (完了)
2. ✅ GitHub Actions ワークフローの作成 (完了)
3. ⏳ ブランチ保護ルールの設定 (手動)
4. ⏳ シークレットの設定 (手動)
5. ⏳ Google Play Console の初期セットアップ
6. ⏳ スクリーンショットの追加
7. ⏳ 初回リリーステスト

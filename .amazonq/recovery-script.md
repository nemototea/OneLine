# obsidian-vault リポジトリ復旧手順

## 前提条件
- obsidian-vaultリポジトリへのアクセス権限
- 元のコミット: `e78c5610d75f11384fe04381cb291a87f72ba143`

## 復旧コマンド（ローカルで実行）

```bash
# 1. 一時的な作業ディレクトリを作成
mkdir ~/temp_recovery
cd ~/temp_recovery

# 2. obsidian-vaultリポジトリをクローン
git clone git@github.com:nemototea/obsidian-vault.git
cd obsidian-vault

# 3. 現在の状況を確認
git log --oneline -10

# 4. 元のコミットが存在することを確認
git show e78c5610d75f11384fe04381cb291a87f72ba143

# 5. 元のコミットから新しいブランチを作成
git checkout -b recovery-main e78c5610d75f11384fe04381cb291a87f72ba143

# 6. 現在のmainブランチをバックアップ（念のため）
git checkout main
git branch backup-corrupted-main

# 7. mainブランチを元のコミットにリセット
git reset --hard e78c5610d75f11384fe04381cb291a87f72ba143

# 8. force pushでリモートのmainブランチを修正
git push --force-with-lease origin main

# 9. 復旧確認
git log --oneline -5
```

## 注意事項
- `--force-with-lease` を使用して安全にforce push
- バックアップブランチを作成してから実行
- 復旧後は必ず内容を確認

## 復旧後の確認
1. GitHubでmainブランチの内容を確認
2. 日記データが正しく復旧されているか確認
3. Androidアプリで再度同期テスト

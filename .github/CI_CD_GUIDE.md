# CI/CD Implementation Guide

## 概要

このドキュメントは Issue #58 の実装内容をまとめたものです。

## 実装内容

### 1. ストアメタデータ管理 (`metadata/`)

```
metadata/
└── ja-JP/
    ├── title.txt                   # アプリタイトル
    ├── short_description.txt       # 簡単な説明
    ├── full_description.txt        # 詳細説明
    ├── video.txt                   # プロモーション動画URL
    ├── images/
    │   ├── phoneScreenshots/       # スクリーンショット
    │   └── featureGraphic/         # フィーチャーグラフィック
    └── changelogs/                 # バージョン別変更履歴
        └── 1.txt
```

**特徴:**
- Google Play Console の要件に準拠した構造
- バージョン管理可能
- 自動化ワークフローとの統合

### 2. GitHub Actions ワークフロー

#### PR Check Workflow (`.github/workflows/pr-check.yml`)
- **目的**: PRのビルド・テスト検証
- **トリガー**: PR作成・更新時
- **処理内容**:
  - Gradle ビルド
  - ユニットテスト実行
  - テスト結果レポート

#### Release Workflow (`.github/workflows/release.yml`)
- **目的**: 本番リリースの自動化
- **トリガー**: `main` ブランチへのプッシュ
- **処理内容**:
  1. Release APK/AAB のビルド
  2. Google Play Console (Internal Track) へ自動アップロード
  3. GitHub Release の作成
  4. ビルド成果物の保存

### 3. ブランチ戦略

```
main (保護済み)
 ↑
 │ PR + レビュー必須
 │ ステータスチェック必須
 │
release (統合ブランチ)
 ↑
 │ PR
 │
issue/xx_description (開発ブランチ)
```

**ルール:**
- `main` への直接プッシュ禁止
- PR + レビュー必須
- CI/CDチェック通過必須
- `main` へのマージで自動デプロイ

## セットアップ手順

### Step 1: GitHub Secrets の設定

以下のシークレットをリポジトリに追加:

```
KEYSTORE_BASE64              # base64エンコードしたkeystore
KEYSTORE_PASSWORD            # keystoreのパスワード
KEY_ALIAS                    # キーエイリアス
KEY_PASSWORD                 # キーのパスワード
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON  # サービスアカウントJSON
```

詳細: `.github/BRANCH_PROTECTION_SETUP.md` 参照

### Step 2: Google Play Console の設定

1. サービスアカウントの作成
2. Play Console での権限付与
3. アプリの初回登録

詳細: `.github/BRANCH_PROTECTION_SETUP.md` 参照

### Step 3: ブランチ保護ルールの設定

GitHub Settings で以下を設定:
- `main` ブランチの保護
- PR必須化
- ステータスチェック必須化

詳細: `.github/BRANCH_PROTECTION_SETUP.md` 参照

### Step 4: スクリーンショットの準備

```bash
# スクリーンショットを配置
cp screenshot1.png metadata/ja-JP/images/phoneScreenshots/01.png
cp screenshot2.png metadata/ja-JP/images/phoneScreenshots/02.png
# ...最小2枚、最大8枚

# フィーチャーグラフィック (1024x500px)
cp feature_graphic.png metadata/ja-JP/images/featureGraphic/feature_graphic.png
```

## 使用方法

### 開発フロー

1. **新機能の開発**
   ```bash
   git checkout main
   git pull
   git checkout -b issue/XX_feature_name
   # 開発...
   git add .
   git commit -m "feat: 新機能の追加"
   git push origin issue/XX_feature_name
   ```

2. **PRの作成**
   - GitHub で PR を作成
   - 自動的に CI チェックが実行される
   - レビュー後、`release` または `main` へマージ

3. **リリース**
   - `main` へのマージで自動的にリリースワークフローが起動
   - Google Play Console (Internal Track) へ自動アップロード
   - GitHub Release が自動作成される

### 変更履歴の追加

新バージョンリリース時:
```bash
# 次のバージョンコードで changelog を作成
echo "バグ修正とパフォーマンス改善" > metadata/ja-JP/changelogs/2.txt
git add metadata/ja-JP/changelogs/2.txt
git commit -m "docs: v2 changelog"
```

## ワークフローの動作確認

### PR Check の確認
1. テストPRを作成
2. Actions タブで実行状況を確認
3. ビルドとテストが通過することを確認

### Release の確認（初回のみ手動）
1. シークレットが正しく設定されているか確認
2. テストコミットを `main` へマージ
3. Actions タブでリリースワークフローを確認
4. Google Play Console で Internal Track を確認

## トラブルシューティング

### ビルドエラー
- JDK バージョンを確認（JDK 17）
- Gradle キャッシュのクリア
- 依存関係の更新

### Google Play アップロードエラー
- サービスアカウントの権限確認
- パッケージ名の確認: `net.chasmine.oneline`
- バージョンコードの重複確認

### Keystore エラー
- Base64 エンコードの確認
- シークレット名の確認
- パスワード/エイリアスの確認

## ファイル一覧

```
.github/
├── workflows/
│   ├── pr-check.yml           # PRチェックワークフロー
│   └── release.yml            # リリースワークフロー
└── BRANCH_PROTECTION_SETUP.md # セットアップガイド

metadata/
├── README.md                  # メタデータ構造の説明
└── ja-JP/
    ├── title.txt
    ├── short_description.txt
    ├── full_description.txt
    ├── video.txt
    ├── images/
    │   ├── phoneScreenshots/
    │   └── featureGraphic/
    └── changelogs/
        └── 1.txt              # 初回リリース変更履歴
```

## 今後の改善案

- [ ] Lint チェックの追加
- [ ] UI テストの自動実行
- [ ] ステージング環境の追加
- [ ] ベータトラックへの自動昇格
- [ ] リリースノートの自動生成
- [ ] Slack/Discord 通知の統合

## 参考資料

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Google Play Console API](https://developers.google.com/android-publisher)
- [upload-google-play Action](https://github.com/r0adkll/upload-google-play)
- [Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher)

---

**作成日**: 2025-11-05
**Issue**: #58
**ブランチ**: `issue/58_ci-cd-store-automation`

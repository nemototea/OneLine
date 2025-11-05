# ストアメタデータ管理

このディレクトリは Google Play Store のリスティング情報を管理します。

## ディレクトリ構造

```
metadata/
└── ja-JP/                          # 日本語ロケール
    ├── title.txt                   # アプリタイトル (最大30文字)
    ├── short_description.txt       # 簡単な説明 (最大80文字)
    ├── full_description.txt        # 詳細説明 (最大4000文字)
    ├── video.txt                   # プロモーションビデオURL (YouTube)
    ├── images/
    │   ├── phoneScreenshots/       # スマートフォン用スクリーンショット (最小2枚、最大8枚)
    │   │                          # JPEG/PNG, 16:9または9:16, 320px-3840px
    │   ├── tenInchScreenshots/     # タブレット用スクリーンショット (オプション)
    │   └── featureGraphic/         # フィーチャーグラフィック (1024x500px, JPEGまたは24-bit PNG)
    └── changelogs/                 # バージョンごとの変更履歴
        └── [versionCode].txt       # 例: 1.txt, 2.txt
```

## Google Play Store の要件

### スクリーンショット
- **スマートフォン**: 最小2枚、最大8枚必須
- **7インチタブレット**: オプション（最大8枚）
- **10インチタブレット**: オプション（最大8枚）
- フォーマット: JPEG または 24-bit PNG (アルファなし)
- アスペクト比: 16:9 または 9:16
- 最小サイズ: 320px
- 最大サイズ: 3840px

### フィーチャーグラフィック
- サイズ: 1024 x 500 px
- フォーマット: JPEG または 24-bit PNG

### アプリアイコン
- サイズ: 512 x 512 px
- フォーマット: 32-bit PNG (アルファあり)
- 注: アプリアイコンは `androidApp/src/main/res/` で管理

### テキスト制限
- タイトル: 最大30文字
- 簡単な説明: 最大80文字
- 詳細説明: 最大4000文字

## 使用方法

### スクリーンショットの追加
1. スクリーンショットを撮影
2. 適切なディレクトリに配置:
   - スマートフォン: `ja-JP/images/phoneScreenshots/`
   - タブレット: `ja-JP/images/tenInchScreenshots/`
3. ファイル名は連番推奨: `01.png`, `02.png`, ...

### 変更履歴の追加
新しいバージョンをリリースする際:
1. `changelogs/[versionCode].txt` を作成
2. 変更内容を記述（最大500文字）

例: `changelogs/1.txt`
```
初回リリース
- シンプルな日記作成機能
- Git同期対応
- ダークモード対応
```

## CI/CDとの統合

このメタデータは GitHub Actions ワークフローで自動的に読み込まれ、
Google Play Console にアップロードされます。

詳細は `.github/workflows/` を参照してください。

# 修正フロー図

## 修正前の動作
```
アプリ起動
    ↓
OneLineApp Composable レンダリング
    ↓
isFirstLaunch = true (初期値)
    ↓
NavHost レンダリング
    ↓
startDestination = "welcome" (既存ユーザーでも)
    ↓
WelcomeScreen 表示 ← 問題！チュートリアル画面が表示される
    ↓
[並行して] LaunchedEffect 実行
    ↓
hasValidSettings() チェック → true (既存ユーザー)
    ↓
isFirstLaunch = false に更新
    ↓
NavHost が再コンポーズ
    ↓
startDestination = "diary_list" に変更
    ↓
DiaryListScreen に遷移
```

## 修正後の動作
```
アプリ起動
    ↓
OneLineApp Composable レンダリング
    ↓
isFirstLaunch = null (初期値)
    ↓
isFirstLaunch == null をチェック → true
    ↓
空白画面を表示して早期リターン ← NavHost はまだレンダリングされない
    ↓
[並行して] LaunchedEffect 実行
    ↓
hasValidSettings() チェック → true (既存ユーザー)
    ↓
isFirstLaunch = false に更新
    ↓
OneLineApp が再コンポーズ
    ↓
isFirstLaunch == null をチェック → false（早期リターンしない）
    ↓
NavHost レンダリング
    ↓
startDestination = "diary_list" ← 最初から正しい画面！
    ↓
DiaryListScreen 表示
```

## メリット
1. ✅ チュートリアル画面のフリッカーが完全になくなる
2. ✅ 既存ユーザーは直接メイン画面が表示される
3. ✅ 新規ユーザーは依然としてチュートリアル画面が表示される
4. ✅ パフォーマンスへの影響は最小限（設定チェックは元々必要な処理）

## 注意点
- `isFirstLaunch!!` で非nullアサーションを使用していますが、このコードに到達する時点で `isFirstLaunch` は必ず `true` または `false` が設定されています（early return により `null` の場合は処理が中断される）

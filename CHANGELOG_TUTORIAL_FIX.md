# チュートリアル画面フリッカー修正

## 問題
アプリ起動時にチュートリアル画面（WelcomeScreen）が一瞬表示されてから、メイン画面に遷移する問題がありました。

## 原因
`MainActivity.kt`の`OneLineApp`コンポーザブルにおいて：
- `isFirstLaunch`の初期値が`true`に設定されていた
- `LaunchedEffect`で設定を非同期にチェックしていた
- NavHostが先にレンダリングされ、その後で`isFirstLaunch`が更新されていた

結果として、既存ユーザーでもNavHostが最初に"welcome"ルートで初期化され、その後"diary_list"に遷移していました。

## 解決策
1. `isFirstLaunch`の型を`Boolean?`（nullable）に変更
2. 初期値を`null`に設定
3. `isFirstLaunch == null`の場合、空白画面を表示して早期リターン
4. `LaunchedEffect`が完了して`isFirstLaunch`が`true`または`false`に設定された後、NavHostをレンダリング

## 変更内容
### MainActivity.kt
```kotlin
// 変更前
var isFirstLaunch by remember { mutableStateOf(true) }

// 変更後
var isFirstLaunch by remember { mutableStateOf<Boolean?>(null) }

// 追加：設定確認中のローディング処理
if (isFirstLaunch == null) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // 空白画面を表示
    }
    return
}
```

## 影響範囲
- 初回起動時：変更なし（チュートリアル画面が表示される）
- 2回目以降の起動：チュートリアル画面のフリッカーがなくなり、直接メイン画面が表示される
- ウィジェットからの起動：変更なし（正常に動作）

## テスト
- 初回インストール後の起動でチュートリアルが表示されることを確認
- 既存ユーザーの起動でメイン画面が直接表示されることを確認
- ウィジェットからの起動が正常に動作することを確認

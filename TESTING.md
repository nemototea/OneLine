# OneLine 日記アプリ - テストガイド

このドキュメントでは、OneLine日記アプリの自動テストの実行方法と、AIアシスタントによるテスト実行について説明します。

## 🎯 テスト戦略

### シンプルで実用的なアプローチ

このプロジェクトでは、**AIアシスタント（Amazon Q）が簡単に実行できる**シンプルで実用的なテストを採用しています。

- ✅ **複雑なMockingは使用しない** - 純粋な機能テストに焦点
- ✅ **依存性注入は最小限** - テストの理解しやすさを優先
- ✅ **実際の動作をテスト** - ユーザーが体験する機能を直接テスト
- ✅ **AIが実行しやすい** - 設定不要で即座に実行可能

### テストの種類

1. **Unit Tests** - 基本的な機能とロジックのテスト
2. **UI Tests** - Compose UIの動作テスト

## 📋 テストファイル一覧

### Unit Tests (`app/src/test/`)

#### `SimpleTest.kt` - 基本機能テスト
- ✅ DiaryEntryの作成と取得
- ✅ 空の内容・長い内容の処理
- ✅ 複数エントリーの管理
- ✅ 日付でのソート機能
- ✅ 今日の日記の識別
- ✅ 文字列処理（日付フォーマット、トリム）
- ✅ リスト操作（フィルタリング）
- ✅ パフォーマンステスト（大量データ）

### UI Tests (`app/src/androidTest/`)

#### `SimpleUITest.kt` - UI動作テスト
- ✅ WelcomeScreenの表示確認
- ✅ タイトルと説明文の表示
- ✅ ローカル保存オプションの表示・クリック
- ✅ Git連携オプションの表示・クリック
- ✅ スクロール動作
- ✅ 複数クリックの処理
- ✅ 両方のオプションのクリック可能性

## 🚀 テスト実行方法

### AIアシスタントによる実行（推奨）

AIアシスタント（Amazon Q）がテストを実行します：

#### 全テスト実行
\`\`\`bash
./gradlew test
\`\`\`

#### ユニットテストのみ実行
\`\`\`bash
./gradlew testDebugUnitTest
\`\`\`

#### UIテスト実行（端末接続が必要）
\`\`\`bash
./gradlew connectedDebugAndroidTest
\`\`\`

#### 特定のテストクラス実行
\`\`\`bash
./gradlew test --tests "net.chasmine.oneline.SimpleTest"
\`\`\`

#### 特定のテストメソッド実行
\`\`\`bash
./gradlew test --tests "net.chasmine.oneline.SimpleTest.DiaryEntry*"
\`\`\`

### 手動実行

開発者が手動でテストを実行する場合：

#### Android Studioから実行
1. テストファイルを右クリック
2. "Run 'SimpleTest'" を選択

#### コマンドラインから実行
\`\`\`bash
# 全テスト
./gradlew test

# 詳細出力付き
./gradlew test --info

# 失敗時のスタックトレース表示
./gradlew test --stacktrace
\`\`\`

## 📊 テスト実行例

### AIアシスタントによる実行例

\`\`\`bash
$ ./gradlew test

> Task :app:testDebugUnitTest
SimpleTest > DiaryEntry - 基本的な作成と取得が正常に動作すること PASSED
SimpleTest > DiaryEntry - 空の内容でも作成できること PASSED
SimpleTest > DiaryEntry - 長い内容でも作成できること PASSED
SimpleTest > DiaryEntry - 複数のエントリーを作成して比較できること PASSED
SimpleTest > DiaryEntry - 日付でソートできること PASSED
SimpleTest > DiaryEntry - 今日の日記を識別できること PASSED
SimpleTest > 文字列処理 - 日付フォーマットが正しく動作すること PASSED
SimpleTest > 文字列処理 - 内容のトリムが正しく動作すること PASSED
SimpleTest > リスト操作 - フィルタリングが正しく動作すること PASSED
SimpleTest > パフォーマンス - 大量のエントリー作成が妥当な時間で完了すること PASSED

BUILD SUCCESSFUL in 3s
10 tests completed, 10 passed
\`\`\`

### UIテスト実行例

\`\`\`bash
$ ./gradlew connectedDebugAndroidTest

> Task :app:connectedDebugAndroidTest
SimpleUITest > welcomeScreen_displaysTitle PASSED
SimpleUITest > welcomeScreen_displaysDescription PASSED
SimpleUITest > welcomeScreen_displaysLocalModeOption PASSED
SimpleUITest > welcomeScreen_displaysGitModeOption PASSED
SimpleUITest > welcomeScreen_localModeClick_triggersCallback PASSED
SimpleUITest > welcomeScreen_gitModeClick_triggersCallback PASSED
SimpleUITest > welcomeScreen_scrollable PASSED
SimpleUITest > welcomeScreen_multipleClicks_handledCorrectly PASSED
SimpleUITest > welcomeScreen_bothOptions_clickable PASSED

BUILD SUCCESSFUL in 15s
9 tests completed, 9 passed
\`\`\`

## 🔧 テスト設定

### 依存関係

\`\`\`kotlin
// Unit Testing
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito:mockito-core:5.5.0")
testImplementation("androidx.arch.core:core-testing:2.2.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

// UI Testing
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
\`\`\`

### カスタムGradleタスク

\`\`\`kotlin
// 全テスト実行
tasks.register("runAllTests") {
    dependsOn("test")
}

// ユニットテストのみ
tasks.register("runUnitTests") {
    dependsOn("testDebugUnitTest")
}

// UIテスト
tasks.register("runUITests") {
    dependsOn("connectedDebugAndroidTest")
}
\`\`\`

## 📈 テスト内容詳細

### SimpleTest - 基本機能テスト

#### DiaryEntryテスト
- **基本的な作成と取得**: 日付と内容の正しい設定
- **空の内容**: 空文字列でも正常に動作
- **長い内容**: 大量の文字でも正常に処理
- **複数エントリー**: リストでの管理と比較
- **日付ソート**: 新しい順での並び替え
- **今日の日記識別**: 現在日付での検索

#### 文字列処理テスト
- **日付フォーマット**: ISO形式での文字列変換
- **内容のトリム**: 前後の空白削除

#### リスト操作テスト
- **フィルタリング**: 空でないエントリーの抽出

#### パフォーマンステスト
- **大量データ**: 1000件のエントリー作成（1秒以内）

### SimpleUITest - UI動作テスト

#### 表示確認テスト
- **タイトル表示**: "OneLine へようこそ"
- **説明文表示**: アプリの説明
- **オプション表示**: ローカル保存とGit連携

#### インタラクションテスト
- **クリック動作**: 各オプションのクリック検出
- **コールバック**: 正しいコールバック関数の呼び出し
- **複数クリック**: 連続クリックの処理

#### レイアウトテスト
- **スクロール**: 縦スクロールの動作確認
- **要素配置**: 全要素の適切な配置

## 🎯 テストのベストプラクティス

### 1. シンプルさを優先
- 複雑なMockingは避ける
- 実際の動作をテストする
- 理解しやすいテスト名を使用

### 2. AIフレンドリー
- 設定不要で実行可能
- 明確なエラーメッセージ
- 自己完結型のテスト

### 3. 実用性重視
- ユーザーが体験する機能をテスト
- エッジケースも含める
- パフォーマンスも考慮

### 4. 保守性
- テストコードも読みやすく
- 変更に強い設計
- 適切なコメント

## 🚨 トラブルシューティング

### よくある問題

#### 1. テスト実行権限エラー
\`\`\`bash
chmod +x gradlew
\`\`\`

#### 2. UIテストでデバイス未接続
\`\`\`bash
# エミュレータまたは実機を接続してから実行
adb devices
./gradlew connectedDebugAndroidTest
\`\`\`

#### 3. テスト失敗時の詳細確認
\`\`\`bash
./gradlew test --info --stacktrace
\`\`\`

### デバッグ方法

#### 1. 特定のテストのみ実行
\`\`\`bash
./gradlew test --tests "*DiaryEntry*"
\`\`\`

#### 2. テスト結果の確認
- HTMLレポート: \`app/build/reports/tests/testDebugUnitTest/index.html\`
- XMLレポート: \`app/build/test-results/testDebugUnitTest/\`

#### 3. ログ出力の確認
\`\`\`kotlin
println("Debug: \$value") // テスト内でのデバッグ出力
\`\`\`

## 📊 継続的改善

### テスト品質指標
- **実行時間**: 全テスト5秒以内
- **成功率**: 100%維持
- **カバレッジ**: 主要機能100%

### 定期実行タイミング
- コード変更時
- プルリクエスト作成時
- リリース前

### 拡張計画
- 新機能追加時のテスト追加
- エラーケースの拡充
- パフォーマンステストの強化

---

このシンプルなテストスイートにより、OneLine日記アプリの基本機能が正常に動作することを確認し、AIアシスタントが簡単にテストを実行して品質を保証できます。

# OneLine iOS アプリ

OneLine の iOS アプリモジュールです。Compose Multiplatform を使用して、共通の UI コードを iOS でも動作させます。

## ディレクトリ構造

```
iosApp/
├── iosApp/
│   ├── iOSApp.swift          # アプリのエントリーポイント
│   ├── ContentView.swift     # Compose UI を表示するビュー
│   └── Info.plist            # アプリの設定ファイル
└── README.md                 # このファイル
```

## セットアップ手順

### 1. 前提条件

- macOS 環境
- Xcode 15.0 以上
- Kotlin Multiplatform Mobile (KMM) プラグイン

### 2. Shared フレームワークのビルド

まず、shared モジュールを iOS フレームワークとしてビルドします：

```bash
# プロジェクトルートで実行
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

または、実機用（iOSデバイス）の場合：

```bash
./gradlew :shared:linkDebugFrameworkIosArm64
```

ビルド成果物は以下のディレクトリに生成されます：
- シミュレータ用: `shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework`
- 実機用: `shared/build/bin/iosArm64/debugFramework/Shared.framework`

### 3. Xcode プロジェクトの作成

**注意**: 現在、Xcode プロジェクトファイル (`.xcodeproj`) は含まれていません。以下の手順で作成してください。

#### 3-1. Xcode で新規プロジェクトを作成

1. Xcode を開く
2. `File` > `New` > `Project...` を選択
3. `iOS` > `App` を選択して `Next` をクリック
4. プロジェクト設定：
   - Product Name: `iosApp`
   - Team: 自分の開発チーム
   - Organization Identifier: `net.chasmine.oneline`
   - Interface: `SwiftUI`
   - Language: `Swift`
5. 保存場所を `OneLine/iosApp/` に設定

#### 3-2. 既存のファイルをプロジェクトに追加

1. Xcode プロジェクトで、既存の `ContentView.swift` と `iOSApp.swift` を削除
2. `iosApp/iosApp/` ディレクトリにある既存のファイルをプロジェクトに追加：
   - `iOSApp.swift`
   - `ContentView.swift`
   - `Info.plist`

#### 3-3. Shared フレームワークをリンク

1. プロジェクトナビゲータでプロジェクト（青いアイコン）を選択
2. `TARGETS` > `iosApp` を選択
3. `General` タブを開く
4. `Frameworks, Libraries, and Embedded Content` セクションで `+` ボタンをクリック
5. `Add Other...` > `Add Files...` を選択
6. `shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework` を選択
7. `Embed & Sign` を選択

#### 3-4. Framework Search Paths の設定

1. `Build Settings` タブを開く
2. `Framework Search Paths` を検索
3. 以下のパスを追加：
   ```
   $(SRCROOT)/../../shared/build/bin/iosSimulatorArm64/debugFramework
   ```

### 4. ビルドスクリプトの追加（オプション）

Xcode ビルド時に自動で Shared フレームワークをビルドするには：

1. `Build Phases` タブを開く
2. `+` > `New Run Script Phase` をクリック
3. 以下のスクリプトを追加：

```bash
cd "$SRCROOT/../../"
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

4. スクリプトフェーズを `Compile Sources` の前に移動

### 5. アプリの実行

1. Xcode でシミュレータまたは実機を選択
2. `Product` > `Run` (⌘R) でアプリを実行

## アーキテクチャ

### Compose Multiplatform の統合

- `MainViewController.kt` (shared/src/iosMain/): Kotlin 側のエントリーポイント
- `ContentView.swift`: Swift 側で Compose UI を表示するブリッジ
- `iOSApp.swift`: アプリのメインエントリーポイント

### データフロー

```
SwiftUI (iOSApp.swift)
  ↓
ContentView.swift (UIViewControllerRepresentable)
  ↓
MainViewController() (Kotlin/Native)
  ↓
App() (Compose Multiplatform)
  ↓
共通UI (shared/src/commonMain/kotlin/ui/)
```

## トラブルシューティング

### フレームワークが見つからないエラー

```
dyld: Library not loaded: @rpath/Shared.framework/Shared
```

**解決方法**:
1. `shared/build/bin/` ディレクトリを確認
2. Shared フレームワークを再ビルド: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
3. Xcode の `Framework Search Paths` 設定を確認

### ビルドエラー: "No such module 'Shared'"

**解決方法**:
1. Shared フレームワークがビルドされていることを確認
2. `Framework Search Paths` が正しく設定されていることを確認
3. Xcode でプロジェクトをクリーン: `Product` > `Clean Build Folder` (⌘⇧K)

## 今後の開発

Phase 7-2 以降では、以下の機能を追加予定です：

- iOS 固有のエントリーポイント実装
- iOS 固有のビルド設定
- iOS での統合テスト

## 参考資料

- [Kotlin Multiplatform for iOS](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html)
- [Compose Multiplatform iOS](https://www.jetbrains.com/lp/compose-multiplatform/)
- [JetBrains Compose Multiplatform Template](https://github.com/JetBrains/compose-multiplatform-ios-android-template)

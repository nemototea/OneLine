# Issue #14: ダークモード視認性改善

## 問題の詳細
ダークモードにおいて、黄色い(ゴールド #FFD700)ボタン内の白文字が視認性不良となっている。

## 対象コンポーネント
1. **FloatingActionButton** (`DiaryListScreen.kt`)
   - 新規日記作成ボタン
   - `containerColor = MaterialTheme.colorScheme.primary`
   - `tint = MaterialTheme.colorScheme.onPrimary`

2. **Button** (`DiaryForm.kt`)
   - 保存ボタン
   - デフォルトのprimaryカラーを使用

## 現在の色設定
```kotlin
// Color.kt
val DarkPrimary = Color(0xFFFFD700) // ゴールド
val DarkOnSurface = Color(0xFFE0E0E0) // 白に近いグレー

// Theme.kt (DarkColorScheme)
primary = DarkPrimary,
onPrimary = DarkOnSurface, // これが問題の原因
```

## 解決方針
ダークモードでのみ、ゴールドボタン内のテキスト色を黒系に変更する。
- `onPrimary`をダークモード時に黒系の色に変更
- または、ボタン固有の色設定を追加

## 関連ファイル
- `app/src/main/java/net/chasmine/oneline/ui/theme/Color.kt`
- `app/src/main/java/net/chasmine/oneline/ui/theme/Theme.kt`
- `app/src/main/java/net/chasmine/oneline/ui/screens/DiaryListScreen.kt`
- `app/src/main/java/net/chasmine/oneline/ui/components/DiaryForm.kt`

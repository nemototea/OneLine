---
name: "OneLine — 墨と和紙 (Sumi & Washi)"
version: "1.1"
platform: "Compose Multiplatform (Android / iOS)"
colors:
  light:
    background: "#F7F2E9"        # 和紙 — 画面の地。純白は使わない
    surface: "#FFFCF5"           # 紙 — カード・ダイアログ
    surfaceVariant: "#EDE5D6"    # 濃い和紙 — セクション背景
    primary: "#BC4B26"           # 朱 — 印章の朱。唯一のアクセント
    onPrimary: "#FFFFFF"
    primaryContainer: "#F8DED1"  # 朱の淡い滲み
    secondary: "#8A7A64"         # 焙じ茶 — 補助アクション
    tertiary: "#5E7F62"          # 抹茶 — 成功表示
    text: "#221E17"              # 墨 — 本文。純黒は使わない
    textMuted: "#5F5749"         # 薄墨 — 補助テキスト
    outline: "#DBD1BF"           # 折り目 — 繊細な境界線
    error: "#B3261E"
  dark:
    background: "#16120D"        # 夜の墨 — 温かみのある黒。純黒は使わない
    surface: "#1F1A14"           # 行灯の影
    surfaceVariant: "#2B241C"
    primary: "#E58E64"           # 灯りの朱
    onPrimary: "#2F1305"
    primaryContainer: "#522815"
    secondary: "#BCAB92"         # 月光の生成り
    tertiary: "#93B295"
    text: "#ECE5D8"              # 紙色の文字
    textMuted: "#AAA08E"
    outline: "#473F32"
    error: "#FFB4AB"
  gradient:
    write: ["#BC4B26", "#D97636", "#E9A23B"]  # 朱→柿→琥珀。「書く」アクション専用
typography:
  fontFamily: "Noto Sans JP"
  display:  { size: 26, weight: 600, lineHeight: 34 }   # 画面タイトル・ブランド
  headline: { size: 20, weight: 500, lineHeight: 26 }
  title:    { size: 16, weight: 500, lineHeight: 22 }
  body:     { size: 16, weight: 400, lineHeight: 26, letterSpacing: 0.3 }  # 日記本文。主役
  caption:  { size: 12, weight: 400, lineHeight: 16 }
  label:    { size: 11, weight: 500, letterSpacing: 0.5 }
  dateNumeral: { size: 24, weight: 300 }                 # 一覧の日付数字。細身で大きく
spacing: [4, 8, 12, 16, 20, 24, 32, 48]
rounded: { xs: 6, sm: 10, md: 14, lg: 20, xl: 28, full: 9999 }
components:
  button-primary:  { backgroundColor: primary, textColor: onPrimary, rounded: md, height: 52 }
  card-entry:      { backgroundColor: surface, borderColor: outline, borderWidth: 1, rounded: 16, padding: 16, elevation: 0 }
  fab-write:       { background: gradient.write, size: 64, rounded: full, icon: "add", iconColor: "#FFFFFF" }
  input-diary:     { backgroundColor: surface, borderColor: outline, rounded: 16, padding: 16, typography: body }
  bottom-bar:      { backgroundColor: surface, rounded: "20 20 0 0", tabs: 2, centerAction: fab-write }
  timeline-thread: { width: 1, color: outline, knotSize: 8, knotColor: primary }
---

# OneLine デザインシステム「墨と和紙」

> このファイルは [DESIGN.md 仕様](https://github.com/google-labs-code/design.md) に準拠した、
> OneLine の唯一のデザイン契約です。UI に関わる実装・提案を行う AI エージェント
> （Claude Code / Claude Design / Cursor 等）と人間は、必ずこのファイルに従ってください。
> 逸脱が必要な場合は、先にこのファイルを更新してから実装します。

## Overview

OneLine は「一行だけの日記」アプリ。デザインの主題は **紙に向かう静かな時間** である。

- 世界観: 和紙のような温かい紙面に墨の文字。アクセントは印章の朱ひとつ。
- 気質: 静か・誠実・軽やか。派手な演出よりも、書いた一行が美しく見えることを優先する。
- ダークテーマは「夜の書斎」。単なる色反転ではなく、行灯の灯りのような温かい暗さにする。

**Non-negotiables（交渉不可の原則）**
1. 純白 (#FFFFFF) と純黒 (#000000) を面に使わない。紙は生成り、墨は温かい黒。
2. アクセントは朱のみ。グラデーション（朱→琥珀）は「書く」アクション（FAB・保存）専用。
3. 影を装飾に使わない。区切りは 1dp の折り目（outline）で表現する。
4. 日記本文（body）が常に画面で最も読みやすいテキストであること。

## Colors

- `background`（和紙）が地、`surface`（紙）が カード。この2つの明度差＋outline の線だけで階層を作る。
- 朱（primary）の使いどころ: 主ボタン、タイムラインの結び目、選択状態、リンク。**面積の5%を超えて使わない。**
- 焙じ茶（secondary）: 破壊的でない補助アクション。抹茶（tertiary）: 成功・達成の表現。
- セマンティックカラー（error 等）はアクセントとは別物として扱い、朱の代わりに流用しない。

**セマンティックの意味づけ（メッセージ・状態表示）**

情報種別ごとに色を固定する。M3 既定の container 色（紫・原色）は使わず、必ずトークンに割り当てる。

| 種別 | 色 | container |
|---|---|---|
| 成功・達成 | tertiary（抹茶） | tertiaryContainer（未定義なら surfaceVariant 地＋抹茶アイコン） |
| エラー | error | errorContainer / onErrorContainer |
| 警告・注意 | error のアイコン tint（新しい橙色を作らない） | 中立地（surface / surfaceVariant） |
| 中立の情報 | onSurfaceVariant（薄墨） | surfaceVariant（濃い和紙） |

**カード地の使い分け（面の階層）**

- **surface ＋ 1dp outline**: 既定。設定項目・メッセージ・情報カードはすべてこれ。
- **surfaceVariant**: セクションの地（複数カードをまとめる背景）や中立情報の帯。
- **primaryContainer（朱の淡い滲み）**: 本物の選択状態だけ。装飾や情報カードの地に使わない（朱 ≤ 5% を守る）。

## Typography

- 書体は Noto Sans JP 一族のみ。ウェイトとサイズの対比で個性を出す（斜体は使わない）。
- 日記本文は `body`（16sp / 行間26sp）。**この行間はアプリで最もゆったりさせる。**
- 日付の数字は `dateNumeral`（細身 Light・24sp）＋極小の英字月ラベル（letterSpacing 1.2sp）。雑誌の日付欄の佇まい。
- 画面タイトルは `display`。装飾的な大見出しは使わず、余白で格を出す。

## Layout

- 基本余白: 画面端 20dp、カード内 16dp、セクション間 24dp。spacing スケール外の値を発明しない。
- タッチターゲットは最小 48dp。主要操作（書く・保存）は親指の届く画面下部に置く。
- 1画面1目的。一覧は「振り返る」、編集は「書く」、カレンダーは「俯瞰する」。目的の違う要素を混ぜない。
- リストは端から端まで線を引かず、要素間の余白と折り目線で呼吸させる。

## Elevation & Depth

- elevation は原則 0。カードは `surface` 色＋1dp outline 線。
- 例外は2つだけ: FAB（浮いている意味がある）とダイアログ/ボトムシート（一時的なレイヤー）。
- 重なりの表現が必要なときは、影ではなく背景色の明度差（background → surface → surfaceVariant）を使う。

## Shapes

- 角丸は rounded スケールから選ぶ。カード 16、ボタン 14、入力欄 16、シート上端 20〜28。
- 完全な直角は使わない（紙の柔らかさを保つ）。円は FAB とタイムラインの結び目だけ。

## Components

- **entry-row（日記一覧の行）**: 左に「一本の糸」= 1dp のタイムライン線と朱の結び目（8dp）。
  日付は糸の上に `dateNumeral`。本文は card-entry に載せ、3行で省略。
- **fab-write**: 画面下中央。朱→琥珀のグラデーション円。アプリで唯一のグラデーション。
- **bottom-bar**: 2タブ（日記・カレンダー）＋中央 FAB。surface 色、上角丸 20。
- **input-diary**: 編集画面の入力欄。ラベルは小さく朱、本文は body。枠は outline 1dp。
- **dialog / sheet**: surface 色、角丸 20以上。ボタンは右寄せ、破壊的操作は error 色のテキストボタン。

## Screens & Navigation

情報設計は日記アプリの定石（Day One / Apple Journal）に従う。**独自の画面構成を発明しない。**

- **ナビゲーション**: ボトムバー2タブ＋中央FAB。階層は最大2段（タブ → 詳細/設定）。
- **日記一覧（ホーム）**: タイムライン。新しい日付が上。pull-to-refresh で同期。
  同期状態は控えめに（ボタンのスピナーのみ、成功は無通知・失敗のみカード表示）。
- **編集画面**: 没入型・無装飾（distraction-free）。画面には日付・入力欄・保存だけ。
  文字数カウントや装飾ツールバーを足さない。「一行」の思想を守る。
- **カレンダー**: 月表示。記録のある日に朱の点。タップでその日のエントリへ。
  下部に継続の実績（連続日数・投稿数）を静かに添える。数字は dateNumeral、絵文字やグラフ装飾は使わない。
- **設定**: リスト型。データ保存（ローカル/Git）・通知・テーマ・情報の4群に整理。
- **オンボーディング**: 機能紹介2枚 → リマインダー設定 → ローカルで開始し初回投稿へ直行。

**将来の拡張（この方向でのみ足す）**: 「この日の思い出 (On This Day)」/ 書くきっかけのプロンプト表示。
どちらも Day One 等で実証済みのパターンに限る。

## Compose 実装マッピング（Material 3）

トークンと Compose Multiplatform（Material 3）の対応。実装は必ずこの表を経由する。
表にないスロット（`titleLarge` 等）は Material コンポーネント内部の補間値であり、画面実装で直接指定しない。

- **単位**: typography の size / lineHeight / letterSpacing は **sp**、spacing / rounded / borderWidth / サイズは **dp**。
- **実装ファイル**: `shared/src/commonMain/kotlin/net/chasmine/oneline/ui/theme/{Color,Type,Theme}.kt`

| DESIGN.md トークン | Compose (Material 3) |
|---|---|
| `background` | `colorScheme.background` |
| `surface` | `colorScheme.surface` |
| `surfaceVariant` | `colorScheme.surfaceVariant` |
| `primary` / `onPrimary` / `primaryContainer` | `colorScheme.primary` / `onPrimary` / `primaryContainer` |
| `secondary` | `colorScheme.secondary` |
| `tertiary` | `colorScheme.tertiary` |
| `text` | `colorScheme.onSurface`（= `onBackground`） |
| `textMuted` | `colorScheme.onSurfaceVariant` |
| `outline` | `colorScheme.outline` |
| `error` | `colorScheme.error` |
| `gradient.write` | `AccentGradientStart/Center/End`（Color.kt の定数。ColorScheme 外） |
| `display` | `typography.displayLarge` |
| `headline` | `typography.headlineMedium` |
| `title` | `typography.titleMedium` |
| `body` | `typography.bodyLarge` |
| `caption` | `typography.bodySmall` |
| `label` | `typography.labelSmall` |
| `dateNumeral` | `DateNumeralStyle`（Type.kt の専用 TextStyle） |
| `rounded` xs/sm/md/lg/xl | `shapes.extraSmall(6)/small(10)/medium(14)/large(20)/extraLarge(28)` |

## Do's and Don'ts

**Do**
- 迷ったら要素を減らす。空白は削るものではなく設計するもの。
- 状態は正直に表示する（できないことを ON に見せない。同期の成功は結果で語る）。
- 新しい画面・コンポーネントは、まずこのファイルの語彙（色・型・部品）で組めないか試す。

**Don't**
- 朱以外のアクセント色や新しいグラデーションを追加しない。
- 影・ぼかし・グロー・パララックスなど「AIが作りがちな」装飾を足さない。
- 絵文字を UI ラベルに使わない（本文中のユーザー入力は自由）。
- 一覧や編集画面に機能を「ついで置き」しない（1画面1目的を破らない）。

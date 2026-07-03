package net.chasmine.oneline.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// OneLine デザインシステム「墨と和紙」
// ==========================================
// コンセプト: 和紙のような温かい紙面に、墨の文字。
// アクセントは印章の朱色ひとつに絞り、引き算で洗練をつくる。
// 日記という「紙に向かう時間」を、色で静かに演出する。

// ライトテーマ - 昼の和紙
val LightBackground = Color(0xFFF7F2E9)        // 和紙 - 温かみのある生成り
val LightSurface = Color(0xFFFFFCF5)           // 明るい紙 - カードやダイアログ
val LightPrimary = Color(0xFFBC4B26)           // 朱色 - 印章の朱。メインアクション
val LightSecondary = Color(0xFF8A7A64)         // 焙じ茶 - 補助的なアクション
val LightTertiary = Color(0xFF5E7F62)          // 抹茶 - 成功・アクセント
val LightOnSurface = Color(0xFF221E17)         // 墨 - 本文テキスト
val LightOnPrimary = Color(0xFFFFFFFF)         // 白 - 朱の上のテキスト
val LightSurfaceVariant = Color(0xFFEDE5D6)    // 濃いめの和紙 - セクション背景
val LightPrimaryContainer = Color(0xFFF8DED1)  // 朱の淡い滲み - 強調カード
val LightSecondaryContainer = Color(0xFFF0E9DB) // 淡い生成り - 補助背景

// ダークテーマ - 夜の書斎
val DarkBackground = Color(0xFF16120D)         // 夜の墨 - 温かみのある黒
val DarkSurface = Color(0xFF1F1A14)            // 行灯の影 - カード面
val DarkPrimary = Color(0xFFE58E64)            // 灯りの朱 - ダークで映える暖色
val DarkSecondary = Color(0xFFBCAB92)          // 月光の生成り
val DarkTertiary = Color(0xFF93B295)           // 夜の抹茶
val DarkOnSurface = Color(0xFFECE5D8)          // 紙色の文字
val DarkOnPrimary = Color(0xFF2F1305)          // 濃墨 - 朱の上のテキスト
val DarkSurfaceVariant = Color(0xFF2B241C)     // 第2レベルサーフェス
val DarkPrimaryContainer = Color(0xFF522815)   // 朱の暗い滲み
val DarkSecondaryContainer = Color(0xFF3B342A) // 補助背景

// ==========================================
// 補助カラー
// ==========================================

// エラー・警告系
val LightError = Color(0xFFB3261E)             // 落ち着いた赤
val DarkError = Color(0xFFFFB4AB)              // ダーク用の淡い赤

// 境界線（和紙の折り目のような繊細な線）
val LightOutline = Color(0xFFDBD1BF)
val DarkOutline = Color(0xFF473F32)

// 補助テキスト
val LightOnSurfaceVariant = Color(0xFF5F5749)  // 薄墨
val DarkOnSurfaceVariant = Color(0xFFAAA08E)   // 夜の薄墨

// ダイアログ・コンテナ背景
val LightSurfaceContainer = Color(0xFFFFFCF5)
val LightSurfaceContainerHigh = Color(0xFFF3ECDF)
val DarkSurfaceContainer = Color(0xFF1F1A14)
val DarkSurfaceContainerHigh = Color(0xFF2B241C)

// ==========================================
// アクセントグラデーション（朱 → 琥珀）
// ==========================================
// FABなど「書く」アクションにだけ使う、夕暮れの空のようなグラデーション
val AccentGradientStart = Color(0xFFBC4B26)    // 朱
val AccentGradientCenter = Color(0xFFD97636)   // 柿
val AccentGradientEnd = Color(0xFFE9A23B)      // 琥珀

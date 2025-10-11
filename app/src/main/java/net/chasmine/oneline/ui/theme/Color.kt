package net.chasmine.oneline.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// サンセット・ジャーナル カラーテーマ
// ==========================================
// コンセプト: 1日の終わりに振り返る、夕暮れの温かみ
// 穏やかで心地よい配色で、毎日開きたくなる優しい雰囲気

// ライトテーマ - 柔らかな夕暮れ
val LightBackground = Color(0xFFFAF8F6)        // オフホワイト - 全体の背景色
val LightSurface = Color(0xFFFFFFFF)           // ピュアホワイト - カードやダイアログ
val LightPrimary = Color(0xFFE07A5F)           // テラコッタオレンジ - メインアクション
val LightSecondary = Color(0xFFF4A261)         // サンドイエロー - 日付などの補助情報
val LightTertiary = Color(0xFF81B29A)          // セージグリーン - アクセント、成功表示など
val LightOnSurface = Color(0xFF3D405B)         // ダークブルーグレー - 本文テキスト
val LightOnPrimary = Color(0xFFFFFFFF)         // ホワイト - プライマリボタン上のテキスト

// ダークテーマ - 深い夜の静寂
val DarkBackground = Color(0xFF1F2937)         // ダークネイビー - 全体の背景色
val DarkSurface = Color(0xFF2D3748)            // スレートグレー - カードやダイアログ
val DarkPrimary = Color(0xFFF4A261)            // 温かみのあるオレンジ - メインアクション
val DarkSecondary = Color(0xFFE07A5F)          // コーラル - 日付などの補助情報
val DarkTertiary = Color(0xFF81B29A)           // セージグリーン - アクセント、成功表示など
val DarkOnSurface = Color(0xFFF3F4F6)          // ライトグレー - 本文テキスト
val DarkOnPrimary = Color(0xFF1F2937)          // ダークネイビー - プライマリボタン上のテキスト

// ==========================================
// 追加の補助カラー（オプション）
// ==========================================
// より細かい UI 表現が必要な場合に使用

// エラー・警告系
val LightError = Color(0xFFDC2626)             // 赤 - エラー表示
val DarkError = Color(0xFFF87171)              // 明るい赤 - ダークモードのエラー

// 半透明・境界線
val LightOutline = Color(0xFFE5E7EB)           // 薄いグレー - 境界線
val DarkOutline = Color(0xFF4B5563)            // 中間グレー - ダークモードの境界線

// 無効状態
val LightOnSurfaceVariant = Color(0xFF6B7280) // グレー - 無効化されたテキスト
val DarkOnSurfaceVariant = Color(0xFF9CA3AF)  // ダークグレー - 無効化されたテキスト
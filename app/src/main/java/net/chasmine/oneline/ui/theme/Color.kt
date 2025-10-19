package net.chasmine.oneline.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// iOS-Inspired OneLine カラーテーマ
// ==========================================
// コンセプト: iOSの洗練された美しさとSwarmの遊び心を融合
// 柔らかく、モダンで、心地よい配色

// ライトテーマ - iOS風の明るく清潔な印象
val LightBackground = Color(0xFFF2F2F7)        // iOS標準のライトグレー背景
val LightSurface = Color(0xFFFFFFFF)           // ピュアホワイト - カードやダイアログ
val LightPrimary = Color(0xFFFF9500)           // iOS風のオレンジ - メインアクション（Swarmのアクセント）
val LightSecondary = Color(0xFFFFCC00)         // ゴールデンイエロー - 日付などの補助情報
val LightTertiary = Color(0xFF34C759)          // iOS風のグリーン - アクセント、成功表示
val LightOnSurface = Color(0xFF000000)         // ブラック - 本文テキスト
val LightOnPrimary = Color(0xFFFFFFFF)         // ホワイト - プライマリボタン上のテキスト
val LightSurfaceVariant = Color(0xFFF9F9F9)    // 極薄グレー - セクション背景

// ダークテーマ - iOS風の深く洗練された印象
val DarkBackground = Color(0xFF000000)         // iOS標準のピュアブラック背景
val DarkSurface = Color(0xFF1C1C1E)            // iOS風のダークサーフェス
val DarkPrimary = Color(0xFFFF9F0A)            // iOS風のオレンジ（ダークモード調整）
val DarkSecondary = Color(0xFFFFD60A)          // ゴールデンイエロー（ダークモード調整）
val DarkTertiary = Color(0xFF32D74B)           // iOS風のグリーン（ダークモード調整）
val DarkOnSurface = Color(0xFFFFFFFF)          // ホワイト - 本文テキスト
val DarkOnPrimary = Color(0xFF000000)          // ブラック - プライマリボタン上のテキスト
val DarkSurfaceVariant = Color(0xFF2C2C2E)     // iOS風の第2レベルサーフェス

// ==========================================
// 追加の補助カラー（iOS風）
// ==========================================

// エラー・警告系
val LightError = Color(0xFFFF3B30)             // iOS風の赤 - エラー表示
val DarkError = Color(0xFFFF453A)              // iOS風の赤（ダークモード）

// 半透明・境界線
val LightOutline = Color(0xFFE5E5EA)           // iOS風の薄いグレー - 境界線
val DarkOutline = Color(0xFF38383A)            // iOS風の境界線（ダークモード）

// 無効状態
val LightOnSurfaceVariant = Color(0xFF8E8E93) // iOS風のグレー - 無効化されたテキスト
val DarkOnSurfaceVariant = Color(0xFF98989D)  // iOS風のグレー（ダークモード）

// Swarm風の遊び心のあるアクセントカラー
val SwarmBlue = Color(0xFF4A90E2)              // Swarm風のブルー
val SwarmPurple = Color(0xFF9B59B6)            // Swarm風のパープル
val SwarmPink = Color(0xFFE91E63)              // Swarm風のピンク
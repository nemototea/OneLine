package net.chasmine.oneline.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.chasmine.oneline.ui.theme.OneLineTheme

/**
 * Compose Multiplatform 共通UI（iOS エントリーポイント）
 *
 * 現状は KMP/CMP の動作確認プレースホルダー。iOS のナビゲーション・DI 配線は未実装で、
 * これらを commonMain へ移植する対応は別タスクとする。
 * ここでは DESIGN.md「墨と和紙」の地・書体・トーンだけ先に適用しておく。
 */
@Composable
fun App() {
    OneLineTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val greeting = remember { Greeting().greet() }

                Text(
                    text = "OneLine",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "iOS 版は準備中です",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 影は使わず、紙のカードは surface ＋ 折り目線で表す
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

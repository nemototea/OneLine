package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.chasmine.oneline.ui.components.NotificationSettingsSection
import net.chasmine.oneline.ui.components.InfoCard
import net.chasmine.oneline.ui.components.WarningCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知設定") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 説明カード
            InfoCard(
                message = "毎日決まった時間に日記を書くリマインダーを受け取ることができます。既に日記を書いている場合は通知されません。",
                modifier = Modifier.fillMaxWidth()
            )

            // 通知設定セクション
            NotificationSettingsSection()

            // 注意事項
            WarningCard(
                message = "• Android 13以降では通知権限の許可が必要です\n• バッテリー最適化の設定により通知が遅延する場合があります\n• 端末の省電力モードでは通知が制限される場合があります",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

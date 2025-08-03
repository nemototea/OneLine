package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.chasmine.oneline.R
import net.chasmine.oneline.data.preferences.SettingsManager

@Composable
fun WelcomeScreen(
    onLocalModeSelected: () -> Unit,
    onGitModeSelected: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // アプリロゴ・タイトル
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // アプリアイコンがあれば表示
            // Image(
            //     painter = painterResource(id = R.drawable.ic_app_logo),
            //     contentDescription = "OneLine",
            //     modifier = Modifier.size(80.dp)
            // )
            
            Text(
                text = "OneLine へようこそ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "毎日の想いを一行で記録する\nシンプルな日記アプリです",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // データ保存方法の選択
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "データの保存方法を選択してください",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // ローカル保存オプション
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                onClick = {
                    scope.launch {
                        settingsManager.setLocalOnlyMode(true)
                        onLocalModeSelected()
                    }
                }
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Column {
                            Text(
                                text = "📱 ローカル保存のみ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "端末内にのみ保存（推奨）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Text(
                        text = "✅ 設定不要ですぐに使用開始\n✅ プライベートで安全\n✅ シンプルで軽快",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 44.dp)
                    )
                    
                    Text(
                        text = "※ 後からGit連携に変更することも可能です",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 44.dp)
                    )
                }
            }

            // Git連携オプション
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                onClick = onGitModeSelected
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Column {
                            Text(
                                text = "☁️ Git連携",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "クラウドで自動バックアップ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Text(
                        text = "✅ 自動バックアップ\n✅ 複数端末での同期\n✅ バージョン管理",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 44.dp)
                    )
                    
                    Text(
                        text = "※ GitHubアカウントと設定が必要です",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 44.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // 補足説明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💡 どちらを選べばいい？",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "初めての方や、とりあえず試してみたい方は「ローカル保存のみ」がおすすめです。設定不要ですぐに日記を書き始められます。",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "複数の端末で同期したい方や、データのバックアップを自動化したい方は「Git連携」をお選びください。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

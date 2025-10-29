package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.chasmine.oneline.R
import net.chasmine.oneline.data.preferences.SettingsManager

data class TutorialPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val details: List<String>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onLocalModeSelected: () -> Unit,
    onGitModeSelected: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }
    
    // チュートリアルページの定義
    val tutorialPages = listOf(
        TutorialPage(
            icon = Icons.Default.EditNote,
            title = "シンプルな日記",
            description = "毎日の想いを一行で記録",
            details = listOf(
                "短い文章で気軽に記録",
                "継続しやすいシンプルさ",
                "日々の振り返りに最適"
            )
        ),
        TutorialPage(
            icon = Icons.Default.CalendarMonth,
            title = "カレンダー表示",
            description = "過去の記録を簡単に振り返り",
            details = listOf(
                "月別でまとめて確認",
                "記録した日が一目でわかる",
                "タップして詳細を表示"
            )
        ),
        TutorialPage(
            icon = Icons.Default.Notifications,
            title = "通知機能",
            description = "書き忘れを防ぐリマインダー",
            details = listOf(
                "毎日決まった時間に通知",
                "通知時間は自由に設定可能",
                "継続的な記録をサポート"
            )
        )
    )
    
    val pagerState = rememberPagerState(pageCount = { tutorialPages.size + 1 }) // +1 for settings page
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ページコンテンツ
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            if (page < tutorialPages.size) {
                // チュートリアルページ
                TutorialPageContent(tutorialPages[page])
            } else {
                // データ保存方法選択ページ
                DataStorageSelectionPage(
                    onLocalModeSelected = onLocalModeSelected,
                    onGitModeSelected = onGitModeSelected,
                    settingsManager = settingsManager,
                    scope = scope
                )
            }
        }
        
        // ページインジケーター
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(tutorialPages.size + 1) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            }
                        )
                )
                if (index < tutorialPages.size) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
        
        // ナビゲーションボタン
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // スキップボタン（最後のページでは非表示）
            if (pagerState.currentPage < tutorialPages.size) {
                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(tutorialPages.size)
                        }
                    }
                ) {
                    Text("スキップ")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            
            // 次へボタン（最後のページでは非表示）
            if (pagerState.currentPage < tutorialPages.size) {
                Button(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage < tutorialPages.size) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                ) {
                    Text(if (pagerState.currentPage == tutorialPages.size - 1) "設定へ" else "次へ")
                }
            }
        }
    }
}

@Composable
private fun TutorialPageContent(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // アイコン
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // タイトル
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 説明
        Text(
            text = page.description,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 詳細リスト
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            page.details.forEach { detail ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun DataStorageSelectionPage(
    onLocalModeSelected: () -> Unit,
    onGitModeSelected: () -> Unit,
    settingsManager: SettingsManager,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // アプリロゴ・タイトル
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "OneLine へようこそ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "データの保存方法を選択してください",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // データ保存方法の選択
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ローカル保存オプション
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            settingsManager.setLocalOnlyMode(true)
                            onLocalModeSelected()
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )

                        Column {
                            Text(
                                text = "📱 ローカル保存のみ（推奨）",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✅ 設定不要ですぐ使える\n✅ 完全プライベート",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Git連携オプション
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGitModeSelected() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )

                        Column {
                            Text(
                                text = "☁️ Git連携",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✅ 自動バックアップ\n✅ 複数端末で同期",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 補足説明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💡",
                    style = MaterialTheme.typography.titleMedium
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "初めての方は「ローカル保存のみ」がおすすめ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "設定不要ですぐに日記を書き始められます。後からGit連携に変更することも可能です。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

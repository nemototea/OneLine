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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.launch
import net.chasmine.oneline.data.preferences.SettingsManager
import oneline.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

data class TutorialPage(
    val lottieFileName: String,
    val title: String,
    val description: String,
    val details: List<String>
)

/**
 * オンボーディング画面
 *
 * 構成（issue #70）:
 * 1. アプリ紹介（シンプルな日記）
 * 2. アプリ紹介（カレンダー）
 * 3. リマインダー設定（その場で通知ON/OFF・時刻を設定）
 * 4. 開始ページ（必ずローカルモードで開始し、Git連携は設定から案内。
 *    CTAでそのまま今日の日記を書いてもらう）
 *
 * 通知の権限リクエストや時刻ピッカーはプラットフォーム固有のため、
 * 呼び出し側（Androidラッパー等）からコールバックで注入する。
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalResourceApi::class)
@Composable
fun WelcomeScreenImpl(
    notificationEnabled: Boolean,
    notificationHour: Int,
    notificationMinute: Int,
    onNotificationToggle: (Boolean) -> Unit,
    onPickNotificationTime: () -> Unit,
    onNotificationPageVisible: () -> Unit,
    onStartFirstEntry: () -> Unit,
    settingsManager: SettingsManager
) {
    val scope = rememberCoroutineScope()

    val featurePages = listOf(
        TutorialPage(
            lottieFileName = "checklist_cubaan.json",
            title = "シンプルな日記",
            description = "毎日の想いを一行で記録",
            details = listOf(
                "短い文章で気軽に記録",
                "継続しやすいシンプルさ",
                "日々の振り返りに最適"
            )
        ),
        TutorialPage(
            lottieFileName = "marking_a_calendar.json",
            title = "カレンダー表示",
            description = "過去の記録を簡単に振り返り",
            details = listOf(
                "月別でまとめて確認",
                "記録した日が一目でわかる",
                "タップして詳細を表示"
            )
        )
    )

    // 機能紹介2ページ + 通知設定 + 開始ページ
    val pageCount = featurePages.size + 2
    val notificationPageIndex = featurePages.size
    val lastPageIndex = pageCount - 1

    val pagerState = rememberPagerState(pageCount = { pageCount })

    // スイッチはデフォルトONのため、通知ページが表示された時点で
    // 権限の確認・リクエストを行う（初回表示時のみ）
    var notificationPageSeen by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == notificationPageIndex && !notificationPageSeen) {
            notificationPageSeen = true
            onNotificationPageVisible()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when {
                page < featurePages.size -> TutorialPageContent(featurePages[page])
                page == notificationPageIndex -> NotificationSetupPage(
                    enabled = notificationEnabled,
                    hour = notificationHour,
                    minute = notificationMinute,
                    onToggle = onNotificationToggle,
                    onPickTime = onPickNotificationTime
                )
                else -> StartPage()
            }
        }

        // ページインジケーター
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pageCount) { index ->
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
                if (index < pageCount - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        // ナビゲーションボタン
        if (pagerState.currentPage == lastPageIndex) {
            // 最終ページ: ローカルモードで開始して最初の日記へ
            Button(
                onClick = {
                    scope.launch {
                        settingsManager.setLocalOnlyMode(true)
                        onStartFirstEntry()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "今日の日記を書いてみる",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(lastPageIndex)
                        }
                    }
                ) {
                    Text("スキップ")
                }

                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("次へ")
                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun TutorialPageContent(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(fileName = page.lottieFileName, size = 200)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

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

/**
 * 通知（リマインダー）設定ページ
 * オンボーディング中にその場でON/OFFと時刻を設定できる
 */
@Composable
private fun NotificationSetupPage(
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onToggle: (Boolean) -> Unit,
    onPickTime: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(fileName = "notifications.json", size = 160)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "書き忘れを防ぐ",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "毎日決まった時間にリマインダーを受け取って、日記の習慣をつくりましょう",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "毎日リマインド",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggle
                    )
                }

                if (enabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPickTime() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "通知時刻",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = formatTime(hour, minute),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "あとから設定画面でいつでも変更できます",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 開始ページ
 * 必ずローカルモードで開始する。Git連携は設定画面から可能なことを案内する
 */
@Composable
private fun StartPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(fileName = "celebrations_begin.json", size = 180)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "準備ができました",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "日記はまず端末の中に保存されます。\n設定不要で、完全にプライベートです。",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Git連携の案内（オンボーディングでは選択させない）
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "クラウド同期もできます",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Gitリポジトリと連携すれば、自動バックアップや複数端末での同期が可能です。「設定 > データ保存」からいつでも切り替えられます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun LottieAnimation(fileName: String, size: Int) {
    val composition = rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/$fileName").decodeToString()
        )
    }

    val progress = animateLottieCompositionAsState(
        composition = composition.value,
        iterations = Compottie.IterateForever
    )

    Image(
        painter = rememberLottiePainter(
            composition = composition.value,
            progress = { progress.value }
        ),
        contentDescription = null,
        modifier = Modifier.size(size.dp)
    )
}

private fun formatTime(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

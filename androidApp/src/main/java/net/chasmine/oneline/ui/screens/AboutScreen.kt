package net.chasmine.oneline.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.launch
import net.chasmine.oneline.R
import net.chasmine.oneline.data.preferences.SettingsManagerFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repositoryUrl = "https://github.com/nemototea/OneLine"
    val settingsManager = remember { SettingsManagerFactory.getInstance(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 開発者モードの状態を取得
    val isDeveloperMode by settingsManager.isDeveloperMode.collectAsState(initial = false)

    // イースターエッグ用の状態変数（アプリアイコン用）
    var iconTapCount by remember { mutableIntStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }
    val iconInteractionSource = remember { MutableInteractionSource() }

    // 開発者モード用の状態変数（バージョン情報用）
    var versionTapCount by remember { mutableIntStateOf(0) }
    val versionInteractionSource = remember { MutableInteractionSource() }

    // アイコンのバウンスアニメーション
    val iconScale by animateFloatAsState(
        targetValue = if (iconTapCount > 0 && iconTapCount < 7) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "アプリについて",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // アプリアイコン（イースターエッグ対応）
            Image(
                painter = painterResource(id = R.drawable.app_icon_full),
                contentDescription = "OneLine アプリアイコン",
                modifier = Modifier
                    .size(120.dp)
                    .scale(iconScale)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = iconInteractionSource,
                        indication = null
                    ) {
                        iconTapCount++
                        if (iconTapCount >= 7) {
                            showEasterEgg = true
                        }
                    },
                contentScale = ContentScale.Crop
            )

            // タップ回数のヒント表示（5回以上タップしたら表示）
            if (iconTapCount in 5..6) {
                Text(
                    text = "あと${7 - iconTapCount}回...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }

            // アプリ名
            Text(
                text = "OneLine",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // バージョン情報（開発者モード有効化用）
            Text(
                text = "バージョン 1.0.0",
                modifier = Modifier.clickable(
                    interactionSource = versionInteractionSource,
                    indication = null
                ) {
                    versionTapCount++
                    if (versionTapCount >= 7) {
                        scope.launch {
                            val newMode = !isDeveloperMode
                            settingsManager.setDeveloperMode(newMode)
                            snackbarHostState.showSnackbar(
                                message = if (newMode) "開発者モードが有効になりました" else "開発者モードが無効になりました",
                                duration = SnackbarDuration.Short
                            )
                            versionTapCount = 0
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // タップ回数のヒント表示（5回以上タップしたら表示）
            if (versionTapCount in 5..6) {
                Text(
                    text = "あと${7 - versionTapCount}回...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // アプリ説明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "OneLineについて",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "OneLineは、手軽に日記を書くことを目的とした日記アプリです。\n\n忙しい毎日の中で忘れてしまいがちな、何でもないできごとを簡単に書き留めて振り返ることができます。\n\n物理的な日記と違い、買い替えや記入忘れの心配がなく、他の日記サービスと違い、データを完全に自分で管理できます。端末内またはGitリポジトリに保存することで、完全にプライベートに保管できます。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 機能一覧
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "主な機能",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val features = listOf(
                        "シンプルな日記作成・編集",
                        "GitHubリポジトリとの自動同期",
                        "毎日のリマインダー通知",
                        "ホーム画面ウィジェット",
                        "プライベートリポジトリ対応"
                    )

                    features.forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // リポジトリ情報
            Card(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repositoryUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "GitHubリポジトリ",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ソースコードの閲覧、Issue報告、フィードバックはこちらから",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "リポジトリを開く",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ライセンス情報
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ライセンス",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "このアプリは MIT License の下で公開されているオープンソースソフトウェアです。\n\n詳細なライセンス情報や使用しているライブラリについては、GitHubリポジトリをご確認ください。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // フッター
            Text(
                text = "© 2025 OneLine",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    // イースターエッグダイアログ
    if (showEasterEgg) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 半透明の背景オーバーレイ
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = iconInteractionSource,
                        indication = null
                    ) {
                        showEasterEgg = false
                        iconTapCount = 0
                    },
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {}

            // イースターエッグカード（一時的なレイヤー。影ではなく折り目線で締める）
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.the_birds_of_heart_valley_no_strings)
                    )
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = LottieConstants.IterateForever
                    )

                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "隠し要素発見",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "OneLineを使ってくれてありがとう！\n\n毎日の小さな瞬間を大切に記録していきましょう。",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showEasterEgg = false
                            iconTapCount = 0
                        }
                    ) {
                        Text("閉じる")
                    }
                }
            }
        }
    }
}

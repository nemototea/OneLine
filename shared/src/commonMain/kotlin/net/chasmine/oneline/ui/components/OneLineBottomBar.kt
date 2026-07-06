package net.chasmine.oneline.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.chasmine.oneline.ui.theme.AccentGradientCenter
import net.chasmine.oneline.ui.theme.AccentGradientEnd
import net.chasmine.oneline.ui.theme.AccentGradientStart

/**
 * ボトムナビゲーションバー（DESIGN.md: bottom-bar / fab-write）
 *
 * - 2タブ（日記・カレンダー）＋中央に「書く」FAB
 * - 紙のバー: surface 単色・上角丸 20・区切りは折り目（outline 1dp）。影は使わない
 * - FAB はアプリで唯一のグラデーション（朱→琥珀）。浮いている意味を持つ唯一の例外として影を持つ
 */
@Composable
fun OneLineBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onNewEntryClick: () -> Unit,
    isSyncing: Boolean = false
) {
    // システムナビゲーションバーの高さを取得
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val totalHeight = 88.dp + bottomInset
    val surfaceHeight = 72.dp + bottomInset

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(surfaceHeight)
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = bottomInset),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomBarTabItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "日記",
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.weight(1f)
                )

                // 中央のスペース（FABのため）
                Spacer(modifier = Modifier.width(80.dp))

                BottomBarTabItem(
                    icon = Icons.Default.CalendarMonth,
                    label = "カレンダー",
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 「書く」FAB — 朱→琥珀のグラデーションはこのアクション専用
        FloatingActionButton(
            onClick = {
                if (!isSyncing) {
                    onNewEntryClick()
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 8.dp)
                .size(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    clip = false
                ),
            shape = CircleShape,
            containerColor = Color.Transparent,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AccentGradientStart,
                                AccentGradientCenter,
                                AccentGradientEnd
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新規作成",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

/**
 * タブアイテム。選択状態は朱＋わずかなスケールで静かに伝える
 */
@Composable
private fun BottomBarTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tabScale"
    )

    val iconColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 40.dp),
                onClick = onClick,
                enabled = !selected  // 選択中のタブはクリック無効化
            )
            .padding(vertical = 8.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = iconColor
        )
    }
}

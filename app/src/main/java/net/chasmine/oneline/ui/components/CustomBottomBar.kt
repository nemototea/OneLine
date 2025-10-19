package net.chasmine.oneline.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.chasmine.oneline.ui.theme.SwarmBlue
import net.chasmine.oneline.ui.theme.SwarmPink
import net.chasmine.oneline.ui.theme.SwarmPurple

/**
 * Swarm風のボトムナビゲーションバー
 * - iOSライクな半透明背景
 * - アニメーション付きのタブ選択
 * - グラデーションFABボタン
 */
@Composable
fun CustomBottomBar(
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
        // iOS風の半透明背景を持つナビゲーションバー
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(surfaceHeight)
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 8.dp,
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
                // 日記一覧タブ
                SwarmTabItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "日記",
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.weight(1f)
                )

                // 中央のスペース（FABのため）
                Spacer(modifier = Modifier.width(80.dp))

                // カレンダータブ
                SwarmTabItem(
                    icon = Icons.Default.CalendarMonth,
                    label = "カレンダー",
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Swarm風のグラデーションFAB
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
                                SwarmPink,
                                SwarmPurple,
                                SwarmBlue
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
 * Swarm風のタブアイテム
 * - アニメーション付きの選択状態表示
 * - iOS風の控えめなデザイン
 */
@Composable
private fun SwarmTabItem(
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
                onClick = onClick
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
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = iconColor
        )
    }
}

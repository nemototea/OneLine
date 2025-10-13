package net.chasmine.oneline.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CustomBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onNewEntryClick: () -> Unit,
    isSyncing: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
    ) {
        // ボトムナビゲーションバー
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // 日記一覧タブ
            NavigationBarItem(
                icon = { 
                    Icon(
                        Icons.AutoMirrored.Filled.List, 
                        contentDescription = "日記一覧",
                        modifier = Modifier.padding(4.dp)
                    ) 
                },
                label = { Text("日記") },
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
            
            // 中央のスペース（FABのため）
            Spacer(modifier = Modifier.weight(1f))
            
            // カレンダータブ
            NavigationBarItem(
                icon = { 
                    Icon(
                        Icons.Default.CalendarMonth, 
                        contentDescription = "カレンダー",
                        modifier = Modifier.padding(4.dp)
                    ) 
                },
                label = { Text("カレンダー") },
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
        
        // 中央のフローティングアクションボタン
        FloatingActionButton(
            onClick = {
                if (!isSyncing) {
                    onNewEntryClick()
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            )
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新規作成",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

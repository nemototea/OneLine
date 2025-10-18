package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.chasmine.oneline.data.repository.RepositoryManager
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val repositoryManager = remember { RepositoryManager.getInstance(context) }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var diaryEntries by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }

    // 日記エントリーを取得
    LaunchedEffect(currentMonth) {
        repositoryManager.getAllEntries().collect { entries ->
            diaryEntries = entries.map { it.date }.toSet()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentMonth = currentMonth.minusMonths(1) }
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "前の月",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${currentMonth.year}年${currentMonth.monthValue}月",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { currentMonth = currentMonth.plusMonths(1) }
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "次の月",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
        
        // 曜日ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("日", "月", "火", "水", "木", "金", "土").forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
            // カレンダーグリッド
            CalendarGrid(
                currentMonth = currentMonth,
                diaryEntries = diaryEntries,
                onDateClick = { date ->
                    val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    onNavigateToEdit(dateString)
                }
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    diaryEntries: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 日曜日を0にする
    
    // カレンダーに表示する日付のリストを作成
    val calendarDays = mutableListOf<LocalDate?>()
    
    // 前月の日付で埋める
    repeat(firstDayOfWeek) {
        calendarDays.add(null)
    }
    
    // 当月の日付を追加
    for (day in 1..lastDayOfMonth.dayOfMonth) {
        calendarDays.add(currentMonth.atDay(day))
    }
    
    // 6週間分（42日）になるまで次月の日付で埋める
    while (calendarDays.size < 42) {
        calendarDays.add(null)
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(calendarDays) { date ->
            CalendarDay(
                date = date,
                hasEntry = date?.let { diaryEntries.contains(it) } ?: false,
                isCurrentMonth = date?.month == currentMonth.month,
                onClick = { date?.let { onDateClick(it) } }
            )
        }
    }
}

@Composable
fun CalendarDay(
    date: LocalDate?,
    hasEntry: Boolean,
    isCurrentMonth: Boolean,
    onClick: () -> Unit
) {
    val today = LocalDate.now()
    val isToday = date == today

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = date != null && isCurrentMonth) { onClick() }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (date != null && isCurrentMonth) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isToday) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isToday)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )

                        // 日記投稿済みの日付にドットを表示
                        if (hasEntry) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        } else if (date != null) {
            // 前月・次月の日付（薄く表示）
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

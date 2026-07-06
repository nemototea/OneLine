package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.chasmine.oneline.ui.theme.DateNumeralStyle
import net.chasmine.oneline.data.model.DiaryEntry
import net.chasmine.oneline.data.repository.RepositoryFactory
import net.chasmine.oneline.util.DiaryStatistics
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.todayIn

/**
 * kotlinx.datetime用の簡易YearMonthクラス
 */
data class YearMonth(val year: Int, val month: Int) {
    fun atDay(day: Int): LocalDate = LocalDate(year, month, day)

    fun lengthOfMonth(): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> throw IllegalArgumentException("Invalid month: $month")
    }

    fun atEndOfMonth(): LocalDate = atDay(lengthOfMonth())

    fun plusMonths(months: Long): YearMonth {
        val totalMonths = year * 12 + month - 1 + months
        return YearMonth((totalMonths / 12).toInt(), (totalMonths % 12).toInt() + 1)
    }

    fun minusMonths(months: Long): YearMonth = plusMonths(-months)

    companion object {
        fun now(): YearMonth {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            return YearMonth(today.year, today.monthNumber)
        }

        private fun isLeapYear(year: Int): Boolean {
            return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreenImpl(
    onNavigateToEdit: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    repositoryFactory: RepositoryFactory
) {
    val scope = rememberCoroutineScope()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var diaryEntries by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var allEntries by remember { mutableStateOf<List<DiaryEntry>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }

    // 日記エントリーを取得
    LaunchedEffect(currentMonth) {
        repositoryFactory.getAllEntries().collect { entries ->
            allEntries = entries
            diaryEntries = entries.map { it.date }.toSet()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OneLine",
                        style = MaterialTheme.typography.displayLarge
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                // 和紙の地と一体化させ、面の色差ではなく余白で区切る
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                repositoryFactory.syncRepository()
                                isSyncing = false
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = "同期",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "設定",
                            tint = MaterialTheme.colorScheme.onSurface
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            // 年月の切り替えヘッダー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                    text = "${currentMonth.year}年${currentMonth.month}月",
                    style = MaterialTheme.typography.headlineMedium
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
                        style = MaterialTheme.typography.labelSmall,
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
                    val dateString = date.toString()
                    onNavigateToEdit(dateString)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 統計情報
            DiaryStatisticsSection(
                allEntries = allEntries,
                currentMonth = currentMonth
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
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.isoDayNumber % 7 // 日曜日を0にする

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
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp), // 固定高を設定してスクロールコンテナとのネストを回避
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = false // 親のColumnでスクロールするため無効化
    ) {
        items(calendarDays) { date ->
            CalendarDay(
                date = date,
                hasEntry = date?.let { diaryEntries.contains(it) } ?: false,
                isCurrentMonth = date?.monthNumber == currentMonth.month,
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
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
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

/**
 * 継続の実績（DESIGN.md: カレンダー）
 *
 * 記録の俯瞰を静かに支える数字。絵文字やグラフ装飾は使わず、
 * 濃い和紙（surfaceVariant）の面の上に紙のカードを並べるだけにする。
 */
@Composable
fun DiaryStatisticsSection(
    allEntries: List<DiaryEntry>,
    currentMonth: YearMonth
) {
    val currentStreak = remember(allEntries) {
        DiaryStatistics.calculateCurrentStreak(allEntries)
    }
    val longestStreak = remember(allEntries) {
        DiaryStatistics.calculateLongestStreak(allEntries)
    }
    val monthlyCount = remember(allEntries, currentMonth) {
        DiaryStatistics.calculateMonthlyCount(allEntries, currentMonth.year, currentMonth.month)
    }
    val totalCount = remember(allEntries) {
        DiaryStatistics.calculateTotalCount(allEntries)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "続いた日々",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticsItem(
                    modifier = Modifier.weight(1f),
                    label = "現在の連続",
                    value = "$currentStreak",
                    unit = "日"
                )

                StatisticsItem(
                    modifier = Modifier.weight(1f),
                    label = "最長連続",
                    value = "$longestStreak",
                    unit = "日"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticsItem(
                    modifier = Modifier.weight(1f),
                    label = "今月",
                    value = "$monthlyCount",
                    unit = "投稿"
                )

                StatisticsItem(
                    modifier = Modifier.weight(1f),
                    label = "総投稿数",
                    value = "$totalCount",
                    unit = "投稿"
                )
            }
        }
    }
}

@Composable
fun StatisticsItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                // 数字は一覧の日付と同じ細身の佇まい（DESIGN.md: dateNumeral）
                Text(
                    text = value,
                    style = DateNumeralStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

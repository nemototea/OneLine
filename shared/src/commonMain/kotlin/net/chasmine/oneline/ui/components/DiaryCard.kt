package net.chasmine.oneline.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import net.chasmine.oneline.data.model.DiaryEntry
import net.chasmine.oneline.ui.theme.DateNumeralStyle

/**
 * 日記一覧のエントリー行
 *
 * デザイン「墨と和紙」:
 * - 左側にアプリ名の由来である「一本の糸」= タイムラインの細い線を通す
 * - 日付は細身の大きな数字で、雑誌の日付欄のように
 * - 本文は影のないフラットな紙のカードに、和紙の折り目のような繊細な枠線
 */
@Composable
fun DiaryCard(
    entry: DiaryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTopLine: Boolean = true,
    showBottomLine: Boolean = true
) {
    val dayText = entry.date.dayOfMonth.toString().padStart(2, '0')
    val monthText = entry.date.month.name.take(3).uppercase()
    // 年は今年と違うときだけ添える（毎行の繰り返しを避け、必要な情報だけ残す）
    val currentYear = Clock.System.todayIn(TimeZone.currentSystemDefault()).year
    val yearText = entry.date.year.takeIf { it != currentYear }?.toString()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // タイムライン（一本の糸）
        Box(
            modifier = Modifier
                .width(52.dp)
                .padding(end = 14.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 上の糸
                if (showTopLine) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                } else {
                    Spacer(modifier = Modifier.height(26.dp))
                }

                // 結び目（その日の点）
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )

                // 下の糸
                if (showBottomLine) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline)
                    )
                }
            }

            // 日付（細身の数字で雑誌の日付欄のように）
            Column(
                modifier = Modifier
                    .padding(top = 42.dp)
                    .width(52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayText,
                    style = DateNumeralStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = monthText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (yearText != null) {
                    Text(
                        text = yearText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 本文（影のない紙のカード）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

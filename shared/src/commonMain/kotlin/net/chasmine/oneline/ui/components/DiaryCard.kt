package net.chasmine.oneline.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.chasmine.oneline.data.model.DiaryEntry

@Composable
fun DiaryCard(
    entry: DiaryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTopLine: Boolean = true,
    showBottomLine: Boolean = true
) {
    // kotlinx.datetime.LocalDate用のフォーマット関数
    val dayText = entry.date.dayOfMonth.toString().padStart(2, '0')
    val monthYearText = "${entry.date.month.name.take(3)} ${entry.date.year}".uppercase()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // タイムライン部分（縦線とドット）
        Box(
            modifier = Modifier
                .width(48.dp)
                .padding(end = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 上の線
                if (showTopLine) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ドット（円）
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )

                // 下の線
                if (showBottomLine) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                }
            }

            // 日付表示（タイムライン上に重ねて表示）
            Column(
                modifier = Modifier
                    .padding(top = 40.dp)
                    .width(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = monthYearText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 日記内容のカード
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
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
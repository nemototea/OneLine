package net.chasmine.oneline.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.chasmine.oneline.data.model.DiaryEntry
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun DiaryCard(
    entry: DiaryEntry,
    onClick: () -> Unit
) {
    val dayFormatter = DateTimeFormatter.ofPattern("dd")
    val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 日付表示部分
        Column(
            modifier = Modifier
                .padding(end = 12.dp)
                .width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = entry.date.format(dayFormatter),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = entry.date.format(monthYearFormatter).uppercase(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // 日記内容のカード
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
package net.chasmine.oneline.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.chasmine.oneline.data.model.DiaryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryForm(
    entry: DiaryEntry,
    isNew: Boolean,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNew) "新しい日記" else "日記を編集"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "削除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "保存"
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 日付表示
            Text(
                text = "日付：${entry.getDisplayDate()}",
                style = MaterialTheme.typography.titleMedium
            )

            // 内容入力フィールド
            OutlinedTextField(
                value = entry.content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder = { Text("今日の一行を記録しましょう...") },
                label = { Text("きょうの一行") },
                maxLines = 10
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存する")
            }
        }
    }
}
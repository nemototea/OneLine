package net.chasmine.oneline.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.chasmine.oneline.data.model.DiaryEntry

/**
 * 編集画面のフォーム（DESIGN.md: 編集画面 / input-diary）
 *
 * 没入型・無装飾。画面に置くのは日付・入力欄・保存だけ。
 * 文字数カウントや装飾ツールバーは足さない（「一行」の思想を守る）。
 */
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
    // 空の日記は保存できない（保存しても一覧に表示されず混乱のもとになる）
    val canSave = entry.content.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                // タイトルは日付そのもの。「新しい日記」等のラベルは足さない
                title = {
                    Text(
                        text = entry.getDisplayDate(),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                // 他のメイン画面と同様にステータスバー分の余白を除き、ヘッダーの高さを揃える
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
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "削除",
                                tint = MaterialTheme.colorScheme.error
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
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 入力欄が残りの空間すべてを使う。紙に向かう時間の主役
            OutlinedTextField(
                value = entry.content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = {
                    Text(
                        text = "今日の一行を記録しましょう",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = { Text("きょうの一行") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            // 保存は親指の届く画面下部に（DESIGN.md: button-primary）
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("保存する")
            }
        }
    }
}

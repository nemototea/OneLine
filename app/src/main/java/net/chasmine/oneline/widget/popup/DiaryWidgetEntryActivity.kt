package net.chasmine.oneline.widget.popup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.model.DiaryEntry
import net.chasmine.oneline.ui.theme.OneLineTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

class DiaryWidgetEntryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 既存のエントリ内容と有無を取得
        val entryContent = intent.getStringExtra("ENTRY_CONTENT") ?: ""
        val hasEntry = intent.getBooleanExtra("HAS_ENTRY", false)

        setContent {
            OneLineTheme {
                DiaryEntryDialog(
                    initialContent = entryContent,
                    isEditing = hasEntry,
                    onSave = { content ->
                        saveEntry(content, hasEntry)
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }

    private fun saveEntry(content: String, isEditing: Boolean) {
        val gitRepository = GitRepository.getInstance(applicationContext)

        lifecycleScope.launch {
            val entry = DiaryEntry(
                date = LocalDate.now(),
                content = content
            )

            gitRepository.saveEntry(entry)
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEntryDialog(
    initialContent: String,
    isEditing: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
//    val focusRequester = remember { FocusRequester() }
//    LaunchedEffect(Unit) {
//        delay(100) // 少し遅延させてダイアログが表示された後にフォーカス
//        focusRequester.requestFocus()
//    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (isEditing) "今日の日記を編集" else "今日の一行を記録",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("今日の一行を記録しましょう...") },
                    maxLines = 4
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("キャンセル")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onSave(content) }
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
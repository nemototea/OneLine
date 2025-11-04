package net.chasmine.oneline.widget.popup

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.model.DiaryEntry
import net.chasmine.oneline.ui.theme.OneLineTheme
import kotlinx.coroutines.launch
import net.chasmine.oneline.data.preferences.SettingsManagerFactory
import net.chasmine.oneline.data.repository.RepositoryManager
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class DiaryWidgetEntryActivity : ComponentActivity() {

    private val TAG = "DiaryWidgetEntryActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "=== DiaryWidgetEntryActivity onCreate ===")

        // ウィンドウ設定でダイアログスタイルを強制
        window?.apply {
            setFlags(
                android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )
            attributes?.apply {
                dimAmount = 0.5f
                width = android.view.WindowManager.LayoutParams.MATCH_PARENT
                height = android.view.WindowManager.LayoutParams.WRAP_CONTENT
            }
        }

        // インテントから情報を取得
        val entryContent = intent.getStringExtra("ENTRY_CONTENT") ?: ""
        val hasEntry = intent.getBooleanExtra("HAS_ENTRY", false)
        val entryDate = intent.getStringExtra("ENTRY_DATE") ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val repositoryInitialized = intent.getBooleanExtra("REPOSITORY_INITIALIZED", false)

        Log.d(TAG, "Activity started - hasEntry: $hasEntry, content length: ${entryContent.length}, date: $entryDate, repo initialized: $repositoryInitialized")

        setContent {
            OneLineTheme {
                var isDialogLoading by remember { mutableStateOf(false) }
                
                DiaryEntryDialog(
                    initialContent = entryContent,
                    isEditing = hasEntry,
                    entryDate = entryDate,
                    repositoryInitialized = repositoryInitialized,
                    isLoading = isDialogLoading,
                    onSave = { content ->
                        isDialogLoading = true
                        saveEntry(content, entryDate, repositoryInitialized) {
                            isDialogLoading = false
                        }
                    },
                    onDismiss = {
                        Log.d(TAG, "Dialog dismissed")
                        finish()
                    }
                )
            }
        }
    }

    private fun saveEntry(content: String, dateStr: String, repositoryInitialized: Boolean, onComplete: () -> Unit = {}) {
        Log.d(TAG, "Saving entry for date: $dateStr, content length: ${content.length}, repo initialized: $repositoryInitialized")

        lifecycleScope.launch {
            try {
                if (!repositoryInitialized) {
                    Log.w(TAG, "Repository not initialized, attempting to initialize before save")

                    // リポジトリの初期化を再試行
                    val settingsManager = SettingsManagerFactory.getInstance(applicationContext)
                    val gitRepository = GitRepository.getInstance(applicationContext)

                    val remoteUrl = settingsManager.gitRepoUrl.first()
                    val username = settingsManager.gitUsername.first()
                    val password = settingsManager.gitToken.first()

                    if (remoteUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                        val initResult = gitRepository.initRepository(remoteUrl, username, password)
                        if (initResult.isFailure) {
                            Log.e(TAG, "Failed to initialize repository for save", initResult.exceptionOrNull())
                            // 初期化に失敗してもローカルファイルとして保存を試行
                        }
                    }
                }

                val gitRepository = GitRepository.getInstance(applicationContext)

                // 日付をパース
                val date = LocalDate.parse(dateStr)

                val entry = DiaryEntry(
                    date = date,
                    content = content.trim()
                )

                val result = gitRepository.saveEntry(entry)

                if (result.isSuccess) {
                    Log.d(TAG, "Entry saved successfully")

                    // バックグラウンドで同期を試行（失敗してもアプリは続行）
                    if (gitRepository.isConfigValid()) {
                        launch {
                            try {
                                gitRepository.syncRepository()
                                Log.d(TAG, "Repository synced successfully")
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to sync repository", e)
                            }
                        }
                    } else {
                        Log.d(TAG, "Repository not valid for sync, skipping sync")
                    }
                } else {
                    Log.e(TAG, "Failed to save entry", result.exceptionOrNull())
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error saving entry", e)
            } finally {
                onComplete() // ローディング状態をリセット
                finish()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEntryDialog(
    initialContent: String,
    isEditing: Boolean,
    entryDate: String,
    repositoryInitialized: Boolean,
    isLoading: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // ダイアログが表示されたときにテキストフィールドにフォーカスを当てる
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // フォーカス要求が失敗してもアプリは続行
        }
    }

    Dialog(
        onDismissRequest = { 
            if (!isLoading) onDismiss() // ローディング中はダイアログを閉じられない
        },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading, // ローディング中はバックボタンで閉じられない
            dismissOnClickOutside = !isLoading, // ローディング中は外側タップで閉じられない
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
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
                // タイトルと日付を表示
                Text(
                    text = if (isEditing) "今日の日記を編集" else "今日の一行を記録",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = formatDateForDisplay(entryDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("今日の一行を記録しましょう...") },
                    maxLines = 4,
                    singleLine = false,
                    enabled = !isLoading // ローディング中は入力無効化
                )

                // ローディング表示
                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "保存中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            focusManager.clearFocus()
                            onDismiss()
                        },
                        enabled = !isLoading // ローディング中は無効化
                    ) {
                        Text("キャンセル")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onSave(content)
                        },
                        enabled = content.trim().isNotEmpty() && !isLoading // ローディング中は無効化
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDateForDisplay(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        "${date.year}年${date.monthNumber.toString().padStart(2, '0')}月${date.dayOfMonth.toString().padStart(2, '0')}日"
    } catch (e: Exception) {
        dateStr
    }
}
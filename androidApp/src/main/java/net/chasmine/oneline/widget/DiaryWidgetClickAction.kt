package net.chasmine.oneline.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.preferences.SettingsManagerFactory
import net.chasmine.oneline.widget.popup.DiaryWidgetEntryActivity
import net.chasmine.oneline.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class DiaryWidgetClickAction : ActionCallback {

    private val TAG = "DiaryWidgetClickAction"

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d(TAG, "=== Widget clicked ===")

        try {
            val settingsManager = SettingsManagerFactory.getInstance(context)

            // まず設定の有効性を確認
            val hasValidSettings = settingsManager.hasValidSettings.first()
            Log.d(TAG, "Has valid settings: $hasValidSettings")

            if (!hasValidSettings) {
                Log.d(TAG, "Opening MainActivity due to invalid settings")
                openMainActivity(context, true)
                return
            }

            // 設定が有効な場合、GitRepositoryを取得して強制的に初期化
            val gitRepository = GitRepository.getInstance(context)

            // 現在の初期化状態をログ出力
            val currentlyValid = gitRepository.isConfigValid()
            Log.d(TAG, "Repository currently valid: $currentlyValid")

            // 初期化されていない場合、強制的に初期化を実行
            if (!currentlyValid) {
                Log.d(TAG, "Repository not initialized, attempting initialization...")

                val remoteUrl = settingsManager.gitRepoUrl.first()
                val username = settingsManager.gitUsername.first()
                val password = settingsManager.gitToken.first()

                Log.d(TAG, "Settings - URL: ${remoteUrl.isNotEmpty()}, Username: ${username.isNotEmpty()}, Token: ${password.isNotEmpty()}")

                if (remoteUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                    val initResult = gitRepository.initRepository(remoteUrl, username, password)
                    Log.d(TAG, "Init result: ${initResult.isSuccess}")

                    if (initResult.isFailure) {
                        Log.e(TAG, "Failed to initialize repository", initResult.exceptionOrNull())
                        // 初期化に失敗してもダイアログは開く（オフライン動作）
                    }
                } else {
                    Log.w(TAG, "Settings are marked as valid but some values are empty")
                }
            }

            // 今日の日記エントリを取得（初期化に失敗してもファイルシステムから読み込みを試行）
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val todayDateStr = today.toString()
            Log.d(TAG, "Checking entry for date: $todayDateStr")

            // 初期化状態に関係なく、エントリ取得を試行
            val todayEntry = try {
                gitRepository.getEntry(todayDateStr)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get entry from repository, will create new", e)
                null
            }

            val entryContent = todayEntry?.content ?: ""
            val hasEntry = todayEntry != null

            Log.d(TAG, "Entry found: $hasEntry, content length: ${entryContent.length}")
            Log.d(TAG, "Opening DiaryWidgetEntryActivity")

            // ダイアログを開く（Git初期化の成功/失敗に関係なく）
            val intent = Intent(context, DiaryWidgetEntryActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("ENTRY_CONTENT", entryContent)
                putExtra("HAS_ENTRY", hasEntry)
                putExtra("ENTRY_DATE", todayDateStr)
                putExtra("REPOSITORY_INITIALIZED", gitRepository.isConfigValid()) // 初期化状態を渡す
            }

            context.startActivity(intent)
            Log.d(TAG, "Activity start requested")

        } catch (e: Exception) {
            Log.e(TAG, "Exception in widget click action", e)
            // 例外が発生した場合でも、ダイアログを開く試みを行う
            try {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val todayDateStr = today.toString()

                val intent = Intent(context, DiaryWidgetEntryActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("ENTRY_CONTENT", "")
                    putExtra("HAS_ENTRY", false)
                    putExtra("ENTRY_DATE", todayDateStr)
                    putExtra("REPOSITORY_INITIALIZED", false)
                }

                context.startActivity(intent)
                Log.d(TAG, "Fallback dialog opened")
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Fallback also failed, opening main activity", fallbackException)
                openMainActivity(context, false)
            }
        }
    }

    private fun openMainActivity(context: Context, openSettings: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (openSettings) {
                putExtra("OPEN_SETTINGS", true)
            }
        }
        context.startActivity(intent)
    }
}
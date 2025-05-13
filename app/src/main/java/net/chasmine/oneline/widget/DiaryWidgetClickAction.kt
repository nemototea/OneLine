package net.chasmine.oneline.widget

import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.widget.popup.DiaryWidgetEntryActivity
import net.chasmine.oneline.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class DiaryWidgetClickAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // 設定とGitリポジトリの状態をチェック
        val settingsManager = SettingsManager.getInstance(context)
        val gitRepository = GitRepository.getInstance(context)

        // Git連携が設定されているかチェック
        val hasValidSettings = settingsManager.hasValidSettings.first()

        if (!hasValidSettings) {
            // Git連携が設定されていない場合、アプリのメイン画面を起動
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("OPEN_SETTINGS", true)
            }
            context.startActivity(intent)
            return
        }

        // 今日の日記をチェック
        val todayEntry = gitRepository.getEntry(LocalDate.now().toString())

        // 専用の入力UI（ポップアップアクティビティ）を表示
        val intent = Intent(context, DiaryWidgetEntryActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("ENTRY_CONTENT", todayEntry?.content ?: "")
            putExtra("HAS_ENTRY", todayEntry != null)
        }
        context.startActivity(intent)
    }
}
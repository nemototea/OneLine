package net.chasmine.oneline.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import net.chasmine.oneline.R
import net.chasmine.oneline.ui.MainActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class DiaryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // ウィジェットのUIを定義
            JournalWidgetContent()
        }
    }

    @Composable
    private fun JournalWidgetContent() {
        // ウィジェットのテーマ設定
        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(
                        onClick = actionRunCallback<DiaryWidgetClickAction>()
                    ),
                contentAlignment = Alignment.Center
            ) {
                // プラスアイコン
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add_diary),
                    contentDescription = "Add note",
                    modifier = GlanceModifier.size(48.dp)
                )
            }
        }
    }
}

// ウィジェットプロバイダー（WidgetProviderに相当）
class JournalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DiaryWidget()

//    override fun onUpdate(
//        context: Context,
//        appWidgetManager: AppWidgetManager,
//        appWidgetIds: IntArray
//    ) {
//        super.onUpdate(context, appWidgetManager, appWidgetIds)
//
//        // プレビュー表示時にもウィジェットを適切に表示するための処理
//        val remoteViews = RemoteViews(context.packageName, R.layout.widget_preview_layout)
//        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
//
//        // Glanceウィジェットの更新も実行
//        MainScope().launch {
//            DiaryWidget().updateAll(context)
//        }
//    }
}
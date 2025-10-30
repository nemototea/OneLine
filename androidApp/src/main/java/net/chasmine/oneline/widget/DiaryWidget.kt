package net.chasmine.oneline.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import net.chasmine.oneline.R

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
}
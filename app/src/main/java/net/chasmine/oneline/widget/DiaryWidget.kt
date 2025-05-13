package net.chasmine.oneline.widget

import android.content.Context
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
import androidx.glance.appwidget.provideContent
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
            // ウィジェットのコンテナ
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_background))
                    .padding(12.dp)
                    .clickable(
                        onClick = actionStartActivity<MainActivity>(
                            parameters = actionParametersOf(
                                ActionParameters.Key<Boolean>("EXTRA_FROM_WIDGET") to true,
                                ActionParameters.Key<Boolean>("EXTRA_OPEN_NEW_ENTRY") to true
                            )
                        )
                    )
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 編集アイコン
                    Image(
                        provider = ImageProvider(R.drawable.ic_edit),
                        contentDescription = "Edit",
                        modifier = GlanceModifier.size(24.dp)
                    )

                    // テキスト
                    Text(
                        text = "今日の一行を書く",
                        style = TextStyle(
                            color = ColorProvider(Color.White)
                        ),
                        modifier = GlanceModifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

// ウィジェットプロバイダー（WidgetProviderに相当）
class JournalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DiaryWidget()
}
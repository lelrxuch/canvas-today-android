package gt.cris.canvastoday.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import gt.cris.canvastoday.R
import gt.cris.canvastoday.core.Planner
import gt.cris.canvastoday.core.PlannerItem
import gt.cris.canvastoday.data.Cache
import gt.cris.canvastoday.data.Repository
import gt.cris.canvastoday.ui.CanvasLinks
import gt.cris.canvastoday.ui.Labels
import gt.cris.canvastoday.ui.MainActivity
import java.time.ZonedDateTime

/** Widget de inicio: atrasadas, hoy y mañana, desde la caché. Repository lo actualiza tras cada sincronización. */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val now = ZonedDateTime.now()
        val configured = Repository.isConfigured(context)
        val shown = setOf(Planner.Group.OVERDUE, Planner.Group.TODAY, Planner.Group.TOMORROW)
        val list = Cache.load(context).items.filter { Planner.groupOf(it.due, now) in shown }
        provideContent {
            GlanceTheme {
                WidgetBody(context, configured, list, now)
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

@Composable
private fun WidgetBody(context: Context, configured: Boolean, list: List<PlannerItem>, now: ZonedDateTime) {
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(openApp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_title),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = list.size.toString(),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        when {
            !configured -> Hint(context.getString(R.string.widget_setup), openApp)
            list.isEmpty() -> Hint(context.getString(R.string.widget_empty), openApp)
            else -> LazyColumn {
                items(list, itemId = { it.id.hashCode().toLong() }) { item ->
                    WidgetRow(context, item, now)
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String, onClick: androidx.glance.action.Action) {
    Text(
        text = text,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
        modifier = GlanceModifier.fillMaxWidth().clickable(onClick),
    )
}

@Composable
private fun WidgetRow(context: Context, item: PlannerItem, now: ZonedDateTime) {
    val label = Planner.dueLabel(item.due, now)
    val action = actionStartActivity(
        item.url?.let { CanvasLinks.intentFor(context, it) } ?: Intent(context, MainActivity::class.java),
    )
    Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp).clickable(action)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.title ?: context.getString(R.string.untitled),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = Labels.due(context, label),
                style = TextStyle(
                    color = when {
                        label.isLate -> GlanceTheme.colors.error
                        label.isSoon -> GlanceTheme.colors.primary
                        else -> GlanceTheme.colors.onSurfaceVariant
                    },
                    fontSize = 13.sp,
                    fontWeight = if (label.isLate || label.isSoon) FontWeight.Bold else FontWeight.Normal,
                ),
            )
        }
        Text(
            text = Labels.course(context, item),
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
    }
}

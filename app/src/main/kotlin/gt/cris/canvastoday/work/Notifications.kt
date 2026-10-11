package gt.cris.canvastoday.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import gt.cris.canvastoday.R
import gt.cris.canvastoday.ui.MainActivity
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object Notifications {
    private const val CHANNEL_DUE = "due"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_DUE,
            context.getString(R.string.channel_due),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.channel_due_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun allowed(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun showDue(context: Context, id: String, title: String?, course: String?, due: Instant, url: String?) {
        if (!allowed(context)) return
        val left = Duration.between(Instant.now(), due)
        val leftText = if (left < Duration.ofHours(1)) {
            context.getString(R.string.notify_minutes, maxOf(1L, left.toMinutes()).toInt())
        } else {
            context.getString(R.string.notify_hours, Math.round(left.seconds / 3600.0).toInt())
        }
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(due.atZone(ZoneId.systemDefault()))
        val intent = url?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)) }
            ?: Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = context.getString(R.string.notify_due_in, leftText, time)
        val notification = NotificationCompat.Builder(context, CHANNEL_DUE)
            .setSmallIcon(R.drawable.ic_stat_due)
            .setContentTitle(title ?: context.getString(R.string.untitled))
            .setContentText(listOfNotNull(course, text).joinToString(" · "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(listOfNotNull(course, text).joinToString("\n")))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
        } catch (_: SecurityException) {
            // El permiso se revocó entre la comprobación y el aviso.
        }
    }
}

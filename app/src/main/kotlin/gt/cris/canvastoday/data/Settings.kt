package gt.cris.canvastoday.data

import android.content.Context

/** Ajustes simples en SharedPreferences. El token va aparte, en [TokenStore]. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var baseUrl: String?
        get() = prefs.getString("baseUrl", null)
        set(value) = prefs.edit().putString("baseUrl", value).apply()

    var notifyHours: Int
        get() = prefs.getInt("notifyHours", DEFAULT_NOTIFY_HOURS)
        set(value) = prefs.edit().putInt("notifyHours", value).apply()

    var daysAhead: Int
        get() = prefs.getInt("daysAhead", DEFAULT_DAYS_AHEAD)
        set(value) = prefs.edit().putInt("daysAhead", value).apply()

    val overdueDays: Int get() = OVERDUE_DAYS

    companion object {
        const val DEFAULT_NOTIFY_HOURS = 3
        const val DEFAULT_DAYS_AHEAD = 14
        const val OVERDUE_DAYS = 14
        val NOTIFY_OPTIONS = listOf(1, 3, 6, 12, 24)
        val DAYS_OPTIONS = listOf(7, 14, 30)
    }
}

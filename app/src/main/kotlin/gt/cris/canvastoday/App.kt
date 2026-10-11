package gt.cris.canvastoday

import android.app.Application
import gt.cris.canvastoday.work.Notifications
import gt.cris.canvastoday.work.SyncWorker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        SyncWorker.schedule(this)
    }
}

package app.linglongdingdong

import android.app.Application
import app.linglongdingdong.call.Notifications
import app.linglongdingdong.data.Store

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Notifications.createChannels(this)
    }
}

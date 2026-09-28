package app.linglongdingdong.call

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.linglongdingdong.data.Caller
import app.linglongdingdong.data.PendingCall
import app.linglongdingdong.data.Store

object CallScheduler {
    fun schedule(context: Context, caller: Caller, delaySeconds: Int) {
        val at = System.currentTimeMillis() + delaySeconds * 1000L
        Store.update { it.copy(pending = PendingCall(caller, at)) }
        if (delaySeconds == 0) {
            CallService.ring(context)
        } else {
            context.getSystemService(AlarmManager::class.java)
                .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent(context))
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
        Store.update { it.copy(pending = null) }
    }

    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, CallAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Exact alarms may start a foreground service from the background, so the call starts here. */
class CallAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CallService.ring(context)
    }
}

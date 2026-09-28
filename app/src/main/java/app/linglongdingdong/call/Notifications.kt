package app.linglongdingdong.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import app.linglongdingdong.MainActivity
import app.linglongdingdong.R
import app.linglongdingdong.data.Caller
import app.linglongdingdong.data.Store

object Notifications {
    const val CALL_ID = 1
    private const val MISSED_ID = 2
    private const val CHANNEL_INCOMING = "incoming"
    private const val CHANNEL_ONGOING = "ongoing"
    private const val CHANNEL_MISSED = "missed"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                // The service plays the ringtone and vibration itself, so the channel stays silent.
                NotificationChannel(CHANNEL_INCOMING, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(CHANNEL_ONGOING, "Ongoing calls", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CHANNEL_MISSED, "Missed calls", NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }

    fun incoming(context: Context, caller: Caller): Notification {
        val screen = CallActivity.intent(context)
        return Notification.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(R.drawable.ic_call)
            .setContentText(subtitle(caller))
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(screen)
            .setFullScreenIntent(screen, true)
            .setStyle(
                Notification.CallStyle.forIncomingCall(
                    person(caller),
                    CallService.pendingIntent(context, CallService.ACTION_DECLINE),
                    CallActivity.intent(context, answer = true),
                ),
            )
            .build()
    }

    fun ongoing(context: Context, caller: Caller, connectedAtWallClock: Long): Notification =
        Notification.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_call)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setWhen(connectedAtWallClock)
            .setContentIntent(CallActivity.intent(context))
            .setStyle(
                Notification.CallStyle.forOngoingCall(
                    person(caller),
                    CallService.pendingIntent(context, CallService.ACTION_HANG_UP),
                ),
            )
            .build()

    fun postMissed(context: Context, caller: Caller) {
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_call_missed)
            .setContentTitle("Missed call")
            .setContentText(caller.displayName)
            .setLargeIcon(photo(caller)?.let(Icon::createWithBitmap))
            .setCategory(Notification.CATEGORY_MISSED_CALL)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(MISSED_ID, notification)
    }

    private fun subtitle(caller: Caller) =
        listOf(caller.label, caller.number).filter { it.isNotBlank() }.joinToString(" ")

    private fun photo(caller: Caller): Bitmap? =
        caller.photo?.let { BitmapFactory.decodeFile(Store.photoFile(it).path) }

    private fun person(caller: Caller): Person = Person.Builder()
        .setName(caller.displayName)
        .setImportant(true)
        .apply { photo(caller)?.let { setIcon(Icon.createWithBitmap(it)) } }
        .build()
}

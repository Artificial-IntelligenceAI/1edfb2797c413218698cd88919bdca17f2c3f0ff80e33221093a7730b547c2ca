package app.linglongdingdong.call

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.net.toUri
import app.linglongdingdong.data.Caller
import app.linglongdingdong.data.Store
import app.linglongdingdong.glyph.GlyphSpinner
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Runs one fake call from ringing to hang-up. */
class CallService : Service() {
    private val scope = MainScope()
    private var ringer: Ringer? = null
    private var glyph: GlyphSpinner? = null
    private var voice: VoicePlayer? = null
    private var timeout: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> ring()
            ACTION_ANSWER -> answer()
            ACTION_DECLINE, ACTION_HANG_UP -> end(missed = false)
            ACTION_SPEAKER -> {
                val on = intent.getBooleanExtra(EXTRA_ON, false)
                CallState._speaker.value = on
                voice?.route(on)
            }
        }
        if (CallState.phase.value !is CallPhase.Ringing && CallState.phase.value !is CallPhase.Active) stopSelf()
        return START_NOT_STICKY
    }

    private fun ring() {
        val data = Store.data.value
        val caller = data.pending?.caller
        val busy = CallState.phase.value.let { it is CallPhase.Ringing || it is CallPhase.Active }
        if (caller == null || busy) {
            // startForegroundService() still has to be answered with startForeground().
            if (!busy) startForeground(Notifications.incoming(this, Caller()))
            return
        }
        Store.update { it.copy(pending = null) }
        startForeground(Notifications.incoming(this, caller))
        CallState._speaker.value = false
        CallState._phase.value = CallPhase.Ringing(caller)

        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LingLongDingDong:ring")
            .apply { acquire(RING_MS + 10_000) }
        ringer = Ringer(this, data.ringtone?.toUri(), data.vibrate).also { it.start() }
        glyph = GlyphSpinner(this).also { it.start(scope) }
        timeout = scope.launch {
            delay(RING_MS)
            end(missed = true)
        }
        // Shows the call screen straight away when the app is already open; otherwise the
        // system decides between the full-screen screen (locked) and a heads-up (in use).
        runCatching { startActivity(Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun answer() {
        val ringing = CallState.phase.value as? CallPhase.Ringing ?: return
        stopRinging()
        val caller = ringing.caller
        CallState._phase.value = CallPhase.Active(caller, SystemClock.elapsedRealtime())
        getSystemService(NotificationManager::class.java)
            .notify(Notifications.CALL_ID, Notifications.ongoing(this, caller, System.currentTimeMillis()))
        voice = VoicePlayer(this, caller).also { player ->
            player.connect(CallState.speaker.value)
            scope.launch {
                delay(VOICE_DELAY_MS)
                player.speak()
            }
        }
    }

    private fun end(missed: Boolean) {
        val phase = CallState.phase.value
        val caller = when (phase) {
            is CallPhase.Ringing -> phase.caller
            is CallPhase.Active -> phase.caller
            else -> return
        }
        stopRinging()
        voice?.release()
        voice = null
        CallState._phase.value = CallPhase.Ended(caller, wasAnswered = phase is CallPhase.Active)
        if (missed) Notifications.postMissed(this, caller)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopRinging() {
        timeout?.cancel()
        timeout = null
        ringer?.stop()
        ringer = null
        glyph?.stop()
        glyph = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun startForeground(notification: android.app.Notification) =
        startForeground(Notifications.CALL_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)

    override fun onDestroy() {
        end(missed = false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_RING = "ring"
        const val ACTION_ANSWER = "answer"
        const val ACTION_DECLINE = "decline"
        const val ACTION_HANG_UP = "hang_up"
        const val ACTION_SPEAKER = "speaker"
        private const val EXTRA_ON = "on"
        const val RING_MS = 2 * 60_000L
        private const val VOICE_DELAY_MS = 1200L

        fun ring(context: Context) =
            context.startForegroundService(Intent(context, CallService::class.java).setAction(ACTION_RING))

        fun send(context: Context, action: String) {
            context.startService(Intent(context, CallService::class.java).setAction(action))
        }

        fun setSpeaker(context: Context, on: Boolean) {
            context.startService(Intent(context, CallService::class.java).setAction(ACTION_SPEAKER).putExtra(EXTRA_ON, on))
        }

        fun pendingIntent(context: Context, action: String): PendingIntent = PendingIntent.getService(
            context, action.hashCode(), Intent(context, CallService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

package app.linglongdingdong.call

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.linglongdingdong.ui.call.CallTheme
import app.linglongdingdong.ui.call.InCallScreen
import app.linglongdingdong.ui.call.IncomingCallScreen
import kotlinx.coroutines.delay

/** Full-screen call UI, shown over the lock screen. */
class CallActivity : ComponentActivity() {
    private var proximity: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Both screens are dark, so the system bar icons stay light.
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handle(intent)
        setContent {
            CallTheme {
                val phase by CallState.phase.collectAsStateWithLifecycle()
                val speaker by CallState.speaker.collectAsStateWithLifecycle()
                when (val p = phase) {
                    is CallPhase.Ringing -> IncomingCallScreen(
                        caller = p.caller,
                        onAnswer = { CallService.send(this, CallService.ACTION_ANSWER) },
                        onDecline = { CallService.send(this, CallService.ACTION_DECLINE) },
                    )
                    is CallPhase.Active -> {
                        LaunchedEffect(Unit) { holdProximity(true) }
                        InCallScreen(
                            caller = p.caller,
                            connectedAt = p.connectedAt,
                            ended = false,
                            speaker = speaker,
                            onSpeaker = { CallService.setSpeaker(this, it) },
                            onHangUp = { CallService.send(this, CallService.ACTION_HANG_UP) },
                        )
                    }
                    is CallPhase.Ended -> {
                        if (p.wasAnswered) {
                            InCallScreen(p.caller, 0L, ended = true, speaker, {}, {})
                        }
                        LaunchedEffect(p) {
                            holdProximity(false)
                            if (p.wasAnswered) delay(1200)
                            finishAndRemoveTask()
                        }
                    }
                    CallPhase.Idle -> LaunchedEffect(Unit) { finishAndRemoveTask() }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onDestroy() {
        holdProximity(false)
        super.onDestroy()
    }

    private fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_ANSWER, false) == true) CallService.send(this, CallService.ACTION_ANSWER)
    }

    /** Blanks the screen when the phone is held to the ear, like a real call. */
    private fun holdProximity(on: Boolean) {
        if (on && proximity == null) {
            proximity = getSystemService(PowerManager::class.java)
                .takeIf { it.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) }
                ?.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "LingLongDingDong:proximity")
                ?.apply { acquire(60 * 60_000L) }
        } else if (!on) {
            proximity?.takeIf { it.isHeld }?.release()
            proximity = null
        }
    }

    companion object {
        private const val EXTRA_ANSWER = "answer"

        fun intent(context: Context, answer: Boolean = false): PendingIntent = PendingIntent.getActivity(
            context,
            if (answer) 1 else 0,
            Intent(context, CallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_ANSWER, answer),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

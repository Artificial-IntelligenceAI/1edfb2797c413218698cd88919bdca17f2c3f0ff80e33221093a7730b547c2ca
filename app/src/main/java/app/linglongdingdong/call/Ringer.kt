package app.linglongdingdong.call

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log

/** Rings and vibrates like a real call, honouring the phone's ring/vibrate/silent switch. */
class Ringer(private val context: Context, private val ringtone: Uri?, private val vibrate: Boolean) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(attributes)
        .build()
    private var player: MediaPlayer? = null

    fun start() {
        val mode = audio.ringerMode
        if (mode == AudioManager.RINGER_MODE_NORMAL) {
            audio.requestAudioFocus(focus)
            player = play(ringtone ?: Settings.System.DEFAULT_RINGTONE_URI)
                ?: play(Settings.System.DEFAULT_RINGTONE_URI)
        }
        if (vibrate && mode != AudioManager.RINGER_MODE_SILENT) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0),
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_RINGTONE),
            )
        }
    }

    fun stop() {
        player?.release()
        player = null
        vibrator.cancel()
        audio.abandonAudioFocusRequest(focus)
    }

    private fun play(uri: Uri): MediaPlayer? = try {
        MediaPlayer().apply {
            setAudioAttributes(attributes)
            setDataSource(context, uri)
            isLooping = true
            prepare()
            start()
        }
    } catch (e: Exception) {
        Log.w("Ringer", "Can't play $uri", e)
        null
    }
}

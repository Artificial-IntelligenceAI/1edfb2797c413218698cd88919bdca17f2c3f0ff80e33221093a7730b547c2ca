package app.linglongdingdong.call

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import app.linglongdingdong.data.Caller
import app.linglongdingdong.data.Store
import app.linglongdingdong.data.VoiceMode

/**
 * Call audio while a fake call is connected: the caller's voice, then the phone's own hang-up
 * sound, through the earpiece (or speaker), as if they were on the line.
 */
class VoicePlayer(private val context: Context, private val caller: Caller) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null

    fun connect(speaker: Boolean) {
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        route(speaker)
    }

    /** Starts the voice. Call after a short pause, like the other side saying hello. */
    fun speak() {
        when (caller.voice) {
            VoiceMode.Speech -> if (caller.speech.isNotBlank()) {
                var engine: TextToSpeech? = null
                engine = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        engine?.setAudioAttributes(attributes)
                        engine?.speak(caller.speech, TextToSpeech.QUEUE_FLUSH, null, "caller")
                    }
                }
                tts = engine
            }
            VoiceMode.Recording -> caller.recording?.let { name ->
                player = runCatching {
                    MediaPlayer().apply {
                        setAudioAttributes(attributes)
                        setDataSource(Store.recordingFile(name).path)
                        prepare()
                        start()
                    }
                }.getOrNull()
            }
            VoiceMode.None -> Unit
        }
    }

    fun route(speaker: Boolean) {
        val type = if (speaker) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        audio.availableCommunicationDevices.firstOrNull { it.type == type }?.let(audio::setCommunicationDevice)
    }

    /** Stops the voice, plays the hang-up sound, then hands the audio back. */
    fun hangUp(onDone: () -> Unit = {}) {
        stopVoice()
        val finish = {
            release()
            onDone()
        }
        val tone = endCallTone()
        if (tone != null) {
            player = tone
            tone.setOnCompletionListener { finish() }
            tone.start()
        } else {
            // Telecom's older behaviour: a short prompt beep from the tone generator.
            val generator = runCatching { ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80) }.getOrNull()
            generator?.startTone(ToneGenerator.TONE_PROP_PROMPT, FALLBACK_MS)
            Handler(Looper.getMainLooper()).postDelayed({
                generator?.release()
                finish()
            }, FALLBACK_MS + 100L)
        }
    }

    fun release() {
        stopVoice()
        audio.clearCommunicationDevice()
        audio.mode = AudioManager.MODE_NORMAL
    }

    private fun stopVoice() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        player?.release()
        player = null
    }

    /**
     * The phone's own call-ended sound. Android's Telecom service plays its `raw/endcall` resource
     * when a call ends, so load that (with any maker overlays) instead of shipping a copy.
     */
    @SuppressLint("DiscouragedApi")
    private fun endCallTone(): MediaPlayer? = try {
        val resources = context.packageManager.getResourcesForApplication(TELECOM)
        val id = resources.getIdentifier("endcall", "raw", TELECOM)
        if (id == 0) {
            null
        } else {
            resources.openRawResourceFd(id).use { fd ->
                MediaPlayer().apply {
                    setAudioAttributes(attributes)
                    setDataSource(fd)
                    prepare()
                }
            }
        }
    } catch (e: Exception) {
        Log.w("VoicePlayer", "No system end-call sound", e)
        null
    }

    companion object {
        private const val TELECOM = "com.android.server.telecom"
        private const val FALLBACK_MS = 200
    }
}

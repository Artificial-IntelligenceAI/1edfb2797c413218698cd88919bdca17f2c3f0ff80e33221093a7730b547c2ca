package app.linglongdingdong.call

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import app.linglongdingdong.data.Caller
import app.linglongdingdong.data.Store
import app.linglongdingdong.data.VoiceMode

/** Plays the caller's voice through the earpiece (or speaker), as if they were on the line. */
class VoicePlayer(private val context: Context, private val caller: Caller) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private val hasVoice = when (caller.voice) {
        VoiceMode.None -> false
        VoiceMode.Speech -> caller.speech.isNotBlank()
        VoiceMode.Recording -> caller.recording != null
    }

    fun connect(speaker: Boolean) {
        if (!hasVoice) return
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
        if (!hasVoice) return
        val type = if (speaker) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        audio.availableCommunicationDevices.firstOrNull { it.type == type }?.let(audio::setCommunicationDevice)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        player?.release()
        player = null
        if (hasVoice) {
            audio.clearCommunicationDevice()
            audio.mode = AudioManager.MODE_NORMAL
        }
    }
}

package app.linglongdingdong.ui.setup

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import app.linglongdingdong.data.Store
import java.util.UUID

/** Records the caller's voice clip into [Store.recordingsDir]. */
class Recorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    /** Returns the new clip's file name. */
    fun start(onMaxDuration: () -> Unit): String {
        stopPlayback()
        val name = "voice-${UUID.randomUUID()}.m4a"
        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setMaxDuration(MAX_MS)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) onMaxDuration()
            }
            setOutputFile(Store.recordingFile(name))
            prepare()
            start()
        }
        return name
    }

    /** Returns false if the clip was too short to keep. */
    fun stop(): Boolean {
        val ok = runCatching { recorder?.stop() }.isSuccess
        recorder?.release()
        recorder = null
        return ok
    }

    fun play(name: String, onDone: () -> Unit) {
        stopPlayback()
        player = runCatching {
            MediaPlayer().apply {
                setDataSource(Store.recordingFile(name).path)
                setOnCompletionListener { onDone() }
                prepare()
                start()
            }
        }.getOrElse { onDone(); null }
    }

    fun stopPlayback() {
        player?.release()
        player = null
    }

    fun release() {
        stop()
        stopPlayback()
    }

    companion object {
        const val MAX_MS = 60_000
    }
}

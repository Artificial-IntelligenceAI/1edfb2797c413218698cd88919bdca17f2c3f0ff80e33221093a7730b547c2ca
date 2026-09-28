package app.linglongdingdong.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class VoiceMode { None, Speech, Recording }

@Serializable
data class Caller(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val number: String = "",
    val label: String = "Mobile",
    /** File name inside [Store.photosDir]. */
    val photo: String? = null,
    val voice: VoiceMode = VoiceMode.None,
    val speech: String = "",
    /** File name inside [Store.recordingsDir]. */
    val recording: String? = null,
) {
    val displayName: String get() = name.ifBlank { number.ifBlank { "Unknown" } }
}

@Serializable
data class PendingCall(val caller: Caller, val at: Long)

@Serializable
data class AppData(
    val draft: Caller = Caller(),
    val presets: List<Caller> = emptyList(),
    val delaySeconds: Int = 10,
    /** Ringtone URI, or null for the phone's default ringtone. */
    val ringtone: String? = null,
    val vibrate: Boolean = true,
    val pending: PendingCall? = null,
)

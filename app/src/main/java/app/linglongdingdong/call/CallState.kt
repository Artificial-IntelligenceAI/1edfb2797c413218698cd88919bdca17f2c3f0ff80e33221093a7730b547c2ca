package app.linglongdingdong.call

import app.linglongdingdong.data.Caller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface CallPhase {
    data object Idle : CallPhase
    data class Ringing(val caller: Caller) : CallPhase
    /** [connectedAt] is in [android.os.SystemClock.elapsedRealtime] time. */
    data class Active(val caller: Caller, val connectedAt: Long) : CallPhase
    data class Ended(val caller: Caller, val wasAnswered: Boolean) : CallPhase
}

/** The one fake call that can be in progress, observed by [CallActivity]. */
object CallState {
    internal val _phase = MutableStateFlow<CallPhase>(CallPhase.Idle)
    val phase: StateFlow<CallPhase> = _phase

    internal val _speaker = MutableStateFlow(false)
    val speaker: StateFlow<Boolean> = _speaker
}

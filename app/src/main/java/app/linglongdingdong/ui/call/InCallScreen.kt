package app.linglongdingdong.ui.call

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.linglongdingdong.data.Caller
import app.linglongdingdong.ui.Avatar
import app.linglongdingdong.ui.Icons
import kotlinx.coroutines.delay

/** Google Phone-style in-call screen. Only the speaker button changes anything; the rest is for show. */
@Composable
fun InCallScreen(
    caller: Caller,
    connectedAt: Long,
    ended: Boolean,
    speaker: Boolean,
    onSpeaker: (Boolean) -> Unit,
    onHangUp: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var muted by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf(false) }
    var keypad by remember { mutableStateOf(false) }
    var digits by remember { mutableStateOf("") }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(ended) {
        while (!ended) {
            now = SystemClock.elapsedRealtime()
            delay(250)
        }
    }
    val status = when {
        ended -> "Call ended"
        held -> "On hold"
        else -> formatDuration(now - connectedAt)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surfaceContainerLowest)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        Text(
            caller.displayName,
            color = colors.onSurface,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(status, color = colors.onSurfaceVariant, fontSize = 16.sp)
        if (keypad) {
            Spacer(Modifier.height(24.dp))
            Text(digits, color = colors.onSurface, fontSize = 28.sp, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Keypad(onDigit = { digits = (digits + it).takeLast(20) })
        } else {
            Spacer(Modifier.height(40.dp))
            Avatar(caller, 112.dp)
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CallButton(Icons.Dialpad, "Keypad", false) { keypad = true }
                CallButton(Icons.MicOff, "Mute", muted) { muted = !muted }
                CallButton(Icons.Speaker, "Speaker", speaker) { onSpeaker(!speaker) }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CallButton(Icons.Add, "Add call", false) {}
                CallButton(Icons.Pause, "Hold", held) { held = !held }
                CallButton(Icons.Videocam, "Video call", false) {}
            }
        }
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(96.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (ended) DeclineRed.copy(alpha = 0.4f) else DeclineRed)
                    .clickable(enabled = !ended, onClick = onHangUp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.CallEnd, "End call", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            if (keypad) {
                Text(
                    "Hide",
                    color = colors.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { keypad = false }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun CallButton(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (on) colors.primary else colors.surfaceContainerHigh)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = if (on) colors.onPrimary else colors.onSurface, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = colors.onSurface, fontSize = 13.sp, maxLines = 1)
    }
}

private val keys = listOf(
    "1" to "", "2" to "ABC", "3" to "DEF",
    "4" to "GHI", "5" to "JKL", "6" to "MNO",
    "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
    "*" to "", "0" to "+", "#" to "",
)

private val dtmf = mapOf(
    "1" to ToneGenerator.TONE_DTMF_1, "2" to ToneGenerator.TONE_DTMF_2, "3" to ToneGenerator.TONE_DTMF_3,
    "4" to ToneGenerator.TONE_DTMF_4, "5" to ToneGenerator.TONE_DTMF_5, "6" to ToneGenerator.TONE_DTMF_6,
    "7" to ToneGenerator.TONE_DTMF_7, "8" to ToneGenerator.TONE_DTMF_8, "9" to ToneGenerator.TONE_DTMF_9,
    "*" to ToneGenerator.TONE_DTMF_S, "0" to ToneGenerator.TONE_DTMF_0, "#" to ToneGenerator.TONE_DTMF_P,
)

@Composable
private fun Keypad(onDigit: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tones = remember { runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 60) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tones?.release() } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { (digit, letters) ->
                    Column(
                        Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceContainerHigh)
                            .clickable {
                                tones?.startTone(dtmf.getValue(digit), 120)
                                onDigit(digit)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(digit, color = colors.onSurface, fontSize = 28.sp)
                        if (letters.isNotEmpty()) Text(letters, color = colors.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

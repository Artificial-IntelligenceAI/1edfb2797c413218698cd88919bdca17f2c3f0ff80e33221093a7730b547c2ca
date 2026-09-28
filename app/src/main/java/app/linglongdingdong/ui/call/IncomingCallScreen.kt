package app.linglongdingdong.ui.call

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.linglongdingdong.data.Caller
import app.linglongdingdong.ui.Avatar
import app.linglongdingdong.ui.Icons
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Pixel / Google Phone-style incoming call: swipe up to answer, down to decline. */
@Composable
fun IncomingCallScreen(caller: Caller, onAnswer: () -> Unit, onDecline: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surfaceContainerLowest)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        Text("Incoming call", color = colors.onSurfaceVariant, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            caller.displayName,
            color = colors.onSurface,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val subtitle = listOf(caller.label, caller.number).filter { it.isNotBlank() }.joinToString(" ")
        if (caller.name.isNotBlank() && subtitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = colors.onSurfaceVariant, fontSize = 16.sp)
        }
        Spacer(Modifier.height(40.dp))
        Avatar(caller, 128.dp)
        Spacer(Modifier.weight(1f))
        AnswerSlider(onAnswer, onDecline)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun AnswerSlider(onAnswer: () -> Unit, onDecline: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { 150.dp.toPx() }
    var dragging by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    // Idle nudge, like the Pixel answer button hopping to hint at the swipe.
    val hop by rememberInfiniteTransition(label = "hop").animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1600
                0f at 0
                -18f at 250
                0f at 500
                -9f at 700
                0f at 900
            },
            RepeatMode.Restart,
        ),
        label = "hop",
    )
    val arrows by rememberInfiniteTransition(label = "arrows").animateFloat(
        0.25f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "arrows",
    )

    val hopPx = with(LocalDensity.current) { hop.dp.toPx() }
    val progress = (offset.value / threshold).coerceIn(-1f, 1f)
    val buttonColor = if (progress > 0) lerp(AnswerGreen, DeclineRed, progress) else AnswerGreen
    val hintAlpha = 1f - abs(progress)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Swipe up to answer", color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.alpha(hintAlpha))
        Spacer(Modifier.height(10.dp))
        Chevrons(up = true, alpha = arrows * hintAlpha)
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .offset { IntOffset(0, (offset.value + if (dragging || done) 0f else hopPx).roundToInt()) }
                .size(76.dp)
                .clip(CircleShape)
                .background(buttonColor)
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = !done,
                    state = rememberDraggableState { delta -> scope.launch { offset.snapTo(offset.value + delta) } },
                    onDragStarted = { dragging = true },
                    onDragStopped = {
                        dragging = false
                        when {
                            offset.value < -threshold -> { done = true; onAnswer() }
                            offset.value > threshold -> { done = true; onDecline() }
                            else -> offset.animateTo(0f, spring(dampingRatio = 0.5f))
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Call,
                contentDescription = "Answer",
                tint = Color.White,
                modifier = Modifier.size(34.dp).rotate(progress.coerceAtLeast(0f) * 135f),
            )
        }
        Spacer(Modifier.height(18.dp))
        Chevrons(up = false, alpha = arrows * hintAlpha)
        Spacer(Modifier.height(10.dp))
        Text("Swipe down to decline", color = colors.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.alpha(hintAlpha))
    }
}

@Composable
private fun Chevrons(up: Boolean, alpha: Float) {
    Text(
        if (up) "︿" else "﹀",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 18.sp,
        modifier = Modifier.alpha(alpha),
    )
}

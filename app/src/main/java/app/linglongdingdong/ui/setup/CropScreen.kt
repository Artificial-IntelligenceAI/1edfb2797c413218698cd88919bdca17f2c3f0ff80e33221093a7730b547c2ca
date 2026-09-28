package app.linglongdingdong.ui.setup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.linglongdingdong.data.Store
import app.linglongdingdong.ui.theme.Dot
import app.linglongdingdong.ui.theme.Nothing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Pinch and drag the picked image inside a circle, then save it as the caller's photo. */
@Composable
fun CropScreen(uri: Uri, onDone: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmap by produceState<Result<Bitmap>?>(null, uri) {
        value = withContext(Dispatchers.IO) { runCatching { decode(context, uri) } }
    }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var diameter by remember { mutableFloatStateOf(0f) }

    Column(Modifier.fillMaxSize().background(Nothing.Black).systemBarsPadding()) {
        Text("CROP PHOTO", fontFamily = Dot, fontSize = 22.sp, color = Nothing.White, modifier = Modifier.padding(20.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val result = bitmap
            val bmp = result?.getOrNull()
            when {
                result == null -> Text("LOADING", fontFamily = Dot, color = Nothing.Grey)
                bmp == null -> Text("CAN'T OPEN THIS IMAGE", fontFamily = Dot, color = Nothing.Grey)
                else -> {
                    val d = min(constraints.maxWidth, constraints.maxHeight) * 0.82f
                    diameter = d
                    val base = d / min(bmp.width, bmp.height)
                    val image = remember(bmp) { bmp.asImageBitmap() }
                    fun clamp(o: Offset, z: Float): Offset {
                        val maxX = (bmp.width * base * z - d) / 2
                        val maxY = (bmp.height * base * z - d) / 2
                        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
                    }
                    Canvas(
                        Modifier.fillMaxSize().pointerInput(bmp) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                zoom = (zoom * gestureZoom).coerceIn(1f, 8f)
                                offset = clamp(offset + pan, zoom)
                            }
                        },
                    ) {
                        val s = base * zoom
                        val w = bmp.width * s
                        val h = bmp.height * s
                        val topLeft = center + offset - Offset(w / 2, h / 2)
                        drawImage(
                            image,
                            dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                            dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                            filterQuality = FilterQuality.High,
                        )
                        val mask = Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(Rect(Offset.Zero, size))
                            addOval(Rect(center, d / 2))
                        }
                        drawPath(mask, Color.Black.copy(alpha = 0.7f))
                        drawCircle(Nothing.White, radius = d / 2, style = Stroke(1.dp.toPx()))
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NothingButton("CANCEL", Modifier.weight(1f), filled = false, onClick = onCancel)
            NothingButton("USE PHOTO", Modifier.weight(1f), enabled = bitmap?.isSuccess == true) {
                val bmp = bitmap?.getOrNull() ?: return@NothingButton
                val s = min(bmp.width, bmp.height).let { diameter / it } * zoom
                val cx = bmp.width / 2f - offset.x / s
                val cy = bmp.height / 2f - offset.y / s
                val r = diameter / 2 / s
                scope.launch {
                    val name = withContext(Dispatchers.IO) { save(bmp, RectF(cx - r, cy - r, cx + r, cy + r)) }
                    onDone(name)
                }
            }
        }
    }
}

private fun decode(context: Context, uri: Uri): Bitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val longest = max(info.size.width, info.size.height)
        if (longest > MAX_SOURCE) {
            val f = MAX_SOURCE.toFloat() / longest
            decoder.setTargetSize((info.size.width * f).roundToInt(), (info.size.height * f).roundToInt())
        }
    }

private fun save(source: Bitmap, crop: RectF): String {
    val out = Bitmap.createBitmap(OUTPUT, OUTPUT, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(out).drawBitmap(
        source,
        android.graphics.Rect(crop.left.roundToInt(), crop.top.roundToInt(), crop.right.roundToInt(), crop.bottom.roundToInt()),
        android.graphics.Rect(0, 0, OUTPUT, OUTPUT),
        Paint(Paint.FILTER_BITMAP_FLAG),
    )
    val name = "photo-${UUID.randomUUID()}.jpg"
    Store.photoFile(name).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    return name
}

private const val MAX_SOURCE = 2048
private const val OUTPUT = 512


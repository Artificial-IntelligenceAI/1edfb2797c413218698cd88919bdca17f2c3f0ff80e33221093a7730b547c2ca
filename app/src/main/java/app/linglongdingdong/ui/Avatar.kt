package app.linglongdingdong.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import app.linglongdingdong.data.Caller
import app.linglongdingdong.data.Store

fun loadPhoto(name: String?): ImageBitmap? =
    name?.let { BitmapFactory.decodeFile(Store.photoFile(it).path)?.asImageBitmap() }

// Google Phone-style letter avatar colours.
private val letterColors = listOf(
    Color(0xFFD3E3FD), Color(0xFFC4EED0), Color(0xFFFFDCC1), Color(0xFFF6D6F7), Color(0xFFFFD8E4), Color(0xFFC2E7FF),
)
private val letterInk = listOf(
    Color(0xFF041E49), Color(0xFF072711), Color(0xFF2E1500), Color(0xFF2F0E33), Color(0xFF3E001D), Color(0xFF001D35),
)

@Composable
fun Avatar(caller: Caller, size: Dp, modifier: Modifier = Modifier) {
    val photo = remember(caller.photo) { loadPhoto(caller.photo) }
    if (photo != null) {
        Image(photo, null, modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        val i = (caller.displayName.hashCode() and 0x7fffffff) % letterColors.size
        Box(modifier.size(size).clip(CircleShape).background(letterColors[i]), contentAlignment = Alignment.Center) {
            Text(
                caller.displayName.first().uppercase(),
                color = letterInk[i],
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}

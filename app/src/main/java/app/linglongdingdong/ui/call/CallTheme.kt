package app.linglongdingdong.ui.call

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Google Phone draws its call screen with Material You colours in dark mode. */
@Composable
fun CallTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = dynamicDarkColorScheme(LocalContext.current), content = content)
}

val AnswerGreen = Color(0xFF1E8E3E)
val DeclineRed = Color(0xFFDC362E)

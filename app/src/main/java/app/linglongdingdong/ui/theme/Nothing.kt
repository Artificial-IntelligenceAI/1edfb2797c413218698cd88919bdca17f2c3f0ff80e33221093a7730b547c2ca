package app.linglongdingdong.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.linglongdingdong.R

/** Nothing-style palette for the setup screen. */
object Nothing {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF111111)
    val Line = Color(0xFF333333)
    val White = Color(0xFFFFFFFF)
    val Grey = Color(0xFF8A8A8A)
    val Red = Color(0xFFD71921)
}

/** Doto (SIL OFL), a dot-matrix face, with round dots to echo Nothing's type. */
@OptIn(ExperimentalTextApi::class)
val Dot = FontFamily(
    Font(
        R.font.doto,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800), FontVariation.Setting("ROND", 100f)),
    ),
)

@Composable
fun NothingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Nothing.Red,
            onPrimary = Nothing.White,
            background = Nothing.Black,
            onBackground = Nothing.White,
            surface = Nothing.Black,
            onSurface = Nothing.White,
            surfaceVariant = Nothing.Surface,
            onSurfaceVariant = Nothing.Grey,
            outline = Nothing.Line,
            surfaceContainerHigh = Nothing.Surface,
        ),
        content = content,
    )
}

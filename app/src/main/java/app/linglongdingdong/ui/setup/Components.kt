package app.linglongdingdong.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.linglongdingdong.ui.theme.Dot
import app.linglongdingdong.ui.theme.Nothing

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, fontFamily = Dot, fontSize = 15.sp, color = Nothing.Grey, modifier = modifier)
}

@Composable
fun NothingButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .then(if (filled) Modifier.background(Nothing.Red) else Modifier.border(BorderStroke(1.dp, Nothing.Line), shape))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontFamily = Dot, fontSize = 18.sp, color = Nothing.White)
    }
}

@Composable
fun NothingChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(Nothing.White)
                else Modifier.border(BorderStroke(1.dp, Nothing.Line), shape),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(text, fontFamily = Dot, fontSize = 15.sp, color = if (selected) Nothing.Black else Nothing.White)
    }
}

@Composable
fun NothingField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Nothing.Grey) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        // Single-line boxes finish on Enter; the multi-line box keeps Enter for new lines.
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Nothing.White,
            unfocusedBorderColor = Nothing.Line,
            cursorColor = Nothing.Red,
            focusedTextColor = Nothing.White,
            unfocusedTextColor = Nothing.White,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

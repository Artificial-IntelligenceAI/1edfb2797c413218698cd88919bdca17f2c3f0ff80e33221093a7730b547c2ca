package app.linglongdingdong.ui.setup

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.linglongdingdong.call.CallPhase
import app.linglongdingdong.call.CallScheduler
import app.linglongdingdong.call.CallState
import app.linglongdingdong.data.AppData
import app.linglongdingdong.data.Caller
import app.linglongdingdong.data.Store
import app.linglongdingdong.data.VoiceMode
import app.linglongdingdong.glyph.GlyphSpinner
import app.linglongdingdong.ui.Avatar
import app.linglongdingdong.ui.Icons
import app.linglongdingdong.ui.theme.Dot
import app.linglongdingdong.ui.theme.Nothing
import kotlinx.coroutines.delay

@Composable
fun SetupApp() {
    var cropping by remember { mutableStateOf<Uri?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) cropping = uri
    }
    cropping?.let { uri ->
        BackHandler { cropping = null }
        CropScreen(
            uri = uri,
            onDone = { name ->
                editDraft { it.copy(photo = name) }
                cropping = null
            },
            onCancel = { cropping = null },
        )
        return
    }
    SetupScreen(onPickPhoto = { pickImage.launch(arrayOf("image/*")) })
}

private fun editDraft(transform: (Caller) -> Caller) = Store.update { it.copy(draft = transform(it.draft)) }

@Composable
private fun SetupScreen(onPickPhoto: () -> Unit) {
    val context = LocalContext.current
    val data by Store.data.collectAsStateWithLifecycle()
    val phase by CallState.phase.collectAsStateWithLifecycle()
    val draft = data.draft

    var notificationsAllowed by remember { mutableStateOf(true) }
    var fullScreenAllowed by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        fullScreenAllowed = context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        onPauseOrDispose {}
    }
    var scheduleAfterPermission by remember { mutableStateOf(false) }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
        if (granted && scheduleAfterPermission) CallScheduler.schedule(context, Store.data.value.draft, Store.data.value.delaySeconds)
        scheduleAfterPermission = false
    }

    Box(Modifier.fillMaxSize().background(Nothing.Black)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Header()
            if (!notificationsAllowed) {
                Notice("Notifications are off, so calls can't ring.", "ALLOW") {
                    requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (!fullScreenAllowed) {
                Notice("Full-screen calls are off, so calls can't wake a locked phone.", "OPEN") {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${context.packageName}".toUri()),
                    )
                }
            }
            Status(data, phase)

            SectionLabel("CALLER", Modifier.padding(top = 28.dp, bottom = 12.dp))
            CallerEditor(draft, onPickPhoto)

            SectionLabel("VOICE WHEN ANSWERED", Modifier.padding(top = 28.dp, bottom = 12.dp))
            VoiceEditor(draft)

            SectionLabel("CALL IN", Modifier.padding(top = 28.dp, bottom = 12.dp))
            DelayPicker(data.delaySeconds)

            SectionLabel("RING", Modifier.padding(top = 28.dp, bottom = 4.dp))
            RingSettings(data)

            SectionLabel("SAVED CALLERS", Modifier.padding(top = 28.dp, bottom = 12.dp))
            SavedCallers(data)

            Spacer(Modifier.height(120.dp))
        }

        val busy = phase is CallPhase.Ringing || phase is CallPhase.Active
        NothingButton(
            text = if (data.delaySeconds == 0) "CALL NOW" else "CALL IN ${formatDelay(data.delaySeconds)}",
            enabled = !busy,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(20.dp)
                .fillMaxWidth(),
        ) {
            if (notificationsAllowed) {
                CallScheduler.schedule(context, data.draft, data.delaySeconds)
            } else {
                scheduleAfterPermission = true
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun Header() {
    Row(Modifier.padding(top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
        Text("LINGLONG\nDINGDONG", fontFamily = Dot, fontSize = 44.sp, lineHeight = 44.sp, color = Nothing.White)
        Box(Modifier.padding(start = 6.dp, top = 8.dp).size(10.dp).clip(CircleShape).background(Nothing.Red))
    }
}

@Composable
private fun Notice(text: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Nothing.Red), RoundedCornerShape(16.dp))
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Nothing.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action, fontFamily = Dot, color = Nothing.Red, fontSize = 16.sp) }
    }
}

/** Countdown to the scheduled call, or the call in progress. */
@Composable
private fun Status(data: AppData, phase: CallPhase) {
    val context = LocalContext.current
    val pending = data.pending
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pending) {
        while (pending != null) {
            now = System.currentTimeMillis()
            delay(200)
        }
    }
    val (headline, detail) = when {
        phase is CallPhase.Ringing -> "RINGING" to phase.caller.displayName
        phase is CallPhase.Active -> "ON A CALL" to phase.caller.displayName
        pending != null && pending.at > now -> formatCountdown(pending.at - now) to "until ${pending.caller.displayName} calls"
        else -> return
    }
    Row(
        Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Nothing.Surface)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(headline, fontFamily = Dot, fontSize = 40.sp, color = Nothing.White)
            Text(detail, color = Nothing.Grey, fontSize = 14.sp)
        }
        if (pending != null && phase !is CallPhase.Ringing && phase !is CallPhase.Active) {
            NothingButton("CANCEL", filled = false) { CallScheduler.cancel(context) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CallerEditor(draft: Caller, onPickPhoto: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(1.dp, Nothing.Line), CircleShape)
                    .clickable(onClick = onPickPhoto),
                contentAlignment = Alignment.Center,
            ) {
                if (draft.photo != null) Avatar(draft, 96.dp)
                else Text("+\nPHOTO", fontFamily = Dot, fontSize = 15.sp, color = Nothing.Grey, lineHeight = 17.sp, textAlign = TextAlign.Center)
            }
            if (draft.photo != null) {
                Text(
                    "REMOVE",
                    fontFamily = Dot,
                    fontSize = 13.sp,
                    color = Nothing.Grey,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { editDraft { it.copy(photo = null) } }
                        .padding(6.dp),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NothingField(draft.name, { v -> editDraft { it.copy(name = v) } }, "Name")
            NothingField(draft.number, { v -> editDraft { it.copy(number = v) } }, "Number", keyboardType = KeyboardType.Phone)
        }
    }
    Spacer(Modifier.height(12.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Mobile", "Work", "Home").forEach { label ->
            NothingChip(label.uppercase(), draft.label == label) { editDraft { it.copy(label = label) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceEditor(draft: Caller) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NothingChip("SILENT", draft.voice == VoiceMode.None) { editDraft { it.copy(voice = VoiceMode.None) } }
        NothingChip("SPEAK TEXT", draft.voice == VoiceMode.Speech) { editDraft { it.copy(voice = VoiceMode.Speech) } }
        NothingChip("MY RECORDING", draft.voice == VoiceMode.Recording) { editDraft { it.copy(voice = VoiceMode.Recording) } }
    }
    Spacer(Modifier.height(12.dp))
    when (draft.voice) {
        VoiceMode.None -> Hint("Nobody speaks when you answer.")
        VoiceMode.Speech -> NothingField(
            draft.speech,
            { v -> editDraft { it.copy(speech = v) } },
            "Hey, it's me. Can you come out right now? It's urgent.",
            singleLine = false,
        )
        VoiceMode.Recording -> RecordingEditor(draft)
    }
    if (draft.voice != VoiceMode.None) Hint("Plays from the earpiece a second after you answer.")
}

@Composable
private fun RecordingEditor(draft: Caller) {
    val context = LocalContext.current
    val recorder = remember { Recorder(context) }
    DisposableEffect(Unit) { onDispose { recorder.release() } }
    var recording by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(recording) {
        elapsed = 0
        while (recording) {
            delay(1000)
            elapsed++
        }
    }
    fun stopRecording(name: String?) {
        recording = false
        val kept = recorder.stop()
        if (kept && name != null) editDraft { it.copy(recording = name) }
    }
    var current by remember { mutableStateOf<String?>(null) }
    fun startRecording() {
        playing = false
        current = recorder.start(onMaxDuration = { stopRecording(current) })
        recording = true
    }
    val requestMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording()
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIcon(if (recording) Icons.Stop else Icons.Mic, if (recording) "Stop" else "Record", red = true) {
            when {
                recording -> stopRecording(current)
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED -> startRecording()
                else -> requestMic.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        if (draft.recording != null && !recording) {
            RoundIcon(if (playing) Icons.Stop else Icons.Play, if (playing) "Stop" else "Play") {
                if (playing) {
                    recorder.stopPlayback()
                    playing = false
                } else {
                    playing = true
                    recorder.play(draft.recording) { playing = false }
                }
            }
            RoundIcon(Icons.Close, "Delete recording") { editDraft { it.copy(recording = null) } }
        }
        Text(
            when {
                recording -> "RECORDING 0:%02d".format(elapsed)
                draft.recording != null -> "CLIP READY"
                else -> "TAP TO RECORD"
            },
            fontFamily = Dot,
            fontSize = 15.sp,
            color = if (recording) Nothing.Red else Nothing.Grey,
        )
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, red: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .then(if (red) Modifier.background(Nothing.Red) else Modifier.border(BorderStroke(1.dp, Nothing.Line), CircleShape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = Nothing.White, modifier = Modifier.size(24.dp))
    }
}

private val delays = listOf(0, 10, 30, 60, 300)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DelayPicker(selected: Int) {
    var custom by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        delays.forEach { seconds ->
            NothingChip(if (seconds == 0) "NOW" else formatDelay(seconds), selected == seconds) {
                Store.update { it.copy(delaySeconds = seconds) }
            }
        }
        NothingChip(if (selected in delays) "CUSTOM" else formatDelay(selected), selected !in delays) { custom = true }
    }
    if (custom) CustomDelayDialog(selected, onDismiss = { custom = false }) { seconds ->
        Store.update { it.copy(delaySeconds = seconds) }
        custom = false
    }
}

@Composable
private fun CustomDelayDialog(initial: Int, onDismiss: () -> Unit, onSet: (Int) -> Unit) {
    var minutes by remember { mutableStateOf((initial / 60).toString()) }
    var seconds by remember { mutableStateOf((initial % 60).toString()) }
    val total = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nothing.Surface,
        title = { Text("CALL IN", fontFamily = Dot, color = Nothing.White) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NothingField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, "Min", Modifier.weight(1f), KeyboardType.Number)
                NothingField(seconds, { seconds = it.filter(Char::isDigit).take(2) }, "Sec", Modifier.weight(1f), KeyboardType.Number)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSet(total) }, enabled = total in 1..(24 * 3600)) {
                Text("SET", fontFamily = Dot, color = Nothing.Red, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", fontFamily = Dot, color = Nothing.Grey, fontSize = 16.sp) }
        },
    )
}

@Composable
private fun RingSettings(data: AppData) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickRingtone = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            ?: return@rememberLauncherForActivityResult
        Store.update { it.copy(ringtone = if (uri == Settings.System.DEFAULT_RINGTONE_URI) null else uri.toString()) }
    }
    val title = remember(data.ringtone) { ringtoneTitle(context, data.ringtone) }

    SettingRow("Ringtone", title) {
        pickRingtone.launch(
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    data.ringtone?.toUri() ?: Settings.System.DEFAULT_RINGTONE_URI,
                ),
        )
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Vibrate", color = Nothing.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = data.vibrate,
            onCheckedChange = { on -> Store.update { it.copy(vibrate = on) } },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Nothing.White,
                checkedTrackColor = Nothing.Red,
                uncheckedThumbColor = Nothing.Grey,
                uncheckedTrackColor = Nothing.Black,
                uncheckedBorderColor = Nothing.Line,
            ),
        )
    }
    Hint("Follows your ring, vibrate and silent switch, like a real call.")
    if (GlyphSpinner.supported) {
        val spinner = remember { GlyphSpinner(context) }
        var previewing by remember { mutableStateOf(false) }
        DisposableEffect(Unit) { onDispose { spinner.stop() } }
        LaunchedEffect(previewing) {
            if (previewing) {
                spinner.start(scope, solidForMs = 3000)
                delay(GlyphSpinner.SPIN_MS + 3500)
                spinner.stop()
                previewing = false
            }
        }
        Spacer(Modifier.height(12.dp))
        NothingButton(if (previewing) "STOP GLYPH PREVIEW" else "PREVIEW GLYPH", filled = false, modifier = Modifier.fillMaxWidth()) {
            if (previewing) spinner.stop()
            previewing = !previewing
        }
        Hint("Spins faster and faster for 20 seconds, then stays lit until you answer.")
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Nothing.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = Nothing.Grey, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun SavedCallers(data: AppData) {
    val draft = data.draft
    val saved = data.presets.any { it.id == draft.id }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NothingButton(if (saved) "UPDATE SAVED" else "SAVE CALLER", filled = false, modifier = Modifier.weight(1f)) {
            Store.update { d ->
                val presets = if (d.presets.any { it.id == d.draft.id }) {
                    d.presets.map { if (it.id == d.draft.id) d.draft else it }
                } else {
                    d.presets + d.draft
                }
                d.copy(presets = presets)
            }
        }
        NothingButton("NEW", filled = false) { Store.update { it.copy(draft = Caller()) } }
    }
    Spacer(Modifier.height(8.dp))
    if (data.presets.isEmpty()) Hint("Save callers like Mom or Boss to set them up in one tap.")
    data.presets.forEach { preset ->
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (preset.id == draft.id) Nothing.Surface else Nothing.Black)
                .clickable { Store.update { it.copy(draft = preset) } }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(preset, 44.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(preset.displayName, color = Nothing.White, fontSize = 16.sp, maxLines = 1)
                if (preset.number.isNotBlank()) Text(preset.number, color = Nothing.Grey, fontSize = 13.sp, maxLines = 1)
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable {
                    Store.update { d -> d.copy(presets = d.presets.filterNot { it.id == preset.id }) }
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Close, "Delete ${preset.displayName}", tint = Nothing.Grey, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Nothing.Grey, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
}

private fun ringtoneTitle(context: Context, uri: String?): String =
    runCatching { RingtoneManager.getRingtone(context, uri?.toUri() ?: Settings.System.DEFAULT_RINGTONE_URI)?.getTitle(context) }
        .getOrNull() ?: if (uri == null) "Default" else "Custom"

private fun formatDelay(seconds: Int): String = when {
    seconds < 60 -> "${seconds}S"
    seconds % 60 == 0 -> "${seconds / 60}M"
    else -> "${seconds / 60}M ${seconds % 60}S"
}

private fun formatCountdown(ms: Long): String {
    val total = (ms + 999) / 1000
    return "%02d:%02d".format(total / 60, total % 60)
}

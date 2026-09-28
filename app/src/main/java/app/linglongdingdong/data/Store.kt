package app.linglongdingdong.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.Executors

/** App state kept in memory and mirrored to a JSON file. The UI and the call service share it. */
object Store {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val io = Executors.newSingleThreadExecutor()
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    private lateinit var file: File
    lateinit var photosDir: File
        private set
    lateinit var recordingsDir: File
        private set

    fun init(context: Context) {
        val dir = context.filesDir
        file = File(dir, "data.json")
        photosDir = File(dir, "photos").apply { mkdirs() }
        recordingsDir = File(dir, "recordings").apply { mkdirs() }
        var loaded = runCatching { json.decodeFromString<AppData>(file.readText()) }.getOrDefault(AppData())
        // A pending call whose alarm time has long passed was lost (for example to a reboot).
        loaded.pending?.let { if (it.at < System.currentTimeMillis() - STALE_MS) loaded = loaded.copy(pending = null) }
        _data.value = loaded
    }

    fun update(transform: (AppData) -> AppData) {
        val next = _data.updateAndGet(transform)
        io.execute {
            write(next)
            collectGarbage(next)
        }
    }

    fun photoFile(name: String) = File(photosDir, name)
    fun recordingFile(name: String) = File(recordingsDir, name)

    private fun write(data: AppData) {
        val tmp = File(file.path + ".tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), data))
        tmp.renameTo(file)
    }

    /** Deletes photos and recordings no caller refers to any more. */
    private fun collectGarbage(data: AppData) {
        val callers = data.presets + data.draft + listOfNotNull(data.pending?.caller)
        val cutoff = System.currentTimeMillis() - GC_GRACE_MS
        fun sweep(dir: File, keep: Set<String>) = dir.listFiles()?.forEach {
            if (it.name !in keep && it.lastModified() < cutoff) it.delete()
        }
        sweep(photosDir, callers.mapNotNull { it.photo }.toSet())
        sweep(recordingsDir, callers.mapNotNull { it.recording }.toSet())
    }

    private const val STALE_MS = 5 * 60_000L
    // New files are written a moment before a caller refers to them.
    private const val GC_GRACE_MS = 10 * 60_000L
}

package app.linglongdingdong.glyph

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * The ring pattern on a Phone (3a) / (3a) Pro: a comet spins around the Glyph strips like a loading
 * icon, speeding up from slow to very fast over [SPIN_MS], then every LED stays solid.
 *
 * Nothing only lets an app drive the Glyphs while it is in the foreground, which holds when the
 * call screen is showing.
 */
class GlyphSpinner(context: Context) {
    private val manager = GlyphManager.getInstance(context.applicationContext)
    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var sessionOpen = false
    private var solidForMs = Long.MAX_VALUE

    private val callback = object : GlyphManager.Callback {
        override fun onServiceConnected(name: ComponentName) {
            try {
                manager.register(Glyph.DEVICE_24111)
                manager.openSession()
                sessionOpen = true
                job = scope?.launch(Dispatchers.Default) { animate() }
            } catch (e: GlyphException) {
                Log.w(TAG, "Can't open a Glyph session", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            sessionOpen = false
        }
    }

    /** Starts spinning. [solidForMs] limits how long the lights stay solid (for previews). */
    fun start(scope: CoroutineScope, solidForMs: Long = Long.MAX_VALUE) {
        if (!supported || this.scope != null) return
        this.scope = scope
        this.solidForMs = solidForMs
        manager.init(callback)
    }

    fun stop() {
        if (scope == null) return
        job?.cancel()
        job = null
        scope = null
        runCatching {
            if (sessionOpen) {
                manager.turnOff()
                manager.closeSession()
            }
        }
        sessionOpen = false
        runCatching { manager.unInit() }
    }

    private suspend fun CoroutineScope.animate() {
        val frame = IntArray(LED_COUNT)
        val start = SystemClock.elapsedRealtime()
        var last = start
        var head = 0.0 // degrees clockwise around the camera ring
        while (isActive) {
            val now = SystemClock.elapsedRealtime()
            val t = (now - start).toDouble()
            if (t >= SPIN_MS) break
            val progress = t / SPIN_MS
            // Exponential speed-up feels like a steady acceleration to the eye.
            val lapsPerSecond = START_LAPS_PER_S * (END_LAPS_PER_S / START_LAPS_PER_S).pow(progress)
            head = (head + lapsPerSecond * 360 * (now - last) / 1000.0) % 360
            last = now
            // The tail grows with speed so the fast end blurs into a ring.
            val tail = 50 + progress.pow(2) * 300
            for (i in 0 until LED_COUNT) {
                val behind = ((head - ANGLES[i]) % 360 + 360) % 360
                frame[i] = if (behind < tail) (MAX * (1 - behind / tail).pow(1.6)).toInt() else 0
            }
            send(frame)
            delay(FRAME_MS)
        }
        frame.fill(MAX)
        val solidSince = SystemClock.elapsedRealtime()
        // Re-send now and then in case the service drops a static frame.
        while (isActive && SystemClock.elapsedRealtime() - solidSince < solidForMs) {
            send(frame)
            delay(1000)
        }
        runCatching { manager.turnOff() }
    }

    private fun send(frame: IntArray) {
        try {
            manager.setFrameColors(frame)
        } catch (e: GlyphException) {
            Log.w(TAG, "setFrameColors failed", e)
        }
    }

    companion object {
        private const val TAG = "GlyphSpinner"
        const val SPIN_MS = 20_000L
        private const val FRAME_MS = 16L
        private const val START_LAPS_PER_S = 0.4
        private const val END_LAPS_PER_S = 14.0
        private const val MAX = 4095
        private const val LED_COUNT = 36

        /**
         * Where each LED sits around the camera ring, in degrees clockwise from 12 o'clock, seen
         * from the back. SDK indices: C1–C20 = 0–19, A1–A11 = 20–30, B1–B5 = 31–35.
         * C runs 9 → 11 o'clock, A 3 → 4 o'clock, B 7 → 8 o'clock. Spinning by angle keeps the
         * speed even across the gaps between strips.
         */
        private val ANGLES = DoubleArray(LED_COUNT) { i ->
            fun along(from: Double, to: Double, index: Int, count: Int) = from + (to - from) * index / (count - 1)
            when {
                i < 20 -> along(281.0, 329.0, i, 20)
                i < 31 -> along(81.0, 117.0, i - 20, 11)
                else -> along(215.0, 236.0, i - 31, 5)
            }
        }

        val supported: Boolean get() = runCatching { Common.is24111() }.getOrDefault(false)
    }
}

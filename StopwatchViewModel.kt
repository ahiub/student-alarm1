package com.example.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Stopwatch state. Lives in the Activity's ViewModelStore, so it keeps running while the user
 * switches tabs or the screen is off. Time is measured with SystemClock.elapsedRealtime(),
 * so it stays exact even while the app is in the background (no ticking coroutine here;
 * the screen only reads the clock while it is visible).
 */
class StopwatchViewModel : ViewModel() {

    data class Lap(val number: Int, val lapMs: Long, val totalMs: Long)

    private var startedAtRealtime = 0L
    private var accumulatedMs = 0L

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /** Time shown while paused / reset (when running, the UI calls elapsedNow()). */
    private val _frozenMs = MutableStateFlow(0L)
    val frozenMs: StateFlow<Long> = _frozenMs.asStateFlow()

    private val _laps = MutableStateFlow<List<Lap>>(emptyList())
    /** Newest lap first. */
    val laps: StateFlow<List<Lap>> = _laps.asStateFlow()

    fun elapsedNow(): Long =
        if (_running.value) accumulatedMs + (SystemClock.elapsedRealtime() - startedAtRealtime)
        else accumulatedMs

    fun start() {
        if (_running.value) return
        startedAtRealtime = SystemClock.elapsedRealtime()
        _running.value = true
    }

    fun pause() {
        if (!_running.value) return
        accumulatedMs = elapsedNow()
        _running.value = false
        _frozenMs.value = accumulatedMs
    }

    fun toggle() = if (_running.value) pause() else start()

    fun lap() {
        if (!_running.value) return
        val total = elapsedNow()
        _laps.update { current ->
            val previousTotal = current.firstOrNull()?.totalMs ?: 0L
            listOf(Lap(current.size + 1, total - previousTotal, total)) + current
        }
    }

    fun reset() {
        _running.value = false
        accumulatedMs = 0L
        startedAtRealtime = 0L
        _frozenMs.value = 0L
        _laps.value = emptyList()
    }
}

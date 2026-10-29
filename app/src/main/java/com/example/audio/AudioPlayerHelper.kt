package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentPlayingPath: String? = null

    /**
     * Plays or pauses the specified audio file.
     * If the same file is currently paused, it resumes.
     * If a new file is specified, it prepares and starts playback.
     */
    fun togglePlay(filePath: String?, onComplete: (() -> Unit)? = null) {
        if (filePath.isNullOrBlank()) {
            _errorMessage.value = "Audio file path is empty"
            return
        }

        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            _errorMessage.value = "Voice note file not found or inaccessible"
            return
        }

        _errorMessage.value = null

        if (currentPlayingPath == filePath && mediaPlayer != null) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        // Initialize and play new file
        stop()
        currentPlayingPath = filePath

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = duration
                    stopProgressTracking()
                    onComplete?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerHelper", "MediaPlayer error: what=$what, extra=$extra")
                    _errorMessage.value = "Could not play audio note"
                    stop()
                    true
                }
            }

            mediaPlayer = player
            _durationMs.value = player.duration
            _currentPositionMs.value = 0
            player.start()
            _isPlaying.value = true
            startProgressTracking()
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Failed to start playback for $filePath", e)
            _errorMessage.value = "Failed to load audio file: ${e.localizedMessage ?: "Unknown error"}"
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _isPlaying.value = true
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let {
            it.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        }
    }

    fun stop() {
        stopProgressTracking()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentPositionMs.value = 0
            currentPlayingPath = null
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let {
                    try {
                        _currentPositionMs.value = it.currentPosition
                    } catch (_: Exception) {}
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
    }
}

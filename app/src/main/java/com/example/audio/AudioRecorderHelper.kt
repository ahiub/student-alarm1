package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
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

class AudioRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    /**
     * Starts recording a voice note to .m4a format.
     * Returns true if recording started successfully.
     */
    fun startRecording(): Boolean {
        stopRecording(discard = true)

        return try {
            val file = AudioFileUtils.createNewVoiceNoteFile(context)
            currentOutputFile = file

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            _isRecording.value = true
            _recordingDurationSec.value = 0

            // Start duration and amplitude polling
            timerJob = scope.launch {
                var seconds = 0
                while (isActive && _isRecording.value) {
                    delay(200)
                    recorder?.let {
                        try {
                            val maxAmp = it.maxAmplitude
                            _amplitude.value = (maxAmp / 32767f).coerceIn(0f, 1f)
                        } catch (_: Exception) {}
                    }
                    delay(800)
                    seconds++
                    _recordingDurationSec.value = seconds
                }
            }
            true
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to start audio recording", e)
            currentOutputFile?.delete()
            currentOutputFile = null
            _isRecording.value = false
            false
        }
    }

    /**
     * Stops the active recording.
     * If discard is true, deletes the recorded file.
     * Returns the absolute path of the saved file if successful and not discarded.
     */
    fun stopRecording(discard: Boolean = false): String? {
        timerJob?.cancel()
        timerJob = null

        val file = currentOutputFile
        try {
            if (_isRecording.value) {
                recorder?.apply {
                    try {
                        stop()
                    } catch (_: Exception) {}
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Error stopping recorder", e)
        } finally {
            recorder = null
            _isRecording.value = false
            _amplitude.value = 0f
        }

        return if (discard) {
            file?.delete()
            currentOutputFile = null
            null
        } else {
            currentOutputFile = null
            if (file != null && file.exists() && file.length() > 0) {
                file.absolutePath
            } else {
                null
            }
        }
    }

    fun release() {
        stopRecording(discard = true)
    }
}

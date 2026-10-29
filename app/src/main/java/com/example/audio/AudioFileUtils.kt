package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object AudioFileUtils {

    private const val AUDIO_DIR = "audio_notes"

    /**
     * Gets or creates the directory for storing voice notes inside the app's private storage
     */
    fun getAudioDirectory(context: Context): File {
        val dir = File(context.filesDir, AUDIO_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Creates a new destination file for recording a voice note (.m4a)
     */
    fun createNewVoiceNoteFile(context: Context): File {
        val dir = getAudioDirectory(context)
        val timestamp = System.currentTimeMillis()
        return File(dir, "voice_note_$timestamp.m4a")
    }

    /**
     * Copies an external audio Uri (e.g. from WhatsApp ACTION_SEND or System File Picker)
     * into the app's internal private storage so it remains accessible and never gets deleted
     * by the external app.
     */
    fun copyAudioUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val dir = getAudioDirectory(context)
            val fileName = "imported_audio_${System.currentTimeMillis()}.m4a"
            val destFile = File(dir, fileName)

            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                destFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AudioFileUtils", "Failed to copy audio from Uri: $sourceUri", e)
            null
        }
    }

    /**
     * Extracts duration in milliseconds for an audio file path.
     * Safely returns 0L if missing or corrupted.
     */
    fun getAudioDurationMs(filePath: String?): Long {
        if (filePath.isNullOrBlank()) return 0L
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) return 0L

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e("AudioFileUtils", "Failed to retrieve duration for $filePath", e)
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Formats milliseconds into human-readable mm:ss format
     */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    /**
     * Resolves the display name of a Uri using ContentResolver or fallback to path
     */
    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        cursor.getString(nameIndex)
                    } else null
                }
            } else {
                uri.lastPathSegment
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    /**
     * Safely deletes a file if it exists
     */
    fun deleteAudioFile(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return false
        return try {
            val file = File(filePath)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            Log.e("AudioFileUtils", "Failed to delete file: $filePath", e)
            false
        }
    }
}

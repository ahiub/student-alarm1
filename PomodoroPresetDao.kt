package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroPresetDao {

    @Query("SELECT * FROM pomodoro_presets ORDER BY id ASC")
    fun getAllPresetsFlow(): Flow<List<PomodoroPreset>>

    @Query("SELECT * FROM pomodoro_presets ORDER BY id ASC")
    suspend fun getAllPresetsList(): List<PomodoroPreset>

    @Query("SELECT * FROM pomodoro_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: Long): PomodoroPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PomodoroPreset): Long

    @Update
    suspend fun updatePreset(preset: PomodoroPreset)

    @Delete
    suspend fun deletePreset(preset: PomodoroPreset)

    @Query("DELETE FROM pomodoro_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)
}

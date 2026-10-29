package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomModeDao {

    @Query("SELECT * FROM custom_modes ORDER BY id ASC")
    fun getAllCustomModesFlow(): Flow<List<CustomMode>>

    @Query("SELECT * FROM custom_modes ORDER BY id ASC")
    suspend fun getAllCustomModesList(): List<CustomMode>

    @Query("SELECT * FROM custom_modes WHERE id = :id LIMIT 1")
    suspend fun getCustomModeById(id: Long): CustomMode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomMode(mode: CustomMode): Long

    @Update
    suspend fun updateCustomMode(mode: CustomMode)

    @Delete
    suspend fun deleteCustomMode(mode: CustomMode)

    @Query("DELETE FROM custom_modes WHERE id = :id")
    suspend fun deleteCustomModeById(id: Long)
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Query("SELECT * FROM timetable ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllEntriesFlow(): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getEntriesForDayFlow(dayOfWeek: Int): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getEntriesForDayList(dayOfWeek: Int): List<TimetableEntry>

    @Query("SELECT * FROM timetable WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): TimetableEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntry): Long

    @Update
    suspend fun updateEntry(entry: TimetableEntry)

    @Delete
    suspend fun deleteEntry(entry: TimetableEntry)

    @Query("DELETE FROM timetable WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
}

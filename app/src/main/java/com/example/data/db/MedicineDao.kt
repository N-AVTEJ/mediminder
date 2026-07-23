package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY time ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY time ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: Int, isActive: Boolean)

    @Query("UPDATE reminders SET time = :newTime, timeCategory = :timeCategory WHERE id = :id")
    suspend fun updateTime(id: Int, newTime: String, timeCategory: String)
}

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs WHERE dateStr = :dateStr ORDER BY scheduledTime ASC")
    fun getLogsForDate(dateStr: String): Flow<List<DoseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DoseLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<DoseLogEntity>)

    @Query("UPDATE dose_logs SET status = :status, takenTime = :takenTime WHERE id = :logId")
    suspend fun updateLogStatus(logId: Int, status: String, takenTime: String?)

    @Query("DELETE FROM dose_logs WHERE reminderId = :reminderId")
    suspend fun deleteLogsForReminder(reminderId: Int)
}

@Dao
interface GuardianDao {
    @Query("SELECT * FROM guardians ORDER BY id ASC")
    fun getAllGuardians(): Flow<List<GuardianEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuardian(guardian: GuardianEntity)

    @Update
    suspend fun updateGuardian(guardian: GuardianEntity)

    @Delete
    suspend fun deleteGuardian(guardian: GuardianEntity)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: ProfileEntity)
}

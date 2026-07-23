package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val time: String, // e.g. "08:00 AM"
    val timeCategory: String, // "Morning", "Afternoon", "Evening", "Night"
    val instructions: String, // e.g. "Take with water after breakfast"
    val isActive: Boolean = true,
    val colorHex: String = "#00796B",
    val shape: String = "Pill" // "Pill", "Tablet", "Capsule", "Liquid", "Injection"
)

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reminderId: Int,
    val medicineName: String,
    val dosage: String,
    val dateStr: String, // "YYYY-MM-DD"
    val scheduledTime: String,
    val status: String, // "PENDING", "TAKEN", "MISSED", "SNOOZED"
    val takenTime: String? = null
)

@Entity(tableName = "guardians")
data class GuardianEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val relationship: String,
    val phone: String,
    val notifyOnMissed: Boolean = true
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val phone: String,
    val emergencyInfo: String,
    val isPremium: Boolean = true,
    val largeTextMode: Boolean = true
)

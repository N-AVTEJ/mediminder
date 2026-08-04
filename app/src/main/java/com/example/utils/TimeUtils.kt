package com.example.utils

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val time12Formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    fun toUtcIsoString(zonedDateTime: ZonedDateTime): String {
        return zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")).format(isoFormatter)
    }

    fun local12HourToTodayUtcIso(timeStr: String): String {
        return try {
            val localTime = java.time.LocalTime.parse(timeStr.trim().uppercase(Locale.US), java.time.format.DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
            val zonedDateTime = ZonedDateTime.of(java.time.LocalDate.now(), localTime, ZoneId.systemDefault())
            toUtcIsoString(zonedDateTime)
        } catch (e: Exception) {
            timeStr
        }
    }

    fun fromUtcIsoStringToLocal12Hour(utcIsoString: String): String {
        return try {
            val zonedDateTime = ZonedDateTime.parse(utcIsoString, isoFormatter)
            zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).format(time12Formatter)
        } catch (e: Exception) {
            utcIsoString // Fallback
        }
    }
}

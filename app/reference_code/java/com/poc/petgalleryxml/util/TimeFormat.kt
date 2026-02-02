package com.poc.petgalleryxml.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object TimeFormat {
    fun isoNowUtc(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

    fun relativeTimeKorean(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val dt = Instant.parse(iso).atZone(ZoneId.systemDefault())
            val now = ZonedDateTime.now()
            val minutes = abs(java.time.Duration.between(dt, now).toMinutes())
            when {
                minutes < 1 -> "방금"
                minutes < 60 -> "${minutes}분 전"
                minutes < 1440 -> "${minutes / 60}시간 전"
                else -> dt.format(DateTimeFormatter.ofPattern("MM/dd"))
            }
        } catch (e: Exception) {
            ""
        }
    }
}

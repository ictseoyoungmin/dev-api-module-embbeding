package com.poc.search.util

import java.time.Instant
import java.time.format.DateTimeFormatter

object TimeUtils {
    fun nowIsoUtc(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    fun nowEpochMs(): Long = System.currentTimeMillis()
}

package com.dungh.concert.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {

    private val isoPatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
        "yyyy-MM-dd'T'HH:mm:ss",
    )

    fun parseIsoToMillis(iso: String): Long {
        for (pattern in isoPatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(iso)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis() + 600_000
    }
}

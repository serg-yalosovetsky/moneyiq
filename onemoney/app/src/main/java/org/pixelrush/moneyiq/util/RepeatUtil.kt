package org.syalosovetskyi.onemoney.util

import java.util.Calendar

fun calculateNextRepeatDate(fromDate: Long, repeatMode: String): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = fromDate }
    when (repeatMode) {
        "DAILY"          -> cal.add(Calendar.DAY_OF_YEAR, 1)
        "EVERY_2_DAYS"   -> cal.add(Calendar.DAY_OF_YEAR, 2)
        "WEEKDAYS"       -> {
            do { cal.add(Calendar.DAY_OF_YEAR, 1) }
            while (cal.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY))
        }
        "WEEKENDS"       -> {
            do { cal.add(Calendar.DAY_OF_YEAR, 1) }
            while (cal.get(Calendar.DAY_OF_WEEK) !in listOf(Calendar.SATURDAY, Calendar.SUNDAY))
        }
        "WEEKLY"         -> cal.add(Calendar.WEEK_OF_YEAR, 1)
        "EVERY_2_WEEKS"  -> cal.add(Calendar.WEEK_OF_YEAR, 2)
        "EVERY_4_WEEKS"  -> cal.add(Calendar.WEEK_OF_YEAR, 4)
        "MONTHLY"        -> cal.add(Calendar.MONTH, 1)
        "EVERY_2_MONTHS" -> cal.add(Calendar.MONTH, 2)
        "EVERY_3_MONTHS" -> cal.add(Calendar.MONTH, 3)
        "EVERY_6_MONTHS" -> cal.add(Calendar.MONTH, 6)
        "YEARLY"         -> cal.add(Calendar.YEAR, 1)
        else             -> return Long.MAX_VALUE
    }
    return cal.timeInMillis
}

fun startOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun reminderOffsetDays(mode: String): Int = when (mode) {
    "SAME_DAY" -> 0
    "1_DAY"    -> 1
    "2_DAYS"   -> 2
    "3_DAYS"   -> 3
    "4_DAYS"   -> 4
    "5_DAYS"   -> 5
    "6_DAYS"   -> 6
    "7_DAYS"   -> 7
    else       -> -1
}

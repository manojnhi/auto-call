package com.gramcall.ai.biz

import java.util.Calendar

enum class DatePreset(val label: String) {
    TODAY("आज"), YESTERDAY("कल"), LAST7("पिछले 7 दिन"), LAST30("पिछले 30 दिन"),
    THIS_MONTH("इस महीने"), CUSTOM("अपनी तारीखें")
}

object DateRanges {
    private fun startOfDay(ts: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ts
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun endOfDay(ts: Long): Long = Calendar.getInstance().apply {
        timeInMillis = startOfDay(ts); add(Calendar.DAY_OF_MONTH, 1); add(Calendar.MILLISECOND, -1)
    }.timeInMillis

    private fun daysAgo(ts: Long, n: Int): Long = Calendar.getInstance().apply {
        timeInMillis = startOfDay(ts); add(Calendar.DAY_OF_MONTH, -n)
    }.timeInMillis

    /** (from, to) milliseconds, dono inclusive. */
    fun resolve(preset: DatePreset, now: Long, customFrom: Long? = null, customTo: Long? = null): Pair<Long, Long> =
        when (preset) {
            DatePreset.TODAY -> startOfDay(now) to endOfDay(now)
            DatePreset.YESTERDAY -> daysAgo(now, 1) to endOfDay(daysAgo(now, 1))
            DatePreset.LAST7 -> daysAgo(now, 6) to endOfDay(now)
            DatePreset.LAST30 -> daysAgo(now, 29) to endOfDay(now)
            DatePreset.THIS_MONTH -> Calendar.getInstance().apply {
                timeInMillis = startOfDay(now); set(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis to endOfDay(now)
            DatePreset.CUSTOM -> {
                val a = customFrom ?: startOfDay(now)
                val b = customTo ?: a
                val lo = minOf(a, b)
                val hi = maxOf(a, b)
                startOfDay(lo) to endOfDay(hi)
            }
        }
}

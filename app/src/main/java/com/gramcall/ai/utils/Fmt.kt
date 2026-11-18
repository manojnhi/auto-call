package com.gramcall.ai.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Fmt {
    fun dateTime(ts: Long?): String =
        if (ts == null) "—" else SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("hi", "IN")).format(Date(ts))

    fun duration(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)
}

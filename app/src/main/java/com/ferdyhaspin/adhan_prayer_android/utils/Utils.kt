package com.ferdyhaspin.adhan_prayer_android.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by ferdyhaspin on 15/03/20.
 * Copyright (c) 2020 All rights reserved.
 */
object Utils {

    fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }

    fun currentTimeToLong(): Long {
        return System.currentTimeMillis()
    }

    fun convertDateToLong(date: String): Long {
        val df = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
        return df.parse(date).time
    }

}
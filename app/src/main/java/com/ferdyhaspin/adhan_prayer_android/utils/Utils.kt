package com.ferdyhaspin.adhan_prayer_android.utils

import android.content.Context
import android.text.format.DateFormat
import java.text.ParseException
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

    private fun calculationTime(context: Context, prayerTime: String, adjustment: Int): String {
        return try {
            var localTime = prayerTime
            val df = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            if (!DateFormat.is24HourFormat(context)) {
                val date12Format = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                val format = date12Format.parse(localTime)
                if (format != null)
                    localTime = df.format(format)
            }
            val d = df.parse(localTime)
            val cal = Calendar.getInstance().apply {
                if (d != null)
                    this.time = d
                add(Calendar.MINUTE, adjustment)
            }
            df.format(cal.time)
        } catch (e: ParseException) {
            e.printStackTrace()
            ""
        }
    }
}
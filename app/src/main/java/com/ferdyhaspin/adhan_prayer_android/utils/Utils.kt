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

//    fun currentTimeToLong(): Long {
//        return System.currentTimeMillis()
//    }
//
//    fun convertDateToLong(date: String): Long {
//        val df = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
//        return df.parse(date).time
//    }

    private fun calculationTime(context: Context, time: String, minute: Int): String {
        return try {
            var localTime = time
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
                add(Calendar.MINUTE, minute)
            }
            df.format(cal.time)
        } catch (e: ParseException) {
            e.printStackTrace()
            ""
        }
    }

}